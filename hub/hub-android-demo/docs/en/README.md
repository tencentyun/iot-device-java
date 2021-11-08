[简体中文](../../README.md) | English

## Overview

This demo shows the basic features and use cases of the [IoT Hub SDK for Android](https://github.com/tencentyun/iot-device-java/tree/master/hub/hub-device-android) through four tab pages.
## Demo Entry Diagram
```
├── Demo
│   ├── Basic feature
│   ├── Device interconnection
│   ├── Device shadow
│   ├── Remote service
```

## Demo Execution Path
### Basic feature
This page contains three sections from top to bottom: **parameter settings section**, **feature operation section**, and **log output section**.
1. Parameter settings section
    * It provides the input box for the drop-down options `key` and `value` (which are optional here). You can set them in [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub/hub-android-demo/src/main/assets/app-config.json).
2. Feature operation section
    * It offers the following operations: connection to/disconnection from MQTT, dynamic registration, topic-related operations (subscribing to/unsubscribing from/publishing to topic and subscribing to RRPC/broadcast topics), subdevice connection/disconnection, check for firmware update, log-related operations, subdevice binding/unbinding, device topological relationship query, and WebSocket-related operations.
    * Note: to use other features in the feature operation section, you must click **Connect to MQTT** and wait until the log output section print out `onConnectComplete, status[ok]`.

3. Log output section
    * Logs of operations performed in the feature operation section will be output in this section; for example, if you click **Subscribe to Topic**, `onSubscribeCompleted` will be printed out here.

### Device interconnection
This page contains two sections from top to bottom: **feature operation section** and **log output section**.
1. Feature operation section
    * [Smart home scenario background](https://cloud.tencent.com/document/product/634/11913)
    * It offers two operations: homecoming and homeleaving. The former turns on the air conditioner in the room, while the latter turns off the air conditioner.

2. Log output section
    * Logs of operations performed in the feature operation section will be output in this section; for example, if you click **Come Home**, `receive command: open airconditioner` will be printed out here.

### Device shadow
This page contains two sections from top to bottom: **feature operation section** and **log output section**.
1. Feature operation section
    * It offers the following operations: connection to/disconnection from IoT Hub, device attribute registration, device document acquisition, regular device shadow update, and topic-related operations (subscribing to/unsubscribing from/publishing to topic).

2. Log output section
    * Logs of operations performed in the feature operation section will be output in this section; for example, if you click **Get Device Document**, `document[{...` will be printed out here.


### Remote service
This page contains two sections from top to bottom: **feature operation section** and **log output section**.
1. Feature operation section
    * It offers the following operations: remote service start/stop and shadow enablement/disablement, where remote service refers to running features in the Android Service component such as disconnecting from MQTT.
    * If you select the **Use Shadow** option, operations such as device attribute registration, device shadow acquisition, and device shadow update will be available.

2. Log output section
    * Logs of operations performed in the feature operation section will be output in this section; for example, if you click **Start Remote Service**, `remote service has been started!` will be printed out here.
