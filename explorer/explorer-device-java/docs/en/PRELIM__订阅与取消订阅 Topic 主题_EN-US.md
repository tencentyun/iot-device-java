* [Subscribing and Unsubscribing](#Subscribing-and-Unsubscribing)
  * [Subscribing to topic associated with data template](#Subscribing-to-topic-associated-with-data-template)
  * [Unsubscribing from topic](#Unsubscribing-from-topic)

# Subscribing and Unsubscribing

When you create a product in the IoT Explorer console, a data template and some standard features will be generated for it by default. You can also customize the features. Such features are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

After a data template is defined for a product, the device can report attributes and events according to the definitions in the data template, and you can also deliver remote control instructions to the device to modify its writable attributes. For more information on how to manage a data template, please see Product Definition. The data template protocol includes device attribute reporting, remote device control, device-reported latest information acquisition, device event reporting, and device action triggering. For more information on the corresponding definitions and the topics used by the cloud to deliver control instructions, please see [Thing Model Protocol](https://cloud.tencent.com/document/product/1081/34916).

This document describes how to subscribe to/unsubscribe from a topic associated with a data template.

## Subscribing to topic associated with data template

Run the `testMqttConnect` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted`, call `subscribeTopic()` to subscribe to the attribute, event, and action topics associated with the data template:
```
$thing/down/property/{ProductID}/{DeviceName}
$thing/down/event/{ProductID}/{DeviceName}
$thing/down/action/{ProductID}/{DeviceName}
```
Below is the sample code:
```
private static void subscribeTopic() {
    mDataTemplateSample.subscribeTopic();
}
```

Observe the logcat log.
```
23/02/2021 19:39:50,660 [MQTT Call: LWVUL5SZ2Llight1] INFO  MqttSampleTest onConnectCompleted 288  - onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=0}], msg[connected to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883]
23/02/2021 19:39:52,686 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onSubscribeCompleted 330  - onSubscribeCompleted, status[OK], topics[[$thing/down/property/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[subscribe success]
23/02/2021 19:39:52,691 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onSubscribeCompleted 330  - onSubscribeCompleted, status[OK], topics[[$thing/down/action/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[subscribe success]
23/02/2021 19:39:52,691 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onSubscribeCompleted 330  - onSubscribeCompleted, status[OK], topics[[$thing/down/event/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```
The above log represents the process in which the topics are subscribed to successfully.

## Unsubscribing from topic

Run the `testMqttConnect` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `unSubscribeTopic()` to unsubscribe from the attribute, event, and action topics:
```
$thing/down/property/{ProductID}/{DeviceName}
$thing/down/event/{ProductID}/{DeviceName}
$thing/down/action/{ProductID}/{DeviceName}
```
Below is the sample code:
```
private static void unSubscribeTopic() {
    mDataTemplateSample.unSubscribeTopic();
}
```

Observe the logcat log.
```
23/02/2021 19:44:28,232 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onUnSubscribeCompleted 342  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/property/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[unsubscribe success]
23/02/2021 19:44:28,236 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onUnSubscribeCompleted 342  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/action/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[unsubscribe success]
23/02/2021 19:44:28,236 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onUnSubscribeCompleted 342  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/event/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[unsubscribe success]
```
The above log represents the process in which the topics are unsubscribed from successfully.
