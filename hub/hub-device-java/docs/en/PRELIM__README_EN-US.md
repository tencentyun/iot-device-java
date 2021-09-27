* [IoT Hub Device SDK for Java](#IoT-Hub-Device-SDK-for-Java)
  * [Prerequisites](#Prerequisites)
  * [Project configuration](#Project-configuration)
  * [Downloading the sample code of IoT Hub SDK for Java demo](#Downloading-the-sample-code-of-IoT-Hub-SDK-for-Java-demo)
  * [Feature documentation](#Feature-documentation)
  * [SDK API and parameter descriptions](#SDK-API-and-parameter-descriptions)
  * [FAQs](#FAQs)

# IoT Hub Device SDK for Java
Welcome to the IoT Hub device SDK for Java.

The IoT Hub device SDK for Java relies on a secure and powerful data channel to enable IoT developers to connect devices (such as sensors, actuators, embedded devices, and smart home appliances) to the cloud for two-way communication. This document describes how to get and call the IoT Hub SDK for Java. If you encounter any issues when using it, please [feel free to submit them at GitHub](https://github.com/tencentyun/iot-device-java/issues/new).

## Prerequisites
* Create a Tencent Cloud account and activate IoT Hub in the Tencent Cloud console.
* Create IoT products and devices in the console and get the product ID, device name, device certificate (for certificate authentication), device private key (for certificate authentication), and device key (for key authentication), which are required for authentication of the devices when you connect them to the cloud. For more information, please see [Device Connection Preparations](https://cloud.tencent.com/document/product/634/14442).
* Understand the topic permissions. After a product is created successfully in the console, it has three permissions by default: subscribing to `${productId}/${deviceName}/control`, subscribing and publishing to `${productId}/${deviceName}/data`, and publishing to `${productId}/${deviceName}/event`. For more information on how to manipulate the topic permissions, please see [Permission List](https://cloud.tencent.com/document/product/634/14444).

## Project configuration

The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [SDK Connection Description](docs/SDK-Connection-Description.md).

## Downloading the sample code of IoT Hub SDK for Java demo
Download the complete code in the [repository](https://github.com/tencentyun/iot-device-java). The sample code of the IoT Hub SDK for Java demo is in the [hub-device-java](../hub-device-java/src/test) module.


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
* [Self-Built Server Connection](docs/Self-Built-Server-Connection.md)
* [Device Status Reporting and Setting](docs/Device-Status-Reporting-and-Setting.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](docs/SDK-API-and-Parameter-Descriptions.md) in the `docs` directory.

## FAQs

For the FAQs, please see [FAQs for Java](docs/FAQs-for-Java.md) in the `docs` directory.
