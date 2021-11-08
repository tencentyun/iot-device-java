[简体中文](../../README.md) | English

## Overview
This demo provides four entries: `IOT DEMO` shows the basic features and use cases of the [IoT Explorer Device SDK for Android](../../../explorer-device-android); `IOT AI FACE Demo` shows the simple use of [IoT Explorer Device Face Recognition SDK](../../../explorer-device-face); `IOT TRTC DEMO` shows the simple use of [IoT Explorer Device RTC SDK](../../../explorer-device-rtc); and `IOT TME DEMO` shows the simple use of [IoT Explorer Device TME SDK](../../../explorer-device-tme).
## Demo Entry Diagram
```
├── Demo
│   ├── IOT DEMO
│   ├── IOT AI FACE DEMO
│   ├── IOT TRTC DEMO
│   ├── IOT TME DEMO
```

## IOT Demo
This demo shows the basic features of the IoT Explorer device SDK for Android through the **Data Template**, **Demo**, and **Gateway Demo** tab pages.
### Data template
This page contains three sections from top to bottom: **parameter settings section**, **feature operation section**, and **log output section**.
1. Parameter settings section
    * It provides the input box for the drop-down options `key` and `value` (which are optional here). You can set them in [app-config.json](../../src/main/assets/app-config.json).
2. Feature operation section
    * It offers the following operations: device connection/disconnection, topic-related operations (such as subscribing to/unsubscribing from topic), attribute reporting, status update, device information reporting, control clearing, event reporting, and check for firmware update.
    * Note: to use other features in the feature operation section, you must click **Connect Device** and wait until the log output section prints out `onConnectComplete, status[ok]`.
3. Log output section
    * Logs of operations performed in the feature operation section will be output in this section; for example, if you click **Subscribe to Topic**, `onSubscribeCompleted` will be printed out here.

### Demo
This page contains two sections from top to bottom: **feature operation section** and **log output section**.
1. Feature operation section
    * It offers the following operations: device connection/disconnection and check for firmware update.
    * After the device is connected, you can modify its attribute values in the [console](https://console.cloud.tencent.com/iotexplorer), such as the brightness, color, and status of the light, and the demo will display the updated attribute values in real time.

2. Log output section
    * Logs of operations in the feature operation section will be output in this section; for example, if you click **Connect Device**, the value of the `Status` field here will be updated to `online`.

### Gateway demo
This page demonstrates how to add/delete a subdevice under a gateway device and connect/disconnect the subdevice and the gateway device. It contains two sections from top to bottom: **feature operation section** and **log output section**.
1. Feature operation section
    * It offers the following operations: subdevice addition/deletion under gateway device, gateway connection/disconnection, and subdevice addition/connection/disconnection.
    * Note: to click **Connect** and **Disconnect**, you must click **Add Smart Light** or **Add Air Conditioner** first.

2. Log output section
    * Logs of operations in the feature operation section will be output in this section; for example, if you click **Connect**, the value of the `Status` field here will be updated to `online`.


## IOT AI FACE DEMO
To be added.
## IOT TRTC DEMO
This page mainly contains two sections: **device QR code generation section** and **audio/video call section**. It demonstrates how the application and TRTC device make an audio/video call.

1. Device QR code generation section
    * Enter the identity information triplet of the TRTC device (product ID, device name, and device key).
    * Click **Connect Device** to generate the QR code of the TRTC device. You can scan the QR code with the **IoT Link application** to bind the device.

2. Audio/Video call section

    * You can click **Make Audio Call** or **Make Video Call** to start a call between the device and the application, provided that the device has been bound to the application.

## IOT TME DEMO
This demo contains two sections: **device information configuration page** and **music playback control page**. It demonstrates how to control the playback on the device through the **IoT Link application/mini program**.

1. Device information configuration page
    * Enter the identity information triplet of the device (product ID, device name, and device key).
    * Click **Configure** to redirect to the **playback control page**.

2. Playback control page
    * You can click **OFFLINE** to connect the device.
    * Other controls on this page are basic playback control buttons. Currently, it offers capabilities such as volume adjustment, playback progress adjustment, playback mode switch, play/pause switch, song selection, and sound quality adjustment.

3. How to enter the prerelease or trial environment
    * Enter the corresponding broker URL of the environment in the **Broker URL** input box on the **device information configuration page**

