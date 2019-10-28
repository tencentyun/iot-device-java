# 腾讯物联云SDK
腾讯物联云 SDK 依靠安全且性能强大的数据通道，为物联网领域开发人员提供终端(如传感器, 执行器, 嵌入式设备或智能家电等等)和云端的双向通信能力。

# 快速开始
本节将讲述如何在腾讯物联云控制台申请设备, 并结合本 SDK 快速体验设备通过 MQTT+TLS/SSL 协议连接到腾讯云, 发送和接收消息。

## 一. 控制台创建设备

#### 1. 注册/登录腾讯云账号
访问[腾讯云登录页面](https://cloud.tencent.com/login?s_url=https%3A%2F%2Fcloud.tencent.com%2F), 点击[立即注册](https://cloud.tencent.com/register?s_url=https%3A%2F%2Fcloud.tencent.com%2F), 免费获取腾讯云账号，若您已有账号，可直接登录。

#### 2. 访问物联云控制台
登录后点击右上角控制台，进入控制台后, 鼠标悬停在云产品上, 弹出层叠菜单。

![](http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/doc/{0603FE05-A96A-41E4-A0B8-AA2D9200928A}.png
)

点击物联云，或直接访问[物联云控制台](https://console.qcloud.com/iotcloud)

#### 3. 创建产品和设备
点击页面**创建新产品**按钮, 创建一个品类, 然后在下方**产品名称**一栏中点击刚刚创建好的产品进入产品设置页面，可在产品设置页面得到 **productID**，并可编辑产品名称及描述，之后再**设备列表**页面新建设备，设备名称在该产品下唯一。

![](http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/doc/iot_15157295174920.png
)

创建设备成功后，**需保存好设备私钥**，腾讯云后台不存储设备私钥，点击**管理**，得到**设备证书**（用于非对称加密连接）。

![](http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/doc/iot_15157296439268.png)

**说明：如果需要网关功能，节点类型选择“网关”，产品类型选择“普通网关产品。”**

![](https://main.qcloudimg.com/raw/5e7574fac3794efefeb68727b137265f.png)  

#### 4. 创建子产品和设备
和第3步创建普通产品和设备的步骤一致
**说明：如果不需要测试网关功能，这一步可跳过。**

#### 5. 建立网关设备和子设备的绑定关系
在网关产品下添加子产品

![](https://main.qcloudimg.com/raw/52835b799dec6cc5147b1e5442aa36fc.png)

在网关设备下添加子设备

![](https://main.qcloudimg.com/raw/e62a04ee502c8f5105664457af53f6a3.png)
**说明：如果不需要测试网关功能，这一步可跳过。**

#### 6. 创建可订阅可发布的Topic

按照**第三步**中进入产品设置页面的方法进入页面后, 点击权限列表，再点击**定义 Topic 权限**, 输入 data, 并设置为可订阅可发布权限，点击创建。

![](https://main.qcloudimg.com/raw/7ffbbebb86c52d92bd70ce06bc373e35.png)

随后将会创建出 productName/${deviceName}/data 的 Topic
**注意：如果该设备是网关设备的话，也可以创建所绑定的子产品的Topic。**  

![](https://main.qcloudimg.com/raw/7e64714a708834ef1bb66b29454f6dae.png)

#### 7. 开启日志上传功能

点击创建产品中的**设备列表**，选择需要开启日志上传功能的设备，点击**管理**。

![](https://main.qcloudimg.com/raw/88c62e9168db0ecbe3d5acd3fff29160.jpg)

点击**编辑**设备日志配置，打开设备日志上传功能，并设置日志等级。

![](https://main.qcloudimg.com/raw/6e7c87f626bb0137e906c8b7deeac991.jpg)
**说明：如果不需要测试日志上传功能，这一步可跳过。**

### 8. 动态注册功能

在控制台使能**动态注册**，可选择使能**自动创建设备**，获取**ProductSecret**。

![](https://main.qcloudimg.com/raw/eca97a0ccf80997c0d224cf03fca713c.jpg)
**说明：详细说明可以参考控制台页面说明。**

## 二. 编译运行示例程序

#### 1. 下载SDK
运行如下命令从 github 克隆代码, 或者访问最新[下载](https://github.com/tencentyun/qcloud-iot-sdk-android/releases)地址, 将下载到的压缩包解压缩。本测试用例展示包括网关设备的功能，网关设备除了具备普通设备一样的功能外，还具备代理子设备上下线的消息。

```git clone https://github.com/tencentyun/qcloud-iot-sdk-android.git```

#### 2. 使用Android Studio打开工程
使用 Android Studio 导入 qcloud-iot-sdk-android/build.gradle 从而打开工程

#### 3. 填入设备信息
- 编辑 app/com.qcloud.iot.samples.IoTMqttFragment.java 文件中如下代码块, 填入之前创建产品和设备步骤中得到的 **PRODUCT_ID**, **DEVICE_NAME**，**DEVICE_SECRET**, **SUB_PRODUCTID**, **SUB_DEVNAME**和**TEST_TOPIC**。
- SDK 提供两种认证方式，一种是PSK，一种是证书的方式。读取设备证书、私钥的接口：一是通过 AssetManager 进行读取，此时需在工程 **app/src/main** 路径下创建 **assets** 目录并将设备证书、私钥放置在该目录中；二是通过 InputStream 进行读取，此时需传入设备证书、私钥的全路径信息。

![](https://main.qcloudimg.com/raw/5de584669495a3b9d5641e10b4041581.png)

如果不在代码中修改，Sample APP也提供了接口进行设置，在**参数**编辑框输入参数，然后通过**选择进行设置**下拉菜单进行相应的设置，可以设置**PRODUCT_ID**，**DEVICE_NAME**，**DEVICE_SECRET**，**SUB_PRODUCTID**，**SUB_DEVNAME**和**TEST_TOPIC**。通常情况下，**BROKER_URL**无需修改，使用默认即可。

![](https://main.qcloudimg.com/raw/0c490d77cfb1a50d2576a488ad588214.jpg)  

#### 4. 运行
点击 Android Studio Run 'app' 按钮安装 Demo。

#### 5. 动态注册  
点击 Demo 中的【动态注册】按钮，查看logcat中日志信息，成功获取到PSK或者Cert/Priv则表明注册成功：
```
D TXMQTT  : Dynamic Register OK!
I TXMQTT  : Dynamic register OK! onGetDevicePSK, devicePSK[**********************]
```
如果不使用动态注册功能，则使用代码中写入的设备参数信息或者通过设置界面设置的参数进行测试。

#### 6. 连接MQTT
点击 Demo 中的【连接 MQTT 】按钮，观察 Demo 及 logcat 中日志信息，以下为 logcat 中日志信息：
```
com.qcloud.iot I/com.qcloud.iot.mqtt.TXMqttConnection: Start connecting to ssl://connect.iot.qcloud.com:8883
com.qcloud.iot D/IoTMqttFragment: onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=0}], msg[connected to ssl://connect.iot.qcloud.com:8883]
```
#### 7. 子设备上线
点击Demo中的【子设备上线】按钮，观察Demo以及logcat中的日志信息：
```
onPublishCompleted, status[OK], topics[[$gateway/operation/******/******]],  userContext[], errMsg[publish success]
message received $gateway/operation/result/******/******
got gate operation messga $gateway/operation/result/******/******{"type":"online","payload":{"devices":[{"product_id":"********","device_name":"*****","result":0}]}}
```
#### 8. 订阅主题
点击 Demo 中的【订阅主题】按钮，观察 Demo 及 logcat 中日志信息，以下为 logcat 中日志信息：
```
com.qcloud.iot I/com.qcloud.iot.mqtt.TXMqttConnection: Starting subscribe topic: ******/******/custom_data
com.qcloud.iot D/IoTMqttFragment: onSubscribeCompleted, status[OK], topics[[******/******/custom_data]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```
#### 9. 发布主题
点击 Demo 中的【发布主题】按钮，观察 Demo 及 logcat 中日志信息，以下为 logcat 中日志信息：
```
com.qcloud.iot I/com.qcloud.iot.mqtt.TXMqttConnection: Starting publish topic: ******/******/custom_data Message: {"temperature":"0","car_type":"suv","maximum_speed":"205","oil_consumption":"6.6"}
com.qcloud.iot D/IoTMqttFragment: onPublishCompleted, status[OK], topics[[******/******/custom_data]],  userContext[MQTTRequest{requestType='publishTopic', requestId=2}], errMsg[publish success]
```

#### 10. 观察消息下发
如下日志信息显示该消息因为是到达已被订阅的 Topic, 所以又被服务器原样推送到示例程序, 并进入相应的回调函数。以下为 logcat 中信息：
```
com.qcloud.iot I/com.qcloud.iot.mqtt.TXMqttConnection: Received topic: ******/******/data, message: {"temperature":"0","car_type":"suv","maximum_speed":"205","oil_consumption":"6.6"}
com.qcloud.iot D/IoTMqttFragment: receive command, topic[******/******/data], message[{"temperature":"0","car_type":"suv","maximum_speed":"205","oil_consumption":"6.6"}]
```

#### 11. 取消订阅主题
点击 Demo 中的【取消订阅主题】按钮，观察 Demo 及 logcat 中日志信息，以下为 logcat 中日志信息：
```
com.qcloud.iot I/com.qcloud.iot.mqtt.TXMqttConnection: Starting unsubscribe topic: ******/******/data
com.qcloud.iot D/IoTMqttFragment: onUnSubscribeCompleted, status[OK], topics[[******/******/data]], userContext[MQTTRequest{requestType='unSubscribeTopic', requestId=3}], errMsg[unsubscribe success]
```

#### 12. 子设备下线
点击Demo中的【子设备上线】按钮，观察Demo以及logcat中的日志信息：
```
Starting publish topic: $gateway/operation/******/****** Message: {"type":"offline","payload":{"devices":[{"product_id":"******","device_name":"******"}]}}
onPublishCompleted, status[OK], topics[[$gateway/operation/******/******]],  userContext[], errMsg[publish success]
message received $gateway/operation/result/******/******
got gate operation messga $gateway/operation/result/******/******{"type":"offline","payload":{"devices":[{"product_id":"******","device_name":"******","result":0}]}}
```

#### 13. 日志上传功能（需要给APP提供访问存储权限）

点击Demo中的【日志测试】按钮，生成4种级别的日志各一条，观察Demo以及logcat中的日志信息：

```
Add log to log Deque! ERR|2019-07-14 15:03:50|TXMQTT|Error level log for test!!!
Add log to log Deque! WRN|2019-07-14 15:03:50|TXMQTT|Warning level log for test!!!
Add log to log Deque! INF|2019-07-14 15:03:50|TXMQTT|Info level log for test!!!
Add log to log Deque! DBG|2019-07-14 15:03:50|TXMQTT|Debug level log for test!!!
```

正常情况下，最多等待30S后日志可以在云日志界面中**设备日志**中查看：

![](https://main.qcloudimg.com/raw/518dbbda46a46729f7a9ccf9f1ec3571.jpg)

点击Demo中的【日志上传】按钮，则可以立即将日志上传，观察Demo以及logcat中的日志信息：

* 网络正常时，日志成功上传到云端，则会出现以下信息：

```
Upload log to http://devicelog.iot.cloud.tencent.com:80/cgi-bin/report-log success
```

* 网络不可用时，本例日志会保存到本地文件中，则会出现以下信息：

```
Lost Connection! Call mMqttCallBack.saveLogOffline()
```

**说明：当网络恢复可用时，本例保存在本地文件中的日志会自动上传到云端。**

#### 14. 断开MQTT连接

点击 Demo 中的【断开 MQTT 连接】按钮，观察 Demo 及 logcat 中日志信息，以下为 logcat 中日志信息：
```
com.qcloud.iot D/IoTMqttFragment: onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=4}], msg[disconnected to ssl://connect.iot.qcloud.com:8883]
```

#### 15. 观察控制台日志
可以登录物联云控制台, 点击左边导航栏中的**云日志**, 查看刚才上报的消息

![](http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/doc/iot_1515734324922.png)

## 三、集成方式
SDK 提供以下两种集成方式：
#### 1. 源码集成
从 [github](https://github.com/tencentyun/qcloud-iot-sdk-android) 上下载 SDK 源码，根据 IoT-SDK 运行方式分为以下两种情况：
- 若需要将 IoT-SDK 运行在 service 组件中，则需同时集成 iot_core、iot_service 两个 module (iot_service 依赖 iot_core);
- 若不需要将 IoT-SDK 运行在 service 组件中，则只需集成 iot_core module。

#### 2. gradle 集成
在 App 的 build.gradle 文件中增加 IoT-SDK 的依赖，根据 IoT-SDK 运行方式分为以下两种情况：
- 若需要将 IoT-SDK 运行在 service 组件中，则需同时依赖 iot-core、iot-service aar，当前版本号为 1.2.0:
```
dependencies {
    compile 'com.qcloud.iot:iot-core:2.0.0'
    compile 'com.qcloud.iot:iot-service:2.0.0'
}
```
- 若不需要将 IoT-SDK 运行在 service 组件中，则只需要依赖 iot-core:
```
dependencies {
    compile 'com.qcloud.iot:iot-core:2.0.0'
}
```
**说明：推荐使用源码集成的方式**

#关于SDK的更多使用方式及接口了解, 请访问[官方WiKi](https://github.com/tencentyun/qcloud-iot-sdk-android/wiki)
