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
The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [SDK Connection Description](docs/SDK-Connection-Description.md).

## Downloading the sample code of IoT Explorer SDK for Java demo
Download the complete code in the [repository](../..). The sample code of the IoT Explorer SDK for Java demo is in the [explorer-device-java/src/test/java](../explorer-device-java/src/test/java) directory.

## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Controlling Device Connection and Disconnection](docs/Controlling-Device-Connection-and-Disconnection.md)
* [Dynamic Registration](docs/Dynamic-Registration.md)
* [Subscribing to and Unsubscribing from Topic](docs/Subscribing-to-and-Unsubscribing-from-Topic.md)
* [Attribute Reporting](docs/Attribute-Reporting.md)
* [Getting Latest Information Reported by Device](docs/Getting-Latest-Information-Reported-by-Device.md)
* [Device Information Reporting](docs/Device-Information-Reporting.md)
* [Clearing Control](docs/Clearing-Control.md)
* [Event Reporting and Multi-Event Reporting](docs/Event-Reporting-and-Multi-Event-Reporting.md)
* [Checking for Firmware Update](docs/Checking-for-Firmware-Update.md)
* [Gateway Use Cases](docs/Gateway-Use-Cases.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](docs/SDK-API-and-Parameter-Descriptions.md) in the `docs` directory.

