* [Getting Latest Information Reported by Device](#Getting-Latest-Information-Reported-by-Device)
  * [Publishing to topic for getting latest information reported by device](#Publishing-to-topic-for-getting-latest-information-reported-by-device)

# Getting Latest Information Reported by Device

When you create a product in the IoT Explorer console, a data template and some standard features will be generated for it by default. You can also customize the features. Such features are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

After a data template is defined for a product, the device can report attributes and events according to the definitions in the data template, and you can also deliver remote control instructions to the device to modify its writable attributes. For more information on how to manage a data template, please see Product Definition. The data template protocol includes device attribute reporting, remote device control, device-reported latest information acquisition, device event reporting, and device action triggering. For more information on the corresponding definitions and the topics used by the cloud to deliver control instructions, please see [Thing Model Protocol](https://cloud.tencent.com/document/product/1081/34916).

This document describes how to get the latest information reported by a device in a data template.

## Publishing to topic for getting latest information reported by device 

Run the `testMqttConnect` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `propertyGetStatus()` to publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}`

Below is the sample code:
```
private static void propertyGetStatus() {
    //get status
    if(Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
        LOG.error("property get report status failed!");
    }
    
    if(Status.OK != mDataTemplateSample.propertyGetStatus("control", false)) {
        LOG.error("property get control status failed!");
    }
}
```

Observe the logcat log.
```
23/02/2021 20:15:59,457 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"get_status","clientToken":"LWVUL5SZ2Llight13","showmeta":0,"type":"report"}
23/02/2021 20:15:59,459 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"get_status","clientToken":"LWVUL5SZ2Llight14","showmeta":0,"type":"control"}
23/02/2021 20:15:59,459 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
23/02/2021 20:15:59,460 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
23/02/2021 20:15:59,491 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"get_status_reply","clientToken":"LWVUL5SZ2Llight13","code":0,"status":"success","type":"report","data":{"reported":{"color":0,"power_switch":0,"name":"test","struct_param":{"bool_param":0,"enum_param":0,"float_param":0.0001,"string_param":"string","int_param":0,"timestamp_param":1577871650},"brightness":0}}}]
23/02/2021 20:15:59,495 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"get_status_reply","clientToken":"LWVUL5SZ2Llight14","code":0,"status":"success","type":"control","data":{}}]
```
The above log represents the process in which the topic for getting the latest information reported by device is published to successfully. If the device has subscribed to the topic, it will receive the `report` and `control` messages as described in the above log. In addition, you can also view the corresponding latest values of the device attributes in the console, which you will find the same as the attribute values in the `data` parameter of the received subscription message. For more information on how to view the device attributes and debug devices online, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

