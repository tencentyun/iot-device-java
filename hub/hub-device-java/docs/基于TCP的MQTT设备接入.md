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

[下载IoT-Hub-Java-SDK-Demo示例代码](../README.md#下载IoT-Hub-Java-SDK-Demo示例代码)

#### 填写认证连接设备的参数
示例中编辑 [unit_test_config.json](../src/test/resources/unit_test_config.json) 文件中的参数配置信息
```
{
  "TESTMQTTSAMPLE_PRODUCT_ID":           "",
  "TESTMQTTSAMPLE_DEVICE_NAME":          "",
  "TESTMQTTSAMPLE_DEVICE_PSK":           "",
  "TESTMQTTSAMPLE_TEST_TOPIC":           "",
}
```
如果在控制台创建设备时使用的是密钥认证方式，需要在 unit_test_config.json 填写 TESTMQTTSAMPLE_PRODUCT_ID（产品ID）、TESTMQTTSAMPLE_DEVICE_NAME（设备名称）、TESTMQTTSAMPLE_DEVICE_PSK（设备密钥）；

如果在控制台创建设备时使用的是证书认证方式，除了需要在 unit_test_config.json 填写 TESTMQTTSAMPLE_PRODUCT_ID（产品ID）、TESTMQTTSAMPLE_DEVICE_NAME（设备名称），还需在 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 将 mDevPSK（设备密钥）设置为null之外，还需将证书和私钥放到 [resources](../src/test/resources/)文件夹中，填写mCertFilePath (设备证书文件名称)、mPrivKeyFilePath(设备私钥文件名称)。

```
private static String mCertFilePath = "";           // 填写 resources 文件夹下设备证书文件名称
private static String mPrivKeyFilePath = "";           // 填写 resources 文件夹下设备私钥文件名称
```

如果在控制台创建设备时使用的是证书认证方式，也可以复制证书和私钥内容填写 mDevCert(设备证书文件内容) mDevPriv(设备私钥文件内容)

```
private static String mDevCert = "";           // 填写 设备证书文件内容
private static String mDevPriv = "";           // 填写 设备私钥文件内容
```

#### 运行示例程序进行 MQTT 认证连接

运行 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 的main函数，调用connect()使设备上线。示例代码如下：
```
private static void connect() {
    try {
        Thread.sleep(2000);
        String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";

        // init connection
        options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        //客户端证书文件名  mDevPSK是设备秘钥

        if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
            LOG.info("Using cert stream " + mDevPriv + "  " + mDevCert);
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        } else if (mDevPSK != null && mDevPSK.length() != 0){
            LOG.info("Using PSK");

        } else {
            LOG.info("Using cert file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
        }
        mqttconnection = new TXMqttConnection(mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,true, new SelfMqttLogCallBack(), new callBack());
        mqttconnection.connect(options, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

以下是设备通过 MQTT 成功连接云端的logcat日志。
```
26/02/2021 09:42:50,157 [main] INFO  TXMqttConnection connect 338  - Start connecting to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883
26/02/2021 09:42:53,654 [MQTT Call: 9RW4A8OOFKdoor1] INFO  MqttSampleTest onConnectCompleted 141  - onConnectCompleted, status[OK], reconnect[false], userContext[], msg[connected to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883]
```

TXMqttActionCallBack为设备行为的回调
```
/**
 * MQTT Connect完成回调
 *
 * @param status        Status.OK: 连接成功； Status.ERROR: 连接失败
 * @param reconnect     true: 重新连接      false: 首次连接
 * @param userContext   用户上下文
 * @param msg           连接信息
 */
@Override
public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg){}
/**
 * MQTT连接断开回调
 *
 * @param cause       连接断开原因
 */
@Override
public void onConnectionLost(Throwable cause) {}
/**
 * MQTT Disconnect完成回调
 *
 * @param status      Status.OK: 断连成功； Status.ERROR: 断连失败
 * @param userContext 用户上下文
 * @param msg         详细信息
 */
@Override
public void onDisconnectCompleted(Status status, Object userContext, String msg) {}
/**
 * 发布消息完成回调
 *
 * @param status      Status.OK: 发布消息成功； Status.ERROR: 发布消息失败
 * @param token       消息token，包含消息内容结构体
 * @param userContext 用户上下文
 * @param msg         详细信息
 */
@Override
public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {}
/**
 * 订阅主题完成回调
 *
 * @param status      Status.OK: 订阅成功； Status.ERROR: 订阅失败
 * @param token       消息token，包含消息内容结构体
 * @param userContext 用户上下文
 * @param msg        详细信息
 */
@Override
public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {}
/**
 * 取消订阅主题完成回调
 *
 * @param status      Status.OK: 取消订阅成功； Status.ERROR: 取消订阅失败
 * @param token       消息token，包含消息内容结构体
 * @param userContext 用户上下文
 * @param msg         详细信息
 */
