package com.tencent.iot.hub.device.android;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.TXLogImpl;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Device OTA sample
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class OTASampleTest {
    private static final String TAG = OTASampleTest.class.getSimpleName();

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546

    private static String mProductID = BuildConfig.TESTOTASAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTOTASAMPLE_DEVICE_NAME;
    private static String mDevPSK = BuildConfig.TESTOTASAMPLE_DEVICE_PSK;
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
    TXOTACallBack otaCallBack;
    @Mock
    TXMqttActionCallBack mqttActionCallBack;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs

        TXLogImpl.init(InstrumentationRegistry.getInstrumentation().getTargetContext());

        doReturn(alarmManager).when(context).getSystemService(Context.ALARM_SERVICE);

        mqttconnection = new TXMqttConnection(context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,false, null, mqttActionCallBack);
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


    @Test
    public void testOTA() {

        connect();

        verify(mqttActionCallBack, timeout(2000).times(1)).onConnectCompleted(Matchers.eq(Status.OK), Matchers.eq(false), Mockito.any(), Mockito.anyString());

        mqttconnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), otaCallBack);
        mqttconnection.reportCurrentFirmwareVersion("0.0.1");

        verify(otaCallBack, timeout(2000).times(1)).onReportFirmwareVersion(Matchers.eq(0), Matchers.eq("0.0.1"), Matchers.eq("success"));

    }
}
