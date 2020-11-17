package com.tencent.iot.explorer.device;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.tencent.iot.explorer.device.android.app.IoTMainActivity;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.face.AuthActivity;

public class GuideActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mBasicDemoBtn;
    private Button mFaceDemoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        initView();
    }

    private void initView() {
        mBasicDemoBtn = findViewById(R.id.btn_explorer_basic);
        mFaceDemoBtn = findViewById(R.id.btn_explorer_face);
        mBasicDemoBtn.setOnClickListener(this);
        mFaceDemoBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.btn_explorer_basic:
                intent = new Intent(GuideActivity.this, IoTMainActivity.class);
                break;
            case R.id.btn_explorer_face:
                intent = new Intent(GuideActivity.this, AuthActivity.class);
                break;
            default:
                break;
        }
        startActivity(intent);
    }
}