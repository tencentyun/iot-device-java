package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Device Websocket Mqtt connect sample
 */
public class WebsocketMqttSampleTest {

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketMqttSampleTest.class);
    private static final String TAG = WebsocketMqttSampleTest.class.getSimpleName();

    private static String mProductID = BuildConfig.TESTWEBSOCKETMQTTSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTWEBSOCKETMQTTSAMPLE_DEVICE_NAME;
    private static String mDevPSK = BuildConfig.TESTWEBSOCKETMQTTSAMPLE_DEVICE_PSK;
    private static String mCertFilePath = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mPrivKeyFilePath = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String

    static {
        Loggor.setLogger(LOG);
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

            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setTXWebSocketActionCallback(new TXWebSocketActionCallback() {

                @Override
                public void onConnected() {
                    Loggor.debug(TAG, "onConnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
                    unlock();
                }

                @Override
                public void onMessageArrived(String topic, MqttMessage message) {
                    Loggor.debug(TAG, "onMessageArrived topic= " + topic);
                }

                @Override
                public void onConnectionLost(Throwable cause) {
                    Loggor.debug(TAG, "onConnectionLost " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
                }

                @Override
                public void onDisconnected() {
                    Loggor.debug(TAG, "onDisconnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
                    unlock();
                }
            });
            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).connect();
            Thread.sleep(2000);
        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
            Loggor.error(TAG, "MqttException " + e.toString());
        }
    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);

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
    public void testWebsocketMqttConnect() {
        // Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
        websocketConnect();
        lock();
        assertTrue(TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).isConnected());
        Loggor.debug(TAG, "after websocketConnect");

        websocketdisconnect();
        lock();
        assertSame(TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState(), ConnectionState.DISCONNECTED);
        Loggor.debug(TAG, "after websocketdisconnect");
    }
}
