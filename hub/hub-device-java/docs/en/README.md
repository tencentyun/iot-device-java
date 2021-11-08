[简体中文](../../README.md) | English

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

The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [SDK Connection Description](PRELIM__SDK接入说明_EN-US.md).

## Downloading the sample code of IoT Hub SDK for Java demo
Download the complete code in the [repository](../../../../). The sample code of the IoT Hub SDK for Java demo is in the [hub-device-java](../../../hub-device-java/src/test) module.


## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Device Connection Through MQTT over TCP](PRELIM__基于TCP的MQTT设备接入_EN-US.md)
* [Device Connection Through MQTT over WebSocket](PRELIM__基于Websocket的MQTT设备接入_EN-US.md)
* [Dynamic Registration](PRELIM__动态注册_EN-US.md)
* [RRPC Sync Communication](PRELIM__RRPC同步通信_EN-US.md)
* [Broadcast Communication](PRELIM__广播通信_EN-US.md)
* [Gateway Feature](PRELIM__网关功能_EN-US.md)
* [Firmware Update](PRELIM__固件升级_EN-US.md)
* [Gateway Subdevice Firmware Update](PRELIM__网关子设备固件升级_EN-US.md)
* [Device Log Reporting](PRELIM__设备日志上报_EN-US.md)
* [Gateway Device Topological Relationship](PRELIM__网关设备拓扑关系_EN-US.md)
* [Device Interconnection](PRELIM__设备互通_EN-US.md)
* [Device Shadow](PRELIM__设备影子_EN-US.md)
* [Self-Built Server Connection](PRELIM__自建服务器接入_EN-US.md)
* [Device Status Reporting and Setting](PRELIM__设备状态上报与状态设置_EN-US.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](PRELIM__SDK%20API及参数说明_EN-US.md) in the `docs` directory.

## FAQs

For the FAQs, please see [FAQs for Java](PRELIM__常见问题java_EN-US.md) in the `docs` directory.
