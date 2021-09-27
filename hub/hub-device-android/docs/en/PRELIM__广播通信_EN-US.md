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

Please connect the device to MQTT for authenticated connection as instructed in [Device Connection Through MQTT over TCP](../../hub-device-android/docs/Device-Connection-Through-MQTT-over-TCP.md) first.
Run the demo and click **Subscribe to Broadcast Topic** in the basic feature module to subscribe to the broadcast topic. Below is the sample code:

```
mMQTTSample.subscribeBroadCastTopic(); // Subscribe to the broadcast topic
```

The following log represents the process in which the broadcast topic is subscribed to successfully.
```
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[$broadcast/rxd/AP9ZLEVFKT/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```

