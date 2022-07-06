# Self-Built Server Connection

This document describes how to connect a self-built server to the SDK.

## Passing in the broker address of self-built server
The SDK allows you to pass in the broker address of a self-built server by initializing `TXMqttConnection` or `TXShadowConnection` as detailed below:

```
TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack)
TXShadowConnection(String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXShadowActionCallBack callBack)
```

Here, the `serverURI` parameter is the broker address of the self-built server to be passed in.

## Passing in the corresponding CA certificate of self-built server

The SDK allows you to pass in the corresponding CA certificate of a self-built server by calling the `AsymcSslUtils.getSocketFactory(String customCA)` method. Below is the sample code:

```
String myCA = "Your CA certificate text"
MqttConnectOptions options = new MqttConnectOptions();
options.setSocketFactory(AsymcSslUtils.getSocketFactory(myCA));
mShadowConnection = new TXShadowConnection(testProductIDString, testDeviceNameString, testPSKString, new callback());
mShadowConnection.connect(options, null);
```

The above are the APIs used to configure the corresponding CA certificate of a self-built server during the authentication of devices and device shadows for connection.

## Passing in the corresponding domain name of self-built server for MQTT over WebSocket connection

The SDK allows you to pass in the corresponding domain name of a self-built server for MQTT over WebSocket connection by getting `TXWebSocketClient` as detailed below:

```
TXWebSocketClient getClient(String wsUrl, String productId, String devicename)
```

Here, the `wsUrl` parameter is the domain name of the self-built server to be passed in for MQTT over WebSocket connection.

## Passing in the corresponding dynamic registration URL of self-built server

The SDK allows you to pass in the dynamic registration URL of a self-built server by initializing `TXMqttDynreg` as detailed below:

```
TXMqttDynreg(String dynregUrl, String productId, String productKey, String deviceName, TXMqttDynregCallback callback)
```

Here, the `dynregUrl` parameter is the dynamic registration URL of the self-built server to be passed in.

## Passing in the connection domain name of self-built server for log reporting
The SDK allows you to pass in the connection domain name of a self-built server for log reporting by initializing `TXMqttConnection` or `TXGatewayConnection` as detailed below:

```
TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl)
TXGatewayConnection(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl)
```

Here, the `logUrl` parameter is the connection domain name of the self-built server to be passed in for log reporting.
