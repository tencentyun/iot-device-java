* [API Description](#API-Description)
  * [MQTT APIs](#MQTT-APIs)
  * [MQTT gateway APIs](#MQTT-gateway-APIs)
  * [Device shadow APIs](#Device-shadow-APIs)
  * [Dynamic registration APIs](#Dynamic-registration-APIs)

## API Description

### MQTT APIs
The APIs related to MQTT are defined in the [TXMqttConnection](../../../hub-device-android/src/main/java/com/tencent/iot/hub/device/android/core/mqtt/TXMqttConnection.java) class and support features such as publishing and subscribing. `TXMqttConnection` class APIs are as detailed below:

| Method | Description |
| ---------------------------------- | ----------------------------------------------- |
| connect | Establishes MQTT connection |
| reconnect | Reconnects |
| disConnect | Disconnects |
| publish | Publishes MQTT message |
| subscribe | Subscribes to MQTT topic |
| unSubscribe | Unsubscribes from MQTT topic |
| initOTA | Initializes OTA feature |
| reportCurrentFirmwareVersion | Reports current device version information to backend server |
| reportOTAState | Reports device update status to backend server |
| mLog | Generates device log |
| uploadLog | Triggers log upload |
| subscribeRRPCTopic | Subscribes to RRPC topic |
| getConnectStatus | Gets the current connection status |
| setBufferOpts | Sets buffer for disconnection status |
| subscribeBroadcastTopic | Subscribes to broadcast topic |

### MQTT Gateway APIs
Devices that don't have direct access to the Ethernet can be connected to the network of the local gateway device first and then connected to the IoT Hub platform through the communication feature of the gateway device. For the subdevices that join or leave the LAN, you need to bind or unbind them on the platform. Note: after a subdevice is connected once, as long as the gateway is successfully connected subsequently, the backend will show that the subdevice is online until it is disconnected. The APIs related to the MQTT gateway are defined in the [TXGatewayConnection](../../../hub-device-android/src/main/java/com/tencent/iot/hub/device/android/core/gateway/TXGatewayConnection.java) class as detailed below:

| Method | Description |
| ---------------------------------- | ----------------------------------------------- |
| connect | Establishes gateway MQTT connection |
| reconnect | Reestablishes gateway MQTT connection |
| disConnect | Closes gateway MQTT connection |
| publish | Publishes MQTT message |
| subscribe | Subscribes to MQTT topic |
| unSubscribe | Unsubscribes from MQTT topic |
| getConnectStatus | Gets the current connection status |
| setBufferOpts | Sets buffer for disconnection status |
| getSubdevStatus | Gets the current subdevice connection status |
| setSubdevStatus | Sets subdevice connection status |
| gatewaySubdevOffline | Disconnects subdevice |
| gatewaySubdevOnline | Connects subdevice |
| gatewayBindSubdev | Binds subdevice |
| gatewayUnbindSubdev | Unbinds subdevice |
| getGatewaySubdevRealtion | Queries device topological relationship |

### Device shadow APIs
If you want the support for the device shadow feature, you need to use the methods in the [TXShadowConnection](../../../hub-device-android/src/main/java/com/tencent/iot/hub/device/android/core/shadow/TXShadowConnection.java) class as detailed below:

| Method | Description |
| ---------------------------------- | ----------------------------------------------- |
| connect | Establishes MQTT connection |
| disConnect | Disconnects |
| publish | Publishes MQTT message |
| subscribe | Subscribes to MQTT topic |
| unSubscribe | Unsubscribes from MQTT topic |
| getConnectStatus | Gets the current connection status |
| setBufferOpts | Sets buffer for disconnection status |
| update | Updates device shadow document |
| get | Gets device shadow document |
| reportNullDesiredInfo | Reports empty `desired` information after updating `delta` information |
| registerProperty | Registers the attribute of the current device |
| unRegisterProperty | Unregisters the specified attribute of the current device |

### Dynamic registration APIs
If you want the support for the dynamic registration feature, you need to use the methods in the [TXMqttDynreg](../../../hub-device-java/src/main/java/com/tencent/iot/hub/device/java/core/dynreg/TXMqttDynreg.java) class as detailed below:

| Method | Description |
| ---------------------------------- | ----------------------------------------------- |
| doDynamicRegister | Dynamic registration API |

[TXMqttDynregCallback](../../../hub-device-java/src/main/java/com/tencent/iot/hub/device/java/core/dynreg/TXMqttDynregCallback.java) is the dynamic registration callback API as detailed below:

| Method | Description |
| ---------------------------------- | ----------------------------------------------------|
| onGetDevicePSK | The device key is called back after dynamic registration authentication success |
| onGetDeviceCert | The device certificate content string and device private key content string are called back after dynamic registration authentication success |
| onFailedDynreg | Callback for dynamic registration authentication failure |
