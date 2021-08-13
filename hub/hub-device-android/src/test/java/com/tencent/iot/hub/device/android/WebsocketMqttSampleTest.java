package com.tencent.iot.hub.device.android;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.TXLogImpl;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.ConnectionState;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketActionCallback;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketManager;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Device Websocket Mqtt connect sample
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class WebsocketMqttSampleTest {

    private static final String TAG = WebsocketMqttSampleTest.class.getSimpleName();

    private static String mProductID = BuildConfig.TESTWEBSOCKETMQTTSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTWEBSOCKETMQTTSAMPLE_DEVICE_NAME;
    private static String mDevPSK = BuildConfig.TESTWEBSOCKETMQTTSAMPLE_DEVICE_PSK;
    private static String mCertFilePath = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mPrivKeyFilePath = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String

    @Mock
    TXWebSocketActionCallback webSocketActionCallback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs

        TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setTXWebSocketActionCallback(webSocketActionCallback);
    }

    private static void websocketdisconnect() {
        try {
            Thread.sleep(2000);
            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).disconnect();
        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void websocketConnect() {

        try {
            MqttConnectOptions conOptions = new MqttConnectOptions();
            conOptions.setCleanSession(true);

            if (mDevPSK != null && mDevPSK.length() != 0) {
                Loggor.info(TAG, "Using PSK");
            } else if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
                Loggor.info(TAG, "Using cert stream " + mDevPriv + "  " + mDevCert);
                conOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
            } else {
                Loggor.info(TAG, "Using cert file");
                String workDir = System.getProperty("user.dir") + "/src/test/resources/";
                conOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
            }

            conOptions.setConnectionTimeout(8);
            conOptions.setKeepAliveInterval(60);
            conOptions.setAutomaticReconnect(true);

            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setMqttConnectOptions(conOptions);

            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).connect();

        } catch (MqttException e) {
            e.printStackTrace();
            Loggor.error(TAG, "MqttException " + e.toString());
        }
    }

    @Test
    public void testWebsocketMqttConnect() {
        // Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
        websocketConnect();

        verify(webSocketActionCallback, timeout(2000).times(1)).onConnected();

        websocketdisconnect();

        verify(webSocketActionCallback, timeout(2000).times(1)).onDisconnected();
    }
}
