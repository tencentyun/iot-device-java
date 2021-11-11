package com.tencent.iot.explorer.device.video;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.tencent.iot.explorer.device.video.recorder.OnRecordListener;
import com.tencent.iot.explorer.device.video.recorder.VideoRecorder;
import com.tencent.iot.explorer.device.video.recorder.opengles.view.CameraView;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;

public class RecordVideoActivity extends AppCompatActivity {

    private String TAG = RecordVideoActivity.class.getSimpleName();
    private CameraView cameraView;
    private AppCompatButton btnSwitch;
    private AppCompatButton btnRecord;
    private final VideoRecorder videoRecorder = new VideoRecorder();
    private String path = ""; // 保存 MP4 文件的路径
    private volatile boolean isRecord = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);
        cameraView = findViewById(R.id.cameraView);
        btnSwitch = findViewById(R.id.btnSwitch);
        btnRecord = findViewById(R.id.btnRecord);
        videoRecorder.attachCameraView(cameraView);

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

        String path = getFilesDir().getAbsolutePath();
        String filePath = path + "/" + MainActivity.devFileName;
        VideoNativeInteface.getInstance().init(filePath);
        int ret = VideoNativeInteface.getInstance().initVideoFormat(0, 1, 320, 480, 25);
        Log.d(TAG, "initVideoFormat ret " + ret);
    }

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
        cameraView.closeCamera();
        videoRecorder.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoNativeInteface.getInstance().release();
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
}
