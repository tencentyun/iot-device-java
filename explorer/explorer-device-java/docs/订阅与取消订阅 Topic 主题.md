* [订阅与取消订阅](#订阅与取消订阅)
  * [订阅 数据模板相关联 Topic 主题](#订阅-数据模板相关联-Topic-主题)
  * [取消订阅 Topic 主题](#取消订阅-Topic-主题)

# 订阅与取消订阅

在腾讯云物联网开发平台控制台（以下简称控制台）创建产品时，会默认生成一套产品的数据模板和一些标准功能，用户也可以自定义功能。数据模板对应的功能包含三大类：属性，事件和行为。控制台数据模板的使用，可参考官网 [数据模板](https://cloud.tencent.com/document/product/1081/44921) 章节。

产品定义数据模板后，设备可以按照数据模板中的定义上报属性、事件，并可对设备下发远程控制指令，即对可写的设备属性进行修改。数据模板的管理详见 产品定义。数据模板协议包括设备属性上报、设备远程控制、获取设备最新上报信息、设备事件上报、设备行为。对应的定义和云端下发控制指令使用的 Topic 请参考官网 [数据模板协议](https://cloud.tencent.com/document/product/1081/34916) 章节。

本文主要描述 如何对数据模板相关联 Topic 的订阅与取消订阅。

## 订阅 数据模板相关联 Topic 主题

运行 [MqttSample.java](../src/test/java/MqttSample.java) 的main函数，设备成功上线后，调用subscribeTopic()，订阅数据模板相关联的属性、事件和行为类型的 Topic:
```
$thing/down/property/{ProductID}/{DeviceName}
$thing/down/event/{ProductID}/{DeviceName}
$thing/down/action/{ProductID}/{DeviceName}
```
示例代码如下：
```
private static void subscribeTopic() {
    try {
        Thread.sleep(2000);
        mDataTemplateSample.subscribeTopic();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

观察Logcat日志。
```
23/02/2021 19:39:50,660 [MQTT Call: LWVUL5SZ2Llight1] INFO  MqttSample onConnectCompleted 288  - onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=0}], msg[connected to ssl://LWVUL5SZ2L.iotcloud.tencentdevices.com:8883]
23/02/2021 19:39:52,686 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSample onSubscribeCompleted 330  - onSubscribeCompleted, status[OK], topics[[$thing/down/property/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[subscribe success]
23/02/2021 19:39:52,691 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSample onSubscribeCompleted 330  - onSubscribeCompleted, status[OK], topics[[$thing/down/action/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[subscribe success]
23/02/2021 19:39:52,691 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSample onSubscribeCompleted 330  - onSubscribeCompleted, status[OK], topics[[$thing/down/event/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```
以上日志为 订阅 Topic 主题 成功。

## 取消订阅 Topic 主题

运行 [MqttSample.java](../src/test/java/MqttSample.java) 的main函数，设备成功上线后，订阅过Topic后，调用unSubscribeTopic()，取消订阅属性、事件和行为类型的 Topic:
```
$thing/down/property/{ProductID}/{DeviceName}
$thing/down/event/{ProductID}/{DeviceName}
$thing/down/action/{ProductID}/{DeviceName}
```
示例代码如下：
```
private static void unSubscribeTopic() {
    try {
        Thread.sleep(2000);
        mDataTemplateSample.unSubscribeTopic();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

观察Logcat日志。
```
23/02/2021 19:44:28,232 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSample onUnSubscribeCompleted 342  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/property/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[unsubscribe success]
23/02/2021 19:44:28,236 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSample onUnSubscribeCompleted 342  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/action/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[unsubscribe success]
23/02/2021 19:44:28,236 [MQTT Call: LWVUL5SZ2Llight1] DEBUG MqttSample onUnSubscribeCompleted 342  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/event/LWVUL5SZ2L/light1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[unsubscribe success]
```
以上日志为 取消订阅 Topic 主题 成功。
