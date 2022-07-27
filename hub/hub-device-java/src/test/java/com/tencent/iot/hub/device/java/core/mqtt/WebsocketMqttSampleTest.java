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
    private static String mWebsocketCA = "-----BEGIN CERTIFICATE-----\n" +
            "MIIG2zCCBcOgAwIBAgIQB1P3ab1jMWo1LRNkq5S8lTANBgkqhkiG9w0BAQsFADBy\n" +
            "MQswCQYDVQQGEwJDTjElMCMGA1UEChMcVHJ1c3RBc2lhIFRlY2hub2xvZ2llcywg\n" +
            "SW5jLjEdMBsGA1UECxMURG9tYWluIFZhbGlkYXRlZCBTU0wxHTAbBgNVBAMTFFRy\n" +
            "dXN0QXNpYSBUTFMgUlNBIENBMB4XDTIxMDkwNjAwMDAwMFoXDTIyMDkwNTIzNTk1\n" +
            "OVowMzExMC8GA1UEAwwoKi5hcC1ndWFuZ3pob3UuaW90aHViLnRlbmNlbnRkZXZp\n" +
            "Y2VzLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKVb9w80wUTt\n" +
            "tQbspCCJGgaL3AGsKnfXi0iXWb60N+UP/Gz4xW5DXNPVSKHpOS7eKs142lPyRX/P\n" +
            "Cf7d3tV/OVz8Hur2OW7HPRb4fSjXC7hQTJznq7e5GC1Qa80MhsqO9SmDmqVa5iZ6\n" +
            "6gCdlvz31vVbG31F/xDpVmE6bCnZQ0TTs9V8ZZs/UbkOWtc/gq8pKAhD9/6eaqJ7\n" +
            "oS3DkCyYRRBCUbcM0jUx8TTmWGNGYea5IY24T/FTKbeVZliZmS7iEPP4FBOAys1h\n" +
            "VVhBUecZnD3k2nMoV6SKIQt9WfeV86q0LYivRkgpVNLzm4Ew1Soz9+2cvC7Z77aR\n" +
            "Hc/vrwcJLQUCAwEAAaOCA6owggOmMB8GA1UdIwQYMBaAFH/TmfOgRw4xAFZWIo63\n" +
            "zJ7dygGKMB0GA1UdDgQWBBRoKH8kwb5UFaBQqttlWXFYGdwk2DCB0AYDVR0RBIHI\n" +
            "MIHFgigqLmFwLWd1YW5nemhvdS5pb3RodWIudGVuY2VudGRldmljZXMuY29tgiUq\n" +
            "LnVzLWVhc3QuaW90Y2xvdWQudGVuY2VudGRldmljZXMuY29tgiIqLmV1cm9wZS5p\n" +
            "b3RodWIudGVuY2VudGRldmljZXMuY29tgiYqLmFwLWJhbmdrb2suaW90aHViLnRl\n" +
            "bmNlbnRkZXZpY2VzLmNvbYImYXAtZ3Vhbmd6aG91LmlvdGh1Yi50ZW5jZW50ZGV2\n" +
            "aWNlcy5jb20wDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggr\n" +
            "BgEFBQcDAjA+BgNVHSAENzA1MDMGBmeBDAECATApMCcGCCsGAQUFBwIBFhtodHRw\n" +
            "Oi8vd3d3LmRpZ2ljZXJ0LmNvbS9DUFMwgZIGCCsGAQUFBwEBBIGFMIGCMDQGCCsG\n" +
            "AQUFBzABhihodHRwOi8vc3RhdHVzZS5kaWdpdGFsY2VydHZhbGlkYXRpb24uY29t\n" +
            "MEoGCCsGAQUFBzAChj5odHRwOi8vY2FjZXJ0cy5kaWdpdGFsY2VydHZhbGlkYXRp\n" +
            "b24uY29tL1RydXN0QXNpYVRMU1JTQUNBLmNydDAJBgNVHRMEAjAAMIIBgAYKKwYB\n" +
            "BAHWeQIEAgSCAXAEggFsAWoAdgApeb7wnjk5IfBWc59jpXflvld9nGAK+PlNXSZc\n" +
            "JV3HhAAAAXu68FUsAAAEAwBHMEUCIBMWhGfG35EWzQpepoOnxMU3wAhHHgR6tZOu\n" +
            "8RAm5CSnAiEAsH00TzJxSK21iBs/Zbl5X9Gn35vqsVuPO+gMhz/pSo4AdwBRo7D1\n" +
            "/QF5nFZtuDd4jwykeswbJ8v3nohCmg3+1IsF5QAAAXu68FUUAAAEAwBIMEYCIQCH\n" +
            "NeLukMhB0RNd7Zm16YlsnAoLb5CAcJ68e5W9EJzZhAIhANOqk486D8etHA4p3nS9\n" +
            "+VZkZD3y9M4fi1Kv8yjeo7dlAHcAQcjKsd8iRkoQxqE6CUKHXk4xixsD6+tLx2jw\n" +
            "kGKWBvYAAAF7uvBUjwAABAMASDBGAiEAkif4MdoQ4E6DAIT0unnxRFyTwFP/myo/\n" +
            "ofYYXfPMPqQCIQDU/jXaShkd1XVxAZpwW332c1MpG5xCsCJbDnjjPa0OCzANBgkq\n" +
            "hkiG9w0BAQsFAAOCAQEASERgtLHdDfYDRfWpDHEX/Rbbx8Bv7agbk6+YWVF5YZ/z\n" +
            "75YsbD8btojrjgksKQaBe1aHAq8cOai8wFkcQsnmmDN7cEJplj3JzJpwFPXU2J0B\n" +
            "p8E59sA/DHzR1Z8DjGzyd70NKPa0Nf3w2EUYEpH0B5kGEhI5G0D3ybs66sXgNfcb\n" +
            "usW1QOjEhPQOJ3X5g5NItqZkjbZEQ41EOQV5qUa8/eHe7GayyE+EZNdY5aj5BVR0\n" +
            "YFLvrA5DVRa+DNbeqLWoQeVNb0kEjq0xnaaD3ohm9xmMYleRx0l8mhEEZ/5Hha1P\n" +
            "Zcrqjyw+6baShrOfotoFDlFE/wqf6FjhgeRkOb5QlA==\n" +
            "-----END CERTIFICATE-----\n";

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
            } else {
                Loggor.info(TAG, "Using cert file");
                conOptions.setSocketFactory(AsymcSslUtils.getSocketFactory(mWebsocketCA));
            }

            conOptions.setConnectionTimeout(8);
            conOptions.setKeepAliveInterval(60);
            conOptions.setAutomaticReconnect(true);

            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, getSecretKey()).setMqttConnectOptions(conOptions);

            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, getSecretKey()).setTXWebSocketActionCallback(new TXWebSocketActionCallback() {

                @Override
                public void onConnected() {
                    Loggor.debug(TAG, "onConnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, getSecretKey()).getConnectionState());
                    unlock();
                }

                @Override
                public void onMessageArrived(String topic, MqttMessage message) {
                    Loggor.debug(TAG, "onMessageArrived topic= " + topic);
                }

                @Override
                public void onConnectionLost(Throwable cause) {
                    Loggor.debug(TAG, "onConnectionLost " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, getSecretKey()).getConnectionState());
                }

                @Override
                public void onDisconnected() {
                    Loggor.debug(TAG, "onDisconnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, getSecretKey()).getConnectionState());
                    unlock();
                }
            });
            TXWebSocketManager.getInstance().getClient(mProductID, mDevName, getSecretKey()).connect();
            Thread.sleep(2000);
        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
            Loggor.error(TAG, "MqttException " + e.toString());
        }
    }

    private static String getSecretKey() {
        if (mDevPSK != null && mDevPSK.length() != 0) {
            return mDevPSK;
        } else {
            return mDevPriv;
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
