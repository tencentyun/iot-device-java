package com.tencent.iot.explorer.device.central;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.iot.explorer.device.android.app.BuildConfig;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.central.data_template.CentralDataTemplateSample;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.rtc.utils.ZXingUtils;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class CentralMainActivity extends AppCompatActivity {

    private static final String TAG = CentralMainActivity.class.getSimpleName();
    private static final String CENTRAL_CONFIG = "central_config";
    private static final String BROKER_URL = "broker_url";
    private static final String PRODUCT_ID = "product_id";
    private static final String DEVICE_NAME = "dev_name";
    private static final String DEVICE_PSK = "dev_psk";

    private static final String JSON_FILE_NAME = "TRTC_watch.json";

    private ImageView mQRCodeImgView;
    private Button mConnectBtn;
    private Button mDisconnectBtn;
    private EditText mBrokerURLEditText;
    private EditText mProductIdEditText;
    private EditText mDevNameEditText;
    private EditText mDevPSKEditText;
    private TextView mLogInfoText;

    //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mBrokerURL = null;
    private String mProductID = BuildConfig.CENTRAL_PRODUCT_ID;
    private String mDevName = BuildConfig.CENTRAL_DEVICE_NAME;
    private String mDevPSK  = BuildConfig.CENTRAL_DEVICE_PSK;

    private CentralDataTemplateSample mDataTemplateSample;

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

            if (mDataTemplateSample != null) return;
            mDataTemplateSample = new CentralDataTemplateSample(CentralMainActivity.this,
                    mBrokerURL, mProductID, mDevName, mDevPSK,
                    new SelfMqttActionCallBack(), JSON_FILE_NAME,
                    new SelfDownStreamCallBack());
            mDataTemplateSample.connect();
        });

        mDisconnectBtn.setOnClickListener(view -> {
            if (mDataTemplateSample == null)
                return;
            mDataTemplateSample.disconnect();
            mDataTemplateSample = null;
        });
    }


    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        public SelfMqttActionCallBack() { }

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
            if (Status.ERROR == status) {
                runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, "上线失败，请检查设备三元组信息是否正确或网络是否正常", Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, "上线成功", Toast.LENGTH_LONG).show());
                if (mDataTemplateSample != null) {
                    if (!reconnect) {
                        mDataTemplateSample.subscribeTopic();
                        runOnUiThread(() -> {
                            // 设置适配器，刷新展示用户列表
                            mQRCodeImgView.setImageBitmap(ZXingUtils.createQRCodeBitmap(mDataTemplateSample.generateDeviceQRCodeContent(), 200, 200,"UTF-8","H", "1", Color.BLACK, Color.WHITE));
                        });
                    }
                }
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
            if (mDataTemplateSample != null) {
                mDataTemplateSample.unSubscribeTopic();
            }
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String topics = Arrays.toString(asyncActionToken.getTopics());
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), topics, userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
            if (Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
                printLogInfo(TAG, "property get status failed!", mLogInfoText, TXLog.LEVEL_ERROR);
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("onMessageReceived, topic[%s], message[%s]", topic, message.toString());
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
        }
    }

    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            TXLog.d(TAG, "onReplyCallBack : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            TXLog.d(TAG, "onGetStatusReplyCallBack : " + data.toString());
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            TXLog.d(TAG, "onControlCallBack : " + msg);
            JSONObject result = new JSONObject();
            try {
                result.put("code",0);
                result.put("status", "success");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            TXLog.d(TAG, String.format("onActionCallBack: actionId=[%s], params=[%s]", actionId, params.toString()));

            JSONObject result = new JSONObject();
            try {
                result.put("code", 0);
                result.put("status", "success");
                JSONObject response = new JSONObject();
                response.put("Code", 0);
                result.put("response", response);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            TXLog.d(TAG, "onUnbindDeviceCallBack : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            TXLog.d(TAG, "onBindDeviceCallBack : " + msg);
        }
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

    /**
     * 打印日志信息
     *
     * @param logInfo
     */
    protected void printLogInfo(final String tag, final String logInfo, final TextView textView, int logLevel) {
        switch (logLevel) {
            case TXLog.LEVEL_DEBUG:
                TXLog.d(tag, logInfo);
                break;

            case TXLog.LEVEL_INFO:
                TXLog.i(tag, logInfo);
                break;

            case TXLog.LEVEL_ERROR:
                TXLog.e(tag, logInfo);
                break;

            default:
                TXLog.d(tag, logInfo);
                break;
        }

        runOnUiThread(() -> textView.append(logInfo + "\n"));
    }
}