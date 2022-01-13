package com.tencent.iot.explorer.device.video;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.video.call.MainActivity;

import androidx.appcompat.app.AppCompatActivity;


public class VideoEntryActivity extends AppCompatActivity {

    private Button mVideoCallBtn;
    private Button mVideoPushStreamBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_entry);

        mVideoCallBtn = findViewById(R.id.iot_video_call);
        mVideoPushStreamBtn = findViewById(R.id.iot_video_push_stream);

        //双向音视频
        mVideoCallBtn.setOnClickListener(v -> {
            Intent intent = new Intent(VideoEntryActivity.this, MainActivity.class);
            startActivity(intent);
        });
        //设备端推流
        mVideoPushStreamBtn.setOnClickListener(v -> {
            Intent intent = new Intent(VideoEntryActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}