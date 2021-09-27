* [Device Interconnection](#Device-Interconnection)
  * [Overview](#Overview)
  * [Compiling and running demo](#Compiling-and-running-demo)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Connection authentication overview](#Connection-authentication-overview)
  * [Trying out homecoming for door device](#Trying-out-homecoming-for-door-device)
  * [Trying out homeleaving for door device](#Trying-out-homeleaving-for-door-device)

# Device Interconnection
## Overview
This document describes how to try out device interconnection based on cross-device messaging and the rule engine in a smart home scenario with the aid of the IoT Hub device SDK for Java. For more information, please see [Scenario 1: Device Interconnection](https://cloud.tencent.com/document/product/634/11913).

To try out device interconnection, you need to create two types of smart devices (`Door` and `AirConditioner`) as instructed in the documentation. You also need to configure the rule engine as instructed in [Overview](https://cloud.tencent.com/document/product/634/14446) and [forward the data to another topic](https://cloud.tencent.com/document/product/634/14449).

## Compiling and running demo

#### Downloading the sample code of IoT Hub SDK for Java demo

The sample code and source code are in the [hub-device-java](../../../hub-device-java) module.

#### Preparing development environment

The development environment used in this demo is as follows:

* OS: macOS
* JDK version: [JDK13](https://www.oracle.com/java/technologies/javase-jdk13-downloads.html)
* IDE: [IntelliJ IDEA CE](https://www.jetbrains.com/idea/)

#### Checking SDK dependencies

Source code dependencies are used in the demo. You can also add dependencies in Maven. For more information, please see [Project configuration](../../../hub-device-java/PRELIM__README_EN-US.md#Project-configuration).

## Entering parameters for authenticating device for connection

The `Door` model corresponds to the [Door.java](../../src/test/java/main/scenarized/Door.java) file in the SDK demo. You need to enter the corresponding parameters in the file.

```
public class Door {
    private static final String PRODUCT_ID = "YOUR_PRODUCT_ID"; // Product ID
    private static final String DEVICE_NAME = "YOUR_DEVICE_NAME"; // Device name
    private static final String SECRET_KEY = "YOUR_SECRET_KEY"; // Device key (for key authentication)
    private static final String DEVICE_CERT_NAME = "YOUR_DEVICE_NAME_cert.crt"; // Device certificate file name (for certificate authentication)
    private static final String DEVICE_KEY_NAME = "YOUR_DEVICE_NAME_private.key"; // Device private key file name (for certificate authentication)
}
```

The `Airconditioner` model corresponds to the [Airconditioner.java](../../src/test/java/main/scenarized/Airconditioner.java) file in the SDK demo. You need to enter the corresponding parameters in the file.

```
public class Airconditioner {
    private static final String PRODUCT_ID = "YOUR_PRODUCT_ID"; // Product ID
    protected static final String DEVICE_NAME = "YOUR_DEVICE_NAME"; // Device name
    private static final String SECRET_KEY = "YOUR_SECRET_KEY"; // Device key (for key authentication)
    private static final String DEVICE_CERT_NAME = "YOUR_DEVICE_NAME_cert.crt"; // Device certificate file name (for certificate authentication)
    private static final String DEVICE_KEY_NAME = "YOUR_DEVICE_NAME_private.key"; // Device private key file name (for certificate authentication)
}
```

## Connection authentication overview

When the MQTT authentication method is key authentication, you don't need to add the SSL configuration to `MqttConnectOptions`; instead, you can use TCP. When you use SDK v3.3.0 or below, key authentication requires adding the SSL configuration `options.setSocketFactory(AsymcSslUtils.getSocketFactory());` to `MqttOptions`.

When the MQTT authentication method is certificate authentication, you need to add the SSL configuration `options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, DEVICE_CERT_NAME, DEVICE_KEY_NAME));` to `MqttConnectOptions`.

Run [DeviceInterworkingApp.java](../../src/test/java/main/DeviceInterworkingApp.java), initialize `Airconditioner`, and connect it to MQTT through authenticated connection. After it is connected, subscribe to its own `${productId}/${deviceName}/control` topic. Below is the sample code:

```
public static void main(String[] args) {
    mAir = new Airconditioner(new AirconditionerMqttActionCallBack()); // Initialize the `Airconditioner` instance for authenticated connection to MQTT and set the MQTT callback
    mDoor = new Door(); // Initialize the `Door` instance without authenticated connection to MQTT
}

private static class AirconditionerMqttActionCallBack extends TXMqttActionCallBack {
    @Override
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) { // Authenticated MQTT connection is completed
        if (status.equals(Status.OK)) {
            mAir.subScribeTopic(); // Subscribe to its own topic
        }
    }
    ...
    @Override
    public void onMessageReceived(String topic, MqttMessage message) { // Receive a message from the cloud
        String logInfo = String.format("Airconditioner onMessageReceived, topic[%s], message[%s]", topic, message.toString());
        System.out.println(logInfo);
        if (message.toString().contains("come_home")) {
            logInfo = "receive command: open airconditioner ";
        } else {
            logInfo = "receive command: close airconditioner ";
        }
        System.out.println(logInfo);
    }
}
```

Observe the logcat log.
```
TXMqttConnection connect 297  - Start connecting to ssl://XMN6AZ4M0Y.iotcloud.tencentdevices.com:8883
[MQTT Call: XMN6AZ4M0YairConditioner1] INFO  TXMqttConnection onSuccess 268  - onSuccess!
connected to ssl://XMN6AZ4M0Y.iotcloud.tencentdevices.com:8883
TXMqttConnection subscribe 633  - Starting subscribe topic: XMN6AZ4M0Y/airConditioner1/control
onSubscribeCompleted, status[OK], message[subscribe success]
```
The above log represents the process in which the `Airconditioner` device is connected to MQTT and subscribes to its own topic successfully. In the console, you can see that the status of the created `Airconditioner` device has been updated to `online`.

## Trying out homecoming for door device

When the `Door` instance calls the `enterRoom` method, it will connect to MQTT through authenticated connection. After it is connected successfully, it will subscribe to its own `${productId}/${deviceName}/event` topic. As the rule engine in the cloud is configured to forward the data, it will forward messages from the topic to the `Airconditioner` device. Below is the sample code:
```
public void enterRoom() {
    ...
    mqttConnection = new TXMqttConnection(PRODUCT_ID, DEVICE_NAME, SECRET_KEY, new DoorMqttActionCallBack());
    mqttConnection.connect(options, null);
    ...
    if (mqttConnection.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected)) { // Directly publish to its own topic if it is connected
        ...
        mqttConnection.publish(topic, message, null);
    }
    ...
}

private class DoorMqttActionCallBack extends TXMqttActionCallBack {
    @Override
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) { // Authenticated MQTT connection is completed
        if (status.equals(Status.OK)) { // Connected successfully
            if (!reconnect) { // Publish to its own topic if it is not reconnected
                MqttMessage message = new MqttMessage();
                message.setPayload(COME_HOME_MESSAGE.getBytes());
                String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "event");
                mqttConnection.publish(topic, message, null);
            }
         }
    }
...
}
```

Observe the logcat log.
```
TXMqttConnection connect 297  - Start connecting to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883
[MQTT Call: 9RW4A8OOFKdoor1] INFO  TXMqttConnection onSuccess 268  - onSuccess!
TXMqttConnection publish 451  - Starting publish topic: 9RW4A8OOFK/door1/event Message: {"action": "come_home", "targetDevice": "airConditioner1"}
Airconditioner onMessageReceived, topic[XMN6AZ4M0Y/airConditioner1/control], message[{"action":"come_home","targetDevice":"airConditioner1"}]
receive command: open airconditioner 
```
The above log represents the process in which the `Door` device is connected to MQTT successfully, publishes to its own topic, and carries the `message` with the `action` being `come_home`, and then the `Airconditioner` device also receives the homecoming topic message forwarded by the rule engine in the cloud, thus fulfilling the purpose of turning on the air conditioner upon homecoming.

## Trying out homeleaving for door device

When the `Door` instance calls the `leaveRoom` method, it will trigger the `Door` device to publish to its own `${productId}/${deviceName}/event` topic. As the rule engine in the cloud is configured to forward the data, it will forward messages from the topic to the `Airconditioner` device, and the `Door` device will disconnect from MQTT. Below is the sample code:
```
public void leaveRoom() {
    ...
    MqttMessage message = new MqttMessage();
    message.setPayload(LEAVE_HOME_MESSAGE.getBytes());
    String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "event");
    mqttConnection.publish(topic, message, null); // Publish the message to its own topic which carries `LEAVE_HOME_MESSAGE` (leave_home)
    closeConnection(); // Disconnect the `Door` device from MQTT
}
```

Observe the logcat log.
```
TXMqttConnection publish 451  - Starting publish topic: 9RW4A8OOFK/door1/event Message: {"action": "leave_home", "targetDevice": "airConditioner1"}
Airconditioner onMessageReceived, topic[XMN6AZ4M0Y/airConditioner1/control], message[{"action":"leave_home","targetDevice":"airConditioner1"}]
receive command: close airconditioner 
```
The above log represents the process in which the `Door` device subscribes and publishes to its own topic, carries the `message` message with the `action` being `leave_home`, and disconnects from MQTT, and then the `Airconditioner` device also receives the homeleaving topic message forwarded by the rule engine in the cloud, thus fulfilling the purpose of turning off the air conditioner upon homeleaving.
