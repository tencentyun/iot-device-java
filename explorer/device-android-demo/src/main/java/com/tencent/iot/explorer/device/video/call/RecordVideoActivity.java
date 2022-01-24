package com.tencent.iot.explorer.device.video.call;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.video.call.entity.PhoneInfo;
import com.tencent.iot.explorer.device.video.push_stream.PushStreamActivity;
import com.tencent.iot.explorer.device.video.recorder.OnRecordListener;
import com.tencent.iot.explorer.device.video.recorder.ReadByteIO;
import com.tencent.iot.explorer.device.video.recorder.VideoRecorder;
import com.tencent.iot.explorer.device.video.recorder.opengles.view.CameraView;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;
import com.tencent.iot.thirdparty.android.device.video.p2p.XP2PCallback;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RecordVideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private String TAG = RecordVideoActivity.class.getSimpleName();
    private CameraView cameraView;
    private Button btnSwitch;
    private final VideoRecorder videoRecorder = new VideoRecorder();
    private String path; // 保存源文件的路径
    private IjkMediaPlayer player;
    private Surface surface;
    private TextureView playView;
    private volatile PhoneInfo phoneInfo;
    private Handler handler = new Handler();
    private Button recordBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getFilesDir().getAbsolutePath();
        Intent inetnt = getIntent();
        if (inetnt != null) {
            Bundle bundle = inetnt.getBundleExtra(PhoneInfo.TAG);
            if (bundle != null) {
                String jsonStr = bundle.getString(PhoneInfo.TAG);
                phoneInfo = JSON.parseObject(jsonStr, PhoneInfo.class);
            }
        }
        setContentView(R.layout.activity_record_video);
        recordBtn = findViewById(R.id.btnRecord);
        recordBtn.setText("Record \n path:"  + path);
        cameraView = findViewById(R.id.cameraView);
        btnSwitch = findViewById(R.id.btnSwitch);
        playView = findViewById(R.id.v_play);
        videoRecorder.attachCameraView(cameraView);
        playView.setSurfaceTextureListener(this);
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            cameraView.setVisibility(View.INVISIBLE);
            btnSwitch.setVisibility(View.GONE);
//            playView.setVisibility(View.INVISIBLE);
        }

        btnSwitch.setOnClickListener(v -> cameraView.switchCamera());
        VideoNativeInteface.getInstance().setCallback(xP2PCallback);
        registVideoOverBrodcast();
        ReadByteIO.Companion.getInstance().reset();
        ReadByteIO.Companion.getInstance().setPlayType(phoneInfo.getCallType());
        recordBtn.setOnClickListener(v-> {
            if (videoRecorder == null) return;

            if (videoRecorder.isRecord()) {
                videoRecorder.stopRecord();
                Toast.makeText(this, "停止录像", Toast.LENGTH_SHORT).show();
            } else {
                int ret = videoRecorder.startRecord(path, PushStreamActivity.csAACFileName, PushStreamActivity.csVideoFileName);
                if (ret == 0) {
                    Toast.makeText(this, "开始录像", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "开启录像失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startRecord() {
        if (phoneInfo != null) {
            videoRecorder.start(phoneInfo.getCallType(), onRecordListener);
            return;
        }
        videoRecorder.start(onRecordListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private XP2PCallback xP2PCallback = new XP2PCallback() {

        @Override
        public void avDataRecvHandle(byte[] data, int len) {
            ReadByteIO.Companion.getInstance().addLast(data);
        }

        @Override
        public void avDataMsgHandle(int type, String msg) {
            Log.e(TAG, "avDataMsgHandle type " + type);
            if (type == 0) {
                Log.e(TAG, "start send video data");
                handler.post(() -> startRecord());

            } else if (type == 1) {
                Log.e(TAG, "this call over");
                if (!RecordVideoActivity.this.isDestroyed() && !RecordVideoActivity.this.isFinishing()) {
                    new Thread(() -> new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)).start();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregistVideoOverBrodcast();
        cameraView.closeCamera();
        videoRecorder.cancel();
        videoRecorder.stop();
        if (player != null) {
            player.stop();
        }
        ReadByteIO.Companion.getInstance().close();
    }

    private void showSaveState(boolean save) {
        runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "Save:" + save + " path:" + path, Toast.LENGTH_SHORT).show());
    }

    private OnRecordListener onRecordListener = new OnRecordListener() {
        @Override public void onRecordStart() { }
        @Override public void onRecordTime(long time) { }
        @Override public void onRecordComplete(String path) {
            showSaveState(true);
        }
        @Override public void onRecordCancel() { }
        @Override public void onRecordError(Exception e) { }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface != null) {
            this.surface = new Surface(surface);
            play();
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {return false; }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) { }

    private void play() {
        player = new IjkMediaPlayer();
        player.reset();
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 64);
        } else {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000);
//            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 100 * 1024);
        }
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec",1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,crypto,file,http,https,tcp,tls,udp");

        player.setSurface(this.surface);
        player.setAndroidIOCallback(ReadByteIO.Companion.getInstance());

        Uri uri = Uri.parse("ijkio:androidio:" + ReadByteIO.Companion.getURL_SUFFIX());
        try {
            player.setDataSource(uri.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
        player.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            if (phoneInfo != null) {
                bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
            }
            intent.putExtra(PhoneInfo.TAG, bundle);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    BroadcastReceiver recevier = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int refreshTag = intent.getIntExtra(Utils.VIDEO_OVER, 0);
            if (refreshTag == 9){
                if (!RecordVideoActivity.this.isDestroyed() && !RecordVideoActivity.this.isFinishing()) {
                    new Thread(() -> new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)).start();
                }
            }
        }
    };

    private void registVideoOverBrodcast() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(RecordVideoActivity.this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CART_BROADCAST");
        broadcastManager.registerReceiver(recevier, intentFilter);
    }

    private void unregistVideoOverBrodcast() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(RecordVideoActivity.this);
        broadcastManager.unregisterReceiver(recevier);
    }
}
