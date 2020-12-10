# 腾讯物联网开发平台Java SDK
腾讯物联开发平台 Java SDK 配合平台对设备数据模板化的定义，实现和云端基于数据模板协议的数据交互框架，开发者基于IoT_Explorer Java SDK数据模板框架，快速实现设备和平台、设备和应用之间的数据交互。

# 快速开始
本节将讲述如何在腾讯物联网开发平台控制台申请设备, 并结合本 SDK 快速体验设备如何接入腾讯物联网开发平台。

## 一. 控制台创建设备

#### 1. 注册/登录腾讯云账号
访问[腾讯云登录页面](https://cloud.tencent.com/login?s_url=https%3A%2F%2Fcloud.tencent.com%2F), 点击[立即注册](https://cloud.tencent.com/register?s_url=https%3A%2F%2Fcloud.tencent.com%2F), 免费获取腾讯云账号，若您已有账号，可直接登录。

#### 2. 访问物联云控制台
登录后点击右上角控制台，进入控制台后, 鼠标悬停在云产品上, 弹出层叠菜单。

![](https://main.qcloudimg.com/raw/ec85d26d1dbef9c90c4a2462f3204403.jpg
)

搜索框中输入物联网开发平台，或直接访问[物联网开发平台控制台](https://console.cloud.tencent.com/iotexplorer)。

#### 3. 创建产品和设备

3.1 创建项目

![](https://main.qcloudimg.com/raw/992fb896ac8028244fb3c39ac4a7c178.jpg)

3.2.创建并选择和产品比较相近的模板产品，此处示例创建3种产品（网关，灯和空调，其中灯和空调作为子设备），更多产品请参阅[产品定义](https://cloud.tencent.com/document/product/1081/34739?!preview&!editLang=zh)。

![](https://main.qcloudimg.com/raw/82d66efc51c9b5d598231c198eed28aa.jpg)

![](https://main.qcloudimg.com/raw/6ec31ad280be850aaf4f5c8308647141.jpg)

![](https://main.qcloudimg.com/raw/694bbe63521ef4f4730eb89d151ed164.jpg)

![](https://main.qcloudimg.com/raw/7d1da93fffe9e0aa5f72a8b48e18b710.jpg)

3.3 定义产品的数据和事件模板，参阅[数据模板创建](https://cloud.tencent.com/document/product/1081/34739?!preview&!editLang=zh#.E6.95.B0.E6.8D.AE.E6.A8.A1.E6.9D.BF)，数据模板的说明参见[数据模板协议](https://cloud.tencent.com/document/product/1081/34916?!preview&!editLang=zh)。（**此处使用的默认产品，数据模板已建好，示例中略过该步**）

3.4 完成产品创建和数据模板定义后，创建设备，则每一个创建的设备都具备这个产品下的数据模板属性，如下图示。

![](https://main.qcloudimg.com/raw/7d05e54fdaf8520c481f375456298257.jpg)

![](https://main.qcloudimg.com/raw/2adee7ee3b7dba94326b96316020755f.jpg)

![](https://main.qcloudimg.com/raw/43201a6ceba57ce8e21381a340c0abfc.jpg)

3.5 查询产品和设备信息，除了子设备不需要设备密钥外，设备连接物联网开发平台需要三元组信息包括设备名称、设备密钥和产品ID

![](https://main.qcloudimg.com/raw/a4f19b2bcaef9f348b24bef35dcab8dc.jpg)

![](https://main.qcloudimg.com/raw/c49b49888af552f29820a3ae1381cf9d.jpg)

3.6 导出数据模板json文件，如果有子设备也需要导出
![](https://main.qcloudimg.com/raw/b0a65a222d1911d71c5893755ede611b.jpg)

#### 4. 添加子设备（网关示例下需要该步骤）

4.1 添加子产品

![](https://main.qcloudimg.com/raw/f0caff57f2ada4bcbd0593344c2b8edd.jpg)

![](https://main.qcloudimg.com/raw/8448010dceb40e792c3fa89d00171448.png)

4.2 添加子设备

![](https://main.qcloudimg.com/raw/782b89420533a3f132304c6e36f1fd56.jpg)

## 二. 编译运行示例程序

#### 1. 使用IDEA打开项目
使用 IDEA 导入 qcloud-iot-explorer-sdk-java 从而打开项目

#### 2. 填入设备信息

SDK提供了三种示例，分别对应数据模板基本功能示例（使用灯产品下的设备），非网关设备示例（使用灯产品下的设备），网关设备示例（使用网关产品下的设备以及绑定的产品子设备）。

| 示例对应文件         | 简介                 | 所需填入设备信息                                             |
| -------------------- | -------------------- | ------------------------------------------------------------ |
| IoTDataTemplate.java | 实现数据模板基本功能 | 设备ID，设备名称，设备密钥                                   |
| IoTLight.java        | 灯产品例子           | 设备ID，设备名称，设备密钥                                   |
| IoTGateway.java      | 网关产品例子         | 网关设备：设备ID，设备名称，设备密钥<br />子设备：设备ID，设备名称 |

IoTDataTemplate.java  和IoTLight.java对应代码段：

![](https://main.qcloudimg.com/raw/6933b83d0a7af12558ca749922a3d0b7.jpg)

IoTGateway.java对应代码段

![](https://main.qcloudimg.com/raw/fcbf3f04b24e0bdee9c1238365770ecb.jpg)

#### 4. 运行
右键debug运行

![](https://main.qcloudimg.com/raw/ad09b967ec9983c70f028842f8f5a3ac.jpg)

#### 5. 数据模板基本功能

数据模板示例DataTemplateSample.java，已实现数据、事件收发及响应的通用处理逻辑。但是具体的数据处理的业务逻辑需要用户自己根据业务逻辑添加，可以参考基于场景的示例IoTDataTemplate.java添加业务处理逻辑。

![](https://main.qcloudimg.com/raw/2d2d6239caf12020ea137a559e773f09.jpg)



## 三. 执行示例程序

#### 执行 IoTLight 示例程序

数据模板示例实现通用的数据模板和事件处理的框架，数据上下行和事件上报的日志如下：

```
24/04/2020 11:41:20,239 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection onSuccess 214  - onSuccess!
24/04/2020 11:41:20,242 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/property/MQAXEZ6UHF/deng
24/04/2020 11:41:20,250 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/event/MQAXEZ6UHF/deng
24/04/2020 11:41:20,251 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/action/MQAXEZ6UHF/deng
24/04/2020 11:41:20,251 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection connectComplete 577  - connectComplete. reconnect flag is false
24/04/2020 11:41:20,252 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/property/MQAXEZ6UHF/deng
24/04/2020 11:41:20,252 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/event/MQAXEZ6UHF/deng
24/04/2020 11:41:20,252 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/action/MQAXEZ6UHF/deng
24/04/2020 11:41:20,282 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"report_info","clientToken":"MQAXEZ6UHFdeng6","params":{"module_softinfo":"v1.0.0","imei":"0","device_label":{"company":"tencent","version":"v1.0.0"},"module_hardinfo":"v1.0.0","fw_ver":"v1.0.0","mac":"00:00:00:00"}}
true
24/04/2020 11:41:20,285 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"get_status","clientToken":"MQAXEZ6UHFdeng7","showmeta":0,"type":"report"}
true
24/04/2020 11:41:20,286 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"get_status","clientToken":"MQAXEZ6UHFdeng8","showmeta":0,"type":"control"}
true
24/04/2020 11:41:20,289 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection deliveryComplete 662  - deliveryComplete, token.getMessageId:0
24/04/2020 11:41:20,289 [Thread-7] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"report","clientToken":"MQAXEZ6UHFdeng9","params":{"brightness":1,"color":0,"power_switch":0,"name":"light"},"timestamp":1587699680289}
true
24/04/2020 11:41:20,290 [MQTT Call: MQAXEZ6UHFdeng] DEBUG DataTemplateSample onPublishCompleted 422  - TXLightSample
24/04/2020 11:41:20,362 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"report_info","clientToken":"MQAXEZ6UHFdeng10","params":{"module_softinfo":"v1.0.0","imei":"0","device_label":{"company":"tencent","version":"v1.0.0"},"module_hardinfo":"v1.0.0","fw_ver":"v1.0.0","mac":"00:00:00:00"}}
true
24/04/2020 11:41:20,362 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"get_status","clientToken":"MQAXEZ6UHFdeng11","showmeta":0,"type":"report"}
true
24/04/2020 11:41:20,363 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/MQAXEZ6UHF/deng
message.toString() = {"method":"get_status","clientToken":"MQAXEZ6UHFdeng12","showmeta":0,"type":"control"}
true
24/04/2020 11:41:20,363 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"get_status_reply","clientToken":"MQAXEZ6UHFdeng8","code":0,"status":"success","type":"control","data":{}}
24/04/2020 11:41:20,364 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"get_status_reply","clientToken":"MQAXEZ6UHFdeng7","code":0,"status":"success","type":"report","data":{"reported":{"brightness":88,"color":1,"power_switch":1,"name":"light"}}}
24/04/2020 11:41:20,365 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/event/MQAXEZ6UHF/deng
message.toString() = {"method":"events_post","clientToken":"MQAXEZ6UHFdeng13","events":[{"eventId":"status_report","type":"info","params":{"message":"switch open!","status":0},"timestamp":1587699680365}]}
true
24/04/2020 11:41:20,366 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"report_info_reply","clientToken":"MQAXEZ6UHFdeng6","code":0,"status":"success"}
24/04/2020 11:41:20,370 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"report_reply","clientToken":"MQAXEZ6UHFdeng9","code":0,"status":"success"}
24/04/2020 11:41:20,405 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"report_info_reply","clientToken":"MQAXEZ6UHFdeng10","code":0,"status":"success"}
24/04/2020 11:41:20,453 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"get_status_reply","clientToken":"MQAXEZ6UHFdeng12","code":0,"status":"success","type":"control","data":{}}
24/04/2020 11:41:20,455 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/event/MQAXEZ6UHF/deng0{"method":"events_reply","clientToken":"MQAXEZ6UHFdeng13","code":0,"status":"","data":{}}
24/04/2020 11:41:20,455 [MQTT Call: MQAXEZ6UHFdeng] INFO  TXMqttConnection messageArrived 638  - Received topic: %s, id: %d, message: %s$thing/down/property/MQAXEZ6UHF/deng0{"method":"get_status_reply","clientToken":"MQAXEZ6UHFdeng11","code":0,"status":"success","type":"report","data":{"reported":{"color":0,"power_switch":0,"name":"light","brightness":1}}}
```

#### 执行 IoTGateway 示例程序

数据模板示例实现网关和子设备的数据模板和事件处理的框架，数据上下行和事件上报的日志如下：

```
24/04/2020 12:26:19,634 [MQTT Call: L5FUS6KVSA123] INFO  TXGatewayClient onSuccess 447  - TXGATEWAYCLIENT
24/04/2020 12:26:19,637 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/property/L5FUS6KVSA/123
24/04/2020 12:26:19,645 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/event/L5FUS6KVSA/123
24/04/2020 12:26:19,647 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/action/L5FUS6KVSA/123
24/04/2020 12:26:19,648 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$gateway/operation/result/L5FUS6KVSA/123
24/04/2020 12:26:19,649 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient onSuccess 454  - TXGATEWAYCLIENT
24/04/2020 12:26:19,650 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection connectComplete 577  - connectComplete. reconnect flag is false
24/04/2020 12:26:19,650 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/property/L5FUS6KVSA/123
24/04/2020 12:26:19,650 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/event/L5FUS6KVSA/123
24/04/2020 12:26:19,651 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/action/L5FUS6KVSA/123
24/04/2020 12:26:20,132 [main] INFO  TXDataTemplateJson registerDataTemplateJson 82  - TX_TEMPLATE_JSON_1.2.1
24/04/2020 12:26:20,132 [main] INFO  TXDataTemplateJson registerDataTemplateJson 83  - TX_TEMPLATE_JSON_1.2.1
24/04/2020 12:26:20,133 [main] INFO  TXDataTemplateJson registerDataTemplateJson 84  - TX_TEMPLATE_JSON_1.2.1
24/04/2020 12:26:21,134 [main] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:21,134 [main] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
24/04/2020 12:26:21,134 [main] DEBUG TXGatewayClient subdevOnline 184  - TXGATEWAYCLIENT
24/04/2020 12:26:21,135 [main] DEBUG TXGatewayClient subdevOnline 202  - TXGATEWAYCLIENT
24/04/2020 12:26:21,135 [main] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $gateway/operation/L5FUS6KVSA/123
message.toString() = {"payload":{"devices":[{"device_name":"hongwai2","product_id":"RIHTZ2WGO9"}]},"type":"online"}
true
24/04/2020 12:26:21,137 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection deliveryComplete 662  - deliveryComplete, token.getMessageId:0
24/04/2020 12:26:21,137 [MQTT Call: L5FUS6KVSA123] DEBUG GatewaySample onPublishCompleted 215  - TXGatewaySample
24/04/2020 12:26:21,185 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient messageArrived 389  - TXGATEWAYCLIENT
24/04/2020 12:26:23,343 [main] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:23,735 [main] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
24/04/2020 12:26:23,737 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient consumeGwOperationMsg 350  - TXGATEWAYCLIENT
24/04/2020 12:26:24,742 [main] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:24,743 [main] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
24/04/2020 12:26:25,078 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:25,079 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
++++++++++RIHTZ2WGO9hongwai2+++++++++++
++++++++++{"result":0,"device_name":"hongwai2","product_id":"RIHTZ2WGO9"}
++++++++++0
24/04/2020 12:26:28,622 [MQTT Call: L5FUS6KVSA123] DEBUG ProductLight onSubDevOnline 201  - TXProductLight
24/04/2020 12:26:28,623 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/property/RIHTZ2WGO9/hongwai2
24/04/2020 12:26:28,624 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/event/RIHTZ2WGO9/hongwai2
24/04/2020 12:26:28,625 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection subscribe 452  - Starting subscribe topic: %s$thing/down/action/RIHTZ2WGO9/hongwai2
24/04/2020 12:26:31,353 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:31,353 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
24/04/2020 12:26:31,354 [MQTT Call: L5FUS6KVSA123] DEBUG ProductLight onSubscribeCompleted 239  - TXProductLight
24/04/2020 12:26:31,354 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:31,355 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
24/04/2020 12:26:31,355 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/RIHTZ2WGO9/hongwai2
message.toString() = {"method":"report_info","clientToken":"RIHTZ2WGO9hongwai29","params":{"module_softinfo":"v1.0.0","imei":"0","device_label":{"company":"tencent","version":"v1.0.0"},"module_hardinfo":"v1.0.0","fw_ver":"v1.0.0","mac":"00:00:00:00"}}
true
24/04/2020 12:26:31,356 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/RIHTZ2WGO9/hongwai2
message.toString() = {"method":"get_status","clientToken":"RIHTZ2WGO9hongwai210","showmeta":0,"type":"report"}
true
24/04/2020 12:26:31,356 [MQTT Call: L5FUS6KVSA123] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/RIHTZ2WGO9/hongwai2
message.toString() = {"method":"get_status","clientToken":"RIHTZ2WGO9hongwai211","showmeta":0,"type":"control"}
true
24/04/2020 12:26:31,365 [MQTT Call: L5FUS6KVSA123] DEBUG ProductLight onSubscribeCompleted 239  - TXProductLight
24/04/2020 12:26:31,366 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 55  - TXGATEWAYCLIENT
24/04/2020 12:26:31,366 [MQTT Call: L5FUS6KVSA123] DEBUG TXGatewayClient findSubdev 56  - TXGATEWAYCLIENT
24/04/2020 12:26:31,366 [MQTT Call: L5FUS6KVSA123] DEBUG ProductLight onSubscribeCompleted 239  - TXProductLight
24/04/2020 12:26:31,366 [Thread-8] INFO  TXMqttConnection publish 398  - Starting publish topic: %s Message: %s
topic = $thing/up/property/RIHTZ2WGO9/hongwai2
message.toString() = {"method":"report","clientToken":"RIHTZ2WGO9hongwai212","params":{"brightness":1,"color":0,"power_switch":0,"name":"light"},"timestamp":1587702391366}
true
```

## 四. 设备调试

##### 1.进入设备调试

![](https://main.qcloudimg.com/raw/1f2ac1d6cac186394ac1a1da6c22749c.jpg)

##### 2.修改目标数据下发设备

![](https://main.qcloudimg.com/raw/911a09872f03a91d1d530537e51147f1.jpg)

##### 3.控制台查看设备当前状态

![](https://main.qcloudimg.com/raw/236b8bf3c88b1c532714b730d0993a79.jpg)

##### 4.控制台查看设备通信日志

![](https://main.qcloudimg.com/raw/10e911975030f2840b9af03a079aec1d.jpg)

##### 5.控制台查看设备事件上报

![](https://main.qcloudimg.com/raw/d3878541b502619158ec206fc2ae2391.jpg)