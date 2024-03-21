* [Dynamic Registration Authentication](#Dynamic-Registration-Authentication)
  * [Overview](#Overview)
  * [Running demo for dynamic registration](#Running-demo-for-dynamic-registration)

# Dynamic Registration Authentication
## Overview
This feature assigns the same key to all devices under the same product, and a device gets a device certificate/key through a registration request for authentication. You can burn the same configuration information for the same batch of devices. For more information on the three authentication schemes provided by IoT Hub, please see [Overview](https://cloud.tencent.com/document/product/634/35272). For more information on the dynamic registration request, please see [Dynamic Registration API Description](https://cloud.tencent.com/document/product/634/47225).

Users need to create a device in advance on the console. During dynamic registration, the device reports the pre-created device name to obtain the device key.

## Running demo for dynamic registration
Edit the parameter configuration information in the [unit_test_config.json](../../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTDYNREGDEVSAMPLE_PRODUCT_ID":      "",
  "TESTDYNREGDEVSAMPLE_DEVICE_NAME":     "",
  "TESTDYNREGDEVSAMPLE_PRODUCT_SECRET":  "",
}
```
Before running the demo, you need to configure the `TESTDYNREGDEVSAMPLE_PRODUCT_ID` (product ID) and `TESTDYNREGDEVSAMPLE_DEVICE_NAME` (device name) parameters in the [DynregDevSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/dynreg/DynregDevSampleTest.java) file and enter `TESTDYNREGDEVSAMPLE_PRODUCT_SECRET` (the `ProductSecret` in the console).

Run the `main` function in [DynregDevSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/dynreg/DynregDevSampleTest.java) and call `dynReg()` to call dynamic registration authentication. After the dynamic registration callback gets the key or certificate information of the corresponding device, call MQTT connection. Below is the sample code:
```
private static void dynReg() {
    try {
        Thread.sleep(2000);
        LOG.debug("Test Dynamic");
        TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductKey, mDevName, new SelfMqttDynregCallback());// Initialize `TXMqttDynreg`
        if (dynreg.doDynamicRegister()) {// Call dynamic registration
            LOG.debug("Dynamic Register OK!");
        } else {
            LOG.error("Dynamic Register failed!");
        }
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which dynamic device registration is authenticated successfully.
```
24/02/2021 23:46:00,972 [main] DEBUG DynregDevSampleTest dynReg 43  - Dynamic Register OK!
24/02/2021 23:46:01,209 [Thread-0] INFO  DynregDevSampleTest onGetDevicePSK 454  - Dynamic register OK! onGetDevicePSK, devicePSK[**********************]// The device is authenticated with key
24/02/2021 23:46:01,209 [Thread-0] INFO  DynregDevSampleTest onGetDevicePSK 454  - Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************] // The device is authenticated with certificate
```

`TXMqttDynregCallback` is the callback for dynamic registration authentication.
```
/**
 * The device key is called back after dynamic registration authentication success (for key authentication)
 * @param devicePsk Device key (for key authentication)
 */
 @Override
 public void onGetDevicePSK(String devicePsk){}

/**
 * The device certificate content string and device private key content string are called back after dynamic registration authentication success (for certificate authentication)
 * @param deivceCert Device certificate content string
 * @param devicePriv Device private key content string
 */
 @Override
 public void onGetDeviceCert(String deviceCert, String devicePriv){}

/**
 * Callback for dynamic registration authentication failure
 * @param cause Exception
 * @param errMsg Failure information
 */
 @Override
 public void onFailedDynreg(Throwable cause, String errMsg){}

/**
 * Callback for dynamic registration authentication failure
 * @param cause Exception
 */
 @Override
 public void onFailedDynreg(Throwable cause){}
```
