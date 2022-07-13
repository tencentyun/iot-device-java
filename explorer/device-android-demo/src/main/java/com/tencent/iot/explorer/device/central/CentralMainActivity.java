package com.tencent.iot.explorer.device.central;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.iot.explorer.device.android.app.BuildConfig;
import com.tencent.iot.explorer.device.android.app.R;


public class CentralMainActivity extends AppCompatActivity {

    private static final String CENTRAL_CONFIG = "central_config";

    private ImageView mQRCodeImgView;
    private Button mConnectBtn;
    private Button mDisconnectBtn;

    private EditText mBrokerURLEditText;
    private EditText mProductIdEditText;
    private EditText mDevNameEditText;
    private EditText mDevPSKEditText;
    private TextView mLogInfoText;

    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = BuildConfig.CENTRAL_PRODUCT_ID;
    private String mDevName = BuildConfig.CENTRAL_DEVICE_NAME;
    private String mDevPSK  = BuildConfig.CENTRAL_DEVICE_PSK;



    private final static String mJsonFileName = "TRTC_watch.json";


    private final static String BROKER_URL = "broker_url";
    private final static String PRODUCT_ID = "product_id";
    private final static String DEVICE_NAME = "dev_name";
    private final static String DEVICE_PSK = "dev_psk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central_main);

        mQRCodeImgView = findViewById(R.id.iv_qrcode);
        mConnectBtn = findViewById(R.id.btn_connect);
        mDisconnectBtn = findViewById(R.id.btn_disconnect);
        mLogInfoText = findViewById(R.id.log_info);

        mBrokerURLEditText = findViewById(R.id.et_broker_url);
        mProductIdEditText = findViewById(R.id.et_productId);
        mDevNameEditText = findViewById(R.id.et_deviceName);
        mDevPSKEditText = findViewById(R.id.et_devicePsk);

        SharedPreferences settings = getSharedPreferences(CENTRAL_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        mBrokerURL = settings.getString(BROKER_URL, mBrokerURL);
        mProductID = settings.getString(PRODUCT_ID, mProductID);
        mDevName = settings.getString(DEVICE_NAME, mDevName);
        mDevPSK = settings.getString(DEVICE_PSK, mDevPSK);
        editor.apply();

        if (!TextUtils.isEmpty(mBrokerURL)) {
            mBrokerURLEditText.setText(mBrokerURL);
        }

        if (!TextUtils.isEmpty(mProductID)) {
            mProductIdEditText.setText(mProductID);
        }

        if (!TextUtils.isEmpty(mDevName)) {
            mDevNameEditText.setText(mDevName);
        }

        if (!TextUtils.isEmpty(mDevPSK)) {
            mDevPSKEditText.setText(mDevPSK);
        }

        mConnectBtn.setOnClickListener(view -> {
            if (!checkInput()) {
                return;
            }
            SharedPreferences settings1 = getSharedPreferences(CENTRAL_CONFIG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = settings1.edit();
            editor1.putString(BROKER_URL, mBrokerURL);
            editor1.putString(PRODUCT_ID, mProductID);
            editor1.putString(DEVICE_NAME, mDevName);
            editor1.putString(DEVICE_PSK, mDevPSK);
            editor1.apply();
        });

        mDisconnectBtn.setOnClickListener(view -> {

        });
    }


    private boolean checkInput() {
        String inputBrokerURL = String.valueOf(mBrokerURLEditText.getText());
        if (TextUtils.isEmpty(inputBrokerURL)) {
            mBrokerURL = null;
        } else {
            mBrokerURL = inputBrokerURL;
        }

        String inputProductId = String.valueOf(mProductIdEditText.getText());
        if (TextUtils.isEmpty(inputProductId)) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入productId", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mProductID = inputProductId;
        }

        String inputDevName = String.valueOf(mDevNameEditText.getText());
        if (TextUtils.isEmpty(inputDevName)) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入deviceName", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mDevName = inputDevName;
        }

        String inputDevPSK = String.valueOf(mDevPSKEditText.getText());
        if (TextUtils.isEmpty(inputDevPSK)) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入devicePsk", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mDevPSK = inputDevPSK;
        }
        return true;
    }
}