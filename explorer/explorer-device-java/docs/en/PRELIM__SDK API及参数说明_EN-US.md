* [API Description](#API-Description)
  * [SDK design description](#SDK-design-description)
  * [SDK API description](#SDK-API-description)

## API Description

### SDK design description

| Class | Feature  |
| -------------------- | -------------------------------------------- |
| TXMqttConnection | Connects to IoT Explorer |
| TXDataTemplate | Implements basic features of data template |
| TXDataTemplateClient | Connects directly connected device to IoT Explorer based on data template |
| TXGatewayClient | Connects gateway device to IoT Explorer based on data template |
| TXGatewaySubdev | Connects gateway subdevice to IoT Explorer based on data template |

![](https://main.qcloudimg.com/raw/ea345acb67bd0f9ef20a7336704bd070.jpg)

### SDK API description

#### TXMqttConnection

| Method | Description |
| ---------------------------- | -----------------------------------|
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

#### TXDataTemplate

| Method | Description |
| ------------------------ | ------------------------ |
| subscribeTemplateTopic | Subscribes to topic related to data template |
| unSubscribeTemplateTopic | Unsubscribes from topic related to data template |
| propertyReport | Reports attribute |
| propertyGetStatus | Updates status |
| propertyReportInfo | Reports device information |
| propertyClearControl | Clears control information |
| eventSinglePost | Reports one event |
| eventsPost | Reports multiple events |

#### TXDataTemplateClient

| Method | Description |
| ------------------------ | -------------------------- |
| isConnected | Checks connection to IoT Explorer |
| subscribeTemplateTopic | Subscribes to topic related to data template |
| unSubscribeTemplateTopic | Unsubscribes from topic related to data template |
| propertyReport | Reports attribute |
| propertyGetStatus | Updates status |
| propertyReportInfo | Reports device information |
| propertyClearControl | Clears control information |
| eventSinglePost | Reports one event |
| eventsPost | Reports multiple events |

#### TXGatewayClient

| Method | Description |
| ------------- | ---------------------------------- |
| findSubdev | Finds subdevice (by product ID and device name) |
| removeSubdev | Removes subdevice |
| addSubdev | Adds subdevice |
| subdevOffline | Disconnects subdevice |
| subdevOnline | Connects subdevice |
| setSubdevStatus | Sets subdevice status |
| subscribeSubDevTopic | Subscribes to topic related to data template |
| unSubscribeSubDevTopic | Unsubscribes from topic related to data template |
| subDevPropertyReport | Reports attribute |

#### TXGatewaySubdev

| Method | Description |
| --------------- | ------------------ |
| getSubdevStatus | Gets subdevice connection status |
| setSubdevStatus | Sets subdevice connection status |

