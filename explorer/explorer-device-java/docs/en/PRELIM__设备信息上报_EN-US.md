* [Device Information Reporting](#Device-Information-Reporting)
  * [Publishing to topic for reporting device information](#Publishing-to-topic-for-reporting-device-information)

# Device Information Reporting

This document describes how to report device information to the cloud.

## Publishing to topic for reporting device information 

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `propertyReportInfo()` to publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}` 

Below is the sample code:
```
private static void propertyReportInfo() {
    //report info
    JSONObject params = new JSONObject();
    try {
        JSONObject label = new JSONObject();  //device label
        label.put("version", "v1.0.0");
        label.put("company", "tencent");
    
        params.put("module_hardinfo", "v1.0.0");
        params.put("module_softinfo", "v1.0.0");
        params.put("fw_ver", "v1.0.0");
        params.put("imei", "0");
        params.put("mac", "00:00:00:00");
        params.put("device_label", label);
    } catch (JSONException e) {
        LOG.error("Construct params failed!");
        return;
    }
    if(Status.OK != mDataTemplateSample.propertyReportInfo(params)) {
        LOG.error("property report failed!");
    } 
}
```

Observe the logcat log.
```
23/02/2021 20:24:57,428 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"report_info","clientToken":"LWVUL5SZ2Llight13","params":{"module_softinfo":"v1.0.0","imei":"0","device_label":{"company":"tencent","version":"v1.0.0"},"module_hardinfo":"v1.0.0","fw_ver":"v1.0.0","mac":"00:00:00:00"}}
23/02/2021 20:24:57,462 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"report_info_reply","clientToken":"LWVUL5SZ2Llight13","code":0,"status":"success"}]
```
The above log represents the process in which the topic for reporting device information is published to successfully.

