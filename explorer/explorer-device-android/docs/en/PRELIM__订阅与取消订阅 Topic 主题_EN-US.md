* [Subscribing and Unsubscribing](#Subscribing-and-Unsubscribing)
  * [Subscribing to topic associated with data template](#Subscribing-to-topic-associated-with-data-template)
  * [Unsubscribing from topic](#Unsubscribing-from-topic)

# Subscribing and Unsubscribing

When you create a product in the IoT Explorer console, a data template and some standard features will be generated for it by default. You can also customize the features. Such features are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

After a data template is defined for a product, the device can report attributes and events according to the definitions in the data template, and you can also deliver remote control instructions to the device to modify its writable attributes. For more information on how to manage a data template, please see Product Definition. The data template protocol includes device attribute reporting, remote device control, device-reported latest information acquisition, device event reporting, and device action triggering. For more information on the corresponding definitions and the topics used by the cloud to deliver control instructions, please see [Thing Model Protocol](https://cloud.tencent.com/document/product/1081/34916).

This document describes how the SDK demo subscribes to/unsubscribes from a topic associated with a data template.

## Subscribing to topic associated with data template

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Subscribe to Topic** to subscribe to the attribute, event, and action topics:
```
$thing/down/property/{ProductID}/{DeviceName}
$thing/down/event/{ProductID}/{DeviceName}
$thing/down/action/{ProductID}/{DeviceName}
```
Below is the sample code:
```
mDataTemplateSample.subscribeTopic(); // Subscribe to the topic
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $thing/down/property/LWVUL5SZ2L/light1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $thing/down/event/LWVUL5SZ2L/light1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $thing/down/action/LWVUL5SZ2L/light1
D/TXDataTemplateFragment: onSubscribeCompleted, status[OK], topics[[$thing/down/property/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[subscribe success]
D/TXDataTemplateFragment: onSubscribeCompleted, status[OK], topics[[$thing/down/event/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
D/TXDataTemplateFragment: onSubscribeCompleted, status[OK], topics[[$thing/down/action/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[subscribe success]
```
The above log represents the process in which the topics are subscribed to successfully.

## Unsubscribing from topic

Run the demo. After the device subscribes to the topics successfully, click **Unsubscribe from Topic** to unsubscribe from the attribute, event, and action topics:
```
$thing/down/property/{ProductID}/{DeviceName}
$thing/down/event/{ProductID}/{DeviceName}
$thing/down/action/{ProductID}/{DeviceName}
```
Below is the sample code:
```
mDataTemplateSample.unSubscribeTopic(); // Unsubscribe from the topic
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $thing/down/property/LWVUL5SZ2L/light1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $thing/down/event/LWVUL5SZ2L/light1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $thing/down/action/LWVUL5SZ2L/light1
D/TXDataTemplateFragment: onUnSubscribeCompleted, status[OK], topics[[$thing/down/property/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=3}], errMsg[unsubscribe success]
D/TXDataTemplateFragment: onUnSubscribeCompleted, status[OK], topics[[$thing/down/action/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=5}], errMsg[unsubscribe success]
D/TXDataTemplateFragment: onUnSubscribeCompleted, status[OK], topics[[$thing/down/event/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=4}], errMsg[unsubscribe success]
```
The above log represents the process in which the topics are unsubscribed from successfully.
