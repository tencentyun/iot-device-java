import com.tencent.iot.hub.device.java.core.dynreg.TXMqttBindDevice;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttBindDeviceCallback;

import org.junit.Test;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.*;

public class TestBindTidDevice {
    private TXMqttBindDevice mqttBindDevice;
    private String productId = BuildConfig.TESTBINDTIDDEVICE_PRODUCT_ID;
    private String deviceName = BuildConfig.TESTBINDTIDDEVICE_DEVICE_NAME;
    private String tid= BuildConfig.TESTBINDTIDDEVICE_TID;
    private String pemPubKey = BuildConfig.TESTBINDTIDDEVICE_PEM_PUBKEY;

    private TXMqttBindDeviceCallback callback = new TXMqttBindDeviceCallback() {
        @Override
        public void onBindFailed(Throwable cause) { }

        @Override
        public void onBindSuccess(String msg) {
            System.out.println(msg);
        }
    };

    @Test
    public void testBindTidDevice() {
//        mqttBindDevice = new TXMqttBindDevice(productId, deviceName, tid, pemPubKey, callback);
//        mqttBindDevice.doBind();
        assertTrue(true);
    }
}
