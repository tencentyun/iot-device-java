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

Edit the parameter configuration information in the [unit_test_config.json](../../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTSHADOWSAMPLE_PRODUCT_ID":         "",
  "TESTSHADOWSAMPLE_DEVICE_NAME":        "",
  "TESTSHADOWSAMPLE_DEVICE_PSK":         ""
}
```

You need to enter the `TESTSHADOWSAMPLE_PRODUCT_ID` (product ID), `TESTSHADOWSAMPLE_DEVICE_NAME` (device name), and `TESTSHADOWSAMPLE_DEVICE_PSK` (device key) parameters of a key-authenticated device in `unit_test_config.json`.

If certificate authentication is used, in addition to entering the `TESTSHADOWSAMPLE_PRODUCT_ID` (product ID) and `TESTSHADOWSAMPLE_DEVICE_NAME` (device name) in `unit_test_config.json`, you also need to set `mDevPSK` (device key) to `null` in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java), place the certificate and private key in the [resources](../../src/test/resources/) folder, and enter the `mCertFilePath` (device certificate file name) and `mPrivKeyFilePath` (device private key file name).

## Running demo to try out connecting device shadow to IoT Hub

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java) to authenticate the device for connection to the cloud. Below is the sample code:

```
public static void main(String[] args) {
    ...
    // init connection
    MqttConnectOptions options = new MqttConnectOptions();
    options.setConnectionTimeout(8);
    options.setKeepAliveInterval(60);
    options.setAutomaticReconnect(true);

    if (mDevPSK != null) {

    } else {
        String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
        options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
    }
    mShadowConnection = new TXShadowConnection(mProductID, mDevName, mDevPSK, new callback());
    mShadowConnection.connect(options, null);
}
```

The following logcat log represents the process in which the device is connected successfully and subscribes to the device shadow topic. In the console, you can see that the status of the created `gateway1` device has been updated to `online`.
```
15/03/2021 20:01:07,918 [main] INFO  TXMqttConnection connect 348  - Start connecting to ssl://DVSVXI409C.iotcloud.tencentdevices.com:8883
15/03/2021 20:01:08,456 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onConnectCompleted 677  - onConnectCompleted, status[OK], reconnect[false], msg[connected to ssl://DVSVXI409C.iotcloud.tencentdevices.com:8883]
15/03/2021 20:01:08,456 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onConnectCompleted 679  - ******subscribe topic:$shadow/operation/result/DVSVXI409C/cert_test_1
15/03/2021 20:01:08,456 [MQTT Call: DVSVXI409Ccert_test_1] INFO  TXMqttConnection subscribe 684  - Starting subscribe topic: $shadow/operation/result/DVSVXI409C/cert_test_1
15/03/2021 20:01:08,471 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onSubscribeCompleted 719  - onSubscribeCompleted, status[OK], errMsg[subscribe success], topics[[$shadow/operation/result/DVSVXI409C/cert_test_1]]
```


The `TXShadowActionCallBack` callback for the device action passed in to initialize `TXShadowConnection` is as described below:
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

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `closeConnect()`. Below is the sample code:
```
private static void closeConnect() {
    try {
        Thread.sleep(2000);
        mShadowConnection.disConnect(null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device successfully unsubscribes from the device shadow topic and is disconnected. In the console, you can see that the status of the created `gateway1` device has been updated to `offline`.
```
15/03/2021 20:07:20,405 [main] INFO  TXMqttConnection unSubscribe 722  - Starting unSubscribe topic: $shadow/operation/result/DVSVXI409C/cert_test_1
15/03/2021 20:07:20,421 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onUnSubscribeCompleted 738  - onUnSubscribeCompleted, status[OK], errMsg[unsubscribe success], topics[[$shadow/operation/result/DVSVXI409C/cert_test_1]]
15/03/2021 20:07:20,423 [MQTT Disc: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onDisconnectCompleted 694  - onDisconnectCompleted, status[OK], msg[disconnected to ssl://DVSVXI409C.iotcloud.tencentdevices.com:8883]
```

## Trying out registering device attribute

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `registerProperty()` to create a `DeviceProperty` attribute instance, add it to the attribute array, and wait for it to be uploaded and updated. Below is the sample code:
```
private static void registerProperty() {
    try {
        Thread.sleep(2000);
        DeviceProperty deviceProperty1 = new DeviceProperty();
        deviceProperty1.key("updateCount").data(String.valueOf(mUpdateCount.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
        mShadowConnection.registerProperty(deviceProperty1);

        DeviceProperty deviceProperty2 = new DeviceProperty();
        deviceProperty2.key("temperatureDesire").data(String.valueOf(mTemperatureDesire.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
        mShadowConnection.registerProperty(deviceProperty2);

        mDevicePropertyList.add(deviceProperty1);
        mDevicePropertyList.add(deviceProperty2);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```
The above method will maintain an `mDevicePropertyList` list of `DeviceProperty` (device attribute) values in `ShadowSampleTest`. When the device shadow is updated, the `DeviceProperty` will be updated to the device shadow JSON document in the cloud.

## Trying out regularly updating device shadow

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `update()` to update the device attribute information once every 10 seconds in the demo. Below is the sample code:
```
private static void update() {
    try {
        while(true) {
            Thread.sleep(10000);

            for (DeviceProperty deviceProperty : mDevicePropertyList) {
                if ("updateCount".equals(deviceProperty.mKey)) {
                    deviceProperty.data(String.valueOf(mUpdateCount.getAndIncrement()));
                } else if ("temperatureDesire".equals(deviceProperty.mKey)) {
                    deviceProperty.data(String.valueOf(mTemperatureDesire.getAndIncrement()));
                }
            }

            LOG.info("update device property");
            mShadowConnection.update(mDevicePropertyList, null);
        }
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

If you click **Register Device Attribute** in the previous step, the registered attribute information will be updated to the device shadow JSON document. Observe the logcat log.
```
16/03/2021 09:33:58,028 [main] DEBUG TXShadowConnection publish 409  - ******publish message id:18197
16/03/2021 09:33:58,028 [main] INFO  TXMqttConnection publish 502  - Starting publish topic: $shadow/operation/DVSVXI409C/cert_test_1 Message: {"clientToken":"DVSVXI409Ccert_test_1-0","state":{"reported":{"updateCount":1,"temperatureDesire":21}},"type":"update","version":0}
16/03/2021 09:33:58,029 [MQTT Call: DVSVXI409Ccert_test_1] INFO  TXMqttConnection deliveryComplete 965  - deliveryComplete, token.getMessageId:2
16/03/2021 09:33:58,029 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onPublishCompleted 708  - onPublishCompleted, status[OK], errMsg[publish success], topics[[$shadow/operation/DVSVXI409C/cert_test_1]]
16/03/2021 09:33:58,058 [MQTT Call: DVSVXI409Ccert_test_1] INFO  TXMqttConnection messageArrived 941  - Received topic: $shadow/operation/result/DVSVXI409C/cert_test_1, id: 0, message: {"clientToken":"DVSVXI409Ccert_test_1-0","payload":{"state":{"reported":{"temperatureDesire":21,"updateCount":1}},"timestamp":1615858438033,"version":0},"result":0,"timestamp":1615858438033,"type":"update"}
16/03/2021 09:33:58,060 [MQTT Call: DVSVXI409Ccert_test_1] INFO  ShadowSampleTest onRequestCallback 211  - onRequestCallback, type[update], result[0], document[{"state":{"reported":{"updateCount":1,"temperatureDesire":21}},"version":0,"timestamp":1615858438033}]
16/03/2021 09:33:58,060 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onMessageReceived 788  - ******update local mDocumentVersion to 0
...
```
As can be seen from the above log, when you click **Regularly Update Device Shadow**, the device shadow will first publish the registered attribute information as a topic message with the `type` being `update`. After the published message is called back successfully, as the device shadow has already subscribed to the `$shadow/operation/result/${productId}/${deviceName}` topic when you [run the demo to try out connecting the device shadow to IoT Hub](#running-demo-to-try-out-connecting-device-shadow-to-IoT-Hub), it will receive the subscribed message with the device attribute and update the local `version`, so as to determine whether the `version` in the message matches the `version` stored on it, and if so, it will perform the device shadow update process.

## Trying out getting device document

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `getDeviceDocument()` to pull the latest document of the device shadow. Below is the sample code:
```
private static void getDeviceDocument() {
    try {
        Thread.sleep(2000);
        mShadowConnection.get(null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

Observe the logcat log.
```
16/03/2021 09:38:26,216 [main] INFO  TXMqttConnection publish 502  - Starting publish topic: $shadow/operation/DVSVXI409C/cert_test_1 Message: {"clientToken":"DVSVXI409Ccert_test_1-0","type":"get"}
16/03/2021 09:38:26,216 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onPublishCompleted 708  - onPublishCompleted, status[OK], errMsg[publish success], topics[[$shadow/operation/DVSVXI409C/cert_test_1]]
16/03/2021 09:38:26,239 [MQTT Call: DVSVXI409Ccert_test_1] INFO  TXMqttConnection messageArrived 941  - Received topic: $shadow/operation/result/DVSVXI409C/cert_test_1, id: 0, message: {"clientToken":"DVSVXI409Ccert_test_1-0","payload":{"state":{"reported":{"temperatureDesire":47,"updateCount":27}},"timestamp":1615858698132,"version":26},"result":0,"timestamp":1615858706,"type":"get"}
16/03/2021 09:38:26,241 [MQTT Call: DVSVXI409Ccert_test_1] INFO  ShadowSampleTest onRequestCallback 213  - onRequestCallback, type[get], result[0], document[{"state":{"reported":{"updateCount":27,"temperatureDesire":47}},"version":26,"timestamp":1615858698132}]
16/03/2021 09:38:26,242 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onMessageReceived 788  - ******update local mDocumentVersion to 26
```
As can be seen from the above log, when you click **Get Device Document**, the device shadow will publish a topic message with the `type` being `get`. As it has already subscribed to the `$shadow/operation/result/${productId}/${deviceName}` topic when you [run the demo to try out connecting the device shadow to IoT Hub](#running-demo-to-try-out-connecting-device-shadow-to-IoT-Hub), it will receive the message subscribed to by the latest device shadow document. If you view the latest device shadow document in the console, you will find that it is the same as the pulled document.

## Trying out subscribing to topic

Before running the demo, you need to configure the topic to be subscribed to as the `SHADOW_TEST_TOPIC` (topic permission) in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file. For more information on how to generate a topic, please see [Device Connection Through MQTT over TCP](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md).

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `subscribeTopic()` to subscribe to a device topic. Below is the sample code:
```
private static void subscribeTopic() {
    try {
        Thread.sleep(2000);
        // QoS level
        int qos = TXMqttConstants.QOS1;
        // User context (request instance)
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
        LOG.debug("Start to subscribe" + mTestTopic);
        // Call the `subscribe` method of `TXShadowConnection` to subscribe to the topic
        mShadowConnection.subcribe(mTestTopic, qos, mqttRequest);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device subscribes to a topic successfully.
```
16/03/2021 09:40:21,269 [main] INFO  TXMqttConnection subscribe 684  - Starting subscribe topic: DVSVXI409C/cert_test_1/data
16/03/2021 09:40:21,284 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onSubscribeCompleted 723  - onSubscribeCompleted, status[OK], errMsg[subscribe success], topics[[DVSVXI409C/cert_test_1/data]]
```

## Trying out unsubscribing from topic

A device can unsubscribe from a previously subscribed topic.

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `unSubscribeTopic()` to unsubscribe. Below is the sample code:
```
private static void unSubscribeTopic() {
    try {
        Thread.sleep(2000);
        // User context (request instance)
        MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
        LOG.debug("Start to unSubscribe" + mTestTopic);
        // Unsubscribe from the topic
        mShadowConnection.unSubscribe(mTestTopic, mqttRequest);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device unsubscribes from a topic successfully.
```
16/03/2021 09:42:42,628 [main] INFO  TXMqttConnection unSubscribe 722  - Starting unSubscribe topic: DVSVXI409C/cert_test_1/data
16/03/2021 09:42:42,640 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TXShadowConnection onUnSubscribeCompleted 742  - onUnSubscribeCompleted, status[OK], errMsg[unsubscribe success], topics[[DVSVXI409C/cert_test_1/data]]
```

## Trying out publishing to topic

Before running the demo, you need to configure the topic to be published to as `mTestTopic` (topic permission) in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). For more information on how to generate a topic, please see [Device Connection Through MQTT over TCP](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md).

Run the `main` function in [ShadowSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java). After the device is connected, call `publishTopic()` to publish to a device topic. Below is the sample code:
```
private static void publishTopic() {
    try {
        Thread.sleep(2000);
        // Data to be published
        Map<String, String> data = new HashMap<String, String>();
        // Vehicle type
        data.put("car_type", "suv");
        // Vehicle fuel consumption
        data.put("oil_consumption", "6.6");
        // Maximum vehicle speed
        data.put("maximum_speed", "205");

        // MQTT message
        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            LOG.error("pack json data failed!" + e.getMessage());
        }
        message.setQos(TXMqttConstants.QOS1);
        message.setPayload(jsonObject.toString().getBytes());

        // User context (request instance)
        MQTTRequest mqttRequest = new MQTTRequest("publishTopic", requestID.getAndIncrement());

        LOG.debug("pub topic " + mTestTopic + message);
        // Publish to the topic
        mShadowConnection.publish(mTestTopic, message, mqttRequest);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which a topic is published to successfully.
```
16/03/2021 09:45:46,807 [main] INFO  TXMqttConnection publish 502  - Starting publish topic: DVSVXI409C/cert_test_1/data Message: {"oil_consumption":"6.6","maximum_speed":"205","car_type":"suv"}
16/03/2021 09:45:46,817 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG ShadowSampleTest onPublishCompleted 259  - onPublishCompleted, status[OK], topics[[DVSVXI409C/cert_test_1/data]],  userContext[], errMsg[publish success]
```
