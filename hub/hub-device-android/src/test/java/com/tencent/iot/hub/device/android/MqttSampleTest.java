package com.tencent.iot.hub.device.android;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.android.core.util.TXLogImpl;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class MqttSampleTest {

    private static final String TAG = MqttSampleTest.class.getSimpleName();

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTMQTTSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTMQTTSAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTMQTTSAMPLE_DEVICE_PSK; //若使用证书验证，设为null
    private static String mTestTopic = BuildConfig.TESTMQTTSAMPLE_TEST_TOPIC;

    private static String mCertFilePath = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mPrivKeyFilePath = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String

    /**日志保存的路径*/
    private final static String mLogPath = Environment.getExternalStorageDirectory().getPath() + "/tencent/";

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    TXMqttConnection mqttconnection = null;

    @Mock
    Context context;
    @Mock
    AlarmManager alarmManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs

        TXLogImpl.init(InstrumentationRegistry.getInstrumentation().getTargetContext());

        doReturn(alarmManager).when(context).getSystemService(Context.ALARM_SERVICE);

        mqttconnection = new TXMqttConnection(context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,false, new SelfMqttLogCallBack(), new SelfMqttActionCallBack());
    }

    private void connect() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        //mCertFilePath客户端证书文件名  mDevPSK是设备秘钥
        if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
            Loggor.info(TAG, "Using cert stream " + mDevPriv + "  " + mDevCert);
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        } else if (mDevPSK != null && mDevPSK.length() != 0){
            Loggor.info(TAG, "Using PSK");
            // options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 hub-device-java sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            Loggor.info(TAG, "Using cert file");
            String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
        }

        MQTTRequest mqttRequest = new MQTTRequest("connect", requestID.getAndIncrement());
        mqttconnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mqttconnection.setBufferOpts(bufferOptions);
    }

    private void subscribeTopic() {
        // 主题
        String topic = mTestTopic;
        // QoS等级 支持QoS 0和1
        int qos = TXMqttConstants.QOS1;
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());

        Log.d(TAG, "sub topic is " + topic);

        // 订阅主题
        mqttconnection.subscribe(topic, qos, mqttRequest);
    }

    private void publishTopic() {
        // 要发布的数据
        Map<String, String> data = new HashMap<String, String>();
        // 车辆类型
        data.put("car_type", "suv");
        // 车辆油耗
        data.put("oil_consumption", "6.6");
        // 车辆最高速度
        data.put("maximum_speed", "205");
        // 温度信息
        data.put("temperature", "25");
        // MQTT消息
        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            Loggor.error(TAG, e.getMessage()+"pack json data failed!");
        }
        message.setQos(TXMqttConstants.QOS1);
        message.setPayload(jsonObject.toString().getBytes());

        // 用户上下文（请求实例）

        Loggor.debug(TAG, "pub topic " + mTestTopic + message);
        // 发布主题
        mqttconnection.publish(mTestTopic, message, null);
    }

    private void unSubscribeTopic() {
        // 主题
        String topic = mTestTopic;
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
        Log.d(TAG, "Start to unSubscribe" + topic);
        // 取消订阅主题
        mqttconnection.unSubscribe(topic, mqttRequest);
    }

    private void subscribeRRPCTopic() {
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
        // 订阅主题
        mqttconnection.subscribeRRPCTopic(TXMqttConstants.QOS0, mqttRequest);
    }

    private void subscribeBroadCastTopic() {
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
        // 订阅广播主题 Topic
        mqttconnection.subscribeBroadcastTopic(TXMqttConstants.QOS1, mqttRequest);
    }

    private void disconnect() {
        MQTTRequest mqttRequest = new MQTTRequest("disconnect", requestID.getAndIncrement());
        mqttconnection.disConnect(mqttRequest);
    }

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
                    AssetManager assetManager = context.getAssets();
                    if (assetManager == null) {
                        return null;
                    }
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(assetManager.open(mPrivKeyFilePath)));
                        String str;
                        while((str = reader.readLine()) != null){
                            builder.append(str);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Get Private Key failed, cannot open Private Key Files.");
                        return null;
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                String privateKey = builder.toString();
                if (privateKey.contains("-----BEGIN PRIVATE KEY-----")) {
                    secertKey = privateKey;
                } else {
                    secertKey = null;
                    Log.e(TAG,"Invaild Private Key File.");
                }
            }
            return secertKey;
        }

        @Override
        public void printDebug(String message){
            Log.d(TAG, message);
        }

        @Override
        public boolean saveLogOffline(String log){
            //判断SD卡是否可用
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "saveLogOffline not ready");
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
                Log.e(TAG,logInfo);
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public String readOfflineLog(){
            //判断SD卡是否可用
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "readOfflineLog not ready");
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
                Log.e(TAG, "delOfflineLog not ready");
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
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            Log.i(TAG, logInfo);
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            cause.printStackTrace();
            Log.e(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            Log.i(TAG, logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            Log.i(TAG, logInfo);
            if (status == Status.OK && Arrays.toString(token.getTopics()).contains(mTestTopic)) {
                publishTopicSuccess = true;
                unlock();
            }
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                Log.e(TAG, logInfo);
            } else {
                Log.i(TAG, logInfo);
                if (Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)){ // 订阅mTestTopic成功
                    subscribeTopicSuccess = true;
                    unlock();
                } else if (Arrays.toString(asyncActionToken.getTopics()).contains("rrpc/rxd")) { // 订阅rrpc Topic成功
                    subscribeRRPCTopicSuccess = true;
                    unlock();
                } else if (Arrays.toString(asyncActionToken.getTopics()).contains("broadcast/rxd")) { // broadcast Topic成功
                    subscribeBroadCastTopicSuccess = true;
                    unlock();
                }
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            Log.i(TAG, logInfo);
            if (status == Status.OK && Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)) {
                unSubscribeTopicSuccess = true;
                unlock();
            }
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            Log.i(TAG, logInfo);
        }
    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);

    private static boolean subscribeTopicSuccess = false;
    private static boolean publishTopicSuccess = false;
    private static boolean unSubscribeTopicSuccess = false;
    private static boolean subscribeRRPCTopicSuccess = false;
    private static boolean subscribeBroadCastTopicSuccess = false;

    private static void lock() {
        latch = new CountDownLatch(COUNT);
        try {
            latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void unlock() {
        latch.countDown();// 回调执行完毕，唤醒主线程
    }

    @Test
    public void testMqttConnect() {

        connect();
        lock();
        assertSame(mqttconnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
        Loggor.debug(TAG, "after connect");

        subscribeTopic();
        lock();
        assertTrue(subscribeTopicSuccess);
        Loggor.debug(TAG, "after subscribe");

        publishTopic();
        lock();
        assertTrue(publishTopicSuccess);
        Loggor.debug(TAG, "after publish");

        unSubscribeTopic();
        lock();
        assertTrue(unSubscribeTopicSuccess);
        Loggor.debug(TAG, "after unSubscribe");

        subscribeRRPCTopic();
        lock();
        assertTrue(subscribeRRPCTopicSuccess);
        Loggor.debug(TAG, "after subscribeRRPCTopic");

        subscribeBroadCastTopic();
        lock();
        assertTrue(subscribeBroadCastTopicSuccess);
        Loggor.debug(TAG, "after subscribeBroadCastTopic");

        disconnect();
        lock();
        assertSame(mqttconnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kDisconnected);
        Loggor.debug(TAG, "after disconnect");
    }
}