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

[Downloading the sample code of IoT Hub SDK for Java demo](../README.md#Downloading-the-sample-code-of-IoT-Hub-SDK-for-Java-demo)

#### Entering parameters for authenticating device for connection
Edit the parameter configuration information in the [unit_test_config.json](../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTMQTTSAMPLE_PRODUCT_ID":           "",
  "TESTMQTTSAMPLE_DEVICE_NAME":          "",
  "TESTMQTTSAMPLE_DEVICE_PSK":           "",
  "TESTMQTTSAMPLE_TEST_TOPIC":           "",
}
```
If key authentication is used during device creation in the console, you need to enter `TESTMQTTSAMPLE_PRODUCT_ID` (product ID), `TESTMQTTSAMPLE_DEVICE_NAME` (device name), and `TESTMQTTSAMPLE_DEVICE_PSK` (device key) in `unit_test_config.json`;

If certificate authentication is used during device creation in the console, in addition to entering `TESTMQTTSAMPLE_PRODUCT_ID` (product ID) and `TESTMQTTSAMPLE_DEVICE_NAME` (device name) in `unit_test_config.json`, you also need to set `mDevPSK` to `null` in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java), place the certificate and private key in the [resources](../src/test/resources/) folder, and enter `mCertFilePath` (device certificate file name) and `mPrivKeyFilePath` (device private key file name).

```
private static String mCertFilePath = "";           // Enter the name of the device certificate file in the `resources` folder
private static String mPrivKeyFilePath = "";           // Enter the name of the device private key file in the `resources` folder
```

If certificate authentication is used during device creation in the console, you can also enter `mDevCert` (device certificate file content) and `mDevPriv` (device private key file content) by copying the certificate and private key content.

```
private static String mDevCert = "";           // Enter the device certificate file content
private static String mDevPriv = "";           // Enter the device private key file content
```

#### Running demo for authenticated MQTT connection

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) and call `connnect()` to connect the device. Below is the sample code:
```
private static void connect() {
    try {
        Thread.sleep(2000);
        String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";

        // init connection
        options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        // Client certificate file name. `mDevPSK` is the device key

        if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
            LOG.info("Using cert stream " + mDevPriv + "  " + mDevCert);
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        } else if (mDevPSK != null && mDevPSK.length() != 0){
            LOG.info("Using PSK");

        } else {
            LOG.info("Using cert file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
        }
        mqttconnection = new TXMqttConnection(mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,true, new SelfMqttLogCallBack(), new callBack());
        mqttconnection.connect(options, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The above log represents the process in which the device is connected to the cloud over MQTT successfully.
```
26/02/2021 09:42:50,157 [main] INFO  TXMqttConnection connect 338  - Start connecting to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883
26/02/2021 09:42:53,654 [MQTT Call: 9RW4A8OOFKdoor1] INFO  MqttSampleTest onConnectCompleted 141  - onConnectCompleted, status[OK], reconnect[false], userContext[], msg[connected to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883]
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

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). After the device is connected, call `disconnect()`. Below is the sample code:
```
private static void disconnect() {
    try {
        Thread.sleep(2000);
        mqttconnection.disConnect(null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device is disconnected from MQTT successfully.
```
26/02/2021 09:46:34,248 [MQTT Disc: 9RW4A8OOFKdoor1] INFO  MqttSampleTest onDisconnectCompleted 207  - onDisconnectCompleted, status[OK], userContext[], msg[disconnected to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883]
```

#### Subscribing to topic
Before running the demo, you need to configure the topic to be subscribed to as `mTestTopic` (topic permission) in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). For more information on how to generate a topic, please see [Creating device in console](#Creating-device-in-console).

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). After the device is connected successfully, call `subscribeTopic()` to subscribe to a topic. Below is the sample code:
```
private static void subscribeTopic() {
    try {
        Thread.sleep(2000);
        mqttconnection.subscribe(mTestTopic, 1, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device subscribes to a topic successfully.
```
26/02/2021 09:51:23,240 [main] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: 9RW4A8OOFK/door1/data
26/02/2021 09:51:23,257 [MQTT Call: 9RW4A8OOFKdoor1] DEBUG MqttSampleTest onSubscribeCompleted 235  - onSubscribeCompleted, status[OK], topics[[9RW4A8OOFK/door1/data]], userContext[], errMsg[subscribe success]
```

#### Unsubscribing from topic
A device can unsubscribe from a previously subscribed topic.

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). After the device is connected successfully, call `unSubscribeTopic()` to subscribe from a topic. Below is the sample code:
```
private static void unSubscribeTopic() {
    try {
        Thread.sleep(2000);
        mqttconnection.unSubscribe(mTestTopic, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device unsubscribes from a topic successfully.
```
26/02/2021 09:53:36,372 [main] INFO  TXMqttConnection unSubscribe 712  - Starting unSubscribe topic: 9RW4A8OOFK/door1/data
26/02/2021 09:51:23,257 [MQTT Call: 9RW4A8OOFKdoor1] DEBUG MqttSampleTest onUnSubscribeCompleted 235  - onUnSubscribeCompleted, status[OK], topics[[9RW4A8OOFK/door1/data]], userContext[MQTTRequest{requestType='unSubscribeTopic', requestId=6}], errMsg[unsubscribe success]
```

#### Publishing to topic
Before running the demo, you need to configure the topic to be published to as `mTestTopic` (topic permission) in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). For more information on how to generate a topic, please see [Creating device in console](#Creating-device-in-console).

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). After the device is connected successfully, call `publishTopic()` to publish to a topic. Below is the sample code:
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
        // Temperature information
        data.put("temperature", "25");
        // MQTT message
        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage()+"pack json data failed!");
        }
        message.setQos(TXMqttConstants.QOS1);
        message.setPayload(jsonObject.toString().getBytes());

        LOG.debug("pub topic " + mTestTopic + message);
        // Publish to the topic
        mqttconnection.publish(mTestTopic, message, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device publishes to a topic successfully.
```
26/02/2021 10:02:40,763 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: 9RW4A8OOFK/door1/data Message: {"oil_consumption":"6.6","temperature":"25","maximum_speed":"205","car_type":"suv"}
26/02/2021 10:02:40,774 [MQTT Call: 9RW4A8OOFKdoor1] DEBUG MqttSampleTest onPublishCompleted 279  - onPublishCompleted, status[OK], topics[[9RW4A8OOFK/door1/data]],  userContext[], errMsg[publish success]
```
