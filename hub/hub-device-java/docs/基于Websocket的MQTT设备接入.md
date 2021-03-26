* [基于Websocket的MQTT设备接入](#基于Websocket的MQTT设备接入)
  * [基于Websocket的MQTT设备接入简介](#基于Websocket的MQTT设备接入简介)
  * [填写认证连接设备的参数](#填写认证连接设备的参数)
  * [运行示例程序体验通过Websocket连接MQTT功能](#运行示例程序体验通过Websocket连接MQTT功能)
  * [运行示例程序体验通过Websocket断开MQTT连接功能](#运行示例程序体验通过Websocket断开MQTT连接功能)
  * [运行示例程序体验查看通过Websocket的MQTT连接状态](#运行示例程序体验查看通过Websocket的MQTT连接状态)

# 基于Websocket的MQTT设备接入
## 基于Websocket的MQTT设备接入简介
物联网平台支持基于 WebSocket 的 MQTT 通信，设备可以在 WebSocket 协议的基础之上使用 MQTT 协议进行消息的传输。请参考官网 [设备基于 WebSocket 的 MQTT 接入](https://cloud.tencent.com/document/product/634/46347)

## 填写认证连接设备的参数
示例中编辑 [TestWebsocketMqttSample.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/TestWebsocketMqttSample.java) 文件中的参数配置信息
```
{
  private static String mProductID = "";
  private static String mDevName = "";
  private static String mDevPSK  = ""; //若使用证书验证，设为null
}
```
如果在控制台创建设备时使用的是密钥认证方式，需要在 TestWebsocketMqttSample.java 填写 mProductID（产品ID）、mDevName（设备名称）、mDevPSK（设备密钥）；

如果在控制台创建设备时使用的是证书认证方式，除了需要在 TestWebsocketMqttSample.java 填写 mProductID（产品ID）、mDevName（设备名称），mDevPSK（设备密钥）设置为null之外，还需将证书和私钥放到 [resources](../src/test/resources/)文件夹中，填写mCertFilePath (设备证书文件名称)、mPrivKeyFilePath(设备私钥文件名称)。

```
private static String mCertFilePath = "";           // 填写 resources 文件夹下设备证书文件名称
private static String mPrivKeyFilePath = "";           // 填写 resources 文件夹下设备私钥文件名称
```

如果在控制台创建设备时使用的是证书认证方式，也可以复制证书和私钥内容填写 mDevCert(设备证书文件内容) mDevPriv(设备私钥文件内容)

```
private static String mDevCert = "";           // 填写 设备证书文件内容
private static String mDevPriv = "";           // 填写 设备私钥文件内容
```

## 运行示例程序体验通过Websocket连接MQTT功能

请先按照 [基于TCP的MQTT设备接入](../../hub-device-java/docs/基于TCP的MQTT设备接入.md) 的步骤 需要填写好 TestWebsocketMqttSample.java 中对应参数，mProductID（产品ID）、mDevName（设备名称）、mDevPSK（设备密钥）。

运行 [TestWebsocketMqttSample.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/TestWebsocketMqttSample.java) 的main函数，调用websocketConnect()，通过Websocket进行MQTT认证连接。示例代码如下：
```
private static void websocketConnect() {

    try {
        // init connection
        MqttConnectOptions conOptions = new MqttConnectOptions();
        conOptions.setCleanSession(true);
        
        if (mDevPSK != null && mDevPSK.length() != 0) {
            LOG.info("Using PSK");
            conOptions.setSocketFactory(AsymcSslUtils.getSocketFactory());
        } else if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
            LOG.info("Using cert stream " + mDevPriv + "  " + mDevCert);
            conOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        } else {
            LOG.info("Using cert file");
            String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
            conOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
        }
        
        conOptions.setConnectionTimeout(8);
        conOptions.setKeepAliveInterval(60);
        conOptions.setAutomaticReconnect(true);
        
        TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setMqttConnectOptions(conOptions);

        TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setTXWebSocketActionCallback(new TXWebSocketActionCallback() {

            @Override
            public void onConnected() {
                LOG.debug("onConnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
            }

            @Override
            public void onMessageArrived(String topic, MqttMessage message) {
                LOG.debug("onMessageArrived topic=" + topic);
            }

            @Override
            public void onConnectionLost(Throwable cause) {
                LOG.debug("onConnectionLost" + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
            }

            @Override
            public void onDisconnected() {
                LOG.debug("onDisconnected" + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
            }
        });
        TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).connect();
    } catch (MqttException e) {
        e.printStackTrace();
        LOG.error("MqttException " + e.toString());
    }
}
```

以下是 Websocket 的 MQTT 成功连接云端的日志，在控制台中观察可发现该设备状态已更新为在线。
```
connectComplete
11/03/2021 19:47:17,509 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TestWebsocketMqttSample onConnected 189  - onConnected CONNECTING
```

## 运行示例程序体验通过Websocket断开MQTT连接功能

运行 [TestWebsocketMqttSample.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/TestWebsocketMqttSample.java) 的main函数，设备通过Websocket上线后调用websocketdisconnect()，断开 MQTT 认证连接。示例代码如下：
```
private static void websocketdisconnect() {
    try {
        Thread.sleep(2000);
        TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).disconnect();
    } catch (MqttException | InterruptedException e) {
        e.printStackTrace();
    }
}
```

以下是 Websocket 的 MQTT 成功断开连接的日志，在控制台中观察可发现该设备状态已更新为离线。
```
11/03/2021 19:48:17,509 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG TestWebsocketMqttSample onConnected 189  - disconnect onSuccess
```

## 运行示例程序体验查看通过Websocket的MQTT连接状态

查看MQTT连接状态。示例代码如下：

```
TXWebSocketManager.getInstance().getClient(mProductID, mDevName).getConnectionState();
```

ConnectionState对应的连接状态
```
public enum ConnectionState {
    CONNECTING(0),      // 连接中
    CONNECTED(1),       // 连接上/上线
    CONNECTION_LOST(2), // 网络波动造成的掉线（被动触发）
    DISCONNECTING(3),   // 断开连接中（主动触发）
    DISCONNECTED(4);    // 已断开连接（主动触发）
}
```
