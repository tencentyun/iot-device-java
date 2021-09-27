* [Broadcast Communication](#Broadcast-Communication)
  * [Overview](#Overview)
  * [Broadcast topic](#Broadcast-topic)
  * [Running demo for broadcast communication](#Running-demo-for-broadcast-communication)

# Broadcast Communication
## Overview
The IoT Hub platform provides a broadcast communication topic. The server can publish a broadcast message by calling the broadcast communication API, which can be received by online devices that have subscribed to the broadcast topic under the same product. For more information, please see [Broadcast Communication](https://cloud.tencent.com/document/product/634/47333).

## Broadcast topic
* The broadcast communication topic is `$broadcast/rxd/${ProductId}/${DeviceName}`, where `ProductId` and `DeviceName` represent the product ID and device name respectively.

## Running demo for broadcast communication

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java). After the device is connected, call `subscribeBroadCastTopic()` to subscribe to the broadcast topic. Below is the sample code:

```
private static void subscribeBroadCastTopic() {
    try {
        Thread.sleep(2000);
        // User context (request instance)
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
        // Subscribe to the broadcast topic
        mqttconnection.subscribeBroadcastTopic(TXMqttConstants.QOS1, mqttRequest);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following log represents the process in which the broadcast topic is subscribed to successfully.
```
17/03/2021 09:30:21,399 [MQTT Call: LWVUL5SZ2Llight3] DEBUG MqttSampleTest onSubscribeCompleted 359  - onSubscribeCompleted, status[OK], topics[[$broadcast/rxd/LWVUL5SZ2L/light3]], userContext[], errMsg[subscribe success]
```

