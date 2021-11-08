[简体中文](../../README.md) | English

* [IoT Explorer Device SDK for Java](#IoT-Explorer-Device-SDK-for-Java)
  * [Prerequisites](#Prerequisites)
  * [Project configuration](#Project-configuration)
  * [Downloading the sample code of IoT Explorer SDK for Java demo](#Downloading-the-sample-code-of IoT-Explorer-SDK-for-Java-demo)
  * [Feature documentation](#Feature-documentation)
  * [SDK API description](#SDK-API-description)

# IoT Explorer Device SDK for Java
Welcome to the IoT Explorer device SDK for Java.

The IoT Explorer device SDK for Java works with the device data template defined by the platform to implement a framework for data interaction between devices and the cloud based on the data template protocol. You can quickly implement data interaction between devices and the platform as well as between devices and applications based on the framework. This document describes how to get and call the IoT Explorer SDK for Java. If you encounter any issues when using it, please [feel free to submit them at GitHub](https://github.com/tencentyun/iot-device-java/issues/new).

## Prerequisites
* Create a Tencent Cloud account and activate IoT Explorer in the Tencent Cloud console.
* Create project products and devices in the console and get the product ID, device name, device certificate (for certificate authentication), device private key (for certificate authentication), and device key (for key authentication), which are required for authentication of the devices when you connect them to the cloud. For detailed directions, please see [Project Management](https://cloud.tencent.com/document/product/1081/40290), [Product Definition](https://cloud.tencent.com/document/product/1081/34739), and [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

## Project configuration
The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [SDK Connection Description](PRELIM__SDK接入说明_EN-US.md).

## Downloading the sample code of IoT Explorer SDK for Java demo
Download the complete code in the [repository](../../../..). The sample code of the IoT Explorer SDK for Java demo is in the [explorer-device-java/src/test/java](../../../explorer-device-java/src/test/java) directory.

## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Controlling Device Connection and Disconnection](PRELIM__控制设备上下线_EN-US.md)
* [Dynamic Registration](PRELIM__动态注册_EN-US.md)
* [Subscribing to and Unsubscribing from Topic](PRELIM__订阅与取消订阅%20Topic%20主题_EN-US.md)
* [Attribute Reporting](PRELIM__属性上报_EN-US.md)
* [Getting Latest Information Reported by Device](PRELIM__获取设备最新上报信息_EN-US.md)
* [Device Information Reporting](PRELIM__设备信息上报_EN-US.md)
* [Clearing Control](PRELIM__清除控制_EN-US.md)
* [Event Reporting and Multi-Event Reporting](PRELIM__事件上报以及多事件上报_EN-US.md)
* [Checking for Firmware Update](PRELIM__检查固件更新_EN-US.md)
* [Gateway Use Cases](PRELIM__网关使用示例_EN-US.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](PRELIM__SDK%20API及参数说明_EN-US.md) in the `docs` directory.

