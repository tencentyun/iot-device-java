## FAQs

#### Why is the log of device-cloud communication displayed in Base64-encoded format in the console rather than in the JSON format in which the data is passed?

Check whether you set the data format to custom format during product creation, and if so, the data will be Base64-encoded after it is transferred to the cloud. To display data in JSON format, you need to select the JSON format as the data format during product creation.

#### Is there a demo for IoT Hub?

The demo for Android is in the [corresponding application module of `hub-demo`](../../../hub-android-demo) in the GitHub repository.

#### What do I need to enter for `PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK` in the demo for Android?

You can enter the `PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK` parameters as instructed in [Device Connection Through MQTT over TCP - Overview - Entering parameters for authenticating device for connection](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md#Entering-parameters-for-authenticating-device-for-connection).

#### What should I do if the SDK for Android threw the `javax.net.ssl.SSLHandshakeException` exception?

It is a certificate handshake exception. Please carefully check whether the certificate and key passed in correspond to the device and whether they are correct.

#### What should I do if the error "Incorrect username or password" is reported when I use the SDK for Android to establish an MQTT connection?

If the device parameters (`PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK`) are all correctly configured, you can check whether the system time of the device is correct; for example, you can run `adb shell date` to view the system time of an Android device.

#### Why does a device keep going online and offline?

The IoT access layer has the logic of exclusive device login. If the same device ID is logged in to in another place, the existing login will be kicked offline by the new login. Therefore, if the device keeps going online and offline, you can check whether there are different users or threads performing login operations by using the same device ID.

#### Why is the `onConnectionLost` callback for disconnection frequently (32109)?

The `onConnectionLost` callback for disconnection (32109) indicates that the device is disconnected. If this error is frequently thrown, it is because that the same device ID is logged in to multiple times and the existing login is kicked offline by the new login. You can find the log of exclusive device login (Device kicked) in **Cloud Log** in the console. As the `connect` method is async, the connection is successful only after `onConnectCompleted` is called back and the status becomes `OK`. After `onDisconnectCompleted` is called back successfully when the device is disconnected, call the `connect` method again to create a new connection to make the device online.

#### The console sends a message to the device, but the device cannot receive it. What should I do?

You can view the message sending status in **Cloud Log** in the console. If it shows that the device is disconnected (device offline), check whether the device is online. If it shows "no subscriber", the device has not subscribed to the topic. If a QoS 1 message is sent and the device does not reply with an `ack` (processed by the SDK internally), the backend will send it again. However, if a QoS 0 message isn't replied to with an `ack`, the backend will not send it again.

#### If an offline device goes online again, can it receive a message sent to it when it is offline?

You need to set `setCleanSession` in `MqttConnectOptions` to `false` and the QoS level to QoS 1 during connection. After the device subscribes to the topic after connection, it can receive up to 150 heaped messages stored for up to 24 hours per device.

#### When I call the SDK to connect, it throws a "Proxy unavailable" error (3). What should I do?

This is because that the device information passed in during MQTT connection doesn't match. You need to check the device information passed in (product ID, device name, and device key or device private key).

#### What should I do if the error "Could not determine java version from '55'" is reported?

This error is caused by the version incompatibility between the two libraries (`bcprov-jdk15on` and `bcpkix-jdk15on`) that the SDK depends on and the JDK development environment. You can downgrade their versions.
```
dependencies {
...
    implementation 'org.bouncycastle:bcprov-jdk15on:1.57'
    implementation 'org.bouncycastle:bcpkix-jdk15on:1.57'
    implementation ('com.tencent.iot.hub:hub-device-android-core:x.x.x') {// `x.x.x` is the version number of the imported SDK
        exclude group: 'org.bouncycastle'
    }
...
}
```

#### How do I set the heartbeat interval?
In `options.setKeepAliveInterval(240)`, the unit of `240` is second. The server will use the maximum interval of `keepalive*1.5` set by the client to determine whether a heartbeat packet is received and whether to disconnect from the client.

#### How do I set whether to reconnect automatically?
options.setAutomaticReconnect(true);

#### What should I do if the `ERROR` is `Connection failed` and `msg` is `MqttException (0) - javax.net.ssl.SSLHandshakeException` in the `status` in the `onConnectCompleted` callback?
If `ssl://${ProductId}.iotcloud.tencentdevices.com:8883` is connected in the key authentication log, you need to add `options.setSocketFactory(AsymcSslUtils.getSocketFactory());` to configure the handshake settings.

#### What should I do if the `SocketFactory` type specified in the exception `MqttClient connect failed` doesn't match the proxy URI (32105).
If `tcp://${ProductId}.iotcloud.tencentdevices.com:1883` is connected in the key authentication log, you can remove `options.setSocketFactory(AsymcSslUtils.getSocketFactory());tcp` with no need to configure the handshake settings.

#### What should I do if "code:1021,message: Device has been activated" is returned during dynamic registration?
This error indicates that the device corresponding to the device information passed in during dynamic registration has already been activated and cannot be dynamically registered again. You can delete it in the console and get its device key or certificate information through dynamic registration again.

#### What should I do if the error `java.net.UnknownHostException` is reported during MQTT connection?
This is because that the server domain name (iotcloud-mqtt.gz.tencentdevices.com) in the SDK v3.2.0 you use is legacy. Starting from SDK v3.2.1, the new domain name `${ProductId}.iotcloud.tencentdevices.com` is used. To solve this error, please update the [SDK version](https://github.com/tencentyun/iot-device-java/releases). Note: the product manager replied that "although the new domain name is available, the legacy versions of the SDK can still use the legacy domain name", so this error may also be because that the local DNS goes wrong and the host cannot be resolved correctly.

#### What obfuscation rules do I need to configure for the SDK?
In the `proguard-rules.pro` file, add the following rules to add the classes related to the SDK to the "do not obfuscate" list:
```
-keep class org.apache.log4j.** { *; }
-keep class de.mindpipe.android.logging.log4j.** { *; }
-keep class org.eclipse.paho.client.mqttv3.** {*;}
-keepclasseswithmembers class org.eclipse.paho.** {*;}
```
For more information, please see the [configuration in the demo](../../../hub-android-demo/proguard-rules.pro).

#### What should I do if the error "java.lang.IllegalArgumentException: no NetworkModule installed for scheme" is reported after the obfuscation rules are configured?
Check the version number 'com.android.tools.build:gradle:x.x.x' of the Gradle plugin in the [build.gradle](../../../../build.gradle) file in the project root directory. We recommend v3.6 or above.

#### Is there a limit on the size of the messages published in the SDK? And if so, what is the limit?
The limit is 16 KB.

