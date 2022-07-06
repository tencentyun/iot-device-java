* [Attribute Reporting](#Attribute-Reporting)
  * [Publishing to topic for reporting attribute](#Publishing-to-topic-for-reporting-attribute)

# Attribute Reporting

When you create a product in the IoT Explorer console, a data template and some standard features will be generated for it by default. You can also customize the features. Such features are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

After a data template is defined for a product, the device can report attributes and events according to the definitions in the data template, and you can also deliver remote control instructions to the device to modify its writable attributes. For more information on how to manage a data template, please see Product Definition. The data template protocol includes device attribute reporting, remote device control, device-reported latest information acquisition, device event reporting, and device action triggering. For more information on the corresponding definitions and the topics used by the cloud to deliver control instructions, please see [Thing Model Protocol](https://cloud.tencent.com/document/product/1081/34916).

This document describes how to report the values of the associated attributes in the data template.

## Publishing to topic for reporting attribute 

Run the `testMqttConnect` function in [MqttSampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/mqtt/MqttSampleTest.java). After `connect()` calls back `onConnectCompleted` and a topic is subscribed to successfully, call `propertyReport()` to publish to the attribute topic:
`$thing/up/property/{ProductID}/{DeviceName}`

Below is the sample code:
```
private static void propertyReport() {
    JSONObject property = new JSONObject();
    JSONObject structJson = new JSONObject();
    structJson.put("bool_param", 1);                    // Boolean type
    structJson.put("int_param", 10);                    // Integer type
    structJson.put("str_param", "testStrAndroid");      // String type
    structJson.put("float_param", 2.1001);              // Floating point type
    structJson.put("enum_param", 1);                    // Enum type
    structJson.put("time_param", 1577871650);           // Timestamp type
    property.put("struct", structJson);   // Custom structure attribute
    
    property.put("power_switch",0);     // When creating a product, select **Smart City** > **Public Utilities** > **Street Lighting** as the product category, which is a standard feature attribute recommended by the system in the data template
    property.put("color",0);            // When creating a product, select **Smart City** > **Public Utilities** > **Street Lighting** as the product category, which is a standard feature attribute recommended by the system in the data template
    property.put("brightness",0);       // When creating a product, select **Smart City** > **Public Utilities** > **Street Lighting** as the product category, which is a standard feature attribute recommended by the system in the data template
    property.put("name","test");        // When creating a product, select **Smart City** > **Public Utilities** > **Street Lighting** as the product category, which is a standard feature attribute recommended by the system in the data template
    
    JSONArray arrInt = new JSONArray();  // Integer array
    arrInt.put(1);
    arrInt.put(3);
    arrInt.put(5);
    arrInt.put(7);
    property.put("arrInt", arrInt);
    
    JSONArray arrStr = new JSONArray();  // String array
    arrStr.put("aaa");
    arrStr.put("bbb");
    arrStr.put("ccc");
    arrStr.put("");
    property.put("arrString", arrStr);
    
    JSONArray arrFloat = new JSONArray();  // Floating point array
    arrFloat.put(5.001);
    arrFloat.put(0.003);
    arrFloat.put(0.004);
    arrFloat.put(0.007);
    property.put("arrFloat", arrFloat);
    
    JSONArray arrStruct = new JSONArray();  // Structure array
    for (int i = 0; i < 7; i++) {
        JSONObject structEleJson = new JSONObject();
        structEleJson.put("boolM", 0);      // Boolean parameter
        structEleJson.put("intM", 0);      // Integer parameter
        structEleJson.put("stringM", "string");  // String parameter
        structEleJson.put("floatM", 0.1); // Floating point parameter
        structEleJson.put("enumM", 0);      // Enum parameter
        structEleJson.put("timeM", 1577871650);        // Time parameter
        arrStruct.put(structEleJson);
    }
    
    property.put("arrStruct", arrStruct);
    
    
    if(Status.OK != mDataTemplateSample.propertyReport(property, null)) {
        LOG.error("property report failed!");
    }
}
```

Observe the logcat log.
```
23/02/2021 20:05:49,021 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $thing/up/property/LWVUL5SZ2L/light1 Message: {"method":"report","clientToken":"LWVUL5SZ2Llight10","params":{"brightness":0,"color":0,"struct_param":{"string_param":"string","int_param":0,"timestamp_param":1577871650,"bool_param":0,"enum_param":0,"float_param":1.0E-4},"power_switch":0,"name":"test"},"timestamp":1614081949019}
23/02/2021 20:05:49,023 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$thing/up/property/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
23/02/2021 20:07:57,551 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSampleTest onMessageReceived 351  - receive message, topic[$thing/down/property/LWVUL5SZ2L/light1], message[{"method":"report_reply","clientToken":"LWVUL5SZ2Llight13","code":0,"status":"success"}]
```
The above log represents the process in which the topic for reporting attributes is successfully published to. If the device has subscribed to the topic, it will receive the `report_reply` message as described in the above log. You can view the log of the device created in the console. In the online debugging section, you can see that the attribute values of the device have changed to the reported ones. For more information on how to view the device logs and debug devices online, please see [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

