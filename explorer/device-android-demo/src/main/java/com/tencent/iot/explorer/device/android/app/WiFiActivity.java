package com.tencent.iot.explorer.device.android.app;

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.iot.explorer.device.android.app.utils.CloseWifiHotspotCallback;
import com.tencent.iot.explorer.device.android.app.utils.CreateWifiHotspotCallback;
import com.tencent.iot.explorer.device.android.app.utils.WiFiHostpotUtil;
import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.android.softAp.SoftApConfigWiFi;
import com.tencent.iot.explorer.device.android.softAp.SoftApConfigWiFiCallback;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.rtc.utils.NetWorkStateReceiver;
import com.tencent.iot.explorer.device.rtc.utils.WifiUtils;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class WiFiActivity extends AppCompatActivity implements View.OnClickListener, NetWorkStateReceiver.NetworkStateReceiverListener {

    private static final String TAG = WiFiActivity.class.getSimpleName();

    private Button mStartSoftApBtn;
    private TextView mLogInfoText;

    private WifiManager wifiManager;

    // Default testing parameters
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = BuildConfig.SUB_PRODUCT_ID;
    private String mDevName = BuildConfig.SUB_DEV_NAME;
    private String mDevPSK  = BuildConfig.SUB_DEV_PSK; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String
    private static AtomicInteger requestID = new AtomicInteger(0);

    private final static String mJsonFileName = "struct.json";

    /**日志保存的路径*/
    private final static String mLogPath = Environment.getExternalStorageDirectory().getPath() + "/tencent/";

    /**
     * MQTT连接实例
     */
    private TXDataTemplateClient mMqttConnection;

    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "TEST";
    private static final String WIFI_HOTSPOT_PASSWORD = "123456789";
    private SoftApConfigWiFi softApConfigWiFi;

    private WiFiHostpotUtil wiFiHostpotUtil;

    private NetWorkStateReceiver netWorkStateReceiver;

    private volatile boolean readyToConnectMqtt = false;
    private volatile boolean readyToConnectWiFi = false;

    private String targetWiFiSsid = "";
    private String targetWiFiPwd = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        initVIew();

        // get wifiManager register network broadcast
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        startNetworkBroadcastReceiver(this);
        registerWiFiEnableBroadcastReceiver(this);
    }

    private void initVIew() {
        mStartSoftApBtn = (Button) findViewById(R.id.btn_start_softAp);
        mLogInfoText = (TextView) findViewById(R.id.log_info);

        mStartSoftApBtn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        registerNetworkBroadcastReceiver(this);
        registerWiFiEnableBroadcastReceiver(this);
        TXLog.e(TAG, "注册netWorkStateReceiver");
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterNetworkBroadcastReceiver(this);
        unregisterWiFiEnableBroadcastReceiver(this);
        TXLog.e(TAG, "注销netWorkStateReceiver");
        super.onPause();
    }

    public void startNetworkBroadcastReceiver(Context currentContext) {
        netWorkStateReceiver = new NetWorkStateReceiver();
        netWorkStateReceiver.addListener((NetWorkStateReceiver.NetworkStateReceiverListener) currentContext);
        registerNetworkBroadcastReceiver(currentContext);
    }

    /**
     * Register the NetworkStateReceiver with your activity
     */
    public void registerNetworkBroadcastReceiver(Context currentContext) {
        currentContext.registerReceiver(netWorkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Register the WiFiEnableReceiver with your activity
     */
    public void registerWiFiEnableBroadcastReceiver(Context currentContext) {
        currentContext.registerReceiver(recevier, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
    }

    /**
     * Unregister the NetworkStateReceiver with your activity
     */
    public void unregisterNetworkBroadcastReceiver(Context currentContext) {
        currentContext.unregisterReceiver(netWorkStateReceiver);
    }

    /**
     * Unregister the WiFiEnableReceiver with your activity
     */
    public void unregisterWiFiEnableBroadcastReceiver(Context currentContext) {
        currentContext.unregisterReceiver(recevier);
    }

    @Override
    public void networkAvailable() {
        TXLog.e(TAG, "networkAvailable");
        if (readyToConnectMqtt) {
//            // 5、连接目标WiFi成功，设备连接mqtt。
            connect();
            readyToConnectMqtt = false;
        }
    }

    @Override
    public void networkUnavailable() {
        TXLog.e(TAG, "networkUnavailable");
    }

    BroadcastReceiver recevier = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()== WifiManager.WIFI_STATE_CHANGED_ACTION) {
                switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WIFI_STATE_UNKNOWN)) {
                    case WIFI_STATE_DISABLED:{
                        printLogInfo(TAG, "WiFi disabled" , mLogInfoText, TXLog.LEVEL_INFO);
                        break;
                    }
                    case WIFI_STATE_DISABLING:{
                        printLogInfo(TAG, "WiFi disabling" , mLogInfoText, TXLog.LEVEL_INFO);
                        break;
                    }
                    case WIFI_STATE_ENABLED :{
                        printLogInfo(TAG, "WiFi enabled" , mLogInfoText, TXLog.LEVEL_INFO);
                        if (readyToConnectWiFi) {
                            // 4、开启系统WiFi开关成功，连接目标WiFi
                            WifiUtils.connectWifiApByNameAndPwd(getApplicationContext(), targetWiFiSsid, targetWiFiPwd, new WifiUtils.WifiConnectCallBack() {
                                @Override
                                public void connectResult(boolean connectResult) {
                                    printLogInfo(TAG, "WifiUtils connectResult: " + connectResult , mLogInfoText, TXLog.LEVEL_INFO);
                                }
                            });
                            readyToConnectWiFi = false;
                            readyToConnectMqtt = true;
                        }
                        break;
                    }
                    case WIFI_STATE_ENABLING:{
                        printLogInfo(TAG, "WiFi enabling" , mLogInfoText, TXLog.LEVEL_INFO);
                        break;
                    }
                    case WIFI_STATE_UNKNOWN:{
                        printLogInfo(TAG, "WiFi state unknown" , mLogInfoText, TXLog.LEVEL_INFO);
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_softAp:
                createWifiHotspot();
                break;
        }
    }

    private void createWifiHotspot() {
        if (wiFiHostpotUtil == null) {
            wiFiHostpotUtil = new WiFiHostpotUtil(this);
        }

        // 1、设备创建WiFi热点。
        wiFiHostpotUtil.createWifiHotspot(WIFI_HOTSPOT_SSID, WIFI_HOTSPOT_PASSWORD, new CreateWifiHotspotCallback() {
            @Override
            public void onCreateWifiHotspotSuccess(String ssid, String pwd) { //成功打开android设备的热点，8.0以上创建的热点ssid和password是随机的。
                mMqttConnection = new TXDataTemplateClient(getApplicationContext(), mBrokerURL, mProductID, mDevName, mDevPSK,null,null, true, new WiFiActivity.SelfMqttLogCallBack(), new WiFiActivity.SelfMqttActionCallBack(),
                        mJsonFileName, new WiFiActivity.SelfDownStreamCallBack());

                // 2、开启UDP服务，监听app或小程序发来的目标WiFi的ssid和password。
                softApConfigWiFi = new SoftApConfigWiFi(getApplicationContext(), 8266, mMqttConnection, new SoftApConfigWiFiCallback() {
                    @Override
                    public void requestConnectWifi(String ssid, String password) { //收到app或小程序发来的目标WiFi的ssid和password，准备连接目标WiFi
                        targetWiFiSsid = ssid;
                        targetWiFiPwd = password;
                        // 3、关闭WiFi热点，开启系统WiFi开关。// 因硬件关闭WiFi热点 立即调用硬件开启wifi 会失败，故延迟3s后再链接。
                        closeWifiHotspot();
                        TimerTask otherEnterRoomTask = new TimerTask(){
                            public void run(){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!wifiManager.isWifiEnabled()) {
                                            boolean result = wifiManager.setWifiEnabled(true);
                                            printLogInfo(TAG, "wifiManager.setWifiEnabled connectResult result:" + result , mLogInfoText, TXLog.LEVEL_INFO);
                                        }
                                        readyToConnectWiFi = true;
                                    }
                                });
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(otherEnterRoomTask, 3000);
                    }
                });
                softApConfigWiFi.startUdpConnect();
                printLogInfo(TAG, "热点已开启 SSID:" + ssid + " password:" + pwd , mLogInfoText, TXLog.LEVEL_INFO);
            }

            @Override
            public void onCreateWifiHotspotFailure(String code, String errorMsg, Exception e) {
                printLogInfo(TAG, "创建热点失败: " + errorMsg , mLogInfoText, TXLog.LEVEL_ERROR);
            }
        });
    }

    private void connect() {
        if (mMqttConnection == null) {
            return;
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            TXLog.i(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-rtc sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);
    }

    private void closeWifiHotspot() {
        if (wiFiHostpotUtil == null) {
            wiFiHostpotUtil = new WiFiHostpotUtil(this);
        }
        wiFiHostpotUtil.closeWifiHotspot(new CloseWifiHotspotCallback() {
            @Override
            public void onCloseWifiHotspotSuccess() {
                softApConfigWiFi.stopUdpConnect();
                printLogInfo(TAG, "onCloseWifiHotspotSuccess", mLogInfoText, TXLog.LEVEL_INFO);
            }

            @Override
            public void onCloseWifiHotspotFailure(String errorMsg, Exception e) {
                printLogInfo(TAG, "onCloseWifiHotspotFailure errorMsg:" + errorMsg, mLogInfoText, TXLog.LEVEL_ERROR);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     * 实现TXMqttLogCallBack回调接口
     */
    private class SelfMqttLogCallBack extends TXMqttLogCallBack {

        @Override
        public String setSecretKey() {
            String secertKey;
            if (mDevPSK != null && mDevPSK.length() != 0) {  //密钥认证
                secertKey = mDevPSK;
                secertKey = secertKey.length() > 24 ? secertKey.substring(0,24) : secertKey;
                return secertKey;
            } else {
                StringBuilder builder = new StringBuilder();
                if (mDevPriv != null && mDevPriv.length() != 0) { //动态注册, 从DevPriv中读取
                    builder.append(mDevPriv);
                } else { //证书认证，从证书文件中读取
                    return null;
                }
                String privateKey = builder.toString();
                if (privateKey.contains("-----BEGIN PRIVATE KEY-----")) {
                    secertKey = privateKey;
                } else {
                    secertKey = null;
                    printLogInfo(TAG,"Invaild Private Key File.", mLogInfoText);
                }
            }
            return secertKey;
        }

        @Override
        public void printDebug(String message){
            printLogInfo(TAG, message, mLogInfoText);
            //TXLog.d(TAG,message);
        }

        @Override
        public boolean saveLogOffline(String log){
            //判断SD卡是否可用
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                printLogInfo(TAG, "saveLogOffline not ready", mLogInfoText);
                return false;
            }

            String logFilePath = mLogPath + mProductID + mDevName + ".log";

            TXLog.i(TAG, "Save log to %s", logFilePath);

            try {
                BufferedWriter wLog = new BufferedWriter(new FileWriter(new File(logFilePath), true));
                wLog.write(log);
                wLog.flush();
                wLog.close();
                return true;
            } catch (IOException e) {
                String logInfo = String.format("Save log to [%s] failed, check the Storage permission!", logFilePath);
                printLogInfo(TAG,logInfo, mLogInfoText);
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public String readOfflineLog(){
            //判断SD卡是否可用
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                printLogInfo(TAG, "readOfflineLog not ready", mLogInfoText);
                return null;
            }

            String logFilePath = mLogPath + mProductID + mDevName + ".log";

            TXLog.i(TAG, "Read log from %s", logFilePath);

            try {
                BufferedReader logReader = new BufferedReader(new FileReader(logFilePath));
                StringBuilder offlineLog = new StringBuilder();
                int data;
                while (( data = logReader.read()) != -1 ) {
                    offlineLog.append((char)data);
                }
                logReader.close();
                return offlineLog.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public boolean delOfflineLog(){

            //判断SD卡是否可用
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                printLogInfo(TAG, "delOfflineLog not ready", mLogInfoText);
                return false;
            }

            String logFilePath = mLogPath + mProductID + mDevName + ".log";

            File file = new File(logFilePath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    return true;
                }
            }
            return false;
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
            if (softApConfigWiFi != null) {
                // 6、设备上线成功，绑定目标app传来的token
                softApConfigWiFi.appBindToken();
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            cause.printStackTrace();
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
