* [Clearing Control](#Clearing-Control)
  * [Publishing to topic for clearing control](#Publishing-to-topic-for-clearing-control)

# Clearing Control

This document describes how a device delivers an instruction to clear control.

## Publishing to topic for clearing control 

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `propertyClearControl()` to publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}`

Below is the sample code:
```
private static void propertyClearControl() {
    //clear control
    if(Status.OK !=  mDataTemplateSample.propertyClearControl()){
        LOG.error("clear control failed!");
    }
}
```

Observe the logcat log.
```
23/02/2021 20:33:23,951 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"clear_control","clientToken":"LWVUL5SZ2Llight13"}
23/02/2021 20:33:23,953 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
23/02/2021 20:33:23,991 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"clear_control_reply","clientToken":"LWVUL5SZ2Llight13","code":0,"status":"success"}]
```
The above log represents the process in which the topic for clearing control is published to successfully.

