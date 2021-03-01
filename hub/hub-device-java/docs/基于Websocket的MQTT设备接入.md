* [基于Websocket的MQTT设备接入](#基于Websocket的MQTT设备接入)
  * [基于Websocket的MQTT设备接入简介](#基于Websocket的MQTT设备接入简介)
  * [运行示例程序体验通过Websocket连接MQTT功能](#运行示例程序体验通过Websocket连接MQTT功能)
  * [运行示例程序体验通过Websocket断开MQTT连接功能](#运行示例程序体验通过Websocket断开MQTT连接功能)
  * [运行示例程序体验查看通过Websocket的MQTT连接状态](#运行示例程序体验查看通过Websocket的MQTT连接状态)

# 基于Websocket的MQTT设备接入
## 基于Websocket的MQTT设备接入简介
物联网平台支持基于 WebSocket 的 MQTT 通信，设备可以在 WebSocket 协议的基础之上使用 MQTT 协议进行消息的传输。请参考官网 [设备基于 WebSocket 的 MQTT 接入](https://cloud.tencent.com/document/product/634/46347)

## 运行示例程序体验通过Websocket连接MQTT功能

请先按照 [基于TCP的MQTT设备接入](../../hub-device-java/docs/基于TCP的MQTT设备接入.md) 的步骤 需要填写好 MqttSample.java 中对应参数，mProductID（产品ID）、mDevName（设备名称）、mDevPSK（设备密钥）。

运行 [MqttSample.java](../src/test/java/MqttSample.java) 的main函数，调用websocketConnect()，通过Websocket进行MQTT认证连接。示例代码如下：
```

SocketFactory socketFactory = null;
if (mDevPSK != null) {
    socketFactory = AsymcSslUtils.getSocketFactory();
} else {
    String workDir = System.getProperty("user.dir") + "/";
    socketFactory = AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath);
}

TXWebSocketManager.getInstance().getClient(mProductID, mDevName).setSecretKey(mDevPSK, socketFactory);
try {
    TXWebSocketManager.getInstance().getClient(mProductID, mDevName).setTXWebSocketActionCallback(new TXWebSocketActionCallback() { //设置回调
        @Override
        public void onConnected() {//MQTT已连接
        }
        @Override
        public void onMessageArrived(String topic, MqttMessage message) {//消息到达回调函数， topic 消息主题， message 消息内容
        }
        @Override
        public void onConnectionLost(Throwable cause) {//MQTT连接断开回调, cause 连接断开原因
        }
        @Override
        public void onDisconnected() {//MQTT Disconnect断开连接完成回调
        }
    });
    TXWebSocketManager.getInstance().getClient(mProductID, mDevName).connect();
} catch (MqttException e) {
    e.printStackTrace();
    LOG.error("MqttException " + e.toString());
}
```

以下是 Websocket 的 MQTT 成功连接云端的日志，在控制台中观察可发现该设备状态已更新为在线。
```
I/System.out: connectComplete
```

## 运行示例程序体验通过Websocket断开MQTT连接功能

运行 [MqttSample.java](../src/test/java/MqttSample.java) 的main函数，设备通过Websocket上线后调用websocketdisconnect()，断开 MQTT 认证连接。示例代码如下：
```
private static void websocketdisconnect() {
    try {
        TXWebSocketManager.getInstance().getClient(mProductID, mDevName).disconnect();
    } catch (MqttException e) {
        e.printStackTrace();
    }
}
```

以下是 Websocket 的 MQTT 成功断开连接的日志，在控制台中观察可发现该设备状态已更新为离线。
```
I/System.out: disconnect onSuccess
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
