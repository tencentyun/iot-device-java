package com.tencent.iot.explorer.device.android.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.tencent.iot.explorer.device.android.app.trtc.TRTCMainActivity;

public class IoTDemoActivity extends AppCompatActivity {

    private Button mIoTDemoBtn;
    private Button mIoTAiFaceBtn;
    private Button mIoTTrtcBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iot_demo);

        mIoTDemoBtn = findViewById(R.id.iot_demo);
        mIoTAiFaceBtn = findViewById(R.id.iot_ai_face);
        mIoTTrtcBtn = findViewById(R.id.iot_trtc);

        mIoTDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, IoTMainActivity.class));
            }
        });

        mIoTAiFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(IoTDemoActivity.this, AIFaceMainActivity.class));
            }
        });

        mIoTTrtcBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, TRTCMainActivity.class));
            }
        });
    }
}