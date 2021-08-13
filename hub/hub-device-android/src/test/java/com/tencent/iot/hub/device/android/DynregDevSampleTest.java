package com.tencent.iot.hub.device.android;

import android.util.Log;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynreg;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynregCallback;
import com.tencent.iot.hub.device.java.core.httppublish.TXHttpPublishCallback;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Device Dynreg sample
 */
public class DynregDevSampleTest {
    private static final String TAG = DynregDevSampleTest.class.getSimpleName();

    private static String mProductID = BuildConfig.TESTDYNREGDEVSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTDYNREGDEVSAMPLE_DEVICE_NAME;
    private static String mProductSecret = BuildConfig.TESTDYNREGDEVSAMPLE_PRODUCT_SECRET;             // Used for dynamic register

    @Mock
    TXMqttDynregCallback mqttDynregCallback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs
    }
    @Test
    public void testDynregDev() {

        TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductSecret, mDevName, mqttDynregCallback);
        if (dynreg.doDynamicRegister()) {
            Loggor.info(TAG, "Dynamic Register OK!");
        } else {
            Loggor.error(TAG, "Dynamic Register failed!");
        }

        verify(mqttDynregCallback, timeout(2000).times(1)).onGetDevicePSK(Mockito.endsWith("=="));
    }
}
