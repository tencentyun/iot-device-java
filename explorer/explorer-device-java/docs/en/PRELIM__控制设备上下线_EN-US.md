* [Getting Started](#Getting-Started)
  *  [Creating device in console](#Creating-device-in-console)
  *  [Compiling and running demo](#Compiling-and-running-demo)
     *  [Key authentication for connection](#Key-authentication-for-connection)
     *  [Certificate authentication for connection](#Certificate-authentication-for-connection)
     *  [Running demo for authenticated device MQTT connection](#Running-demo-for-authenticated-device-MQTT-connection)
     *  [Disconnecting device](#Disconnecting-device)

# Getting Started
This document describes how to create a device in the IoT Explorer console and quickly try out how it connects to Tencent Cloud over MQTT and disconnects from MQTT with the aid of the demo.

## Creating device in console

Before connecting devices to the SDK, you need to create project products and devices in the console and get the product ID, device name, device certificate (for certificate authentication), device private key (for certificate authentication), and device key (for key authentication), which are required for authentication of the devices when you connect them to the cloud. For more information, please see [Project Management](https://cloud.tencent.com/document/product/1081/40290), [Product Definition](https://cloud.tencent.com/document/product/1081/34739), and [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

## Compiling and running demo

Download the [sample code of IoT Explorer SDK for Java demo](README.md#Downloading-the-sample-code-of-IoT-Explorer-SDK-for-Java-demo).

#### Key authentication for connection

Edit the parameter configuration information in the [unit_test_config.json](../../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTMQTTSAMPLE_PRODUCT_ID":           "",
  "TESTMQTTSAMPLE_DEVICE_NAME":          "",
  "TESTMQTTSAMPLE_DEVICE_PSK":           "",
}
```
If key authentication is used during device creation in the console, you need to enter `TESTMQTTSAMPLE_PRODUCT_ID` (product ID), `TESTMQTTSAMPLE_DEVICE_NAME` (device name), and `TESTMQTTSAMPLE_DEVICE_PSK` (device key) in `unit_test_config.json`. Key authentication is used in the demo.

#### Certificate authentication for connection

Place the certificate and private key in the [resources](../../src/test/resources/) folder.

If certificate authentication is used during device creation in the console, in addition to entering the `TESTMQTTSAMPLE_PRODUCT_ID` (product ID) and `TESTMQTTSAMPLE_DEVICE_NAME` (device name) in `unit_test_config.json.json`, you also need to enter `mDevCert(DEVICE_CERT_FILE_NAME)` (device certificate file name) and `mDevPriv(DEVICE_PRIVATE_KEY_FILE_NAME)` (device private key file name) in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java).

#### Data template import in JSON

You can set the data template for each product created in the console. The features of the data template are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

For the devices in the demo, you need to import the JSON files downloaded from the console into the demo to standardize the data verification during attribute and event reporting. Please place the JSON file in the [resources](../../src/test/resources/) folder and set the `mJsonFileName` (JSON file name) in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java) for the gateway device.

#### Running demo for authenticated device MQTT connection

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java) and call `connect()` in `testMqttConnect`. Below is the sample code:
```
private static void connect() {
    // init connection
    mDataTemplateSample = new DataTemplateSample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack());
    mDataTemplateSample.connect();
}
```

Observe the logcat log.
```
23/02/2021 19:15:30,410 [MQTT Call: LWVUL5SZ2Llight1] INFO  MqttSampleTest onConnectCompleted 288  - onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=0}], msg[connected to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883]
```

The above log represents the process in which the device is connected to the cloud over MQTT successfully. In the console, you can see that the status of the device has been updated to `online`.

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

`TXDataTemplateDownStreamCallBack` is the callback for downstream message processing. The prerequisite for receiving the following callbacks is for the device to subscribe to the relevant data template topics after it is connected. For more information, please see [Subscribing to and Unsubscribing from Topic](../../docs/en/PRELIM__订阅与取消订阅%20Topic%20主题_EN-US.md).
```
/**
 * Reply callbacks for attribute reporting, device information reporting, control clearing, event reporting, and multi-event reporting
 *
 * @param replyMsg       Reply information for attribute reporting, device information reporting, control clearing, event reporting, and multi-event reporting
 */
@Override
public void onReplyCallBack(String replyMsg) {}
/**
 * Callback for reply to device-reported latest information acquisition
 *
 * @param data           Reply information for device-reported latest information acquisition
 */
@Override
public void onGetStatusReplyCallBack(JSONObject data) {}
/**
 * Callback for the control information received for attribute delivery from the cloud
 *
 * @param msg            Control information received for attribute delivery from the cloud 
 * @return               `JSONObject` containing `code` (reply code) and `status` (reply information), which is used to reply to the attribute delivery control message
 */
@Override
public JSONObject onControlCallBack(JSONObject msg) {}
/**
 * Callback for the received action information delivered by the cloud to the device
 *
 * @param msg            Received action information delivered by the cloud to the device 
 * @return               `JSONObject` containing `code` (reply code) and `status` (reply information), which is used to reply to the delivered action message
 */
@Override
public JSONObject onActionCallBack(String actionId, JSONObject params){}
/**
 * The notification message sent to the device by the cloud when the user deletes the device on the Tencent IoT Link mini program or application, which is used to reset the device or used by a gateway device to clear the subdevice data
 *
 * @param msg        The notification message sent to the device by the cloud when the user deletes the device on the Tencent IoT Link mini program or application
 */
@Override
public void onUnbindDeviceCallBack(String msg) {}
/**
 * The notification message sent to the device by the cloud when the user binds the device on the Tencent IoT Link mini program or application, which can be processed based on the business needs after being received by the device
 *
 * @param msg        The notification message sent to the device by the cloud when the user binds the device on the Tencent IoT Link mini program or application
 */
@Override
public void onBindDeviceCallBack(String msg) {}
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

#### Disconnecting device

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java) and call `disconnect()` after `connect()` calls back `onConnectCompleted`. Below is the sample code:
```
private static void disconnect() {
    mDataTemplateSample.disconnect();
}
```

Observe the logcat log.
```
23/02/2021 19:31:24,315 [MQTT Disc: LWVUL5SZ2Llight1] INFO  MqttSampleTest onDisconnectCompleted 305  - onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=1}], msg[disconnected to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883]
```
The above log represents the process in which the device disconnects from MQTT successfully. In the console, you can see that the status of the device has been updated to `offline`.
