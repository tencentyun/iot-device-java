* [Event Reporting and Multi-Event Reporting](#Event-Reporting-and-Multi-Event-Reporting)
  * [Publishing to topic for reporting event](#Publishing-to-topic-for-reporting-event)
  * [Publishing to topic for reporting multiple events](#Publishing-to-topic-for-reporting-multiple-events)

# Event Reporting and Multi-Event Reporting

This document describes how a device publishes to topics for event reporting and multi-event reporting.

## Publishing to topic for reporting event 

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `eventSinglePost()` to publish to the event topic:
`$thing/up/event/{ProductID}/{DeviceName}`

Below is the sample code:
```
private static void eventSinglePost() {
    String eventId = "status_report";
    String type = "info";
    JSONObject params = new JSONObject();
    try {
        params.put("status",0);
        params.put("message","");
    } catch (JSONException e) {
        LOG.error("Construct params failed!");
    }
    if(Status.OK != mDataTemplateSample.eventSinglePost(eventId, type, params)){
        LOG.error("single event post failed!");
    }
}
```

Observe the logcat log.
```
23/02/2021 20:43:58,024 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/event/LWVUL5SZ2L/light1 Message: {"eventId":"status_report","method":"event_post","clientToken":"LWVUL5SZ2Llight13","type":"info","params":{"message":"","status":0},"timestamp":1614084238023}
23/02/2021 20:43:58,026 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$thing/up/event/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
23/02/2021 20:43:58,064 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/event/LWVUL5SZ2L/light1], message[{"method":"event_reply","clientToken":"LWVUL5SZ2Llight13","code":0,"status":"","data":{}}]
```
The above log represents the process in which the device publishes to the topic for reporting a single event successfully. If the device has subscribed to the topic, it will receive the `event_reply` message as described in the above log, In the information of the device created in the console, you can view the corresponding device event. If the `type` passed in is `info`, the event is of the information type. For more information on how to view device events in the console, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

## Publishing to topic for reporting multiple events 

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `eventsPost()` to publish to the event topic:
`$thing/up/event/{ProductID}/{DeviceName}`

Below is the sample code:
```
private static void eventsPost() {
    JSONArray events = new JSONArray();
    
    //event:status_report
    try {
        JSONObject event = new JSONObject();
        event.put("eventId","status_report");
        event.put("type", "info");
        event.put("timestamp", System.currentTimeMillis());
    
        JSONObject params = new JSONObject();
        params.put("status",0);
        params.put("message","");
    
        event.put("params", params);
    
        events.put(event);
    } catch (JSONException e) {
        LOG.error("Construct params failed!");
        return;
    }
    
    //event:low_voltage
    try {
        JSONObject event = new JSONObject();
        event.put("eventId","low_voltage");
        event.put("type", "alert");
        event.put("timestamp", System.currentTimeMillis());
    
        JSONObject params = new JSONObject();
        params.put("voltage",1.000000f);
    
        event.put("params", params);
    
        events.put(event);
    } catch (JSONException e) {
        LOG.error("Construct params failed!");
        return;
    }
    
    //event:hardware_fault
    try {
        JSONObject event = new JSONObject();
        event.put("eventId","hardware_fault");
        event.put("type", "fault");
        event.put("timestamp", System.currentTimeMillis());
    
        JSONObject params = new JSONObject();
        params.put("name","");
        params.put("error_code",1);
    
        event.put("params", params);
    
        events.put(event);
    } catch (JSONException e) {
        LOG.error("Construct params failed!");
        return;
    }
    
    if(Status.OK != mDataTemplateSample.eventsPost(events)){
        LOG.error("events post failed!");
    }
}
```

Observe the logcat log.
```
23/02/2021 21:08:28,081 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/event/LWVUL5SZ2L/light1 Message: {"method":"events_post","clientToken":"LWVUL5SZ2Llight13","events":[{"eventId":"status_report","type":"info","params":{"message":"","status":0},"timestamp":1614085664165},{"eventId":"low_voltage","type":"alert","params":{"voltage":1},"timestamp":1614085664165},{"eventId":"hardware_fault","type":"fault","params":{"name":"","error_code":1},"timestamp":1614085664165}]}
23/02/2021 21:08:28,084 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$thing/up/event/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
23/02/2021 21:08:28,115 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/event/LWVUL5SZ2L/light1], message[{"method":"events_reply","clientToken":"LWVUL5SZ2Llight13","code":0,"status":"","data":{}}]
```
The above log represents the process in which the device publishes to the topic for reporting multiple events successfully. If the device has subscribed to the topic, it will receive the `events_reply` message as described in the above log. In the information of the device created in the console, you can view the corresponding device events. If the `type` passed in is `info`, the event is of the information type; if `alert`, the alarm type; if `fault`, the fault type. For more information on how to view device events in the console, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).
