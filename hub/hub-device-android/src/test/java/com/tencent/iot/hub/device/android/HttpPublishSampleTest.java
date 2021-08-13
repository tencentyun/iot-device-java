package com.tencent.iot.hub.device.android;

import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynregCallback;
import com.tencent.iot.hub.device.java.core.httppublish.TXHTTPPulish;
import com.tencent.iot.hub.device.java.core.httppublish.TXHttpPublishCallback;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketActionCallback;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketManager;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Device http publish sample
 */
public class HttpPublishSampleTest {

    private static final String TAG = HttpPublishSampleTest.class.getSimpleName();

    private static String mProductID = BuildConfig.TESTOTASAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTOTASAMPLE_DEVICE_NAME;
    private static String mDevPSK = BuildConfig.TESTOTASAMPLE_DEVICE_PSK;

    /**
     * 请求ID
     */
    private static AtomicInteger temperature = new AtomicInteger(0);


    @Mock
    TXHttpPublishCallback httpPublishCallback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);//create all @Mock objetcs
    }

    private void httpPublish() {

        JSONObject property = new JSONObject();
        try {
            // 车辆类型
            property.put("car_type", "suv");
            // 车辆油耗
            property.put("oil_consumption", "6.6");
            // 车辆最高速度
            property.put("maximum_speed", "205");
            // 温度信息
            property.put("temperature", String.valueOf(temperature.getAndIncrement()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object = new JSONObject();
        String clientToken = mProductID + mDevName + UUID.randomUUID().toString();
        try {
            object.put("method", "report");
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
            object.put("params", property);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String topic = String.format("%s/%s/data", mProductID, mDevName);
        TXHTTPPulish httpPulish = new TXHTTPPulish(mProductID, mDevPSK, mDevName, httpPublishCallback);
        if (httpPulish.doHttpPublish(topic, property, 0)) {
            Loggor.debug(TAG, "http publish OK!");
        } else {
            Loggor.error(TAG, "http publish failed!");
        }
    }

    @Test
    public void testHttpPublishDev() {
        // Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
        httpPublish();

        verify(httpPublishCallback, timeout(2000).times(1)).onSuccessPublishGetRequestId(Mockito.anyString());
    }
}
