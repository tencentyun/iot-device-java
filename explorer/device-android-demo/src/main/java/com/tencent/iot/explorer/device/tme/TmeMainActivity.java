package com.tencent.iot.explorer.device.tme;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.tencent.iot.explorer.device.android.app.R;

public class TmeMainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mOnlineBtn;
    private Button mOfflineBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tme_main);
        initView();
    }

    private void initView() {
        mOnlineBtn = findViewById(R.id.online);
        mOfflineBtn = findViewById(R.id.offline);
        mOnlineBtn.setOnClickListener(this);
        mOfflineBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.online:
                break;
            case R.id.offline:
                break;
            default:
                break;
        }
    }
}