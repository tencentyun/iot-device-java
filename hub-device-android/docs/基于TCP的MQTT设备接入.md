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
本文将介绍如何在腾讯云物联网通信IoT Hub控制台创建设备, 介绍如何在腾讯云物联网通信IoT Hub控制台创建设备, 并结合 SDK Demo 快速体验设备端通过 MQTT 协议连接到腾讯云IoT Hub, 发送和接收消息。

## 控制台创建设备

设备接入SDK前需要在控制台中创建产品设备，获取产品ID、设备名称、设备证书（证书认证）、设备私钥（证书认证）、设备密钥（密钥认证），将设备与云端认证连接时需要用到。请参考官网 [控制台使用手册-设备接入准备](https://cloud.tencent.com/document/product/634/14442)。

当在控制台中成功创建产品后，该产品默认有三条权限。

订阅：${productId}/${deviceName}/control

订阅和发布：${productId}/${deviceName}/data

发布：${productId}/${deviceName}/event

请参考官网 [控制台使用手册-权限列表](https://cloud.tencent.com/document/product/634/14444) 操作Topic权限。

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
  "SUB_DEV_NAME":      "",
  "SUB_PRODUCT_KEY":   "",
  "TEST_TOPIC":        "",
  "SHADOW_TEST_TOPIC": "",
  "PRODUCT_KEY":       ""
}
```
如果控制台创建设备使用的是密钥认证方式，需要在 app-config.json 填写 PRODUCT_ID（产品ID）、DEVICE_NAME（设备名称）、DEVICE_PSK（设备密钥）。
如果控制台创建设备使用的是证书认证方式，除了需要在 app-config.json 填写 PRODUCT_ID（产品ID）、DEVICE_NAME（设备名称），
还需在 [IoTMqttFragment.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) 文件中的配置 mDevCert（设备证书内容字符串）mDevPriv（设备私钥内容字符串）。
或者通过 AssetManager 进行读取，在工程 hub-device-android/hub-demo/src/main 路径下创建 assets 目录并将设备证书、私钥放置在该目录中，在 [IoTMqttFragment.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) 文件中的配置 mDevCertName（设备证书文件名称）mDevKeyName（设备私钥文件名称）。
```
private String mDevCertName = "";
private String mDevKeyName  = "";
private String mDevCert     = "";           // Cert String
private String mDevPriv     = "";           // Priv String
```

#### 运行示例程序进行 MQTT 认证连接
选择hub-demo的 Android App ，点击 Android Studio 菜单栏上的 Run 'hub-demo' 按钮安装 Demo。

运行示例程序，在基础功能模块上，点击 连接MQTT 按钮，进行认证连接。示例代码如下：
```
mMQTTSample = new MQTTSample(mParent, new SelfMqttActionCallBack(), mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mSubProductID, mSubDevName, mTestTopic, null, null, true, new SelfMqttLogCallBack()); //MQTTSample类是Demo对SDK接口调用的一层封装类。
mMQTTSample.connect(); // MQTT连接
```

观察Logcat日志。
```
I/TXMQTT1.3.0: Start connecting to ssl://iotcloud-mqtt.gz.tencentdevices.com:8883
I/TXMQTT: onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=6}], msg[connected to ssl://iotcloud-mqtt.gz.tencentdevices.com:8883]
```

以上日志为 MQTT 认证连接成功。

#### 运行示例程序进行断开 MQTT 连接

运行示例程序，在基础功能模块上，点击 断开MQTT连接 按钮，断开 MQTT 认证连接。示例代码如下：
```
mMQTTSample.disconnect(); // 断开 MQTT 连接
```

观察Logcat日志。
```
I/TXMQTT: onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=4}], msg[disconnected to ssl://iotcloud-mqtt.gz.tencentdevices.com:8883]
```
以上日志为断开 MQTT 连接成功。

#### 订阅 Topic 主题
运行示例程序前，需要把将要订阅的 Topic 主题配置在 [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/app-config.json) 文件中的TEST_TOPIC（Topic权限），Topic的生成请参考 [控制台创建设备](#控制台创建设备) 中权限的使用。

运行示例程序，在基础功能模块上，点击 订阅主题 按钮，订阅 Topic 主题。示例代码如下：
```
mMQTTSample.subscribeTopic();//订阅 Topic 主题
```

观察Logcat日志。
```
I/TXMQTT_1.3.0: Starting subscribe topic: 4A8E1MAMCT/car1/control
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[4A8E1MAMCT/car1/control]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```
以上日志为订阅 Topic 主题成功。

#### 取消订阅 Topic 主题
设备之前订阅过的 Topic 主题，可以取消订阅。

运行示例程序，在基础功能模块上，点击 取消订阅主题 按钮，取消订阅 Topic 主题。示例代码如下：
```
mMQTTSample.unSubscribeTopic();//取消订阅 Topic 主题
```

观察Logcat日志。
```
D/TXMQTT: Start to unSubscribe4A8E1MAMCT/car1/control
D/TXMQTT: onUnSubscribeCompleted, status[OK], topics[[4A8E1MAMCT/car1/control]], userContext[MQTTRequest{requestType='unSubscribeTopic', requestId=2}], errMsg[unsubscribe success]
```
以上日志为取消订阅 Topic 主题成功。

#### 发布 Topic 主题
运行示例程序前，需要把将要发布的 Topic 主题配置在 [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/app-config.json) 文件中的TEST_TOPIC（Topic权限），Topic的生成请参考 [控制台创建设备](#控制台创建设备) 中权限的使用。

运行示例程序，在基础功能模块上，点击 发布主题 按钮，发布 Topic 主题。示例代码如下：
```
mMQTTSample.publishTopic("data", data);//发布 Topic 主题，publishTopic方法中会将data包装成 MqttMessage 发布出去。
```

观察Logcat日志。
```
I/TXMQTT_1.3.0: Starting publish topic: 4A8E1MAMCT/car1/event Message: {"oil_consumption":"6.6","temperature":"0","maximum_speed":"205","car_type":"suv"}
D/TXMQTT: onPublishCompleted, status[OK], topics[[4A8E1MAMCT/car1/event]],  userContext[MQTTRequest{requestType='publishTopic', requestId=1}], errMsg[publish success]
```
以上日志为发布 Topic 主题成功。
