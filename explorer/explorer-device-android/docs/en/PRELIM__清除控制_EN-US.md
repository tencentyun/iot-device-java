* [Clearing Control](#Clearing-Control)
  * [Publishing to topic for clearing control](#Publishing-to-topic-for-clearing-control)

# Clearing Control

This document describes how a device delivers an instruction to clear control.

## Publishing to topic for clearing control 

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Clear Control**, concatenate the device information parameters, and publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}`

Below is the sample code:
```
if(Status.OK !=  mDataTemplateSample.propertyClearControl()){
    mParent.printLogInfo(TAG, "clear control failed!", mLogInfoText, TXLog.LEVEL_ERROR);
}
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"clear_control","clientToken":"LWVUL5SZ2Llight14"}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the topic for clearing control is published to successfully.

