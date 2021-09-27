## FAQs

#### Why is the log of device-cloud communication displayed in Base64-encoded format in the console rather than in the JSON format in which the data is passed?

Check whether you set the data format to custom format during product creation, and if so, the data will be Base64-encoded after it is transferred to the cloud. To display data in JSON format, you need to select the JSON format as the data format during product creation.

#### What should I do if the error `cannot resolve symbol "TXMqttConnection"` is reported during the dependency import to the IoT Hub SDK for Java through Maven?

Please check whether the corresponding Maven dependency address and version of the SDK are correct and whether the downloaded JAR package is complete, and if not, please clear the cache and pull the JAR package again. If you use the IntelliJ IDEA editor, we recommend you click **Invalidate and Restart** to clear the invalid cache and restart.

#### Is there a demo for IoT Hub?

For the Java demo, please see [Feature documentation](../../PRELIM__README_EN-US.md#Feature-documentation).

#### What do I need to enter for `mProductID`, `mDevName`, and `mDevPSK` in the Java demo?

Enter the product ID for `mProductID`, the device name for `mDevName`, and the device key for `mDevPSK` (for key authentication). If certificate authentication is used, pleas see [MqttSampleTest.java](../../../hub-device-java/src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java).

#### Why does a device keep going online and offline?

The IoT access layer has the logic of exclusive device login. If the same device ID is logged in to in another place, the existing login will be kicked offline by the new login. Therefore, if the device keeps going online and offline, you can check whether there are different users or threads performing login operations by using the same device ID.

#### What should I do if the error `No permission to connect (5)` is reported during connection authentication for a dynamically registered device?

The log of successful dynamic registration in the SDK returns `I/TXMQTT: Dynamic register OK! onGetDevicePSK, devicePSK[**********************]` or `I/TXMQTT: Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************]`. You can check whether the key or certificate passed in during connection authentication is the key or certificate obtained through dynamic registration.

#### Can I register multiple devices at a time through APIs?

Currently, the SDKs for Android and Java do not support batch dynamic registration. For more information, please see [Dynamic Registration API Description](../../../hub-device-android/docs/en/PRELIM__动态注册_EN-US.md).

#### The SDK that runs on Java 1.1.0 doesn't send ping messages, causing device disconnection. What should I do?

This issue has been resolved in the new version of the SDK. Please upgrade to the latest SDK.

#### What should I do if the error `UTF-8 encoding is recognized as GBK encoding` is reported during compilation with Android Studio?

Close Android Studio. Open the `bin` directory in the Android Studio installation directory, find `studio.exe.vmoptions` and `studio64.exe.vmoptions`, open them with a text editor, add `-Dfile.encoding=UTF-8` to them, save, and restart Android Studio.


#### How do I set the server root certificate?

For connection in a public cloud, you don't need to set the CA certificate, as the SDK will use the default CA certificate; for connection in a private cloud, you can refer to the CA certificate customization section in [Self-Built Server Connection](PRELIM__自建服务器接入_EN-US.md).

#### I entered the device identity information triplet but received the error message `Proxy unavailable (3)` during connection. What should I do?

If the created product is key-authenticated, please check whether its identity information triplet (productId/deviceName/devicePsk) is entered correctly, particularly `devicePsk` (which generally ends with '==' and should be copied completely). If the created product is certificate-authenticated, you need to download the device certificate and private key from the device details page, pass in the correct certificate and private key paths when calling `AsymcSslUtils.getSocketFactoryByFile()` to get `socketFactory`, and pass in a null value for `devicePsk` when constructing `TXMqttConnection`.

#### How do I save the SDK logs? What is the storage path?

Logs can be saved in the following two ways:

1. Before using SDK feature APIs, you can call the following method to save the SDK logs:
```
Loggor.saveLogs(String path)
```

The following sample will save the SDK logs in the `hub/hub-device-java.log` file.
```
public void doJob() {
    Loggor.saveLogs("hub/hub-device-java.log");
    ... // Business code
}
```

2. Configure the `log4j.properties` file in the project

Storage path of the configuration file:
```
${parent_path}/src/main/resources/log4j.properties
```

Sample content of the configuration file:
```
log4j.rootLogger = debug,file

log4j.appender.file = com.tencent.iot.hub.device.java.utils.MyDailyRollingFileAppender
log4j.appender.file.File = hub/hub-device-java.log
log4j.appender.file.Append = true
log4j.appender.file.Threshold = DEBUG
log4j.appender.file.layout = org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern = %d{HH:mm:ss,SSS} [%t] %-5p %c{1} %L %x - %m%n
```
The above sample will save the SDK log files in the `hub/hub-device-java.log` file.