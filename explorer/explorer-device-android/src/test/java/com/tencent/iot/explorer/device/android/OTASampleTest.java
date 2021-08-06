package com.tencent.iot.explorer.device.android;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
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
public class OTASampleTest {

    private static final String TAG = OTASampleTest.class.getSimpleName();

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTOTASAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTOTASAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTOTASAMPLE_DEVICE_PSK; //若使用证书验证，设为null
    private static String mJsonFileName = "struct.json";

    private static String mDevCertName = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevKeyName = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    @Test
    public void testOTA() {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TXMqttActionCallBack mqttActionCallBack = mock(TXMqttActionCallBack.class);
        TXDataTemplateDownStreamCallBack dataTemplateDownStreamCallBack = mock(TXDataTemplateDownStreamCallBack.class);
        TXDataTemplateClient mMqttConnection = new TXDataTemplateClient(context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mqttActionCallBack,
                mJsonFileName, dataTemplateDownStreamCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            Log.i(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-android sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            Log.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(context, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest1 = new TXMqttRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest1);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);

        verify(mqttActionCallBack, timeout(2000).times(1)).onConnectCompleted(Matchers.eq(Status.OK), Matchers.eq(false), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TXOTACallBack otaCallBack = mock(TXOTACallBack.class);

        mMqttConnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), otaCallBack);
        mMqttConnection.reportCurrentFirmwareVersion("0.0.1");

        verify(otaCallBack, timeout(2000).times(1)).onReportFirmwareVersion(Matchers.eq(0), Matchers.eq("0.0.1"), Matchers.eq("success"));
    }
}