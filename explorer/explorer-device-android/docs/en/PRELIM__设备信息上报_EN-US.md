* [Device Information Reporting](#Device-Information-Reporting)
  * [Publishing to topic for reporting device information](#Publishing-to-topic-for-reporting-device-information)

# Device Information Reporting

This document describes how to report device information to the cloud with the aid of the demo.

## Publishing to topic for reporting device information 

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Report Device Information**, concatenate the device information parameters, and publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}` 

Below is the sample code:
```
JSONObject params = new JSONObject();
JSONObject label = new JSONObject();  //device label
label.put("version", "v1.0.0");
label.put("company", "tencent");
params.put("module_hardinfo", "v1.0.0");
params.put("module_softinfo", "v1.0.0");
params.put("fw_ver", "v1.0.0");
params.put("imei", "0");
params.put("mac", "00:00:00:00");
params.put("device_label", label);
if(Status.OK != mDataTemplateSample.propertyReportInfo(params)) {
   mParent.printLogInfo(TAG, "property report failed!", mLogInfoText, TXLog.LEVEL_ERROR);
}
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"report_info","clientToken":"LWVUL5SZ2Llight13","params":{"module_hardinfo":"v1.0.0","module_softinfo":"v1.0.0","fw_ver":"v1.0.0","imei":"0","mac":"00:00:00:00","device_label":{"version":"v1.0.0","company":"tencent"}}}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the topic for reporting device information is published to successfully.