@Override
public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {}
/**
 * 收到订阅主题的消息Push
 *
 * @param topic        主题名称
 * @param message      消息内容
 */
@Override
public void onMessageReceived(final String topic, final MqttMessage message) {}
```

可通过getConnectStatus()API获取设备连接状态
```
enum ConnectStatus {
    kConnectIdle,  //初始状态
    kConnecting,   // 连接中
    kConnected,    // 连接上/上线
    kConnectFailed,// 连接失败
    kDisconnected  // 已断开连接
}
```

#### 运行示例程序进行断开 MQTT 连接

运行 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 的main函数，设备上线后调用disconnect()。示例代码如下：
```
private static void disconnect() {
    try {
        Thread.sleep(2000);
        mqttconnection.disConnect(null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

以下是设备成功断开 MQTT 连接的logcat日志。
```
26/02/2021 09:46:34,248 [MQTT Disc: 9RW4A8OOFKdoor1] INFO  MqttSampleTest onDisconnectCompleted 207  - onDisconnectCompleted, status[OK], userContext[], msg[disconnected to ssl://9RW4A8OOFK.iotcloud.tencentdevices.com:8883]
```

#### 订阅 Topic 主题
运行示例程序前，需要把将要订阅的 Topic 主题配置在 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 中的mTestTopic（Topic权限），Topic的生成请参考 [控制台创建设备](#控制台创建设备) 中权限的使用。

运行 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 的main函数，设备成功上线后，调用subscribeTopic()，订阅 Topic 主题。示例代码如下：
```
private static void subscribeTopic() {
    try {
        Thread.sleep(2000);
        mqttconnection.subscribe(mTestTopic, 1, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

以下是设备成功订阅 Topic 主题的logcat日志。
```
26/02/2021 09:51:23,240 [main] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: 9RW4A8OOFK/door1/data
26/02/2021 09:51:23,257 [MQTT Call: 9RW4A8OOFKdoor1] DEBUG MqttSampleTest onSubscribeCompleted 235  - onSubscribeCompleted, status[OK], topics[[9RW4A8OOFK/door1/data]], userContext[], errMsg[subscribe success]
```

#### 取消订阅 Topic 主题
设备之前订阅过的 Topic 主题，可以取消订阅。

运行 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 的main函数，设备成功上线后，调用unSubscribeTopic()，取消订阅 Topic 主题。示例代码如下：
```
private static void unSubscribeTopic() {
    try {
        Thread.sleep(2000);
        mqttconnection.unSubscribe(mTestTopic, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

以下是设备成功取消订阅 Topic 主题的logcat日志。
```
26/02/2021 09:53:36,372 [main] INFO  TXMqttConnection unSubscribe 712  - Starting unSubscribe topic: 9RW4A8OOFK/door1/data
26/02/2021 09:51:23,257 [MQTT Call: 9RW4A8OOFKdoor1] DEBUG MqttSampleTest onUnSubscribeCompleted 235  - onUnSubscribeCompleted, status[OK], topics[[9RW4A8OOFK/door1/data]], userContext[MQTTRequest{requestType='unSubscribeTopic', requestId=6}], errMsg[unsubscribe success]
```

#### 发布 Topic 主题
运行示例程序前，需要把将要发布的 Topic 主题配置在 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 中的mTestTopic（Topic权限），Topic的生成请参考 [控制台创建设备](#控制台创建设备) 中权限的使用。

运行 [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) 的main函数，设备成功上线后，调用publishTopic()，发布 Topic 主题。示例代码如下：
```
private static void publishTopic() {
    try {
        Thread.sleep(2000);
        // 要发布的数据
        Map<String, String> data = new HashMap<String, String>();
        // 车辆类型
        data.put("car_type", "suv");
        // 车辆油耗
        data.put("oil_consumption", "6.6");
        // 车辆最高速度
        data.put("maximum_speed", "205");
        // 温度信息
        data.put("temperature", "25");
        // MQTT消息
        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage()+"pack json data failed!");
        }
        message.setQos(TXMqttConstants.QOS1);
        message.setPayload(jsonObject.toString().getBytes());

        LOG.debug("pub topic " + mTestTopic + message);
        // 发布主题
        mqttconnection.publish(mTestTopic, message, null);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

以下是设备成功发布 Topic 主题的logcat日志。
```
26/02/2021 10:02:40,763 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: 9RW4A8OOFK/door1/data Message: {"oil_consumption":"6.6","temperature":"25","maximum_speed":"205","car_type":"suv"}
26/02/2021 10:02:40,774 [MQTT Call: 9RW4A8OOFKdoor1] DEBUG MqttSampleTest onPublishCompleted 279  - onPublishCompleted, status[OK], topics[[9RW4A8OOFK/door1/data]],  userContext[], errMsg[publish success]
```
