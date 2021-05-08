package com.tencent.iot.explorer.device.java.core.gateway;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.iot.explorer.device.java.core.samples.gateway.GatewaySample;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import java.util.concurrent.atomic.AtomicInteger;

import explorer.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;

public class GatewaySampleTest {
    private static final Logger LOG = LoggerFactory.getLogger(GatewaySampleTest.class);
    private static final String TAG = "TXGatewaySample";
    private static GatewaySample mGatewaySample;
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTGATEWAYSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_PSK; //若使用证书验证，设为null
    private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mJsonFileName = "gateway.json";

    private static String mSubDev1ProductId = BuildConfig.TESTGATEWAYSAMPLE_SUB1_PRODUCT_ID;
    private static String mSubDev1DeviceName = BuildConfig.TESTGATEWAYSAMPLE_SUB1_DEV_NAME;
    private static String mSubDev1DevicePSK  = BuildConfig.TESTGATEWAYSAMPLE_SUB1_DEV_PSK;

    private static String mSubDev2ProductId = BuildConfig.TESTGATEWAYSAMPLE_SUB2_PRODUCT_ID;
    private static String mSubDev2DeviceName = BuildConfig.TESTGATEWAYSAMPLE_SUB2_DEV_NAME;

    private static void gatewayOffline() {
        try {
            Thread.sleep(2000);
            mGatewaySample.offline();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayBindSubdev(String productId, String deviceName, String devicePsk) {
        try {
            Thread.sleep(2000);
            mGatewaySample.gatewayBindSubdev(productId, deviceName, devicePsk);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayUnbindSubdev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.gatewayUnbindSubdev(productId, deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayAddSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.addSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayDelSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.delSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayOnlineSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.onlineSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayOfflineSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.offlineSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testGatewayConnect() {
        mGatewaySample = new GatewaySample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mJsonFileName, mSubDev1ProductId, mSubDev2ProductId);

        mGatewaySample.online();

        gatewayBindSubdev(mSubDev1ProductId,mSubDev1DeviceName,mSubDev1DevicePSK);

        gatewayAddSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayOnlineSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayOfflineSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayDelSubDev(mSubDev1ProductId,mSubDev1DeviceName);

//        gatewayUnbindSubdev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayOffline();

        try {
            Thread.sleep(2000);
            assertSame(mGatewaySample.getConnectStatus(), TXMqttConstants.ConnectStatus.kDisconnected);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
