package com.tencent.iot.explorer.device.video.call;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.video.call.data_template.VideoDataTemplateSample;
import com.tencent.iot.explorer.device.video.call.entity.DeviceConnectCondition;
import com.tencent.iot.device.video.advanced.recorder.TXVideoCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String devFileName = "device.json";
    private static String USER_ID = "callUserId";
    private ImageView qrImg;
    private EditText brokerUrlEt;
    private EditText productIdEt;
    private EditText devNameEt;
    private EditText devPskEt;
    private Button online;
    private Button offline;
    private TextView logTv;
    private String jsonFileName = "watch.json";
    private volatile VideoDataTemplateSample videoDataTemplateSample = null;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        brokerUrlEt = findViewById(R.id.et_broker_url);
        productIdEt = findViewById(R.id.et_productId);
        devNameEt = findViewById(R.id.et_deviceName);
        devPskEt = findViewById(R.id.et_devicePsk);
        logTv = findViewById(R.id.tv_log);
        logTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        online = findViewById(R.id.connect);
        offline = findViewById(R.id.disconnect);

        DeviceConnectCondition values = getDeviceConnectCondition();
        if (values != null) {
            productIdEt.setText(values.getProductId());
            devNameEt.setText(values.getDevName());
            devPskEt.setText(values.getDevPsk());
        }

        online.setOnClickListener(v -> {
            if (TextUtils.isEmpty(productIdEt.getText().toString()) || TextUtils.isEmpty(devNameEt.getText().toString())
            || TextUtils.isEmpty(devPskEt.getText().toString())) {
                return;
            }
            videoDataTemplateSample = new VideoDataTemplateSample(MainActivity.this,
                    "tcp://pre-iotcloud-mqtt.gz.tencentdevices.com:1883", productIdEt.getText().toString(), devNameEt.getText().toString(),
                    devPskEt.getText().toString(), jsonFileName, txMqttActionCallBack, videoCallBack, downStreamCallBack);
            videoDataTemplateSample.connect();
        });

        offline.setOnClickListener(v -> {
            if (videoDataTemplateSample != null) {
                videoDataTemplateSample.disconnect();
            }
            videoDataTemplateSample = null;
        });
    }

    private TXDataTemplateDownStreamCallBack downStreamCallBack = new TXDataTemplateDownStreamCallBack() {

        @Override
        public void onReplyCallBack(String msg) {
            Log.d(TAG, "reply received : " + msg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            Log.d(TAG, "onGetStatusReplyCallBack " + data.toString());
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            Log.d(TAG, "onControlCallBack " + msg);

            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "video is ok");
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            Log.d(TAG, "onActionCallBack " + actionId + " received, input:" + params);
            return null;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            Log.d(TAG, "onUnbindDeviceCallBack " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            Log.d(TAG, "onBindDeviceCallBack " + msg);
        }
    };

    private void updateLog(String msg) {
        handler.post(() -> logTv.setText(logTv.getText().toString() + "\n" + msg));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoDataTemplateSample != null) {
            videoDataTemplateSample.disconnect();
        }
    }

    private TXVideoCallBack videoCallBack = new TXVideoCallBack() {
        @Override
        public void receiveRtcJoinRoomAction(RoomKey room) {
            RecordVideoActivity.startBeingCall(MainActivity.this, room);
        }

        @Override
        public void callOtherDeviceSuccess(RoomKey room) {

        }

        @Override
        public void callOtherDeviceFailed(int code, String reason) {

        }

        @Override
        public void onNewCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) { }

        @Override
        public void onUserAccept(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) { }

        @Override
        public void onCallOver(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) { }

        @Override
        public void onAutoRejectCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) { }

        @Override
        public void onGetCallStatusCallBack(Integer callStatus, String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) { }

        @Override
        public void trtcJoinRoomCallBack(RoomKey room) { }

        @Override
        public void trtcGetUserAvatarCallBack(Integer code, String errorMsg, JSONObject avatarList) { }
    };

    private TXMqttActionCallBack txMqttActionCallBack = new TXMqttActionCallBack() {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            if (reconnect) {
                videoDataTemplateSample.subscribeTopic();
                updateLog("已自动重连 在线状态"+status);
            } else {
                Log.e(TAG, "TXMqttActionCallBack onConnectCompleted");
                Log.e(TAG, "TXMqttActionCallBack " + Thread.currentThread().getId());
                updateLog("首次在线状态"+status);
                if (videoDataTemplateSample == null) return;
                videoDataTemplateSample.subscribeTopic();

                DeviceConnectCondition condtion = new DeviceConnectCondition(productIdEt.getText().toString(), devNameEt.getText().toString(), devPskEt.getText().toString());
                saveDeviceConnectCondition(condtion);
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            Log.e(TAG, "TXMqttActionCallBack onConnectionLost");
            updateLog("掉线 " + cause.getMessage());
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg, Throwable cause) {
            Log.e(TAG, "TXMqttActionCallBack onDisconnectCompleted");
            updateLog("离线 " + msg);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg, Throwable cause) {
            Log.e(TAG, "TXMqttActionCallBack onSubscribeCompleted status " + status);
            if (Status.OK != videoDataTemplateSample.propertyGetStatus("report", false)) {
                Log.e(TAG, "property get status failed!");
            }
        }
    };

    private void saveDeviceConnectCondition(DeviceConnectCondition deviceConnectCondition) {
        if (deviceConnectCondition == null || TextUtils.isEmpty(deviceConnectCondition.getProductId())
            || TextUtils.isEmpty(deviceConnectCondition.getDevName())
            || TextUtils.isEmpty(deviceConnectCondition.getDevPsk())) {
            return;
        }
        SharedPreferences sp = getSharedPreferences(DeviceConnectCondition.TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(DeviceConnectCondition.TAG, JSON.toJSONString(deviceConnectCondition));
        editor.commit();
    }

    private DeviceConnectCondition getDeviceConnectCondition() {
        DeviceConnectCondition ret = null;
        SharedPreferences sp = getSharedPreferences(DeviceConnectCondition.TAG, MODE_PRIVATE);
        String value = sp.getString(DeviceConnectCondition.TAG, "");
        ret = JSON.parseObject(value, DeviceConnectCondition.class);
        return ret;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}