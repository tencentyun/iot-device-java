package com.tencent.iot.explorer.device.video;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.CallState;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.rtc.entity.UserEntity;
import com.tencent.iot.explorer.device.rtc.utils.ZXingUtils;
import com.tencent.iot.explorer.device.video.data_template.VideoDataTemplateSample;
import com.tencent.iot.explorer.device.video.entity.DeviceConnectCondition;
import com.tencent.iot.explorer.device.video.entity.PhoneInfo;
import com.tencent.iot.explorer.device.video.recorder.TXVideoCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.tencent.iot.explorer.device.common.stateflow.entity.TXCallDataTemplateConstants.PROPERTY_SYS_CALL_USERLIST;
import static com.tencent.iot.explorer.device.common.stateflow.entity.TXCallDataTemplateConstants.PROPERTY_SYS_CALL_USERLIST_USERID;

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
    private Button videoCall;
    private Button audioCall;
    private Button accpetCall;
    private Button rejectCall;
    private Button hangUp;
    private LinearLayout callActionLayout;
    private RelativeLayout callingLayout;
    private TextView callerUserId;
    private TextView callType;
    private TextView logTv;
    private EditText toCalledUserId;
    private String jsonFileName = "video_watch.json";
    private volatile VideoDataTemplateSample videoDataTemplateSample = null;
    private Handler handler = new Handler();
    private String defaultAgent = String.format("device/3.3.1 (Android %d;%s %s;%s-%s)", android.os.Build.VERSION.SDK_INT, android.os.Build.BRAND, android.os.Build.MODEL, Locale.getDefault().getLanguage(), Locale.getDefault().getCountry());
    private volatile Timer timer = new Timer();
    private volatile Timer callingTimer = new Timer();
    private int callUserType = CallingType.TYPE_AUDIO_CALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrImg = findViewById(R.id.iv_qrcode);
        brokerUrlEt = findViewById(R.id.et_broker_url);
        productIdEt = findViewById(R.id.et_productId);
        devNameEt = findViewById(R.id.et_deviceName);
        devPskEt = findViewById(R.id.et_devicePsk);
        videoCall = findViewById(R.id.video_call);
        audioCall = findViewById(R.id.audio_call);
        accpetCall = findViewById(R.id.accept_call);
        rejectCall = findViewById(R.id.reject_call);
        callActionLayout = findViewById(R.id.call_btn_layout);
        callerUserId = findViewById(R.id.call_user_id);
        callType = findViewById(R.id.call_type);
        toCalledUserId = findViewById(R.id.to_call_user_id);
        callingLayout = findViewById(R.id.calling_layout);
        logTv = findViewById(R.id.tv_log);
        logTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        online = findViewById(R.id.connect);
        offline = findViewById(R.id.disconnect);
        hangUp = findViewById(R.id.btn_hang_up);
//        VideoNativeInteface.getInstance().setCallback(xP2PCallback);
        toCalledUserId.setText(getUserId());

        DeviceConnectCondition values = getDeviceConnectCondition();
        if (values != null) {
            productIdEt.setText(values.getProductId());
            devNameEt.setText(values.getDevName());
            devPskEt.setText(values.getDevPsk());
        }

        callingLayout.setOnClickListener(v -> { });
        hangUp.setOnClickListener(v -> {
            callingLayout.setVisibility(View.GONE);
            videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_IDLE_OR_REFUSE, callUserType, callerUserId.getText().toString(), defaultAgent);
            callingTimer.cancel();
        });

        accpetCall.setOnClickListener(v -> {
            int value = Integer.valueOf(callType.getText().toString());
            videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_ON_THE_PHONE, value, callerUserId.getText().toString(), defaultAgent);
            try {
                timer.cancel();
            } catch (Exception e){}
            startPhoneCall(callerUserId.getText().toString(), defaultAgent, value);
            callActionLayout.setVisibility(View.GONE);
        });

        rejectCall.setOnClickListener(v -> {
            int value = Integer.valueOf(callType.getText().toString());
            videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_IDLE_OR_REFUSE, value, callerUserId.getText().toString(), defaultAgent);
            try {
                timer.cancel();
            } catch (Exception e) { }
            callActionLayout.setVisibility(View.GONE);
        });

        online.setOnClickListener(v -> {
            if (TextUtils.isEmpty(productIdEt.getText().toString()) || TextUtils.isEmpty(devNameEt.getText().toString())
            || TextUtils.isEmpty(devPskEt.getText().toString())) {
                return;
            }
            videoDataTemplateSample = new VideoDataTemplateSample(MainActivity.this,
                    null, productIdEt.getText().toString(), devNameEt.getText().toString(),
                    devPskEt.getText().toString(), jsonFileName, txMqttActionCallBack, videoCallBack, downStreamCallBack);
            videoDataTemplateSample.connect();
        });

        offline.setOnClickListener(v -> {
            if (videoDataTemplateSample != null) {
                videoDataTemplateSample.disconnect();
            }
            videoDataTemplateSample = null;
        });

        videoCall.setOnClickListener( v -> {
            if (TextUtils.isEmpty(toCalledUserId.getText().toString())) {
                Toast.makeText(MainActivity.this, "请输入用户ID", Toast.LENGTH_SHORT).show();
                return;
            }
            callUser(CallingType.TYPE_VIDEO_CALL);
        });

        audioCall.setOnClickListener( v -> {
            if (TextUtils.isEmpty(toCalledUserId.getText().toString())) {
                Toast.makeText(MainActivity.this, "请输入用户ID", Toast.LENGTH_SHORT).show();
                return;
            }
            callUser(CallingType.TYPE_AUDIO_CALL);
        });
    }

