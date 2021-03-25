package com.tencent.iot.explorer.device.face;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.cloud.ai.fr.RegWithFileActivity;
import com.tencent.cloud.ai.fr.business.thread.AIThreadPool;
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
    static {
        System.loadLibrary("YTCommon");
        System.loadLibrary("YTFaceFeature");
        System.loadLibrary("YTFaceAlignment");
        System.loadLibrary("YTFaceQuality");
        System.loadLibrary("YTFaceTracker");
        System.loadLibrary("YTFaceQualityPro");
        System.loadLibrary("YTFaceRetrieval");
    }

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

    private Button mConnectBtn;
    private Spinner mSpinner;
    private EditText mItemText;
    private TextView mLogInfoText;
    private Button mRegisterBtn;
    private Button mSearchBtn;

    private final static String BROKER_URL = "broker_url";
    private final static String PRODUCT_ID = "product_id";
    private final static String DEVICE_NAME = "dev_name";
    private final static String DEVICE_PSK = "dev_psk";
    private final static String DEVICE_CERT = "dev_cert";
    private final static String DEVICE_PRIV  = "dev_priv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);

        mConnectBtn = findViewById(R.id.connect);
        mItemText = findViewById(R.id.editText2);
        mSpinner = findViewById(R.id.spinner4);
        mLogInfoText = findViewById(R.id.log_info);
        mRegisterBtn = findViewById(R.id.btn_register);
        mSearchBtn = findViewById(R.id.btn_search);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] items = getResources().getStringArray(R.array.setup_items);
                String paraStr = mItemText.getText().toString();

                if (position == 0) {
                    return;
                }

                if (paraStr.equals("")) {
                    return;
                }

                Log.d("TXMQTT", "Set " + items[position] + " to " + paraStr);
                Toast toast = Toast.makeText(getApplicationContext(), "Set " + items[position] + " to " + paraStr, Toast.LENGTH_LONG);
                toast.show();
                SharedPreferences sharedPreferences = getSharedPreferences("config",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                switch(position) {
                    case 1:
                        mBrokerURL = paraStr;
                        editor.putString(BROKER_URL, mBrokerURL);
                        break;
                    case 2:
                        mProductID = paraStr;
                        editor.putString(PRODUCT_ID, mProductID);
                    case 3:
                        mDevName = paraStr;
                        editor.putString(DEVICE_NAME, mDevName);
                        break;
                    case 4:
                        mDevPSK = paraStr;
                        editor.putString(DEVICE_PSK, mDevPSK);
                        break;
                    default:
                        break;
                }
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FaceKitSample.getInstance().isAuthoried()) {
                    startActivity(new Intent(AuthActivity.this, RegWithFileActivity.class));
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "授权失败", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FaceKitSample.getInstance().isAuthoried()) {
                    startActivity(new Intent(AuthActivity.this, RetrieveWithAndroidCameraActivity.class));
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "授权失败", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

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

    private void showMessage(final String msg) {
        Log.d(TAG, msg);
        ((TextView) findViewById(R.id.log_info)).setText(msg);
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

        }
    };

    private void showButton() {

        AIThreadPool.instance().init(AuthActivity.this);//提前全局初始化, 后续的 Activity 就不必再执行初始化了
        mRegisterBtn.setVisibility(View.VISIBLE);
        mSearchBtn.setVisibility(View.VISIBLE);
    }

    private void connect() {
        SharedPreferences settings = getSharedPreferences("config", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BROKER_URL, mBrokerURL);
        editor.putString(PRODUCT_ID, mProductID);
        editor.putString(DEVICE_NAME, mDevName);
        editor.putString(DEVICE_PSK, mDevPSK);
        editor.commit();

        mBrokerURL = settings.getString(BROKER_URL, mBrokerURL);
        mProductID = settings.getString(PRODUCT_ID, mProductID);
        mDevName = settings.getString(DEVICE_NAME, mDevName);
        mDevPSK = settings.getString(DEVICE_PSK, mDevPSK);

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
                printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
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
            printLogInfo(TAG, logInfo, mLogInfoText);
            Log.d(TAG, logInfo);
            if (!reconnect) {
                FaceKitSample.getInstance().subscribeServiceTopic();
                FaceKitSample.getInstance().initAuth(new TXAuthCallBack() {
                    @Override
                    public void onSuccess() {
                        printLogInfo(TAG, "initAuth success", mLogInfoText);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showButton();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Integer code, String status) {
                        String logInfo = String.format("initAuth onFailure, code[%d], status[%s]", code, status);
                        printLogInfo(TAG, logInfo, mLogInfoText);
                    }
                });
                FaceKitSample.getInstance().checkResource(FACE_IMG_DIR);
                FaceKitSample.getInstance().reportOfflineSysRetrievalResultData();
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
//            printLogInfo(TAG, logInfo, mLogInfoText);
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
                printLogInfo(TAG, logInfo, mLogInfoText);
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
        public void onMessageReceived(String topic, MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            printLogInfo(TAG, logInfo, mLogInfoText);
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
