package com.tencent.iot.explorer.device.video.push_stream;

import android.app.Instrumentation;
import android.content.Intent;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.video.call.entity.PhoneInfo;
import com.tencent.iot.explorer.device.video.recorder.OnRecordListener;
import com.tencent.iot.explorer.device.video.recorder.ReadByteIO;
import com.tencent.iot.explorer.device.video.recorder.VideoRecorder;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.opengles.view.CameraView;
import com.tencent.iot.explorer.device.video.recorder.utils.ByteUtils;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;
import com.tencent.iot.thirdparty.android.device.video.p2p.XP2PCallback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RecordVideoActivity2 extends AppCompatActivity implements TextureView.SurfaceTextureListener, OnEncodeListener {

    private static final String TAG = RecordVideoActivity2.class.getSimpleName();
    private static final int MAX_CONNECT_NUM = 4;
    private CameraView cameraView;
    private Button btnSwitch;
    private Button btnStop;
    private VideoRecorder videoRecorder = new VideoRecorder(this);
    private IjkMediaPlayer player;
    private Surface surface;
    private TextureView playView;
    private volatile PhoneInfo phoneInfo;
    private Handler handler = new Handler();
    private ReadByteIO io;
    private final HashMap<Integer, Boolean> visitors = new HashMap<>(4);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getBundleExtra(PhoneInfo.TAG);
            if (bundle != null) {
                String jsonStr = bundle.getString(PhoneInfo.TAG);
                phoneInfo = JSON.parseObject(jsonStr, PhoneInfo.class);
            }
        }
        setContentView(R.layout.activity_record_video2);
        cameraView = findViewById(R.id.cameraView);
        btnSwitch = findViewById(R.id.btnSwitch);
        btnStop = findViewById(R.id.btnStop);
        playView = findViewById(R.id.v_play);
        videoRecorder.attachCameraView(cameraView);
        playView.setSurfaceTextureListener(this);
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            cameraView.setVisibility(View.INVISIBLE);
            btnSwitch.setVisibility(View.GONE);
        }

        btnSwitch.setOnClickListener(v -> cameraView.switchCamera());
        btnStop.setOnClickListener(v -> {
            new Thread(() -> new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)).start();
        });
        VideoNativeInteface.getInstance().setCallback(xP2PCallback);
        for (int i = 0; i < MAX_CONNECT_NUM; i++) {
            visitors.put(i, false);
        }
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
            Log.e(TAG, "====datalen: " + len + "bytes: " + ByteUtils.Companion.bytesToHex(data));
            io.addLast(data);
        }

        @Override
        public void avDataMsgHandle(int type, String msg) {
            Log.e(TAG, "====avDataMsgHandle type: " + type + " msg: " + msg);
            JSONObject obj = JSON.parseObject(msg);
            int visitorId = -1;
            if (obj != null) {
                visitorId = obj.getIntValue("visitor");
            }
            if (type == 0) { //开始预览
                visitors.put(visitorId, true);
                Log.e(TAG, "visitor " + visitorId + " 开始预览.");
                handler.post(() -> startRecord());
            } else if (type == 1) { //结束预览
                visitors.put(visitorId, false);
                Log.e(TAG, "visitor " + visitorId + " 结束预览.");
            } else if (type == 3) { //结束对讲
                Log.e(TAG, "visitor " + visitorId + " 结束对讲.");
                play();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoNativeInteface.getInstance().setCallback(null);
        cameraView.closeCamera();
        videoRecorder.cancel();
        videoRecorder.stop();
        if (player != null) {
            player.stop();
        }
        io.close();
    }

    private final OnRecordListener onRecordListener = new OnRecordListener() {
        @Override public void onRecordStart() { }
        @Override public void onRecordTime(long time) { }
        @Override public void onRecordComplete(String path) { }
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

        io = new ReadByteIO();
        io.reset();
        io.setPlayType(phoneInfo.getCallType());
        player.setAndroidIOCallback(io);

        Uri uri = Uri.parse("ijkio:androidio:" + io.getURL_SUFFIX());
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

    @Override
    public void onAudioEncoded(byte[] datas, long pts, long seq) {
        for (int i = 0; i < MAX_CONNECT_NUM; i++) {
            if (visitors.get(i)) {
                VideoNativeInteface.getInstance().sendAudioData(datas, System.currentTimeMillis(), seq, i);
            }
        }
    }

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq) {
        for (int i = 0; i < MAX_CONNECT_NUM; i++) {
            if (visitors.get(i)) {
                VideoNativeInteface.getInstance().sendVideoData(datas, System.currentTimeMillis(), seq, i);
            }
        }
    }
}

