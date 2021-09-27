* [Event Reporting and Multi-Event Reporting](#Event-Reporting-and-Multi-Event-Reporting)
  * [Publishing to topic for reporting event](#Publishing-to-topic-for-reporting-event)
  * [Publishing to topic for reporting multiple events](#Publishing-to-topic-for-reporting-multiple-events)

# Event Reporting and Multi-Event Reporting

This document describes how a device publishes to topics for event reporting and multi-event reporting.

## Publishing to topic for reporting event 

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Report Event**, concatenate the device information parameters, and publish to the event topic:
`$thing/up/event/{ProductID}/{DeviceName}`

Below is the sample code:
```
String eventId = "status_report";
String type = "info";
JSONObject params = new JSONObject();
params.put("status",0);
params.put("message","");
if(Status.OK != mDataTemplateSample.eventSinglePost(eventId, type, params)){
    mParent.printLogInfo(TAG, "single event post failed!", mLogInfoText, TXLog.LEVEL_ERROR);
}
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $thing/up/event/LWVUL5SZ2L/light1 Message: {"method":"event_post","clientToken":"LWVUL5SZ2Llight15","eventId":"status_report","type":"info","timestamp":1603160347446,"params":{"status":0,"message":""}}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$thing/up/event/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
D/TXDataTemplateFragment: receive command, topic[$thing/down/event/LWVUL5SZ2L/light1], message[{"method":"event_reply","clientToken":"LWVUL5SZ2Llight15","code":0,"status":"","data":{}}]
D/TXDATATEMPLATE: event down stream message received : {"method":"event_reply","clientToken":"LWVUL5SZ2Llight15","code":0,"status":"","data":{}}
D/TXDataTemplateFragment: reply received : {"method":"event_reply","clientToken":"LWVUL5SZ2Llight15","code":0,"status":"","data":{}}
```
The above log represents the process in which the device publishes to the topic for reporting a single event successfully. If the device has subscribed to the topic, it will receive the `event_reply` message as described in the above log. In the information of the device created in the console, you can view the corresponding device event. If the `type` passed in is `info`, the event is of the information type. For more information on how to view device events in the console, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

## Publishing to topic for reporting multiple events 

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Report Multiple Events**, concatenate the device information parameters, and publish to the event topic:
`$thing/up/event/{ProductID}/{DeviceName}`

Below is the sample code:
```
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
    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
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
    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
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
    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
    return;
}
if(Status.OK != mDataTemplateSample.eventsPost(events)){
    mParent.printLogInfo(TAG, "events post failed!", mLogInfoText, TXLog.LEVEL_ERROR);
}
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $thing/up/event/LWVUL5SZ2L/light1 Message: {"method":"events_post","clientToken":"LWVUL5SZ2Llight16","events":[{"eventId":"status_report","type":"info","timestamp":1603160653628,"params":{"status":0,"message":""}},{"eventId":"low_voltage","type":"alert","timestamp":1603160653628,"params":{"voltage":1}},{"eventId":"hardware_fault","type":"fault","timestamp":1603160653628,"params":{"name":"","error_code":1}}]}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$thing/up/event/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
D/TXDataTemplateFragment: receive command, topic[$thing/down/event/LWVUL5SZ2L/light1], message[{"method":"events_reply","clientToken":"LWVUL5SZ2Llight16","code":0,"status":"","data":{}}]
D/TXDATATEMPLATE: event down stream message received : {"method":"events_reply","clientToken":"LWVUL5SZ2Llight16","code":0,"status":"","data":{}}
D/TXDataTemplateFragment: reply received : {"method":"events_reply","clientToken":"LWVUL5SZ2Llight16","code":0,"status":"","data":{}}
```
The above log represents the process in which the device publishes to the topic for reporting multiple events successfully. If the device has subscribed to the topic, it will receive the `events_reply` message as described in the above log. In the information of the device created in the console, you can view the corresponding device events. If the `type` passed in is `info`, the event is of the information type; if `alert`, the alarm type; if `fault`, the fault type. For more information on how to view device events in the console, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).
