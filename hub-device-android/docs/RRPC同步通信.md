* [RRPC同步通信](#RRPC同步通信)
  * [RRPC同步通信简介](#RRPC同步通信简介)
  * [通信原理](#通信原理)
  * [通信流程](#通信流程)
  * [运行示例程序进行 RRPC 同步通信](#运行示例程序进行-RRPC-同步通信)

# RRPC同步通信
## RRPC同步通信简介
因 MQTT 协议基于发布/订阅的异步通信模式，服务器控制设备后，将无法同步感知设备的返回结果。为解决此问题，物联网通信平台利用 RRPC（Revert RPC）实现同步通信机制。请参考 [RRPC 通信](https://cloud.tencent.com/document/product/634/47334)

## 通信原理
* 订阅消息Topic: `$rrpc/rxd/{productID}/{deviceName}/+`  用于订阅云端下发（下行）的 RRPC 请求消息。
* 请求消息Topic: `$rrpc/rxd/{productID}/{deviceName}/{processID}`  用于云端发布（下行）RRPC 请求消息。
* 应答消息Topic: `$rrpc/txd/{productID}/{deviceName}/{processID}`  用于发布（上行）RRPC 应答消息。

## 通信流程
1. 设备端订阅 RRPC 订阅消息 Topic。
2. 服务器通过调用 [PublishRRPCMessage](https://cloud.tencent.com/document/product/634/47078) 接口发布 RRPC 请求消息。
3. 设备端接收到消息之后截取请求消息 Topic 中云端下发的 processID，设备将应答消息 Topic 的 processID 设置为截取的 processID，并向应答消息 Topic 发布设备的返回消息 。
4. 物联网通信平台接收到设备端返回消息之后，根据 processID 对消息进行匹配并将设备返回消息发送给服务器。
>! **RRPC请求4s超时**，即4s内设备端没有应答就认为请求超时。

## 运行示例程序进行 RRPC 同步通信

步骤一：在设备中订阅 RRPC 消息 Topic

请先按照 [基于TCP的MQTT设备接入](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/基于TCP的MQTT设备接入.md) 的步骤，将设备 连接MQTT 进行认证接入。
运行示例程序，在基础功能模块上，点击`订阅RRPC主题`按钮，进行 [通信原理](#通信原理) 中的订阅消息Topic。示例代码如下：
```
mMQTTSample.subscribeRRPCTopic(); //订阅RRPC消息Topic
```

以下是设备成功订阅 RRPC 消息 Topic 的日志
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $rrpc/rxd/AP9ZLEVFKT/gateway1/+
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[$rrpc/rxd/AP9ZLEVFKT/gateway1/+]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```

步骤二：调用云 API PublishRRPCMessage 发送 RRPC 请求消息。
打开腾讯云 [API控制台](https://console.cloud.tencent.com/api/explorer?Product=iotcloud&Version=2018-06-14&Action=PublishRRPCMessage&SignVersion=)，填写个人密钥和设备参数信息，选择在线调用并发送请求。

步骤三：观察设备端接收到发布 RRPC 请求消息Logcat日志，获取 **processID** 

```
I/TXMQTT_1.2.3: Received topic: $rrpc/rxd/AP9ZLEVFKT/gateway1/8209, id: 0, message: hello
```
以上日志为 设备端成功接收到发布 RRPC 请求消息，其中可以观察到此时 **processID** 为8209。

步骤四：设备将应答消息 Topic 的 processID 设置为截取的 processID，并向应答消息 Topic 发布设备的返回消息。

SDK 内部 TXMqttConnection 类中，成功接收到发布的 RRPC 请求消息会发布应答消息。
```
/**
 * 收到MQTT消息
 *
 * @param topic   消息主题
 * @param message 消息内容结构体
 * @throws Exception
 */
@Override
public void messageArrived(String topic, MqttMessage message) throws Exception {
    if (topic != null && topic.contains("rrpc/rxd")) {
       publishRRPCToCloud(null, processId, replyMessage);
    }
}
```

以下是成功发送 RRPC 应答消息 Topic 的日志
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $rrpc/txd/AP9ZLEVFKT/gateway1/8209 Message: {"test-key":"test-value"}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$rrpc/txd/AP9ZLEVFKT/gateway1/8209]],  userContext[], errMsg[publish success]
```
