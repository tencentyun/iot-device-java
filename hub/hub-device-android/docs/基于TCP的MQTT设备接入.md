* [快速开始](#快速开始)
  *  [控制台创建设备](#控制台创建设备)
  *  [编译运行示例程序](#编译运行示例程序)
     *  [填写认证连接设备的参数](#填写认证连接设备的参数)
     *  [运行示例程序进行 MQTT 认证连接](#运行示例程序进行-MQTT-认证连接)
     *  [运行示例程序进行断开 MQTT 连接](#运行示例程序进行断开-MQTT-连接)
     *  [订阅 Topic 主题](#订阅-Topic-主题)
     *  [取消订阅 Topic 主题](#取消订阅-Topic-主题)
     *  [发布 Topic 主题](#发布-Topic-主题)

# 快速开始
本文将介绍如何在腾讯云物联网通信IoT Hub控制台创建设备, 并结合 SDK Demo 快速体验设备端通过 MQTT 协议连接腾讯云IoT Hub, 发送和接收消息。

## 控制台创建设备

设备接入SDK前需要在控制台中创建产品设备，获取产品ID、设备名称、设备证书（证书认证）、设备私钥（证书认证）、设备密钥（密钥认证），设备与云端认证连接时需要用到以上信息。详情请参考官网 [控制台使用手册-设备接入准备](https://cloud.tencent.com/document/product/634/14442)。

当在控制台中成功创建产品后，该产品默认有三条权限：
```
${productId}/${deviceName}/control  // 订阅
${productId}/${deviceName}/data     // 订阅和发布
${productId}/${deviceName}/event    // 发布
```
详情请参考官网 [控制台使用手册-权限列表](https://cloud.tencent.com/document/product/634/14444) 操作Topic权限。

## 编译运行示例程序

[下载IoT Hub Android-SDK Demo示例代码](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/README.md#下载IoT-Hub-Android-SDK-Demo示例代码)

#### 填写认证连接设备的参数
编辑 [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/app-config.json) 文件中的配置信息
```
{
  "PRODUCT_ID":        "",
  "DEVICE_NAME":       "",
  "DEVICE_PSK":        "",
  "SUB_PRODUCT_ID":    "",
  "SUB_DEVICE_PSK":    "",
  "SUB_DEV_NAME":      "",
  "TEST_TOPIC":        "",
  "SHADOW_TEST_TOPIC": "",
  "PRODUCT_KEY":       ""
}
```
如果在控制台创建设备时使用的是密钥认证方式，需要在 app-config.json 填写 PRODUCT_ID（产品ID）、DEVICE_NAME（设备名称）、DEVICE_PSK（设备密钥）；

如果在控制台创建设备时使用的是证书认证方式，除了需要在 app-config.json 填写 PRODUCT_ID（产品ID）、DEVICE_NAME（设备名称），DEVICE_PSK（设备密钥）设置为null之外，还需在 [IoTMqttFragment.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) 文件中配置 mDevCert（设备证书内容字符串）mDevPriv（设备私钥内容字符串）。
或者通过 AssetManager 读取证书，具体地，在工程 hub-device-android/hub-demo/src/main 路径下创建 assets 目录并将设备证书、私钥放置在该目录中，在 [IoTMqttFragment.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) 文件中配置 mDevCertName（设备证书文件名称）和mDevKeyName（设备私钥文件名称）。
```
private String mDevCertName = "";
private String mDevKeyName  = "";
private String mDevCert     = "";           // Cert String
private String mDevPriv     = "";           // Priv String
```

#### 运行示例程序进行 MQTT 认证连接
选择hub-demo的 Android App ，点击 Android Studio 菜单栏上的 Run 'hub-demo' 按钮安装 Demo。

运行示例程序，在基础功能模块上，点击`连接MQTT`按钮，进行认证连接。示例代码如下：
```
mMQTTSample = new MQTTSample(mParent, new SelfMqttActionCallBack(), mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mSubProductID, mSubDevName, mTestTopic, null, null, true, new SelfMqttLogCallBack()); //MQTTSample类是Demo对SDK接口调用的一层封装类。
mMQTTSample.connect(); // MQTT连接
```

以下是设备通过 MQTT 成功连接云端的logcat日志。
```
I/TXMQTT1.2.3: Start connecting to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883
I/TXMQTT: onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=2}], msg[connected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883]
```

#### 运行示例程序进行断开 MQTT 连接

运行示例程序，在基础功能模块上，点击`断开MQTT连接`按钮，断开 MQTT 认证连接。示例代码如下：
```
mMQTTSample.disconnect(); // 断开 MQTT 连接
```

以下是设备成功断开 MQTT 连接的logcat日志。
```
I/TXMQTT: onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=3}], msg[disconnected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883]
```

#### 订阅 Topic 主题
运行示例程序前，需要把将要订阅的 Topic 主题配置在 [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/app-config.json) 文件中的TEST_TOPIC（Topic权限），Topic的生成请参考 [控制台创建设备](#控制台创建设备) 中权限的使用。

运行示例程序，在基础功能模块上，点击`订阅主题`按钮，订阅 Topic 主题。示例代码如下：
```
mMQTTSample.subscribeTopic();//订阅 Topic 主题
```

以下是设备成功订阅 Topic 主题的logcat日志。
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: AP9ZLEVFKT/gateway1/data
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=5}], errMsg[subscribe success]
```

#### 取消订阅 Topic 主题
设备之前订阅过的 Topic 主题，可以取消订阅。

运行示例程序，在基础功能模块上，点击`取消订阅主题`按钮，取消订阅 Topic 主题。示例代码如下：
```
mMQTTSample.unSubscribeTopic();//取消订阅 Topic 主题
```

以下是设备成功取消订阅 Topic 主题的logcat日志。
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: AP9ZLEVFKT/gateway1/data
D/TXMQTT: onUnSubscribeCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]], userContext[MQTTRequest{requestType='unSubscribeTopic', requestId=6}], errMsg[unsubscribe success]
```

#### 发布 Topic 主题
运行示例程序前，需要把将要发布的 Topic 主题配置在 [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/app-config.json) 文件中的TEST_TOPIC（Topic权限），Topic的生成请参考 [控制台创建设备](#控制台创建设备) 中权限的使用。

运行示例程序，在基础功能模块上，点击`发布主题`按钮，发布 Topic 主题。示例代码如下：
```
Map<String, String> data = new HashMap<String, String>();// 要发布的数据
data.put("car_type", "suv");// 车辆类型
data.put("oil_consumption", "6.6");// 车辆油耗
data.put("maximum_speed", "205");// 车辆最高速度
data.put("temperature", String.valueOf(temperature.getAndIncrement()));// 温度信息
mMQTTSample.publishTopic("data", data);//发布 Topic 主题，publishTopic方法中会将data包装成 MqttMessage 发布出去。
```

以下是设备成功发布 Topic 主题的logcat日志。
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: AP9ZLEVFKT/gateway1/data Message: {"oil_consumption":"6.6","temperature":"0","maximum_speed":"205","car_type":"suv"}
D/TXMQTT: onPublishCompleted, status[OK], topics[[AP9ZLEVFKT/gateway1/data]],  userContext[MQTTRequest{requestType='publishTopic', requestId=8}], errMsg[publish success]
```
