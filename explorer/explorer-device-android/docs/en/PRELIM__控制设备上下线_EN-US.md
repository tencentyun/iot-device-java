* [Getting Started](#Getting-Started)
  *  [Creating device in console](#Creating-device-in-console)
  *  [Compiling and running demo](#Compiling-and-running-demo)
     *  [Downloading the sample code of IoT Explorer SDK for Android demo](#Downloading-the-sample-code-of-IoT-Explorer-SDK-for-Android-demo)
     *  [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
     *  [Running demo for authenticated device MQTT connection](#Running-demo-for-authenticated-device-MQTT-connection)
     *  [Disconnecting device](#Disconnecting-device)

# Getting Started
This document describes how to create a device in the IoT Explorer console and quickly try out how it connects to Tencent Cloud over MQTT and disconnects from MQTT with the aid of the SDK demo.

## Creating device in console

Before connecting devices to the SDK, you need to create project products and devices in the console and get the product ID, device name, device certificate (for certificate authentication), device private key (for certificate authentication), and device key (for key authentication), which are required for authentication of the devices when you connect them to the cloud. For more information, please see [Project Management](https://cloud.tencent.com/document/product/1081/40290), [Product Definition](https://cloud.tencent.com/document/product/1081/34739), and [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

## Compiling and running demo

[Downloading the sample code of IoT Explorer SDK for Android demo](../../PRELIM__README_EN-US.md#Downloading-the-sample-code-of-IoT-Explorer-SDK-for-Android-demo)

#### Entering parameters for authenticating device for connection
Edit the parameter configuration information in the [app-config.json](../../../device-android-demo/src/main/assets/app-config.json) file.
```
{
  "PRODUCT_ID":        "",
  "DEVICE_NAME":       "",
  "DEVICE_PSK":        "",
  "SUB_PRODUCT_ID":    "",
  "SUB_DEV_NAME":      "",
  "SUB_DEV_PSK":       "",
  "SUB_PRODUCT_ID2":   "",
  "SUB_DEV_NAME2":     "",
  "SUB_DEV_PSK2":      ""
}
```
If key authentication is used during device creation in the console, you need to enter `PRODUCT_ID` (product ID), `DEVICE_NAME` (device name), and `DEVICE_PSK` (device key) in `app-config.json`. Key authentication is used in the demo.

If certificate authentication is used during device creation in the console, in addition to entering the `PRODUCT_ID` (product ID) and `DEVICE_NAME` (device name) in `app-config.json` and setting `DEVICE_PSK` (device key) to `null`, you also need to change the `DataTemplateSample` initialization method to 

```
public DataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack, final String jsonFileName,TXDataTemplateDownStreamCallBack downStreamCallBack)
```

Read the certificate through `AssetManager`. You need to create the `assets` directory under the `explorer/device-android-demo/src/main` path of the demo, place the device certificate and private key in it, and pass in the `devCertName` (device certificate file name) and `devKeyName` (device private key file name) during `DataTemplateSample` initialization.

You can set the data template for each product created in the console. The features of the data template are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

For the devices in the demo, you need to import the JSON files downloaded from the console into the demo to standardize the data verification during attribute and event reporting. Please place the JSON file in the `assets` directory and set `mJsonFileName` (JSON file name).

#### Running demo for authenticated device MQTT connection
Select the `explorer-demo` for Android application and click **Run 'explorer-demo'** on the Android Studio menu bar to install the demo.

Run the demo and click **Connect Device** in the data template module for authenticated device connection. Below is the sample code:
```
mDataTemplateSample = new DataTemplateSample(mParent, mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack());
mDataTemplateSample.connect(); // Connect to MQTT
```

Observe the logcat log.
```
I/TXMQTT_1.2.3: Start connecting to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883
I/TXDataTemplateFragment: onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=0}], msg[connected to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883]
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

`TXDataTemplateDownStreamCallBack` is the callback for downstream message processing. The prerequisite for receiving the following callbacks is for the device to subscribe to the relevant data template topics after it is connected. For more information, please see [Subscribing to and Unsubscribing from Topic](../docs/Subscribing-to-and-Unsubscribing-from-Topic.md).
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

Run the demo. After the device is connected, click **Disconnect Device** in the data template module to disconnect it from MQTT. Below is the sample code:
```
mDataTemplateSample.disconnect(); // Close the MQTT connection
```

Observe the logcat log.
```
I/TXDataTemplateFragment: onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=1}], msg[disconnected to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883]
```
The above log represents the process in which the device disconnects from MQTT successfully. In the console, you can see that the status of the device has been updated to `offline`.