//    private XP2PCallback xP2PCallback = new XP2PCallback() {
//
//        @Override
//        public void avDataRecvHandle(byte[] data, int len) {
//            ReadByteIO.Companion.getInstance().addLast(data);
//        }
//
//        @Override
//        public void avDataMsgHandle(int type, String msg) {
//            Log.e(TAG, "avDataMsgHandle type " + type);
//            if (type == 0) {
//                Log.e(TAG, "start send video data");
//                Utils.sendVideoBroadcast(MainActivity.this, 1);
//
//            } else if (type == 1) {
//                Log.e(TAG, "this call over");
//                Utils.sendVideoBroadcast(MainActivity.this, 2);
//            }
//        }
//    };

    private TXDataTemplateDownStreamCallBack downStreamCallBack = new TXDataTemplateDownStreamCallBack() {

        @Override
        public void onReplyCallBack(String msg) {
            Log.d(TAG, "reply received : " + msg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            Log.d(TAG, "onGetStatusReplyCallBack " + data.toString());
            autofillUserId(data);
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

    private void autofillUserId(JSONObject data) {
        try {
            JSONObject property = data.getJSONObject("reported");
            if (property.has(PROPERTY_SYS_CALL_USERLIST)) {
                String userList = property.getString(PROPERTY_SYS_CALL_USERLIST);
                JSONArray userArrayList = new JSONArray(userList);
                if (userArrayList != null && userArrayList.length() > 0) {
                    JSONObject userJson = (JSONObject) userArrayList.get(userArrayList.length() - 1);
                    UserEntity user = new UserEntity();
                    if (userJson.has(PROPERTY_SYS_CALL_USERLIST_USERID)) {
                        user.setUserid(userJson.getString(PROPERTY_SYS_CALL_USERLIST_USERID));
                    } else { //没有获取到UserID
                        user.setUserid("");
                    }

                    runOnUiThread(() -> toCalledUserId.setText(user.getUserid()));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void callUser(int callOtherUserType) {
        callUserType = callOtherUserType;
        String userId = toCalledUserId.getText().toString();
        videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_CALLING, callOtherUserType, userId, defaultAgent);
        runOnUiThread(() -> callingLayout.setVisibility(View.VISIBLE));
        TimerTask task = new TimerTask(){
            public void run(){
                //自己已进入房间15秒内对方没有进入房间 则显示对方已挂断，并主动退出，进入了就取消
                runOnUiThread(() -> callingLayout.setVisibility(View.GONE));
                videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_IDLE_OR_REFUSE, callOtherUserType, userId, defaultAgent);
            }
        };
        callingTimer = new Timer();
        callingTimer.schedule(task, 15000);
    }

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

    private TXVideoCallBack videoCallBack = new TXVideoCallBack() {

        @Override
        public void onNewCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) {
            // 收到新的童话请求
            String info = String.format("一个通话请求 callerId %s, calledId %s, agent %s, callType %d",
                    callExtraInfo.getCallerId(), callExtraInfo.getCalledId(), agent, callType);
            updateLog(info);
            showNewCall(userid, callType);
        }

        @Override
        public void onUserAccept(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) {
            String info = String.format("对方接受通话请求 callerId %s, calledId %s, agent %s, callType %d",
                    callExtraInfo.getCallerId(), callExtraInfo.getCalledId(), agent, callType);
            updateLog(info);
            // 对端接受通话请求
            try {
                callingTimer.cancel();
            } catch (Exception e){}
            handler.post(() -> {
                callingLayout.setVisibility(View.GONE);
            });
            startPhoneCall(userid, agent, callType);
        }

        @Override
        public void onCallOver(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) {
            // 通话结束
            String info = String.format("通话结束 callerId %s, calledId %s, agent %s, callType %d",
                    callExtraInfo.getCallerId(), callExtraInfo.getCalledId(), agent, callType);
            updateLog(info);
            try {
                callingTimer.cancel();
            } catch (Exception e){}
            runOnUiThread(() -> { callActionLayout.setVisibility(View.GONE);
                callingLayout.setVisibility(View.GONE); });
            Utils.sendVideoOverBroadcast(MainActivity.this);
            videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_IDLE_OR_REFUSE, callType, userid, agent);
        }

        @Override
        public void onAutoRejectCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) {
            updateLog("拒绝通话请求 userid " + userid + ", agent " + agent + ", callType " + callType);
        }

        @Override
        public void onGetCallStatusCallBack(Integer callStatus, String userid, String agent, Integer callType, CallExtraInfo callExtraInfo) { }

        @Override
        public void trtcJoinRoomCallBack(RoomKey room) {}
    };

    private void startPhoneCall(String userid, String agent, Integer callType) {
        Log.e(TAG, "callType " + callType);
        videoDataTemplateSample.reportCallStatusProperty(CallState.TYPE_ON_THE_PHONE, callType, userid, agent);
        Intent intent = new Intent(MainActivity.this, RecordVideoActivity.class);
        Bundle bundle = new Bundle();
        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.setCallType(callType);
        phoneInfo.setAgent(agent);
        phoneInfo.setUserid(userid);
        bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
        intent.putExtra(PhoneInfo.TAG, bundle);
        startActivityForResult(intent, 2);
    }

    private void showNewCall(String userId, int callTypeInt) {
        handler.post(() -> {
            callerUserId.setText(userId);
            callType.setText(String.valueOf(callTypeInt));
            callActionLayout.setVisibility(View.VISIBLE);
        });

        TimerTask task = new TimerTask(){
            public void run(){
                //自己已进入房间15秒内对方没有进入房间 则显示对方已挂断，并主动退出，进入了就取消
                runOnUiThread(() -> {
                    callActionLayout.setVisibility(View.GONE);
                    rejectCall.performClick(); }
                );

            }
        };
        timer = new Timer();
        timer.schedule(task, 15000);
    }

    private TXMqttActionCallBack txMqttActionCallBack = new TXMqttActionCallBack() {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            Log.e(TAG, "TXMqttActionCallBack onConnectCompleted");
            Log.e(TAG, "TXMqttActionCallBack " + Thread.currentThread().getId());
            updateLog("在线");
            if (videoDataTemplateSample == null) return;
            handler.post(() -> qrImg.setImageBitmap(ZXingUtils.createQRCodeBitmap(videoDataTemplateSample.generateDeviceQRCodeContent(), 200, 200,"UTF-8","H", "1", Color.BLACK, Color.WHITE)));
            videoDataTemplateSample.subscribeTopic();

            DeviceConnectCondition condtion = new DeviceConnectCondition(productIdEt.getText().toString(), devNameEt.getText().toString(), devPskEt.getText().toString());
            handler.post(() -> initVideoModeul(condtion));
            saveDeviceConnectCondition(condtion);
            saveUserId(toCalledUserId.getText().toString());
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
            VideoNativeInteface.getInstance().release();
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

    private void saveUserId(String userId) {
        SharedPreferences sp = getSharedPreferences(USER_ID, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(USER_ID, userId);
        editor.commit();
    }

    private String getUserId() {
        DeviceConnectCondition ret = null;
        SharedPreferences sp = getSharedPreferences(USER_ID, MODE_PRIVATE);
        String userId = sp.getString(USER_ID, "");
        return userId;
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