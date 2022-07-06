* [Device Interconnection](#Device-Interconnection)
  * [Overview](#Overview)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Running demo to try out device interconnection module](#Running-demo-to-try-out-device-interconnection-module)
  * [Trying out homecoming for door device](#Trying-out-homecoming-for-door-device)
  * [Trying out homeleaving for door device](#Trying-out-homeleaving-for-door-device)

# Device Interconnection
## Overview
This document describes how to try out device interconnection based on cross-device messaging and the rule engine in a smart home scenario with the aid of the IoT Hub device SDK for Android. For more information, please see [Scenario 1: Device Interconnection](https://cloud.tencent.com/document/product/634/11913).

To try out device interconnection, you need to create two types of smart devices (`Door` and `AirConditioner`) as instructed in the documentation. You also need to configure the rule engine as instructed in [Overview](https://cloud.tencent.com/document/product/634/14446) and [forward the data to another topic](https://cloud.tencent.com/document/product/634/14449).

## Entering parameters for authenticating device for connection

The `Door` model corresponds to the [Door.java](../../../hub-android-demo/src/main/java/com/tencent/iot/hub/device/android/app/scenarized/Door.java) file in the SDK demo. You need to enter the corresponding parameters in [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json).

```
{
  ...
  "DOOR_PRODUCT_ID":            "",
  "DOOR_DEVICE_NAME":           "",
  "DOOR_DEVICE_PSK":            "",
  ...
}
```

The `Airconditioner` model corresponds to the [Airconditioner.java](../../../hub-android-demo/src/main/java/com/tencent/iot/hub/device/android/app/scenarized/Airconditioner.java) file in the SDK demo. You need to enter the corresponding parameters in [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json).

```
{
  ...
  "AIRCONDITIONER_PRODUCT_ID":  "",
  "AIRCONDITIONER_DEVICE_NAME": "",
  "AIRCONDITIONER_DEVICE_PSK":  ""
}
```
Note: the `Door` and `Airconditioner` in the demo connect to IoT Hub through device key. If you want to connect through certificate/device private key, you need to configure the following two fields in the [Door.java](../../../hub-android-demo/src/main/java/com/tencent/iot/hub/device/android/app/scenarized/Door.java) and [Airconditioner.java](../../../hub-android-demo/src/main/java/com/tencent/iot/hub/device/android/app/scenarized/Airconditioner.java) files:
`DEVICE_KEY_NAME` and `DEVICE_CERT_NAME`

## Running demo to try out device interconnection module

When the MQTT authentication method is key authentication, you don't need to add the SSL configuration to `MqttOptions`; instead, you can use TCP. When you use SDK v3.3.0 or below, key authentication requires adding the SSL configuration `options.setSocketFactory(AsymcSslUtils.getSocketFactory());` to `MqttOptions`.

When the MQTT authentication method is certificate authentication, you need to add the SSL configuration `options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, DEVICE_CERT_NAME, DEVICE_KEY_NAME));` to `MqttOptions`.

Run the demo, switch the tab at the bottom to select the device interconnection module, initialize `Airconditioner`, and connect it to MQTT through authenticated connection. After it is connected, subscribe to its own `${productId}/${deviceName}/control` topic. Below is the sample code:

```
@Override
public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mDoor = new Door(IoTEntryFragment.this.getContext()); // Initialize the `Door` instance without authenticated connection to MQTT
    mAir = new Airconditioner(this.getContext(), new AirMqttActionCallBack()); // Initialize the `Airconditioner` instance for authenticated connection to MQTT and set the MQTT callback
}

private class AirMqttActionCallBack extends TXMqttActionCallBack {
    @Override
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) { // Authenticated MQTT connection is completed
        if (status.equals(Status.OK)) {
            mAir.subScribeTopic(); // Subscribe to its own topic
        }
    }
    ...
    @Override
    public void onMessageReceived(String topic, MqttMessage message) { // Receive a message from the cloud
        String logInfo;
        if (message.toString().contains("come_home")) {
            logInfo = "receive command: open airconditioner, count: " + atomicInteger.getAndIncrement();
        } else {
            logInfo = "receive command: close airconditioner, count: " + atomicInteger.getAndIncrement();
        }
        mParent.printLogInfo(TAG, logInfo, textView);
    }
}
```

The following logcat log represents the process in which the `Airconditioner` device is connected successfully and subscribes to its own topic. In the console, you can see that the status of the `Airconditioner` device has been updated to `online`.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: XMN6AZ4M0Y/airConditioner1/control
I/IoTEntryActivity: onSubscribeCompleted, status[OK], message[subscribe success]
```

## Trying out homecoming for door device

In the device interconnection module, click **Come Home**, and it will trigger the authentication of the `Door` device for connection to MQTT. After the device is connected successfully, it will subscribe to its own `${productId}/${deviceName}/event` topic. As the rule engine in the cloud is configured to forward the data, it will forward messages from the topic to the `Airconditioner` device. Below is the sample code:
```
public void enterRoom() {
    ...
    mqttConnection = new TXMqttConnection(mContext, PRODUCT_ID, DEVICE_NAME, SECRET_KEY, new DoorMqttActionCallBack());
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
I/TXMQTT_1.2.3: Start connecting to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883
I/iot.scenarized.Door: onConnectCompleted:connected to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: 9RW4A8OOFK/door1/event Message: {"action": "come_home", "targetDevice": "airConditioner1"}
I/TXMQTT_1.2.3: Received topic: XMN6AZ4M0Y/airConditioner1/control, id: 350, message: {"action":"come_home","targetDevice":"airConditioner1"}
D/IoTEntryActivity: receive command: open airconditioner, count: 0
```
The above log represents the process in which the `Door` device is connected to MQTT successfully, publishes to its own topic, and carries the `message` with the `action` being `come_home`, and then the `Airconditioner` device also receives the homecoming topic message forwarded by the rule engine in the cloud, thus fulfilling the purpose of turning on the air conditioner upon homecoming.

## Trying out homeleaving for door device

In the device interconnection module, click **Leave Home** to trigger the `Door` device to publish to its own `${productId}/${deviceName}/event` topic. As the rule engine in the cloud is configured to forward the data, it will forward messages from the topic to the `Airconditioner` device, and the `Door` device will disconnect from MQTT. Below is the sample code:
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
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: 9RW4A8OOFK/door1/event Message: {"action": "leave_home", "targetDevice": "airConditioner1"}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: deliveryComplete, token.getMessageId:2
I/iot.scenarized.Door: onDisconnectCompleted, status[OK], msg[disconnected to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883]
I/TXMQTT_1.2.3: Received topic: XMN6AZ4M0Y/airConditioner1/control, id: 351, message: {"action":"leave_home","targetDevice":"airConditioner1"}
D/IoTEntryActivity: receive command: close airconditioner, count: 1
```
The above log represents the process in which the `Door` device subscribes and publishes to its own topic, carries the `message` message with the `action` being `leave_home`, and disconnects from MQTT, and then the `Airconditioner` device also receives the homeleaving topic message forwarded by the rule engine in the cloud, thus fulfilling the purpose of turning off the air conditioner upon homeleaving.
