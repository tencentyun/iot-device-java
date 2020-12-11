package com.tencent.iot.explorer.device.trtc;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.tencent.cloud.ai.fr.utils.PermissionHandler;
import com.tencent.iot.explorer.device.android.app.BuildConfig;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.trtc.data_template.TRTCCallStatus;
import com.tencent.iot.explorer.device.trtc.data_template.TRTCDataTemplateSample;
import com.tencent.iot.explorer.device.trtc.data_template.TRTCExplorerDemoSessionManager;
import com.tencent.iot.explorer.device.trtc.data_template.TXTRTCCallBack;
import com.tencent.iot.explorer.device.trtc.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCUIManager;
import com.tencent.iot.explorer.device.trtc.data_template.ui.audiocall.TRTCAudioCallActivity;
import com.tencent.iot.explorer.device.trtc.data_template.ui.videocall.TRTCVideoCallActivity;
import com.tencent.iot.explorer.device.trtc.utils.ZXingUtils;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class TRTCMainActivity extends AppCompatActivity {

    private static final String TAG = "TRTCMainActivity";

    private TRTCDataTemplateSample mDataTemplateSample;

    private ImageView mQRCodeImgView;
    private Button mConnectBtn;
    private Button mCloseConnectBtn;
    private Button mAudioCallBtn;
    private Button mVideoCallBtn;

    private TextView mLogInfoText;

    private AlertDialog mAlertDialog;

    // Default testing parameters
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = BuildConfig.SUB_PRODUCT_ID;
    private String mDevName = BuildConfig.SUB_DEV_NAME;
    private String mDevPSK  = BuildConfig.SUB_DEV_PSK; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String

    private Integer mCallType = TRTCCalling.TYPE_UNKNOWN;
    private String mUserId = "";
    private boolean callMobile = false;

    private final static String mJsonFileName = "TRTC_watch.json";

    private static final int REQUEST_PERMISSTION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trtc_main);
        MultiDex.install(this);

        mQRCodeImgView = findViewById(R.id.iv_qrcode);
        mConnectBtn = findViewById(R.id.connect);
        mCloseConnectBtn = findViewById(R.id.close_connect);
        mAudioCallBtn = findViewById(R.id.audio_call);
        mVideoCallBtn = findViewById(R.id.video_call);
        mLogInfoText = findViewById(R.id.log_info);

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mDataTemplateSample = new TRTCDataTemplateSample(TRTCMainActivity.this, mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack(), new TRTCCallBack());
                mDataTemplateSample.connect();
            }
        });

        mCloseConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                mDataTemplateSample.disconnect();
            }
        });

        mVideoCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                callMobile = true;
                mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_CALLING, TRTCCalling.TYPE_VIDEO_CALL);
                TRTCUIManager.getInstance().setSessionManager(new TRTCExplorerDemoSessionManager(mDataTemplateSample));
                TRTCUIManager.getInstance().isCalling = true;
                TRTCVideoCallActivity.startCallSomeone(TRTCMainActivity.this, new RoomKey(), "");
            }
        });
        mAudioCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                callMobile = true;
                mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_CALLING, TRTCCalling.TYPE_AUDIO_CALL);
                TRTCUIManager.getInstance().setSessionManager(new TRTCExplorerDemoSessionManager(mDataTemplateSample));
                TRTCUIManager.getInstance().isCalling = true;
                TRTCAudioCallActivity.startCallSomeone(TRTCMainActivity.this, new RoomKey(), "");
            }
        });

        initPermission();
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionUtils.permission(PermissionConstants.STORAGE, PermissionConstants.MICROPHONE, PermissionConstants.CAMERA)
                    .request();
        }
    }

    /**
     * 实现下行消息处理的回调接口
     */
    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            Log.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //可根据自己需求进行处理状态和控制信息的获取结果
            Log.d(TAG, "event down stream message received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            Log.d(TAG, "control down stream message received : " + msg);
            //do something

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                return null;
            }
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            TXLog.d(TAG, "action [%s] received, input:" + params, actionId);
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    Iterator<String> it = params.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        TXLog.d(TAG,"Input parameter[%s]:" + params.get(key), key);
                    }
                    //construct result
                    JSONObject result = new JSONObject();
                    result.put("code",0);
                    result.put("status", "some message wher errorsome message when error");

                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);

                    result.put("response", response);
                    return result;
                } catch (JSONException e) {
                    printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return null;
                }
            } else if (actionId.equals("YOUR ACTION")) {
                //do your action
            }
            return null;
        }
    }

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
            mDataTemplateSample.subscribeTopic();
            if (!reconnect) {
                mQRCodeImgView.setImageBitmap(ZXingUtils.createQRCodeBitmap(mDataTemplateSample.generalDeviceQRCodeContent(), 200, 200,"UTF-8","H", "1", Color.BLACK, Color.WHITE));
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
            } else {
                printLogInfo(TAG, logInfo, mLogInfoText);
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            printLogInfo(TAG, logInfo, mLogInfoText);
        }
    }

    private class TRTCCallBack extends TXTRTCCallBack {

        @Override
        public void onGetCallStatusCallBack(Integer callStatus, final String userid, Integer callType) {
            if (callStatus == 1) { //表示被呼叫了
                mCallType = callType;
                mUserId = userid;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!callMobile) { //被呼叫了
                            TRTCUIManager.getInstance().setSessionManager(new TRTCExplorerDemoSessionManager(mDataTemplateSample));
                            if (mCallType == TRTCCalling.TYPE_AUDIO_CALL) {
                                TRTCUIManager.getInstance().isCalling = true;
                                TRTCAudioCallActivity.startBeingCall(TRTCMainActivity.this, new RoomKey(), userid);
                            } else if (mCallType == TRTCCalling.TYPE_VIDEO_CALL) {
                                TRTCUIManager.getInstance().isCalling = true;
                                TRTCVideoCallActivity.startBeingCall(TRTCMainActivity.this, new RoomKey(), userid);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void trtcJoinRoomCallBack(final RoomKey room) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TRTCUIManager.getInstance().joinRoom(mCallType, "", room);
                    callMobile = false;
                }
            });
        }
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(logInfo + "\n");
            }
        });
    }

    /**
     * 打印日志信息
     *
     * @param logInfo
     */
    protected void printLogInfo(final String tag, final String logInfo, final TextView textView) {
        printLogInfo(tag, logInfo, textView, TXLog.LEVEL_DEBUG);
    }
}