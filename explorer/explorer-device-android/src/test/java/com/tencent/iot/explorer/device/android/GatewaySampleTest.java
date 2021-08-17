package com.tencent.iot.explorer.device.android;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.AssetManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.explorer.device.android.gateway.TXGatewayClient;
import com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.android.utils.TXLogImpl;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdevActionCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.unit.test.BuildConfig;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class GatewaySampleTest {

    private static final String TAG = GatewaySampleTest.class.getSimpleName();
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTGATEWAYSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_PSK; //若使用证书验证，设为null
    private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mJsonFileName = "gateway.json";
    private static String mSubDevJsonFileName = "struct.json";

    private static String mSubDev1ProductId = BuildConfig.TESTGATEWAYSAMPLE_SUB1_PRODUCT_ID;
    private static String mSubDev1DeviceName = BuildConfig.TESTGATEWAYSAMPLE_SUB1_DEV_NAME;
    private static String mSubDev1DevicePSK  = BuildConfig.TESTGATEWAYSAMPLE_SUB1_DEV_PSK;

    TXGatewayClient mConnection = null;
    TXGatewaySubdev mGatewaySubdev = null;

    @Mock
    Context context;
    @Mock
    AssetManager assetManager;
    @Mock
    InputStream inputStream;
    @Mock
    AlarmManager alarmManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs

        TXLogImpl.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        doReturn(assetManager).when(context).getAssets();

        URL resource = GatewaySampleTest.class.getClassLoader().getResource(mJsonFileName);
        InputStream inputStream = new FileInputStream(resource.getPath());
        doReturn(inputStream).when(assetManager).open(anyString());

        doReturn(alarmManager).when(context).getSystemService(Context.ALARM_SERVICE);

        mConnection = new TXGatewayClient( context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null,
                new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack());

        doReturn(assetManager).when(context).getAssets();

        URL resource1 = GatewaySampleTest.class.getClassLoader().getResource(mSubDevJsonFileName);
        InputStream inputStream1 = new FileInputStream(resource1.getPath());
        doReturn(inputStream1).when(assetManager).open(anyString());

        mGatewaySubdev = new TXGatewaySubdev(mConnection, context,  mSubDev1ProductId, mSubDev1DeviceName, mJsonFileName,
                new SelfGatewaySubdevActionCallBack(), new SelfDownStreamCallBack());
    }


    private void gatewayConnect() {
        //初始化连接
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0) {
            TXLog.i(TAG, "Using PSK");
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(context, mDevCert, mDevPriv));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mConnection.setBufferOpts(bufferOptions);
    }

    private void gatewayDisconnect() {
        TXMqttRequest mqttRequest1 = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mConnection.disConnect(mqttRequest1);
    }

    private void gatewayBindSubdev() {
        mConnection.gatewayBindSubdev(mSubDev1ProductId, mSubDev1DeviceName, mSubDev1DevicePSK);
    }

    private void gatewayUnbindSubdev() {
        mConnection.gatewayUnbindSubdev(mSubDev1ProductId, mSubDev1DeviceName);
    }

    private void gatewayOnlineSubDev() {
        mConnection.addSubdev(mGatewaySubdev); //添加子设备到网关
        mConnection.subdevOnline(mSubDev1ProductId, mSubDev1DeviceName);
    }

    private void gatewayOfflineSubDev() {
        mConnection.subdevOffline(mSubDev1ProductId, mSubDev1DeviceName);
    }

    /**
     * 实现子设备上下线，订阅成功接口
     */
    private class SelfGatewaySubdevActionCallBack extends TXGatewaySubdevActionCallBack {

        /**上线后，订阅相关主题*/
        @Override
        public void onSubDevOnline() {
            TXLog.d(TAG, "dev[%s] online!", mGatewaySubdev.mDeviceName);
            if (Status.OK != mGatewaySubdev.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)) {
                TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
            }
            if (Status.OK != mGatewaySubdev.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
                TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
            }
            if (Status.OK != mGatewaySubdev.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
                TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
            }
            gatewayOnlineSubDevSuccess = true;
            unlock();
        }

        @Override
        public void onSubDevOffline() {
            gatewayOfflineSubDevSuccess = true;
            unlock();
        }

        @Override
        public void onSubDevBind(int result) {
            TXLog.d(TAG,"onSubDevBind:result:" + result);
        }

        @Override
        public void onSubDevUnbind(int result) {
            TXLog.d(TAG,"onSubDevUnbind:result:" + result);
        }

        /**订阅属性下行主题成功则获取状态和上报信息，启动周期性上报属性线程*/
        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                TXLog.e(TAG,logInfo);
            } else {
                TXLog.d(TAG,logInfo);
            }
        }

    }

    /**
     * 实现下行消息处理的回调接口
     */
    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            TXLog.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //可根据自己需求进行处理状态和控制信息的获取结果
            TXLog.d(TAG, "event down stream message received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            TXLog.d(TAG, "control down stream message received : " + msg);
            //do something

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                TXLog.e(TAG, "Construct params failed!");
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
                    TXLog.e(TAG, "Construct params failed!");
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
            TXLog.d(TAG, "unbind device received : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            //用户绑定设备的通知消息
            TXLog.d(TAG, "bind device received : " + msg);
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
            TXLog.i(TAG, logInfo);
            if (!reconnect){unlock();}
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            cause.printStackTrace();
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            TXLog.i(TAG, logInfo);
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
                TXLog.e(TAG, logInfo);
            } else {
                TXLog.i(TAG, logInfo);
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
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            TXLog.i(TAG, logInfo);
            if (message.toString().contains("\"type\":\"unbind\"") && message.toString().contains(mSubDev1DeviceName)) {
                gatewayUnbindSubdevSuccess = true;
                unlock();
            } else if (message.toString().contains("\"type\":\"bind\"") && message.toString().contains(mSubDev1DeviceName)) {
                gatewayBindSubdevSuccess = true;
                unlock();
            }
        }
    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);

    private static boolean gatewayBindSubdevSuccess = false;
    private static boolean gatewayOnlineSubDevSuccess = false;
    private static boolean gatewayUnbindSubdevSuccess = false;
    private static boolean gatewayOfflineSubDevSuccess = false;

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
    public void testGatewayConnect() {

        gatewayConnect();
        lock();
        assertSame(mConnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
        TXLog.d(TAG, "after connect");

        gatewayUnbindSubdev();
        lock();
        assertTrue(gatewayUnbindSubdevSuccess);
        TXLog.d(TAG, "after setSubDevUnbinded");

        gatewayBindSubdev();
        lock();
        assertTrue(gatewayBindSubdevSuccess);
        TXLog.d(TAG, "after setSubDevBinded");

        gatewayOnlineSubDev();
        lock();
        assertTrue(gatewayOnlineSubDevSuccess);
        TXLog.d(TAG, "after gatewaySubdevOnline");

        gatewayOfflineSubDev();
        lock();
        assertTrue(gatewayOfflineSubDevSuccess);
        TXLog.d(TAG, "after gatewaySubdevOffline");

        gatewayDisconnect();
        lock();
        TXLog.d(TAG, "after disconnect");
        assertSame(mConnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kDisconnected);
    }
}