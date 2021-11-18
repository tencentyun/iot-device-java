package com.tencent.iot.explorer.device.video;

import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.iot.explorer.device.android.app.R;
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
    private Button btnRecord;
    private final VideoRecorder videoRecorder = new VideoRecorder();
    private String path = ""; // 保存 MP4 文件的路径
    private volatile boolean isRecord = false;
    private IjkMediaPlayer player;
    private Surface surface;
    private TextureView playView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);
        cameraView = findViewById(R.id.cameraView);
        btnSwitch = findViewById(R.id.btnSwitch);
        btnRecord = findViewById(R.id.btnRecord);
        playView = findViewById(R.id.v_play);
        videoRecorder.attachCameraView(cameraView);
        playView.setSurfaceTextureListener(this);

        btnRecord.setOnClickListener(v -> {
            if (isRecord) {
                stopRecord();
                btnRecord.setText("Record");
            } else {
                startRecord();
                btnRecord.setText("Stop");
            }
            isRecord = !isRecord;
        });
        btnSwitch.setOnClickListener(v -> cameraView.switchCamera());
        VideoNativeInteface.getInstance().setCallback(xP2PCallback);
    }

    private XP2PCallback xP2PCallback = (data, len) -> {
        ReadByteIO.Companion.getInstance().addLast(data);
    };

    private void stopRecord() {
        videoRecorder.stop();
    }

    private void startRecord() {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.closeCamera();
        videoRecorder.cancel();
        videoRecorder.stop();
        if (player != null) {
            player.stop();
        }
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
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 25 * 1024);
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
}
