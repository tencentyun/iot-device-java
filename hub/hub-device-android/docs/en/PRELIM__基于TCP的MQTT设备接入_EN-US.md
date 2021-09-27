* [Getting Started](#Getting-Started)
  *  [Creating device in console](#Creating-device-in-console)
  *  [Compiling and running demo](#Compiling-and-running-demo)
     *  [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
     *  [Running demo for authenticated MQTT connection](#Running-demo-for-authenticated-MQTT-connection)
     *  [Running demo to close MQTT connection](#Running-demo-to-close-MQTT-connection)
     *  [Subscribing to topic](#Subscribing-to-topic)
     *  [Unsubscribing from topic](#Unsubscribing-from-topic)
     *  [Publishing to topic](#Publishing-to-topic)

# Getting Started
This document describes how to create devices in the IoT Hub console and quickly try out device connection to IoT Hub over the MQTT protocol for message sending/receiving on the SDK demo.

## Creating device in console

Before connecting devices to the SDK, you need to create products and devices in the console and get the product ID, device name, device certificate (for certificate authentication), device private key (for certificate authentication), and device key (for key authentication), which are required for authentication of the devices when you connect them to the cloud. For more information, please see [Device Connection Preparations](https://cloud.tencent.com/document/product/634/14442).

After a product is created successfully in the console, it has three permissions by default:
```
${productId}/${deviceName}/control  // Subscribe
${productId}/${deviceName}/data     // Subscribe and publish
${productId}/${deviceName}/event    // Publish
```
For more information on how to manipulate the topic permissions, please see [Permission List](https://cloud.tencent.com/document/product/634/14444).

## Compiling and running demo

[Downloading the sample code of IoT Hub SDK for Android demo](../../PRELIM__README_EN-US.md#Downloading-the-sample-code-of-IoT-Hub-SDK-for-Android-demo)

#### Entering parameters for authenticating device for connection
Edit the configuration information in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file.
```
{
  "PRODUCT_ID":        "",
  "DEVICE_NAME":       "",
  "DEVICE_PSK":        "",
  "SUB_PRODUCT_ID":    "",
  "SUB_DEVICE_PSK":    "",
  "SUB_DEV_NAME":      "",
  "TEST_TOPIC":        "",
  "SHADOW_TEST_TOPIC": "",
  "PRODUCT_KEY":       ""
}
```
If key authentication is used during device creation in the console, you need to enter `PRODUCT_ID` (product ID), `DEVICE_NAME` (device name), and `DEVICE_PSK` (device key) in `app-config.json`;

If certificate authentication is used during device creation in the console, in addition to entering `PRODUCT_ID` (product ID) and `DEVICE_NAME` (device name) in `app-config.json` and setting `DEVICE_PSK` (device key) to `null`, you also need to configure `mDevCert` (device certificate content string) and `mDevPriv` (device private key content string) in the [IoTMqttFragment.java](../../../hub-android-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) file.
Alternatively, you can read the certificate through `AssetManager`. Specifically, create the `assets` directory in the `hub-android-demo/src/main` path of the demo, place the device certificate and private key in it, and configure `mDevCertName` (device certificate file name) and `mDevKeyName` (device private key file name) in the [IoTMqttFragment.java](../../../hub-android-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) file.

```
private String mDevCertName = "";
private String mDevKeyName  = "";
private String mDevCert     = "";           // Cert String
private String mDevPriv     = "";           // Priv String
```

#### Running demo for authenticated MQTT connection
Select the `hub-demo` for Android application and click **Run 'hub-demo** on the Android Studio menu bar to install the demo.

Run the demo and click **Connect to MQTT** in the basic feature module for authenticated connection. Below is the sample code:
```
mMQTTSample = new MQTTSample(mParent, new SelfMqttActionCallBack(), mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mSubProductID, mSubDevName, mTestTopic, null, null, true, new SelfMqttLogCallBack()); // The `MQTTSample` class is used by the SDK to encapsulate API calls.
mMQTTSample.connect(); // Connect to MQTT
```

The above log represents the process in which the device is connected to the cloud over MQTT successfully.
```
I/TXMQTT1.2.3: Start connecting to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883
I/TXMQTT: onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=2}], msg[connected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883]
```

`TXMqttActionCallBack` is the callback for device action.
```
/**
 * Callback for MQTT connection completion
 *
 * @param status        Status.OK: connection succeeded; Status.ERROR: connection failed
 * @param reconnect     true: reconnection      false: first connection
 * @param userContext   User context
 * @param msg           Connection information
 */
@Override
public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg){}
/**
 * Callback for MQTT disconnection
 *
 * @param cause       Disconnection cause
 */
@Override
public void onConnectionLost(Throwable cause) {}
/**
 * Callback for MQTT disconnection completion
 *
 * @param status      Status.OK: disconnection succeeded; Status.ERROR: disconnection failed
 * @param userContext User context
 * @param msg         Details
 */
@Override
public void onDisconnectCompleted(Status status, Object userContext, String msg) {}
/**
 * Callback for message publishing completion
 *
 * @param status      Status.OK: message publishing succeeded; Status.ERROR: message publishing failed
 * @param token       Message token, containing the message content structure
 * @param userContext User context
 * @param msg         Details
 */
@Override
public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {}
/**
 * Callback for topic subscription completion
 *
 * @param status      Status.OK: subscription succeeded; Status.ERROR: subscription failed
 * @param token       Message token, containing the message content structure
 * @param userContext User context
 * @param msg        Details
 */
@Override
public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {}
/**
 * Callback for topic unsubscription completion
 *
 * @param status      Status.OK: unsubscription succeeded; Status.ERROR: unsubscription failed
 * @param token       Message token, containing the message content structure
 * @param userContext User context
 * @param msg         Details
 */
@Override
public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {}
/**
 * Push for the message received from the subscribed topic
 *
 * @param topic        Topic name
 * @param message      Message content
 */
@Override
public void onMessageReceived(final String topic, final MqttMessage message) {}
```

`getConnectStatus()API` can be used to get the device connection status.
```
enum ConnectStatus {
    kConnectIdle,  // Initial status
    kConnecting,   // Connecting
    kConnected,    // Connected
    kConnectFailed,// Connection failed
    kDisconnected  // Disconnected
}
```
#### Running demo to close MQTT connection

Run the demo and click **Disconnect from MQTT** in the basic feature module to disconnect from MQTT. Below is the sample code:
```
mMQTTSample.disconnect(); // Close the MQTT connection
```

The following logcat log represents the process in which the device is disconnected from MQTT successfully.
```
I/TXMQTT: onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=3}], msg[disconnected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883]
```

#### Subscribing to topic
Before running the demo, you need to configure the topic to be subscribed to as `TEST_TOPIC` (topic permission) in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file. For more information on how to generate a topic, please see [Creating device in console](#Creating-device-in-console).

Run the demo and click **Subscribe to Topic** in the basic feature module to subscribe to a topic. Below is the sample code:
```
mMQTTSample.subscribeTopic();// Subscribe to the topic
```

The following logcat log represents the process in which the device subscribes to a topic successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: AP9ZLEVFKT/gateway1/data
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=5}], errMsg[subscribe success]
```

#### Unsubscribing from topic
A device can unsubscribe from a previously subscribed topic.

Run the demo and click **Unsubscribe from Topic** in the basic feature module to unsubscribe from a topic. Below is the sample code:
```
mMQTTSample.unSubscribeTopic();// Unsubscribe from the topic
```

The following logcat log represents the process in which the device unsubscribes from a topic successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: AP9ZLEVFKT/gateway1/data
D/TXMQTT: onUnSubscribeCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]], userContext[MQTTRequest{requestType='unSubscribeTopic', requestId=6}], errMsg[unsubscribe success]
```

#### Publishing to topic
Before running the demo, you need to configure the topic to be published to as `TEST_TOPIC` (topic permission) in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file. For more information on how to generate a topic, please see [Creating device in console](#Creating-device-in-console).

Run the demo and click **Publish to Topic** in the basic feature module to publish to a topic. Below is the sample code:
```
Map<String, String> data = new HashMap<String, String>();// Data to be published
data.put("car_type", "suv");// Vehicle type
data.put("oil_consumption", "6.6");// Vehicle fuel consumption
data.put("maximum_speed", "205");// Maximum vehicle speed
data.put("temperature", String.valueOf(temperature.getAndIncrement()));// Temperature information
mMQTTSample.publishTopic("data", data);// Publish to the topic. The `publishTopic` method packages the `data` into `MqttMessage` for publishing.
```

The following logcat log represents the process in which the device publishes to a topic successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: AP9ZLEVFKT/gateway1/data Message: {"oil_consumption":"6.6","temperature":"0","maximum_speed":"205","car_type":"suv"}
D/TXMQTT: onPublishCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]],  userContext[MQTTRequest{requestType='publishTopic', requestId=8}], errMsg[publish success]
```
