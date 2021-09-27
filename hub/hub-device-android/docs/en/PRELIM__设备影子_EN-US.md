* [Device Shadow](#Device-Shadow)
  * [Overview](#Overview)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Running demo to try out connecting device shadow to IoT Hub](#Running-demo-to-try-out-connecting-device-shadow-to-IoT-Hub)
  * [Trying out disconnecting device shadow](#Trying-out-disconnecting-device-shadow)
  * [Trying out registering device attribute](#Trying-out-registering-device-attribute)
  * [Trying out regularly updating device shadow](#Trying-out-regularly-updating-device-shadow)
  * [Trying out getting device document](#Trying-out-getting-device-document)
  * [Trying out subscribing to topic](#Trying-out-subscribing-to-topic)
  * [Trying out unsubscribing from topic](#Trying-out-unsubscribing-from-topic)
  * [Trying out publishing to topic](#Trying-out-publishing-to-topic)

# Device Shadow
## Overview
Device shadow is essentially a copy of device status and configuration data in JSON format cached by the server for the device. For more information, please see [Device Shadow Details](https://cloud.tencent.com/document/product/634/11918) and [Device Shadow Data Flow](https://cloud.tencent.com/document/product/634/14072).

As an intermediary, device shadow can effectively implement two-way data sync between device and user application:

* For device configuration, the user application does not need to directly modify the device; instead, it can modify the device shadow on the server, which will sync modifications to the device. In this way, if the device is offline at the time of modification, it will receive the latest configuration from the shadow once coming back online.
* For device status, the device reports the status to the device shadow, and when users initiate queries, they can simply query the shadow. This can effectively reduce the network interactions between the device and the server, especially for low-power devices.

## Entering parameters for authenticating device for connection

Edit the configuration information in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file.
```
{
  "PRODUCT_ID":        "",
  "DEVICE_NAME":       "",
  "DEVICE_PSK":        "",
  "SUB_PRODUCT_ID":    "",
  "SUB_DEV_NAME":      "",
  "SUB_PRODUCT_KEY":   "",
  "TEST_TOPIC":        "",
  "SHADOW_TEST_TOPIC": "",
  "PRODUCT_KEY":       ""
}
```

You need to enter the `PRODUCT_ID` (product ID), `DEVICE_NAME` (device name), and `DEVICE_PSK` (device key) parameters of a key-authenticated device in `app-config.json`.

## Running demo to try out connecting device shadow to IoT Hub

Run the demo, switch the tab at the bottom to select the device shadow module, and click **Connect to IoT Hub** to connect the device to the cloud through authenticated connection. Below is the sample code:

```
mShadowSample.connect();
```

The following logcat log represents the process in which the device is connected successfully and subscribes to the device shadow topic. In the console, you can see that the status of the created `gateway1` device has been updated to `online`.
```
I/TXMQTT_1.2.3: Start connecting to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onConnectCompleted, status[OK], reconnect[false], msg[connected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883]
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: ******subscribe topic:$shadow/operation/result/AP9ZLEVFKT/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $shadow/operation/result/AP9ZLEVFKT/gateway1
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onSubscribeCompleted, status[OK], errMsg[subscribe success], topics[[$shadow/operation/result/AP9ZLEVFKT/gateway1]]
    ***subscribe topic:$shadow/operation/result/AP9ZLEVFKT/gateway1 success!!!!
D/ShadowSample: connect IoT completed, status[OK]
```

The `TXShadowActionCallBack` callback for the device action is as described below:
```
/**
 * Callback for MQTT connection completion
 *
 * @param status        Status.OK: connection succeeded; Status.ERROR: connection failed
 * @param reconnect     true: reconnection      false: first connection
 * @param userContext   User context
 * @param msg           Connection information
 */
public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {}
    
/**
 * Callback for MQTT disconnection
 *
 * @param cause       Disconnection cause
 */
public void onConnectionLost(Throwable cause) {}

/**
 * Callback API for response to document request
 *
 * @param type Document operation method (get/update/delete)
 * @param result Response result, 0: success; non-zero value: failure
 * @param jsonDocument   JSON document returned by the cloud
 */
public void onRequestCallback(String type, int result, String jsonDocument) {}

/**
 * Callback API for device attribute update
 *
 * @param propertyJSONDocument Original device attribute document in JSON format received from the cloud
 * @param propertyList   Updated device attribute set
 */
public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends DeviceProperty> propertyList) {}

/**
 * Message received from the cloud
 *
 * @param topic   Topic name
 * @param message Message content
 */
public void onMessageReceived(String topic, MqttMessage message) {}

/**
 * Callback for message publishing completion
 *
 * @param status        Status.OK: message publishing succeeded; Status.ERROR: message publishing failed
 * @param token         Message token, containing the message content structure
 * @param userContext   User context
 * @param msg           Details
 */
public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String msg) {}

/**
 * Callback for topic subscription completion
 *
 * @param status           Status.OK: subscription succeeded; Status.ERROR: subscription failed
 * @param token            Message token, containing the message content structure
 * @param userContext      User context
 * @param msg              Details
 */
public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {}

/**
 * Callback for topic unsubscription completion
 *
 * @param status           Status.OK: unsubscription succeeded; Status.ERROR: unsubscription failed
 * @param token            Message token, containing the message content structure
 * @param userContext      User context
 * @param msg              Details
 */
public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {}
```

`TXShadowConnection getConnectStatus()API` can be used to get the device connection status.
```
enum ConnectStatus {
    kConnectIdle,  // Initial status
    kConnecting,   // Connecting
    kConnected,    // Connected
    kConnectFailed,// Connection failed
    kDisconnected  // Disconnected
}
```

## Trying out disconnecting device shadow

Run the demo and click **Disconnect** in the device shadow module to disconnect from MQTT. Below is the sample code:
```
mShadowSample.closeConnect(); // Close the MQTT connection
```

The following logcat log represents the process in which the device successfully unsubscribes from the device shadow topic and is disconnected. In the console, you can see that the status of the created `gateway1` device has been updated to `offline`.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $shadow/operation/result/AP9ZLEVFKT/gateway1
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onUnSubscribeCompleted, status[OK], errMsg[unsubscribe success], topics[[$shadow/operation/result/AP9ZLEVFKT/gateway1]]
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onDisconnectCompleted, status[OK], msg[disconnected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883]
```

## Trying out registering device attribute

Run the demo and click **Register Device Attribute** in the device shadow module to create a `DeviceProperty` attribute instance, add it to the attribute array, and wait for it to be uploaded and updated. Below is the sample code:
```
mShadowSample.registerProperty(); // Register the device attribute
```
The above method will maintain an `mDevicePropertyList` list of `DeviceProperty` (device attribute) values in `mShadowSample`. When the device shadow is updated, the `DeviceProperty` will be updated to the device shadow JSON document in the cloud.

## Trying out regularly updating device shadow

Run the demo and click **Regularly Update Device Shadow** in the device shadow module to update the device attribute information once every 10 seconds in the demo. Below is the sample code:
```
mShadowSample.loop(); // Update the device attribute information once every 10 seconds
```

If you click **Register Device Attribute** in the previous step, the registered attribute information will be updated to the device shadow JSON document. Observe the logcat log.
```
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: ******publish message id:62848
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $shadow/operation/AP9ZLEVFKT/gateway1 Message: {"type":"update","state":{"reported":{"updateCount":1,"temperatureDesire":21}},"clientToken":"AP9ZLEVFKTgateway1-1","version":0}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: deliveryComplete, token.getMessageId:0
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onPublishCompleted, status[OK], errMsg[publish success], topics[[$shadow/operation/AP9ZLEVFKT/gateway1]]
I/TXMQTT_1.2.3: Received topic: $shadow/operation/result/AP9ZLEVFKT/gateway1, id: 0, message: {"clientToken":"AP9ZLEVFKTgateway1-1","payload":{"state":{"reported":{"temperatureDesire":21,"updateCount":1}},"timestamp":1603252024994,"version":1},"result":0,"timestamp":1603252024994,"type":"update"}
D/IoTShadowFragment: onRequestCallback, type[update], result[0], document[{"state":{"reported":{"temperatureDesire":21,"updateCount":1}},"timestamp":1603252024994,"version":1}]
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: ******update local mDocumentVersion to 1
...
```
As can be seen from the above log, when you click **Regularly Update Device Shadow**, the device shadow will first publish the registered attribute information as a topic message with the `type` being `update`. After the published message is called back successfully, as the device shadow has already subscribed to the `$shadow/operation/result/${productId}/${deviceName}` topic when you [run the demo to try out connecting the device shadow to IoT Hub](#running-demo-to-try-out-connecting-device-shadow-to-IoT-Hub), it will receive the subscribed message with the device attribute and update the local `version`, so as to determine whether the `version` in the message matches the `version` stored on it, and if so, it will perform the device shadow update process.

## Trying out getting device document

Run the demo and click **Get Device Document** in the device shadow module to pull the latest device shadow document. Below is the sample code:
```
mShadowSample.getDeviceDocument(); // Get the latest device shadow document
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $shadow/operation/AP9ZLEVFKT/gateway1 Message: {"type":"get","clientToken":"AP9ZLEVFKTgateway1-23"}
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onPublishCompleted, status[OK], errMsg[publish success], topics[[$shadow/operation/AP9ZLEVFKT/gateway1]]
I/TXMQTT_1.2.3: Received topic: $shadow/operation/result/AP9ZLEVFKT/gateway1, id: 0, message: {"clientToken":"AP9ZLEVFKTgateway1-23","payload":{"state":{"reported":{"temperatureDesire":42,"updateCount":22}},"timestamp":1603252235473,"version":22},"result":0,"timestamp":1603252237,"type":"get"}
D/IoTShadowFragment: onRequestCallback, type[get], result[0], document[{"state":{"reported":{"temperatureDesire":42,"updateCount":22}},"timestamp":1603252235473,"version":22}]
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: ******update local mDocumentVersion to 22
```
As can be seen from the above log, when you click **Get Device Document**, the device shadow will publish a topic message with the `type` being `get`. As it has already subscribed to the `$shadow/operation/result/${productId}/${deviceName}` topic when you [run the demo to try out connecting the device shadow to IoT Hub](#running-demo-to-try-out-connecting-device-shadow-to-IoT-Hub), it will receive the message subscribed to by the latest device shadow document. If you view the latest device shadow document in the console, you will find that it is the same as the pulled document.

## Trying out subscribing to topic

Before running the demo, you need to configure the topic to be subscribed to as the `SHADOW_TEST_TOPIC` (topic permission) in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file. For more information on how to generate a topic, please see [Device Connection Through MQTT over TCP](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md).

Run the demo and click **Subscribe to Topic** in the device shadow module to subscribe to a device topic. Below is the sample code:
```
mShadowSample.subscribeTopic(BuildConfig.SHADOW_TEST_TOPIC);
```

The following logcat log represents the process in which the device subscribes to a topic successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: AP9ZLEVFKT/gateway1/data
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onSubscribeCompleted, status[OK], errMsg[subscribe success], topics[[AP9ZLEVFKT/gateway1/data]]
```

## Trying out unsubscribing from topic

A device can unsubscribe from a previously subscribed topic.

Run the demo and click **Unsubscribe from Topic** in the device shadow module to unsubscribe from a topic. Below is the sample code:
```
mShadowSample.unSubscribeTopic(BuildConfig.SHADOW_TEST_TOPIC);// Unsubscribe from the topic
```

The following logcat log represents the process in which the device unsubscribes from a topic successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: AP9ZLEVFKT/gateway1/data
D/com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection: onUnSubscribeCompleted, status[OK], errMsg[unsubscribe success], topics[[AP9ZLEVFKT/gateway1/data]]
```

## Trying out publishing to topic

Before running the demo, you need to configure the topic to be published to as the `SHADOW_TEST_TOPIC` (topic permission) in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file. For more information on how to generate a topic, please see [Device Connection Through MQTT over TCP](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md).

Run the demo and click **Publish to Topic** in the device shadow module to publish to a device topic. Below is the sample code:
```
Map<String, String> data = new HashMap<String, String>(); // Data to be published
data.put("car_type", "suv"); // Vehicle type
data.put("oil_consumption", "6.6"); // Vehicle fuel consumption
data.put("maximum_speed", "205"); // Maximum vehicle speed
mShadowSample.publishTopic(BuildConfig.SHADOW_TEST_TOPIC, data); // Publish to the topic
```

The following logcat log represents the process in which a topic is published to successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: AP9ZLEVFKT/gateway1/data Message: {"oil_consumption":"6.6","maximum_speed":"205","car_type":"suv"}
D/IoTShadowFragment: onPublishCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]],  userContext[MQTTRequest{requestType='publishTopic', requestId=3}], errMsg[publish success]
```
