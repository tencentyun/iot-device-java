package com.tencent.iot.explorer.device.video.push_stream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.CallState;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.rtc.utils.ZXingUtils;
import com.tencent.iot.explorer.device.video.call.data_template.VideoDataTemplateSample;
import com.tencent.iot.explorer.device.video.call.entity.DeviceConnectCondition;
import com.tencent.iot.explorer.device.video.call.entity.PhoneInfo;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Timer;


public class PushRealTimeStreamActivity extends AppCompatActivity {

    private static final String TAG = PushRealTimeStreamActivity.class.getSimpleName();
    public static final String devFileName = "device.json";
    private ImageView qrImg;
    private EditText brokerUrlEt;
    private EditText productIdEt;
    private EditText devNameEt;
    private EditText devPskEt;
    private Button online;
    private Button offline;
    private Button videoCall;
    private Button audioCall;
    private TextView logTv;
    private String jsonFileName = "video_watch.json";
    private volatile VideoDataTemplateSample videoDataTemplateSample = null;
    private Handler handler = new Handler();
    private String defaultAgent = String.format("device/3.3.1 (Android %d;%s %s;%s-%s)", android.os.Build.VERSION.SDK_INT, android.os.Build.BRAND, android.os.Build.MODEL, Locale.getDefault().getLanguage(), Locale.getDefault().getCountry());
    private volatile Timer timer = new Timer();
    private volatile Timer callingTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_realtime);

        qrImg = findViewById(R.id.iv_qrcode);
        brokerUrlEt = findViewById(R.id.et_broker_url);
        productIdEt = findViewById(R.id.et_productId);
        devNameEt = findViewById(R.id.et_deviceName);
        devPskEt = findViewById(R.id.et_devicePsk);
        videoCall = findViewById(R.id.video_call);
        audioCall = findViewById(R.id.audio_call);
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
            videoDataTemplateSample = new VideoDataTemplateSample(PushRealTimeStreamActivity.this,
                    null, productIdEt.getText().toString(), devNameEt.getText().toString(),
                    devPskEt.getText().toString(), jsonFileName, txMqttActionCallBack, null, downStreamCallBack);
            videoDataTemplateSample.connect();
        });

        offline.setOnClickListener(v -> {
            if (videoDataTemplateSample != null) {
                videoDataTemplateSample.disconnect();
            }
            videoDataTemplateSample = null;
        });

        videoCall.setOnClickListener( v -> {
            if (videoDataTemplateSample != null && videoDataTemplateSample.isConnected()) {
                startPhoneCall("1234567", defaultAgent, CallingType.TYPE_VIDEO_CALL);
            } else {
                updateLog("设备未上线");
            }
        });

        audioCall.setOnClickListener( v -> {
            if (videoDataTemplateSample != null && videoDataTemplateSample.isConnected()) {
                startPhoneCall("1234567", defaultAgent, CallingType.TYPE_AUDIO_CALL);
            } else {
                updateLog("设备未上线");
            }
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

    private void initVideoModeul(DeviceConnectCondition condition) {
        int initRet = VideoNativeInteface.getInstance().initWithDevice(condition.getProductId(),
                condition.getDevName(), condition.getDevPsk());
        updateLog("init video module return " + initRet);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String xp2pInfo = VideoNativeInteface.getInstance().getXp2pInfo();
                    if (!TextUtils.isEmpty(xp2pInfo) && videoDataTemplateSample != null) {
                        Status status = videoDataTemplateSample.reportXp2pInfo(xp2pInfo);
                        Log.e(TAG, "reportCallStatusProperty status " + status);
                        break;
                    }
                }
            }
        }).start();
    }

    private void startPhoneCall(String userid, String agent, Integer callType) {
        Log.e(TAG, "callType " + callType);
        videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_ON_THE_PHONE, callType, userid, agent);
        Intent intent = new Intent(PushRealTimeStreamActivity.this, RecordVideoActivity2.class);
        Bundle bundle = new Bundle();
        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.setCallType(callType);
        phoneInfo.setAgent(agent);
        phoneInfo.setUserid(userid);
        bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
        intent.putExtra(PhoneInfo.TAG, bundle);
        startActivityForResult(intent, 2);
    }

    private TXMqttActionCallBack txMqttActionCallBack = new TXMqttActionCallBack() {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            if (reconnect) {
                videoDataTemplateSample.subscribeTopic();
                VideoNativeInteface.getInstance().release();
                handler.post(() -> initVideoModeul(getDeviceConnectCondition()));
                updateLog("已自动重连 在线");
            } else {
                Log.e(TAG, "TXMqttActionCallBack onConnectCompleted");
                Log.e(TAG, "TXMqttActionCallBack " + Thread.currentThread().getId());
                updateLog("在线");
                if (videoDataTemplateSample == null) return;
                handler.post(() -> qrImg.setImageBitmap(ZXingUtils.createQRCodeBitmap(videoDataTemplateSample.generateDeviceQRCodeContent(), 200, 200, "UTF-8", "H", "1", Color.BLACK, Color.WHITE)));
                videoDataTemplateSample.subscribeTopic();

                DeviceConnectCondition condtion = new DeviceConnectCondition(productIdEt.getText().toString(), devNameEt.getText().toString(), devPskEt.getText().toString());
                handler.post(() -> initVideoModeul(condtion));
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
            VideoNativeInteface.getInstance().release();
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
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            Bundle bundle = data.getBundleExtra(PhoneInfo.TAG);
            if (bundle != null) {
                String jsonStr = bundle.getString(PhoneInfo.TAG);
                Log.e(TAG, "jsonStr " + jsonStr);
                PhoneInfo phoneInfo = JSON.parseObject(jsonStr, PhoneInfo.class);
                if (phoneInfo == null) return;
                videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_IDLE_OR_REFUSE, phoneInfo.getCallType(), phoneInfo.getUserid(), phoneInfo.getAgent());
            }
        }
    }
}