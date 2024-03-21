* [Dynamic Registration Authentication](#Dynamic-Registration-Authentication)
  * [Overview](#Overview)
  * [Running demo for dynamic registration](#Running-demo-for-dynamic-registration)

# Dynamic Registration Authentication
## Overview
This feature assigns the same key to all devices under the same product, and a device gets a device certificate/key through a registration request for authentication. You can burn the same configuration information for the same batch of devices. For more information on the three authentication schemes provided by IoT Hub, please see [Overview](https://cloud.tencent.com/document/product/634/35272). For more information on the dynamic registration request, please see [Dynamic Registration API Description](https://cloud.tencent.com/document/product/634/47225).

Users need to create a device in advance on the console. During dynamic registration, the device reports the pre-created device name to obtain the device key.

## Running demo for dynamic registration
Before running the demo, you need to configure `PRODUCT_ID` (product ID), `DEVICE_NAME` (device name), and `PRODUCT_KEY` (`ProductSecret` in the console) in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file.

Click **Dynamic Registration** in the basic feature module to call dynamic registration authentication. Below is the sample code:
```
TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductKey, mDevName, new SelfMqttDynregCallback());// Initialize `TXMqttDynreg`
dynreg.doDynamicRegister();// Call dynamic registration
```

The following logcat log represents the process in which dynamic device registration is authenticated successfully.
```
D/TXMQTT: Dynamic Register OK!
I/TXMQTT: Dynamic register OK! onGetDevicePSK, devicePSK[**********************] // The device is authenticated with key
I/TXMQTT: Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************] // The device is authenticated with certificate
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
