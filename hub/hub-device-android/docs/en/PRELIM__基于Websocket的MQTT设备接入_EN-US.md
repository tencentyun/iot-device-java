* [Device Connection Through MQTT over WebSocket](#Device-Connection-Through-MQTT-over-WebSocket)
  * [Overview](#Overview)
  * [Running demo to try out connecting to MQTT over WebSocket](#Running-demo-to-try-out-connecting-to-MQTT-over-WebSocket)
  * [Running demo to try out disconnecting from MQTT over WebSocket](#Running-demo-to-try-out-disconnecting-from-MQTT-over-WebSocket)
  * [Running demo to try out viewing the status of MQTT connection over WebSocket](#Running-demo-to-try-out-viewing-the-status-of-MQTT-connection-over-WebSocket)

# Device Connection Through MQTT over WebSocket
## Overview
The IoT Hub platform supports MQTT communication over WebSocket, so that devices can use the MQTT protocol for message transfer on the basis of the WebSocket protocol. For more information, please see [Device Connection Through MQTT over WebSocket](https://cloud.tencent.com/document/product/634/46347).

## Running demo to try out connecting to MQTT over WebSocket

Please enter `PRODUCT_ID` (product ID), `DEVICE_NAME` (device name), and `DEVICE_PSK` (device key) in `app-config.json` as instructed in [Device Connection Through MQTT over TCP](../../hub-device-android/docs/Device-Connection-Through-MQTT-over-TCP.md).

Run the demo and click **Connect to MQTT (over WebSocket)** in the basic feature module for authenticated connection to MQTT. Below is the sample code:
```
TXWebSocketManager.getInstance().getClient(mProductID, mDevName).setSecretKey(mDevPSK, socketFactory); // Set `PRODUCT_ID`, `DEVICE_NAME`, and `DEVICE_PSK`
TXWebSocketManager.getInstance().getClient(mProductID, mDevName).setTXWebSocketActionCallback(new TXWebSocketActionCallback() { // Set the callback
    @Override
    public void onConnected() {// Connected to MQTT
    }
    @Override
    public void onMessageArrived(String topic, MqttMessage message) {// Message arrival callback function, where `topic` is the message topic and `message` is the message content
    }
    @Override
    public void onConnectionLost(Throwable cause) {// Callback for MQTT disconnection, where `cause` is the disconnection cause
    }
    @Override
    public void onDisconnected() {// Callback for MQTT disconnection completion
    }
});
```

The following log represents the process in which MQTT connects to the cloud over WebSocket successfully. In the console, you can see that the status of the device has been updated to `online`.
```
I/System.out: connectComplete
```

## Running demo to try out disconnecting from MQTT over WebSocket

Run the demo and click **Disconnect from MQTT (over WebSocket)** in the basic feature module to disconnect from MQTT. Below is the sample code:
```
TXWebSocketManager.getInstance().getClient(mProductID, mDevName).disconnect(); // Close the MQTT connection
TXWebSocketManager.getInstance().releaseClient(mProductID, mDevName); // Remove the object and close the MQTT connection
```

The following log represents the process in which MQTT disconnects over WebSocket successfully. In the console, you can see that the status of the device has been updated to `offline`.
```
I/System.out: disconnect onSuccess
```

## Running demo to try out viewing the status of MQTT connection over WebSocket

Run the demo and click **MQTT Connection Status (over WebSocket)** in the basic feature module to view the MQTT connection status. Below is the sample code:

```
ConnectionState show = TXWebSocketManager.getInstance().getClient(mProductID, mDevName).getConnectionState();// Query the status of the MQTT connection over WebSocket
Log.e(TAG, "current state " + show);
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
