* [API Description](#API-Description)
  * [MQTT APIs](#MQTT-APIs)
  * [MQTT gateway APIs](#MQTT-gateway-APIs)
  * [Device shadow APIs](#Device-shadow-APIs)
## API Description

### MQTT APIs ###
The APIs related to MQTT are defined in the [TXMqttConnection](../src/main/java/com/tencent/iot/hub/device/java/core/mqtt/TXMqttConnection.java) class and support publishing and subscribing. If you want the support for the device shadow feature, you need to use the `TXShadowConnection` class and its methods. `TXMqttConnection` class APIs are as detailed below:

| Method | Description |
| ------------------ | ------------------------------------------------------------ |
| connect | Establishes MQTT connection |
| reconnect | Reestablishes MQTT connection |
| disConnect | Closes MQTT connection |
| publish | Publishes MQTT message |
| subscribe | Subscribes to MQTT topic |
| unSubscribe | Unsubscribes from MQTT topic |
| getConnectStatus | Gets MQTT connection status |
| setBufferOpts | Sets buffer for disconnection status |
| initOTA | Initializes OTA feature |
| reportCurrentFirmwareVersion | Reports current device version information to backend server |
| reportOTAState | Reports device update status to backend server |


### MQTT Gateway APIs ###

Devices that don't have direct access to the Ethernet can be connected to the network of the local gateway device first and then connected to the IoT Hub platform through the communication feature of the gateway device. For the subdevices that join or leave the LAN, you need to bind or unbind them on the platform.
Note: after a subdevice is connected once, as long as the gateway is successfully connected subsequently, the backend will show that the subdevice is online until it is disconnected.
The APIs related to MQTT gateway are defined in the [TXGatewayConnection](../src/main/java/com/tencent/iot/hub/device/java/core/gateway/TXGatewayConnection.java) class as detailed below:

| Method | Description |
| ------------------ | ------------------------------------------------------------ |
| connect | Establishes gateway MQTT connection |
| reconnect | Reestablishes gateway MQTT connection |
| disConnect | Closes gateway MQTT connection |
| publish | Publishes MQTT message |
| subscribe | Subscribes to MQTT topic |
| unSubscribe | Unsubscribes from MQTT topic |
| getConnectStatus | Gets MQTT connection status |
| setBufferOpts | Sets buffer for disconnection status |
| gatewaySubdevOffline | Disconnects subdevice |
| gatewaySubdevOnline | Connects subdevice |
| gatewayBindSubdev | Binds subdevice |
| gatewayUnbindSubdev | Unbinds subdevice |
| getSubdevStatus | Gets subdevice status |
| setSubdevStatus | Sets subdevice status |
| gatewayGetSubdevRelation | Gets gateway topological relationship |

### Device shadow APIs ###

If you want the support for the device shadow feature, you need to use the methods in the [TXShadowConnection](../src/main/java/com/tencent/iot/hub/device/java/core/shadow/TXShadowConnection.java) class as detailed below:

| Method | Description |
| ------------------ | ------------------------------------------------------------ |
| connect | Establishes MQTT connection |
| disConnect | Closes MQTT connection |
| publish | Publishes MQTT message |
| subscribe | Subscribes to MQTT topic |
| update | Updates device shadow document |
| get | Gets device shadow document |
| reportNullDesiredInfo | Reports empty `desired` information after updating `delta` information |
| setBufferOpts | Sets buffer for disconnection status |
| getMqttConnection | Gets `TXMqttConnection` instance |
| getConnectStatus | Gets MQTT connection status |
| registerProperty | Registers the attribute of the current device |
| unRegisterProperty | Unregisters the specified attribute of the current device |
