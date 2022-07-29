package com.tencent.iot.explorer.device.video;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.app.utils.LogcatHelper;
import com.tencent.iot.explorer.device.video.call.MainActivity;
import com.tencent.iot.explorer.device.video.device_and_device.DeviceAndDeviceCommunicationActivity;
import com.tencent.iot.explorer.device.video.push_stream.PushRealTimeStreamActivity;
import com.tencent.iot.explorer.device.video.push_stream.PushStreamActivity;


public class VideoEntryActivity extends AppCompatActivity {

    private Button mVideoCallBtn;
    private Button mVideoPushStreamBtn;
    private Button mVideoPushRealTimeStreamBtn;
    private Button mVideoDeviceAndDeviceCallBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_entry);

        mVideoCallBtn = findViewById(R.id.iot_video_call);
        mVideoPushStreamBtn = findViewById(R.id.iot_video_push_stream);
        mVideoPushRealTimeStreamBtn = findViewById(R.id.iot_video_push_realtime_stream);
        mVideoDeviceAndDeviceCallBtn = findViewById(R.id.iot_video_device_and_device_communication);

        //双向音视频
        mVideoCallBtn.setOnClickListener(v -> {
            Intent intent = new Intent(VideoEntryActivity.this, MainActivity.class);
            startActivity(intent);
        });
        //设备端推流
        mVideoPushStreamBtn.setOnClickListener(v -> {
            Intent intent = new Intent(VideoEntryActivity.this, PushStreamActivity.class);
            startActivity(intent);
        });
        //设备端推流
        mVideoPushRealTimeStreamBtn.setOnClickListener(v -> {
            Intent intent = new Intent(VideoEntryActivity.this, PushRealTimeStreamActivity.class);
            startActivity(intent);
        });
        mVideoDeviceAndDeviceCallBtn.setOnClickListener(v -> {
            Intent intent = new Intent(VideoEntryActivity.this, DeviceAndDeviceCommunicationActivity.class);
            startActivity(intent);
        });
        LogcatHelper.getInstance(this).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogcatHelper.getInstance(this).stop();
    }
}