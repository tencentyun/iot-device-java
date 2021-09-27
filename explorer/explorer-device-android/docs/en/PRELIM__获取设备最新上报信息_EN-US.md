* [Getting Latest Information Reported by Device](#Getting-Latest-Information-Reported-by-Device)
  * [Publishing to topic for getting latest information reported by device](#Publishing-to-topic-for-getting-latest-information-reported-by-device)

# Getting Latest Information Reported by Device

When you create a product in the IoT Explorer console, a data template and some standard features will be generated for it by default. You can also customize the features. Such features are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

After a data template is defined for a product, the device can report attributes and events according to the definitions in the data template, and you can also deliver remote control instructions to the device to modify its writable attributes. For more information on how to manage a data template, please see Product Definition. The data template protocol includes device attribute reporting, remote device control, device-reported latest information acquisition, device event reporting, and device action triggering. For more information on the corresponding definitions and the topics used by the cloud to deliver control instructions, please see [Thing Model Protocol](https://cloud.tencent.com/document/product/1081/34916).

This document describes how the SDK demo gets the latest information reported by a device from the data template.

## Publishing to topic for getting latest information reported by device 

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Update Status** to publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}`

Below is the sample code:
```
if(Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
    mParent.printLogInfo(TAG, "property get status failed!", mLogInfoText, TXLog.LEVEL_ERROR);
}
if(Status.OK != mDataTemplateSample.propertyGetStatus("control", false)) {
    mParent.printLogInfo(TAG, "property get status failed!", mLogInfoText, TXLog.LEVEL_ERROR);
}
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"get_status","clientToken":"LWVUL5SZ2Llight14","type":"report","showmeta":0}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"get_status","clientToken":"LWVUL5SZ2Llight15","type":"control","showmeta":0}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
D/TXDataTemplateFragment: receive command, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"get_status_reply","clientToken":"LWVUL5SZ2Llight14","code":0,"status":"success","type":"report","data":{"reported":{"power_switch":0,"name":"test","brightness":0,"color":0}}}]
D/TXDATATEMPLATE: handle_reply: reply OK! ClientToken:LWVUL5SZ2Llight14
D/TXDataTemplateFragment: event down stream message received : {"reported":{"power_switch":0,"name":"test","brightness":0,"color":0}}
D/TXDataTemplateFragment: receive command, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"get_status_reply","clientToken":"LWVUL5SZ2Llight15","code":0,"status":"success","type":"control","data":{}}]
D/TXDATATEMPLATE: handle_reply: reply OK! ClientToken:LWVUL5SZ2Llight15
D/TXDataTemplateFragment: event down stream message received : {}
```
The above log represents the process in which the topic for getting the latest information reported by device is published to successfully. If the device has subscribed to the topic, it will receive the `report` and `control` messages as described in the above log. In addition, you can also view the corresponding latest values of the device attributes in the console, which you will find the same as the attribute values in the `data` parameter of the received subscription message. For more information on how to view the device attributes and debug devices online, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

