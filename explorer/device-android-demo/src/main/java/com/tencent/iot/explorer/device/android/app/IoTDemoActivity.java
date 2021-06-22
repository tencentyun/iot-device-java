package com.tencent.iot.explorer.device.android.app;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.iot.explorer.device.face.AuthActivity;
import com.tencent.iot.explorer.device.rtc.TRTCMainActivity;

public class IoTDemoActivity extends AppCompatActivity {

    private Button mIoTDemoBtn;
    private Button mIoTAiFaceBtn;
    private Button mIoTTrtcBtn;
    private Button mIoTTmeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iot_demo);

        mIoTDemoBtn = findViewById(R.id.iot_demo);
        mIoTAiFaceBtn = findViewById(R.id.iot_ai_face);
        mIoTTrtcBtn = findViewById(R.id.iot_trtc);
        mIoTTmeBtn = findViewById(R.id.iot_tme);

        mIoTDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, IoTMainActivity.class));
            }
        });

        mIoTAiFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, AuthActivity.class));
            }
        });

        mIoTTrtcBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, TRTCMainActivity.class));
            }
        });

        mIoTTmeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Class c = Class.forName("com.tencent.iot.explorer.device.tme.TmeMainActivity");
                    startActivity(new Intent(IoTDemoActivity.this, c));
                } catch (ClassNotFoundException e) {
                    Toast.makeText(IoTDemoActivity.this, "请线下联系", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }
}