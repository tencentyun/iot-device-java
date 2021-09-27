* [Dynamic Registration Authentication](#Dynamic-Registration-Authentication)
  * [Overview](#Overview)
  * [Running demo for dynamic registration](#Running-demo-for-dynamic-registration)

# Dynamic Registration Authentication
## Overview
This feature assigns a unified key to all devices under the same product, and a device gets a device certificate/key through a registration request for authentication. You can burn the same configuration information for the same batch of devices. For more information on the dynamic registration request, please see [Dynamic Registration API Description](https://cloud.tencent.com/document/product/1081/47612).

If you enable automatic device creation in the console, you don't need to create devices in advance, but you must guarantee that device names are unique under the same product ID, which are generally unique device identifiers (such as MAC address). This method is more flexible. If you disable it in the console, you must create devices in advance and enter the same device names during dynamic registration, which is more secure but less convenient.

## Running demo for dynamic registration
Edit the parameter configuration information in the [unit_test_config.json](../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTDYNREGDEVSAMPLE_PRODUCT_ID":           "",
  "TESTDYNREGDEVSAMPLE_DEVICE_NAME":          "",
  "TESTDYNREGDEVSAMPLE_PRODUCT_SECRET":       "",
}
```
`TESTDYNREGDEVSAMPLE_PRODUCT_ID` (product ID), `TESTDYNREGDEVSAMPLE_DEVICE_NAME` (device name), and `TESTDYNREGDEVSAMPLE_PRODUCT_SECRET` (you need to enable dynamic registration in the console to get the `ProductSecret`).

Run the `testDynregDev` function in [DynregDevSampleTest.java](../src/test/java/com/tencent/iot/explorer/device/java/core/dynreg/DynregDevSampleTest.java) and call `dynReg()` in it to call dynamic registration authentication. After the dynamic registration callback gets the key or certificate information of the corresponding device, call MQTT connection. Below is the sample code:
```
public void testDynregDev() {
    ...
    dynReg();
    ...
}
private static void dynReg() {
    LOG.debug("Test Dynamic");
    TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductSecret, mDevName, new SelfMqttDynregCallback());
    if (dynreg.doDynamicRegister()) {
        LOG.debug("Dynamic Register OK!");
    } else {
        LOG.error("Dynamic Register failed!");
    }
}
```

The following logcat log represents the process in which dynamic device registration is authenticated successfully.
```
24/02/2021 23:46:00,972 [main] DEBUG TestMqttSample dynReg 43  - Dynamic Register OK!
24/02/2021 23:46:01,209 [Thread-0] INFO  TestMqttSample onGetDevicePSK 454  - Dynamic register OK! onGetDevicePSK, devicePSK[**********************]// The device is authenticated with key
24/02/2021 23:46:01,209 [Thread-0] INFO  TestMqttSample onGetDevicePSK 454  - Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************] // The device is authenticated with certificate
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
