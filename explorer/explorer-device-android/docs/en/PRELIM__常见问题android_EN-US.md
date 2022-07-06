## FAQs

#### Is there a demo for IoT Explorer?

The demo for Android is in the [device-android-demo](../../../device-android-demo) module in the GitHub repository.

#### What do I need to enter for `PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK` in the demo for Android?

You can enter the `PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK` parameters as instructed in [Device Connection Through MQTT over TCP - Overview - Entering parameters for authenticating device for connection](../../../explorer-device-android/docs/en/PRELIM__控制设备上下线_EN-US.md#Entering-parameters-for-authenticating-device-for-connection).

#### What should I do if the SDK for Android threw the `javax.net.ssl.SSLHandshakeException` exception?

It is a handshake exception that occurs during device certificate authentication. Please carefully check whether the certificate and key passed in correspond to the device and whether they are correct.

#### What should I do if the error "Incorrect username or password" is reported when I use the SDK for Android to establish an MQTT connection?

If the device parameters (`PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK`) are all correctly configured, you can check whether the system time of the device is correct; for example, you can run `adb shell date` to view the system time of an Android device.

#### Why does a device keep going online and offline?

The IoT access layer has the logic of exclusive device login. If the same device ID is logged in to in another place, the existing login will be kicked offline by the new login. Therefore, if the device keeps going online and offline, you can check whether there are different users or threads performing login operations by using the same device ID.

#### What is the difference between silent update and manual update for a firmware update task created in the IoT Explorer console?

If **Silent Update** is selected, after you create an update task, the device will receive the firmware update message.
If **Manual Update** is selected, the mini program can prompt that the device has new firmware available for update. Once the update is confirmed on the mini program, the device will receive the firmware update message.

#### What should I do if the remote dependency `explorer-device-android sdk` threw the error `Execution failed for task ':app:compileStagDebugJavaWithJavac'.  Could not resolve all files for configuration ':app:stagDebugCompileClasspath'. Failed to transform explorer-device-android-3.3.0.jar`?

The project depends on the implementation 'com.tencent.iot.explorer:explorer-device-android:3.3.0', but the error "explorer-device-android-3.3.0.jar file not found" is thrown here. This may be because that the development environment is messed up. In this case, we recommend you completely delete Android Studio and the relevant environment and Gradle configuration, download Android Studio, configure the environment information, and add the dependencies by syncing Gradle.

#### What should I do if the error message for the dynamic registration callback `onFailedDynreg` is `{"code":1010,"message":"Check signature failed"`?

It is most likely because that you entered the `ProductSecret` incorrectly. We recommend you check the three parameters (`productId`, `deviceName`, and `ProductSecret`) passed in when calling dynamic registration against the device information in the console.

#### How do I save the SDK logs? What is the storage path?

Before using SDK feature APIs, you can call one of the following methods to save the SDK logs:
1. TXLogImpl.init(Context context)
> It saves the SDK logs to the `/sdcard/tencent/package name (the corresponding package name of `context`)/iot_${yyyyMMdd}.log` file. Log files from the last 7 days are saved by default.
2. TXLogImpl.init(Context context, int duration)
> It saves the SDK logs to the `/sdcard/tencent/package name (the corresponding package name of `context`)/iot_${yyyyMMdd}.log` file. Log files from the last ${duration} days are saved.
3. TXLogImpl.init(Context context, int duration, String logPath)
> It saves the SDK logs to the `/sdcard/${logPath}/iot_${yyyyMMdd}.log` file. Log files from the last ${duration} days are saved. Note: the `logPath` parameter is in the format of `test/log/`, ending with '/'.

Sample:

If the application package name is `com.tencent.iot.explorer.demo`, and you add `TXLogImpl.init(this)` before calling the SDK feature APIs, the SDK logs will be saved to the `/sdcard/tencent/com/tencent/iot/explorer/demo/iot_${yyyyMMdd}.log` file.
```
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trtc_main);
        TXLogImpl.init(this);
        ... // Business code
}
```