package com.tencent.iot.hub.device.android;

import android.text.TextUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynreg;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynregCallback;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertTrue;

/**
 * Device Dynreg sample
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class DynregDevSampleTest {
    private static final String TAG = DynregDevSampleTest.class.getSimpleName();

    private static String mProductID = BuildConfig.TESTDYNREGDEVSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTDYNREGDEVSAMPLE_DEVICE_NAME;
    private static String mDevPSK  = "";
    private static String mProductSecret = BuildConfig.TESTDYNREGDEVSAMPLE_PRODUCT_SECRET;             // Used for dynamic register

    /**
     * Callback for dynamic register
     */
    private static class SelfMqttDynregCallback extends TXMqttDynregCallback {

        @Override
        public void onGetDevicePSK(String devicePsk) {
            mDevPSK = devicePsk;
            String logInfo = String.format("Dynamic register OK! onGetDevicePSK, devicePSK");
            Loggor.info(TAG, logInfo);
            dynregSuccess = true;
            unlock();
        }

        @Override
        public void onGetDeviceCert(String deviceCert, String devicePriv) {
            String logInfo = String.format("Dynamic register OK!onGetDeviceCert, deviceCert devicePriv");
            Loggor.info(TAG, logInfo);
        }

        @Override
        public void onFailedDynreg(Throwable cause, String errMsg) {
            String logInfo = String.format("Dynamic register failed! onFailedDynreg, ErrMsg[%s]", cause.toString() + errMsg);
            Loggor.error(TAG, logInfo);
            unlock();
        }

        @Override
        public void onFailedDynreg(Throwable cause) {
            String logInfo = String.format("Dynamic register failed! onFailedDynreg, ErrMsg[%s]", cause.toString());
            Loggor.error(TAG, logInfo);
            unlock();
        }
    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);
    private static boolean dynregSuccess = false;

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
    public void testDynregDev() {

        TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductSecret, mDevName, new SelfMqttDynregCallback());
        if (dynreg.doDynamicRegister()) {
            Loggor.info(TAG, "Dynamic Register OK!");
        } else {
            Loggor.error(TAG, "Dynamic Register failed!");
        }
        lock();

        assertTrue(!TextUtils.isEmpty(mDevPSK) && mDevPSK.contains("==") && dynregSuccess);
    }
}
