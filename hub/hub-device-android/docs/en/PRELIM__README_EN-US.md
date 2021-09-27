* [IoT Hub Device SDK for Android](#IoT-Hub-Device-SDK-for-Android)
  * [Prerequisites](#Prerequisites)
  * [SDK connection guide](#SDK-connection-guide)
  * [Downloading the sample code of IoT Hub SDK for Android demo](#Downloading-the-sample-code-of-IoT-Hub-SDK-for-Android-demo)
  * [Feature documentation](#Feature-documentation)
  * [SDK API and parameter descriptions](#SDK-API-and-parameter-descriptions)
  * [FAQs](#FAQs)

# IoT Hub Device SDK for Android
Welcome to the IoT Hub device SDK for Android.

The IoT Hub device SDK for Android relies on a secure and powerful data channel to enable IoT developers to connect devices (such as sensors, actuators, embedded devices, and smart home appliances) to the cloud for two-way communication. This document describes how to get and call the IoT Hub SDK for Android. If you encounter any issues when using it, please [feel free to submit them at GitHub](https://github.com/tencentyun/iot-device-java/issues/new).

## Prerequisites
* Create a Tencent Cloud account and activate IoT Hub in the Tencent Cloud console.
* Create IoT products and devices in the console. For more information, please see [Device Connection Preparations](https://cloud.tencent.com/document/product/634/14442).

## SDK connection guide
The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [Compilation Environment and SDK Connection Description](docs/Compilation-Environment-and-SDK-Connection-Description.md).

## Downloading the sample code of IoT Hub SDK for Android demo
Download the complete code in the [repository](https://github.com/tencentyun/iot-device-java). The sample code of the IoT Hub SDK for Android demo is in the [hub-android-demo](../hub-android-demo) module.

Download the [installation package](https://github.com/tencentyun/iot-device-android/wiki/下载安装).

## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Device Connection Through MQTT over TCP](docs/Device-Connection-Through-MQTT-over-TCP.md)
* [Device Connection Through MQTT over WebSocket](docs/Device-Connection-Through-MQTT-over-WebSocket.md)
* [Dynamic Registration](docs/Dynamic-Registration.md)
* [RRPC Sync Communication](docs/RRPC-Sync-Communication.md)
* [Broadcast Communication](docs/Broadcast-Communication.md)
* [Gateway Feature](docs/Gateway-Feature.md)
* [Firmware Update](docs/Firmware-Update.md)
* [Gateway Subdevice Firmware Update](docs/Gateway-Subdevice-Firmware-Update.md)
* [Device Log Reporting](docs/Device-Log-Reporting.md)
* [Gateway Device Topological Relationship](docs/Gateway-Device-Topological-Relationship.md)
* [Device Interconnection](docs/Device-Interconnection.md)
* [Device Shadow](docs/Device-Shadow.md)
* [Self-Built Server Connection](../hub-device-java/docs/Self-Built-Server-Connection.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](docs/SDK-API-and-Parameter-Descriptions.md) in the `docs` directory.

## FAQs
For the FAQs, please see [FAQs for Android](docs/FAQs-for-Android.md) in the `docs` directory.
