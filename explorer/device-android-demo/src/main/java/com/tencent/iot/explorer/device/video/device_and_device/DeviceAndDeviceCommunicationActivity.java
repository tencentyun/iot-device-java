package com.tencent.iot.explorer.device.video.device_and_device;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.view.View.GONE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.device.video.advanced.recorder.TXVideoCallBack;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.video.call.RecordVideoActivity;
import com.tencent.iot.explorer.device.video.call.data_template.VideoDataTemplateSample;
import com.tencent.iot.explorer.device.video.call.entity.DeviceConnectCondition;
import com.tencent.iot.explorer.device.video.call.entity.PhoneInfo;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;


public class DeviceAndDeviceCommunicationActivity extends AppCompatActivity {

    private static final String TAG = DeviceAndDeviceCommunicationActivity.class.getSimpleName();
    public static final String devFileName = "device.json";
    private ImageView qrImg;
    private EditText brokerUrlEt;
    private EditText productIdEt;
    private EditText devNameEt;
    private EditText devPskEt;
    private Button online;
    private Button offline;
    private Button callerCall;
    private TextView logTv;
    private EditText deviceNameEv;
    private String jsonFileName = "watch.json";
    private volatile VideoDataTemplateSample videoDataTemplateSample = null;
    private Handler handler = new Handler();
    private volatile Timer timer = new Timer();
    private volatile Timer callingTimer = new Timer();
    private long onlineClickedTime = 0L;
    private long offlineClickedTime = 0L;
    private volatile boolean isCalling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_and_device_communication);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        brokerUrlEt = findViewById(R.id.et_broker_url);
        productIdEt = findViewById(R.id.et_productId);
        devNameEt = findViewById(R.id.et_deviceName);
        devPskEt = findViewById(R.id.et_devicePsk);
        callerCall = findViewById(R.id.caller_btn);
        logTv = findViewById(R.id.tv_log);
        logTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        online = findViewById(R.id.connect);
        offline = findViewById(R.id.disconnect);

        View device_name_layout = findViewById(R.id.device_name_layout);
        TextView device_name_tv_tip = device_name_layout.findViewById(R.id.tv_tip);
        device_name_tv_tip.setText(R.string.device_name);
        deviceNameEv = device_name_layout.findViewById(R.id.ev_content);
        deviceNameEv.setHint(R.string.hint_device_name);
        deviceNameEv.setInputType(TYPE_CLASS_TEXT);
        ImageView device_name_iv_more = device_name_layout.findViewById(R.id.iv_more);
        device_name_iv_more.setVisibility(GONE);

        DeviceConnectCondition values = getDeviceConnectCondition();
        if (values != null) {
            productIdEt.setText(values.getProductId());
            devNameEt.setText(values.getDevName());
            devPskEt.setText(values.getDevPsk());
        }

        online.setOnClickListener(v -> {
            onlineClickedTime = System.currentTimeMillis()/1000;
            if (onlineClickedTime - offlineClickedTime < 2) {
                updateLog("请勿频繁点击上下线按钮.");
            } else {
                if (TextUtils.isEmpty(productIdEt.getText().toString()) || TextUtils.isEmpty(devNameEt.getText().toString())
                        || TextUtils.isEmpty(devPskEt.getText().toString())) {
                    return;
                }
                videoDataTemplateSample = new VideoDataTemplateSample(DeviceAndDeviceCommunicationActivity.this,
                        null, productIdEt.getText().toString(), devNameEt.getText().toString(),
                        devPskEt.getText().toString(), jsonFileName, txMqttActionCallBack, videoCallBack, downStreamCallBack);
                videoDataTemplateSample.connect();
            }
        });

        offline.setOnClickListener(v -> {
            offlineClickedTime = System.currentTimeMillis()/1000;
            if (offlineClickedTime - onlineClickedTime < 2) {
                updateLog("请勿频繁点击上下线按钮.");
            } else {
                if (videoDataTemplateSample != null) {
                    videoDataTemplateSample.disconnect();
                }
                videoDataTemplateSample = null;
            }
        });

//        videoCalled.setOnClickListener(v -> {
//            if (videoDataTemplateSample != null && videoDataTemplateSample.isConnected()) {
//                startPhoneCalled(CallingType.TYPE_VIDEO_CALL);
//            } else {
//                updateLog("设备未上线");
//            }
//        });

        callerCall.setOnClickListener(v -> {
            if (videoDataTemplateSample != null) {
                if (!isCalling) {
                    isCalling = true;
                    startPhoneCall(CallingType.TYPE_VIDEO_CALL);
                } else {
                    Toast.makeText(DeviceAndDeviceCommunicationActivity.this, "正在呼叫...", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(DeviceAndDeviceCommunicationActivity.this, "设备未上线", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private TXVideoCallBack videoCallBack = new TXVideoCallBack() {
        @Override
        public void receiveRtcJoinRoomAction(RoomKey room) {
            RecordVideoActivity.startBeingCall(DeviceAndDeviceCommunicationActivity.this, room);
        }

        @Override
        public void callOtherDeviceSuccess(RoomKey room) {
            RecordVideoActivity.startCallSomeone(DeviceAndDeviceCommunicationActivity.this, room);
            isCalling = false;
        }

        @Override
        public void callOtherDeviceFailed(int code, String reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DeviceAndDeviceCommunicationActivity.this, "呼叫失败"+reason, Toast.LENGTH_SHORT).show();
                    isCalling = false;
                }
            });
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

    private void startPhoneCall(Integer callType) {
        String otherProductID = productIdEt.getText().toString();

        String otherDeviceName = String.valueOf(deviceNameEv.getText());
        Log.i(TAG, "callOtherDevice result: " + (Status.OK == videoDataTemplateSample.callOtherDevice(otherProductID, otherDeviceName)));

        /*
        if (TextUtils.isEmpty(deviceName)) {
            Toast.makeText(DeviceAndDeviceCommunicationActivity.this, "请输入deviceName", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e(TAG, "callType " + callType);
        Intent intent = new Intent(DeviceAndDeviceCommunicationActivity.this, RecordVideoActivity4.class);
        Bundle bundle = new Bundle();
        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.setCallType(callType);
        bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
        bundle.putString("deviceName", deviceName);
        intent.putExtra(PhoneInfo.TAG, bundle);
        startActivityForResult(intent, 4);
         */
    }

    private void startPhoneCalled(Integer callType) {
        Log.e(TAG, "callType " + callType);
        Intent intent = new Intent(DeviceAndDeviceCommunicationActivity.this, RecordVideoActivity3.class);
        Bundle bundle = new Bundle();
        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.setCallType(callType);
        bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
        intent.putExtra(PhoneInfo.TAG, bundle);
        startActivityForResult(intent, 3);
    }

    private TXMqttActionCallBack txMqttActionCallBack = new TXMqttActionCallBack() {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            if (status == Status.OK) {
                if (reconnect) {
                    videoDataTemplateSample.subscribeTopic();
                    updateLog("已自动重连 在线");
                } else {
                    Log.e(TAG, "TXMqttActionCallBack onConnectCompleted");
                    Log.e(TAG, "TXMqttActionCallBack " + Thread.currentThread().getId());
                    updateLog("在线");
                    if (videoDataTemplateSample == null) return;
                    videoDataTemplateSample.subscribeTopic();

                    DeviceConnectCondition condtion = new DeviceConnectCondition(productIdEt.getText().toString(), devNameEt.getText().toString(), devPskEt.getText().toString());
                    saveDeviceConnectCondition(condtion);
                }
            } else {
                updateLog("mqtt 上线失败"+msg);
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
}