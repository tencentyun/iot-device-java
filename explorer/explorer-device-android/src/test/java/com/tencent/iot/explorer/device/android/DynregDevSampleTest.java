package com.tencent.iot.explorer.device.android;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynreg;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynregCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import explorer.unit.test.BuildConfig;

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
public class DynregDevSampleTest {

    private static final String TAG = DynregDevSampleTest.class.getSimpleName();
    private static String mProductID = BuildConfig.TESTDYNREGDEVSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTDYNREGDEVSAMPLE_DEVICE_NAME;
    private static String mProductSecret = BuildConfig.TESTDYNREGDEVSAMPLE_PRODUCT_SECRET;             // Used for dynamic register

    @Test
    public void testDynregDev() {

        TXMqttDynregCallback mqttDynregCallback = mock(TXMqttDynregCallback.class);
        TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductSecret, mDevName, mqttDynregCallback);
        if (dynreg.doDynamicRegister()) {
            Log.e(TAG, "Dynamic Register OK!");
        } else {
            Log.e(TAG, "Dynamic Register failed!");
        }

        verify(mqttDynregCallback, timeout(2000).times(1)).onGetDevicePSK(Mockito.endsWith("=="));
    }
}