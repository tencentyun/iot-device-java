package com.tencent.iot.explorer.device.android;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.explorer.device.android.gateway.TXGatewayClient;
import com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdevActionCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

import explorer.unit.test.BuildConfig;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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

    private static String mSubDev1ProductId = BuildConfig.TESTGATEWAYSAMPLE_SUB1_PRODUCT_ID;
    private static String mSubDev1DeviceName = BuildConfig.TESTGATEWAYSAMPLE_SUB1_DEV_NAME;
    private static String mSubDev1DevicePSK  = BuildConfig.TESTGATEWAYSAMPLE_SUB1_DEV_PSK;

    @Test
    public void testGatewayConnect() {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TXMqttActionCallBack mqttActionCallBack = mock(TXMqttActionCallBack.class);
        TXDataTemplateDownStreamCallBack dataTemplateDownStreamCallBack = mock(TXDataTemplateDownStreamCallBack.class);

        TXGatewayClient mConnection = new TXGatewayClient( context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null,
                mqttActionCallBack, mJsonFileName, dataTemplateDownStreamCallBack);

        //初始化连接
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0) {
            Log.i(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-java sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            Log.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(context, mDevCert, mDevPriv));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mConnection.setBufferOpts(bufferOptions);

        verify(mqttActionCallBack, timeout(2000).times(1)).onConnectCompleted(Matchers.eq(Status.OK), Matchers.eq(false), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mConnection.gatewayBindSubdev(mSubDev1ProductId, mSubDev1DeviceName, mSubDev1DevicePSK);

        verify(mqttActionCallBack, timeout(2000).times(1)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TXGatewaySubdevActionCallBack subdevActionCallBack = mock(TXGatewaySubdevActionCallBack.class);
        TXDataTemplateDownStreamCallBack subdevDataTemplateDownStreamCallBack = mock(TXDataTemplateDownStreamCallBack.class);
        TXGatewaySubdev mGatewaySubdev = new TXGatewaySubdev(mConnection, context,  mSubDev1ProductId, mSubDev1DeviceName, mJsonFileName,
                subdevActionCallBack, subdevDataTemplateDownStreamCallBack);
        mConnection.addSubdev(mGatewaySubdev); //添加子设备到网关
        mConnection.subdevOnline(mSubDev1ProductId, mSubDev1DeviceName);

        verify(mqttActionCallBack, timeout(2000).times(2)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mConnection.subdevOffline(mSubDev1ProductId, mSubDev1DeviceName);

        verify(mqttActionCallBack, timeout(2000).times(3)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TXMqttRequest mqttRequest1 = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mConnection.disConnect(mqttRequest1);

        verify(mqttActionCallBack, timeout(2000).times(1)).onDisconnectCompleted(Matchers.eq(Status.OK), Mockito.any(), Mockito.anyString());
    }
}