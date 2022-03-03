package com.tencent.iot.hub.device.java.core.httppublish;

import com.tencent.iot.hub.device.java.utils.Loggor;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertTrue;

/**
 * Device http publish sample
 */
public class HttpPublishSampleTest {

    private static final Logger LOG = LoggerFactory.getLogger(HttpPublishSampleTest.class);

    private static final String TAG = HttpPublishSampleTest.class.getSimpleName();

    private static String mProductID = BuildConfig.TESTOTASAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTOTASAMPLE_DEVICE_NAME;
    private static String mDevPSK = BuildConfig.TESTOTASAMPLE_DEVICE_PSK; //密钥类型设备的psk
    private static String mDevPrivateKey = ""; //证书类型设备的私钥文件内容

    static {
        Loggor.setLogger(LOG);
    }

    /**
     * 请求ID
     */
    private static AtomicInteger temperature = new AtomicInteger(0);

    private static void httpPublish() {

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
        // 密钥类型设备
        TXHTTPPulish httpPulish = new TXHTTPPulish(mProductID, mDevPSK, mDevName, new SelfHttpPublishCallback());
        // 证书类型设备
        // TXHTTPPulish httpPulish = new TXHTTPPulish(mProductID, mDevPrivateKey, mDevPrivateKey, new SelfHttpPublishCallback());
        if (httpPulish.doHttpPublish(topic, property, 0)) {
            Loggor.debug(TAG, "http publish OK!");
        } else {
            Loggor.error(TAG, "http publish failed!");
        }
    }

    /**
     * Callback for http publish
     */
    private static class SelfHttpPublishCallback implements TXHttpPublishCallback {

        @Override
        public void onFailedPublish(Throwable cause) {
            String logInfo = String.format("http publish failed! onFailedPublish, ErrMsg[%s]", cause.toString());
            Loggor.error(TAG, logInfo);
            httpPublishTopicSuccess = false;
            unlock();
        }

        @Override
        public void onFailedPublish(Throwable cause, String errMsg) {
            String logInfo = String.format("http publish failed! onFailedPublish, ErrMsg[%s]", cause.toString() + errMsg);
            Loggor.error(TAG, logInfo);
            httpPublishTopicSuccess = false;
            unlock();
        }

        @Override
        public void onSuccessPublishGetRequestId(String requestId) {
            String logInfo = String.format("http publish OK!onSuccessPublishGetRequestId, requestId[%s]", requestId);
            Loggor.info(TAG, logInfo);
            httpPublishTopicSuccess = true;
            unlock();
        }
    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);
    private static boolean httpPublishTopicSuccess = false;

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
    public void testHttpPublishDev() {
        // Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
        httpPublish();
        lock();
        Loggor.debug(TAG, "after http publish");
        assertTrue(httpPublishTopicSuccess);
    }
}
