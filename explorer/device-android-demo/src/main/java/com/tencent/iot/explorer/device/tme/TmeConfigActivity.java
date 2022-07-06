package com.tencent.iot.explorer.device.tme;

import androidx.appcompat.app.AppCompatActivity;

import com.kugou.ultimatetv.util.ToastUtil;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.tme.consts.TmeConst;
import com.tencent.iot.explorer.device.tme.utils.SharePreferenceUtil;
import com.tencent.iot.explorer.device.android.app.BuildConfig;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TmeConfigActivity extends AppCompatActivity implements View.OnClickListener {

    private int counts = 5; //点击次数
    private long duration = 3 * 1000; //规定有效时间
    private long[] hits = new long[counts];

    private Button mConfigBtn;
    private EditText mProductIdEt;
    private EditText mDeviceNameEt;
    private EditText mDevicePSKEt;
    private EditText mBrokerUrlEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tme_config);
        initView();
        initConfig();
    }

    private void initView() {
        mConfigBtn = findViewById(R.id.btn_config);
        mProductIdEt = findViewById(R.id.et_productid);
        mDeviceNameEt = findViewById(R.id.et_device_name);
        mDevicePSKEt = findViewById(R.id.et_device_psk);
        mBrokerUrlEt = findViewById(R.id.et_broker_url);
        mConfigBtn.setOnClickListener(this);
    }

    private void initConfig() {
        String productId = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_PRODUCT_ID);
        String deviceName = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_DEVICE_NAME);
        String devicePSK = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_DEVICE_PSK);
        String brokerUrl = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_BROKER_URL);

        if (TextUtils.isEmpty(productId.trim())) {
            productId = BuildConfig.TME_PRODUCT_ID.trim();
        }
        if (TextUtils.isEmpty(deviceName.trim())) {
            deviceName = BuildConfig.TME_DEVICE_NAME.trim();
        }
        if (TextUtils.isEmpty(devicePSK.trim())) {
            devicePSK = BuildConfig.TME_DEVICE_PSK.trim();
        }
        if (TextUtils.isEmpty(brokerUrl.trim())) {
            brokerUrl = BuildConfig.TME_BROKER_URL.trim();
        }
        if (!TextUtils.isEmpty(productId)) {
            mProductIdEt.setText(productId);
        }
        if (!TextUtils.isEmpty(deviceName)) {
            mDeviceNameEt.setText(deviceName);
        }
        if (!TextUtils.isEmpty(devicePSK)) {
            mDevicePSKEt.setText(devicePSK);
        }
        if (!TextUtils.isEmpty(brokerUrl)) {
            mBrokerUrlEt.setText(brokerUrl);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_config) {
            if (checkInput()) {
                Intent intent = new Intent(TmeConfigActivity.this, TmeMainActivity.class);
                startActivity(intent);
            }
        }
    }

    private boolean checkInput() {
        String productId = mProductIdEt.getText().toString();
        if (TextUtils.isEmpty(productId)) {
            Toast.makeText(this, "请输入Product Id", Toast.LENGTH_SHORT).show();
            return false;
        }
        String deviceName = mDeviceNameEt.getText().toString();
        if (TextUtils.isEmpty(deviceName)) {
            Toast.makeText(this, "请输入Device Name", Toast.LENGTH_SHORT).show();
            return false;
        }
        String devicePsk = mDevicePSKEt.getText().toString();
        if (TextUtils.isEmpty(devicePsk)) {
            Toast.makeText(this, "请输入Device PSK", Toast.LENGTH_SHORT).show();
            return false;
        }

        String brokerUrl = mBrokerUrlEt.getText().toString();

        SharePreferenceUtil.saveString(this, TmeConst.TME_CONFIG, TmeConst.TME_PRODUCT_ID, productId);
        SharePreferenceUtil.saveString(this, TmeConst.TME_CONFIG, TmeConst.TME_DEVICE_NAME, deviceName);
        SharePreferenceUtil.saveString(this, TmeConst.TME_CONFIG, TmeConst.TME_DEVICE_PSK, devicePsk);
        SharePreferenceUtil.saveString(this, TmeConst.TME_CONFIG, TmeConst.TME_BROKER_URL, brokerUrl);

        return true;
    }
}