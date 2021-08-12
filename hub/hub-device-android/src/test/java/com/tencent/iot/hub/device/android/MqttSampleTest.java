package com.tencent.iot.hub.device.android;

import android.app.AlarmManager;
import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Config(sdk = 28, manifest=Config.NONE)
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

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    TXMqttConnection mqttconnection = null;

    @Mock
    Context context;
    @Mock
    AlarmManager alarmManager;

    @Mock
    TXMqttLogCallBack mqttLogCallBack;
    @Mock
    TXMqttActionCallBack mqttActionCallBack;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs

        TXLogImpl.init(InstrumentationRegistry.getInstrumentation().getTargetContext());

        doReturn(alarmManager).when(context).getSystemService(Context.ALARM_SERVICE);

        mqttconnection = new TXMqttConnection(context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,true, mqttLogCallBack, mqttActionCallBack);
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

    private void disconnect() {
        MQTTRequest mqttRequest = new MQTTRequest("disconnect", requestID.getAndIncrement());
        mqttconnection.disConnect(mqttRequest);
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

    private void unSubscribeTopic() {
        // 主题
        String topic = mTestTopic;
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
        Log.d(TAG, "Start to unSubscribe" + topic);
        // 取消订阅主题
        mqttconnection.unSubscribe(topic, mqttRequest);
    }

    @Test
    public void testMqttConnect() {

        connect();

        verify(mqttActionCallBack, timeout(2000).times(1)).onConnectCompleted(Matchers.eq(Status.OK), Matchers.eq(false), Mockito.any(), Mockito.anyString());

        subscribeTopic();

        verify(mqttActionCallBack, timeout(2000).times(1)).onSubscribeCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

//        propertyReport();
//
//        verify(mqttActionCallBack, timeout(2000).times(1)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        propertyGetStatus();
//
//        verify(mqttActionCallBack, timeout(2000).times(2)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        propertyReportInfo();
//
//        verify(mqttActionCallBack, timeout(2000).times(3)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        propertyClearControl();
//
//        verify(mqttActionCallBack, timeout(2000).times(4)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        eventSinglePost();
//
//        verify(mqttActionCallBack, timeout(2000).times(5)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        eventsPost();
//
//        verify(mqttActionCallBack, timeout(2000).times(6)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        unSubscribeTopic();
//
//        verify(mqttActionCallBack, timeout(2000).times(4)).onUnSubscribeCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());
//
//        disconnect();
//
//        verify(mqttActionCallBack, timeout(2000).times(1)).onDisconnectCompleted(Matchers.eq(Status.OK), Mockito.any(), Mockito.anyString());
    }
}