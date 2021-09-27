* [Device Connection Through MQTT over WebSocket](#Device-Connection-Through-MQTT-over-WebSocket)
  * [Overview](#Overview)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Running demo to try out connecting to MQTT over WebSocket](#Running-demo-to-try-out-connecting-to-MQTT-over-WebSocket)
  * [Running demo to try out disconnecting from MQTT over WebSocket](#Running-demo-to-try-out-disconnecting-from-MQTT-over-WebSocket)
  * [Running demo to try out viewing the status of MQTT connection over WebSocket](#Running-demo-to-try-out-viewing-the-status-of-MQTT-connection-over-WebSocket)

# Device Connection Through MQTT over WebSocket
## Overview
The IoT Hub platform supports MQTT communication over WebSocket, so that devices can use the MQTT protocol for message transfer on the basis of the WebSocket protocol. For more information, please see [Device Connection Through MQTT over WebSocket](https://cloud.tencent.com/document/product/634/46347).

## Entering parameters for authenticating device for connection
Edit the parameter configuration information in the [unit_test_config.json](../../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTWEBSOCKETMQTTSAMPLE_PRODUCT_ID":  "",
  "TESTWEBSOCKETMQTTSAMPLE_DEVICE_NAME": "",
  "TESTWEBSOCKETMQTTSAMPLE_DEVICE_PSK":  "",
}
```
If key authentication is used during device creation in the console, you need to enter `TESTWEBSOCKETMQTTSAMPLE_PRODUCT_ID` (product ID), `TESTWEBSOCKETMQTTSAMPLE_DEVICE_NAME` (device name), and `TESTWEBSOCKETMQTTSAMPLE_DEVICE_PSK` (device key) in `unit_test_config.json`;

If certificate authentication is used during device creation in the console, in addition to entering `TESTWEBSOCKETMQTTSAMPLE_PRODUCT_ID` (product ID) and `TESTWEBSOCKETMQTTSAMPLE_DEVICE_NAME` (device name) in `unit_test_config.json`, you also need to set `mDevPSK` (device key) to `null` in [WebsocketMqttSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/WebsocketMqttSampleTest.java), place the certificate and private key in the [resources](../../src/test/resources/) folder, and enter `mCertFilePath` (device certificate file name) and `mPrivKeyFilePath` (device private key file name).

```
private static String mCertFilePath = "";           // Enter the name of the device certificate file in the `resources` folder
private static String mPrivKeyFilePath = "";           // Enter the name of the device private key file in the `resources` folder
```

If certificate authentication is used during device creation in the console, you can also enter `mDevCert` (device certificate file content) and `mDevPriv` (device private key file content) by copying the certificate and private key content.

```
private static String mDevCert = "";           // Enter the device certificate file content
private static String mDevPriv = "";           // Enter the device private key file content
```

## Running demo to try out connecting to MQTT over WebSocket

Please enter `mProductID` (product ID), `mDevName` (device name), and `mDevPSK` (device key) in `WebsocketMqttSampleTest.java` as instructed in [Device Connection Through MQTT over TCP](../../../hub-device-java/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md) first.

Run the `main` function in [WebsocketMqttSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/WebsocketMqttSampleTest.java) and call `websocketConnect()` for authenticated connection to MQTT over WebSocket. Below is the sample code:
```
private static void websocketConnect() {

    try {
        // init connection
        MqttConnectOptions conOptions = new MqttConnectOptions();
        conOptions.setCleanSession(true);
        
        if (mDevPSK != null && mDevPSK.length() != 0) {
            LOG.info("Using PSK");
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

The following log represents the process in which MQTT connects to the cloud over WebSocket successfully. In the console, you can see that the status of the device has been updated to `online`.
```
connectComplete
11/03/2021 19:47:17,509 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG WebsocketMqttSampleTest onConnected 189  - onConnected CONNECTING
```

## Running demo to try out disconnecting from MQTT over WebSocket

Run the `main` function in [WebsocketMqttSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/WebsocketMqttSampleTest.java). After the device is connected over WebSocket, call `websocketdisconnect()` to close its authenticated connection to MQTT. Below is the sample code:
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

The following log represents the process in which MQTT disconnects over WebSocket successfully. In the console, you can see that the status of the device has been updated to `offline`.
```
11/03/2021 19:48:17,509 [MQTT Call: DVSVXI409Ccert_test_1] DEBUG WebsocketMqttSampleTest onConnected 189  - disconnect onSuccess
```

## Running demo to try out viewing the status of MQTT connection over WebSocket

View the MQTT connection status. Below is the sample code:

```
TXWebSocketManager.getInstance().getClient(mProductID, mDevName).getConnectionState();
```

Connection status corresponding to `ConnectionState`
```
public enum ConnectionState {
    CONNECTING(0),      // Connecting
    CONNECTED(1),       // Connected
    CONNECTION_LOST(2), // Disconnected due to network fluctuations (passively triggered)
    DISCONNECTING(3),   // Disconnecting (actively triggered)
    DISCONNECTED(4);    // Disconnected (actively triggered)
}
```
