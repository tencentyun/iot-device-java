
package com.tencent.iot.explorer.device.broadcast;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.multidex.MultiDex;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.tencent.iot.explorer.device.android.app.BuildConfig;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.broadcast.data_template.BroadcastDataTemplateSample;
import com.tencent.iot.explorer.device.broadcast.data_template.callback.BroadcastCallback;
import com.tencent.iot.explorer.device.broadcast.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.broadcast.ui.broadcastcall.BroadcastAudioCallActivity;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.rtc.TRTCMainActivity;
import com.tencent.iot.explorer.device.rtc.utils.NetWorkStateReceiver;
import com.tencent.iot.explorer.device.rtc.utils.ZXingUtils;
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


public class BroadCastMainActivity extends AppCompatActivity {

    private static final String TAG = BroadCastMainActivity.class.getSimpleName();

    private BroadcastDataTemplateSample mDataTemplateSample;

    private ImageView mQRCodeImgView;
    private Button mConnectBtn;
    private Button mCloseConnectBtn;

    private EditText mBrokerURLEditText;
    private EditText mProductIdEditText;
    private EditText mDevNameEditText;
    private EditText mDevPSKEditText;

    private TextView mLogInfoText;

    // Default testing parameters
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = BuildConfig.SUB_PRODUCT_ID;
    private String mDevName = BuildConfig.SUB_DEV_NAME;
    private String mDevPSK  = BuildConfig.SUB_DEV_PSK; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String


    private final static String mJsonFileName = "TRTC_watch.json";

    private static final int REQUEST_PERMISSTION = 1;

    private final static String BROKER_URL = "broker_url";
    private final static String PRODUCT_ID = "product_id";
    private final static String DEVICE_NAME = "dev_name";
    private final static String DEVICE_PSK = "dev_psk";

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private NetWorkStateReceiver netWorkStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_main);
        netWorkStateReceiver = new NetWorkStateReceiver();

        //日志功能开启写权限
        try {
            for (String ele: PERMISSIONS_STORAGE) {
                int granted = ActivityCompat.checkSelfPermission(this, ele);
                if (granted != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                    break;
                } else {
                    initLogConfigurator();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        MultiDex.install(this);

        mQRCodeImgView = findViewById(R.id.iv_qrcode);
        mConnectBtn = findViewById(R.id.connect);
        mCloseConnectBtn = findViewById(R.id.close_connect);
        mLogInfoText = findViewById(R.id.log_info);

        mBrokerURLEditText = findViewById(R.id.et_broker_url);
        mProductIdEditText = findViewById(R.id.et_productId);
        mDevNameEditText = findViewById(R.id.et_deviceName);
        mDevPSKEditText = findViewById(R.id.et_devicePsk);

        SharedPreferences settings = getSharedPreferences("broadcast_config", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = settings.edit();
        mBrokerURL = settings.getString(BROKER_URL, mBrokerURL);
        mProductID = settings.getString(PRODUCT_ID, mProductID);
        mDevName = settings.getString(DEVICE_NAME, mDevName);
        mDevPSK = settings.getString(DEVICE_PSK, mDevPSK);
        editor.commit();

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

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkInput()) {
                    return;
                }
                SharedPreferences settings = getSharedPreferences("broadcast_config", Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(BROKER_URL, mBrokerURL);
                editor.putString(PRODUCT_ID, mProductID);
                editor.putString(DEVICE_NAME, mDevName);
                editor.putString(DEVICE_PSK, mDevPSK);
                editor.commit();
                if (mDataTemplateSample != null) {
                    return;
                }
                mDataTemplateSample = new BroadcastDataTemplateSample(BroadCastMainActivity.this, mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack(), new BroadcastCallback() {
                    @Override
                    public void joinBroadcast(RoomKey roomKey) {
                        BroadcastAudioCallActivity.startBeingCall(BroadCastMainActivity.this, roomKey, "userid", "agent");
                        Log.d("tag" , "joinBroadcast called");
                    }
                });
                mDataTemplateSample.connect();
            }
        });

        mCloseConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                mDataTemplateSample.disconnect();
                mDataTemplateSample = null;
            }
        });

        initPermission();
    }

    private void initLogConfigurator() {
        // 下面配置是为了让sdk中用log4j记录的日志可以输出至logcat
        LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "explorer-broadcast-demo.log");
        logConfigurator.configure();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "必要权限申请失败");
                    finish();
                } else {
                    initLogConfigurator();
                    break;
                }
            }
        }
    }

    private boolean checkInput() {
        String inputBrokerURL = String.valueOf(mBrokerURLEditText.getText());
        if (inputBrokerURL.equals("")) {
            mBrokerURL = null;
        } else {
            mBrokerURL = inputBrokerURL;
        }

        String inputProductId = String.valueOf(mProductIdEditText.getText());
        if (inputProductId.equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入productId", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mProductID = inputProductId;
        }

        String inputDevName = String.valueOf(mDevNameEditText.getText());
        if (inputDevName.equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入deviceName", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mDevName = inputDevName;
        }

        String inputDevPSK = String.valueOf(mDevPSKEditText.getText());
        if (inputDevPSK.equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入devicePsk", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mDevPSK = inputDevPSK;
        }
        return true;
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

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            //用户删除设备的通知消息
            Log.d(TAG, "unbind device received : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            //用户绑定设备的通知消息
            Log.d(TAG, "bind device received : " + msg);
        }
    }

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);

            if (mDataTemplateSample != null) {
                if (!reconnect) {
                    mDataTemplateSample.subscribeTopic();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 设置适配器，刷新展示用户列表
                            mQRCodeImgView.setImageBitmap(ZXingUtils.createQRCodeBitmap(mDataTemplateSample.generateDeviceQRCodeContent(), 200, 200,"UTF-8","H", "1", Color.BLACK, Color.WHITE));
                        }
                    });
                }
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg, Throwable cause) {
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
            //get status
            if(Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
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
            printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            printLogInfo(TAG, logInfo, mLogInfoText);
        }
    }

    @Override
    protected void onDestroy() {
        if (mDataTemplateSample != null) {
            mDataTemplateSample.disconnect();
        }
        mDataTemplateSample = null;
        super.onDestroy();
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