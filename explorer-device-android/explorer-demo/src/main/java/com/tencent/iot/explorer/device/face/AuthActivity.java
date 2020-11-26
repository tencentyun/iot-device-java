package com.tencent.iot.explorer.device.face;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.cloud.ai.fr.BuildConfig;
import com.tencent.cloud.ai.fr.RegWithFileActivity;
import com.tencent.cloud.ai.fr.business.thread.AIThreadPool;
import com.tencent.cloud.ai.fr.sdksupport.Auth;
import com.tencent.cloud.ai.fr.sdksupport.Auth.AuthResult;
import com.tencent.cloud.ai.fr.utils.PermissionHandler;
import com.tencent.cloud.ai.fr.utils.PermissionHandler.GetPermissionsException;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.face.data_template.FaceKitSample;
import com.tencent.iot.explorer.device.face.data_template.TXAuthCallBack;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;

public class AuthActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    /** 需要注册的人脸图片文件夹 */
    private static final String FACE_IMG_DIR = "/sdcard/face_for_reg";

    // Default testing parameters
    private String mBrokerURL = null;
    private String mProductID = com.tencent.iot.explorer.device.android.app.BuildConfig.SUB_PRODUCT_ID;
    private String mDevName = com.tencent.iot.explorer.device.android.app.BuildConfig.SUB_DEV_NAME;
    private String mDevPSK  = com.tencent.iot.explorer.device.android.app.BuildConfig.SUB_DEV_PSK; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String

    private final static String mJsonFileName = "facekit.json";

    private EditText mItemText;

    private final static String BROKER_URL = "broker_url";
    private final static String PRODUCT_ID = "product_id";
    private final static String DEVICE_NAME = "dev_name";
    private final static String DEVICE_PSK = "dev_psk";
    private final static String DEVICE_CERT = "dev_cert";
    private final static String DEVICE_PRIV  = "dev_priv";

    private boolean mAuthorized = false;
    private boolean mAccessPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashReport.initCrashReport(getApplicationContext(), "YOUR_BUGLY_APPID"/*修改BUGLY的APPID为实际的值*/, BuildConfig.DEBUG);

        setContentView(R.layout.activity_auth);
        connect();

        try {
            mPermissionHandler.start();// 先申请系统权限
        } catch (GetPermissionsException e) {
            e.printStackTrace();
            Toast.makeText(this, "GetPermissionsException: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);// 必须有这个调用, mPermissionHandler 才能正常工作
    }

    private AuthResult auth(Context context, String appId, String secretKey) {
        AuthResult authResult = Auth.authWithDeviceSn(context, appId, secretKey);
        String msg = String.format("授权%s, appId=%s, %s", authResult.isSucceeded() ? "成功" : "失败", appId, authResult.toString());
        showMessage(msg);
        return authResult;
    }

    private AuthResult auth(Context context, String licenceFileName) {
        AuthResult authResult = Auth.authWithLicence(context, licenceFileName);
        String msg = String.format("授权%s, licenceFileName=%s, %s", authResult.isSucceeded() ? "成功" : "失败", licenceFileName, authResult.toString());
        showMessage(msg);
        return authResult;
    }

    private void showMessage(final String msg) {
        Log.d(TAG, msg);
        ((TextView) findViewById(R.id.tips)).setText(msg);
    }

    private final PermissionHandler mPermissionHandler = new PermissionHandler(this) {
        @Override
        protected boolean shouldIgnore(String permission) {
            return false;
            // return permission.equals(Manifest.permission.WRITE_SETTINGS) //API 23 或以上, 无法通过授权对话框获得授权, 忽略之
        }

        @Override
        protected void onPermissionsDecline(String[] permissions) {
            String msg = "没有获得系统权限: " + Arrays.toString(permissions);
            showMessage(msg);
        }

        @Override
        protected void onAllPermissionGranted() {
            mAccessPermission = true;
            showPage();
        }
    };

    private void showPage() {
        if (mAccessPermission && mAuthorized) {
            // 请修改人脸识别 SDK 授权信息
            AuthResult authResult = auth(AuthActivity.this, "APP_ID"/*修改APPID为实际的值*/, "SECRET_KEY"/*修改SECRET_KEY为实际的值*/);

            if (authResult.isSucceeded()) {//授权成功

                AIThreadPool.instance().init(AuthActivity.this);//提前全局初始化, 后续的 Activity 就不必再执行初始化了

                addButton("1:N 注册(图片文件)", RegWithFileActivity.class);
                addButton("1:N 搜索(Android相机)", RetrieveWithAndroidCameraActivity.class);
            }
        }
    }

    private void addButton(String buttonText, final Class targetActivity) {
        Button button = new Button(this);
        button.setText(buttonText);
        button.setAllCaps(false);
        button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AuthActivity.this, targetActivity));
            }
        });
        ((ViewGroup) findViewById(R.id.button_container)).addView(button);
    }

    private void connect() {
        FaceKitSample.getInstance().init(getApplicationContext(), mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack());
        FaceKitSample.getInstance().connect();
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
                result.put("status", "some message when error occurred or other info message");
                return result;
            } catch (JSONException e) {
                Log.d(TAG, "Construct params failed!");
//                mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                return null;
            }
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
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
                    result.put("status", "some message when error occurred or other info message");

                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);

                    result.put("response", response);
                    return result;
                } catch (JSONException e) {
                    Log.d(TAG, "Construct params failed!");
//                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
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
            Log.d(TAG, logInfo);
            if (!reconnect) {
                FaceKitSample.getInstance().subscribeServiceTopic();
                FaceKitSample.getInstance().initAuth(new TXAuthCallBack() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAuthorized = true;
                                showPage();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Integer code, String status) {

                    }
                });
                FaceKitSample.getInstance().checkResource(FACE_IMG_DIR);
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            Log.d(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            Log.d(TAG, logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            Log.d(TAG, logInfo);
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
                Log.d(TAG, logInfo);
            } else {
                Log.d(TAG, logInfo);
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
            Log.d(TAG, logInfo);
        }

        @Override
        public void onMessageReceived(String topic, MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            Log.d(TAG, logInfo);
        }
    }

}
