* [IoT Explorer Device SDK for Android](#IoT-Explorer-Device-SDK-for-Android)
  * [Prerequisites](#Prerequisites)
  * [SDK connection guide](#SDK-connection-guide)
  * [Downloading the sample code of IoT Explorer SDK for Android demo](#Downloading-the-sample-code-of-IoT-Explorer-SDK-for-Android-demo)
  * [Feature documentation](#Feature-documentation)
  * [Third-party service connection guide](#Third-Party-service-connection-guide)
  * [SDK API and parameter descriptions](#SDK-API-and-parameter-descriptions)
  * [FAQs](#FAQs)

# IoT Explorer Device SDK for Android
Welcome to the IoT Explorer device SDK for Android.

The IoT Explorer device SDK for Android works with the device data template defined by the platform to implement a framework for data interaction between devices and the cloud based on the data template protocol. You can quickly implement data interaction between devices and the platform as well as between devices and applications based on the framework. This document describes how to get and call the IoT Explorer SDK for Android. If you encounter any issues when using it, please [feel free to submit them at GitHub](https://github.com/tencentyun/iot-device-java/issues/new).

## Prerequisites
* Create a Tencent Cloud account and activate IoT Explorer in the Tencent Cloud console.
* Create project products and devices in the console. For detailed directions, please see [Project Management](https://cloud.tencent.com/document/product/1081/40290), [Product Definition](https://cloud.tencent.com/document/product/1081/34739), and [Device Debugging](https://cloud.tencent.com/document/product/1081/34741).

## SDK connection guide
The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [Compilation Environment and SDK Connection Description](docs/Compilation-Environment-and-SDK-Connection-Description.md).

## Downloading the sample code of IoT Explorer SDK for Android demo
Download the complete code in the [repository](../../../../../../../Downloads). The sample code of the IoT Explorer SDK for Android demo is in the [device-android-demo](../device-android-demo) module.

Download the [installation package](https://github.com/tencentyun/iot-device-android/wiki/下载安装).

## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Controlling Device Connection and Disconnection](docs/Controlling-Device-Connection-and-Disconnection.md)
* [Subscribing to and Unsubscribing from Topic](docs/Subscribing-to-and-Unsubscribing-from-Topic.md)
* [Attribute Reporting](docs/Attribute-Reporting.md)
* [Getting Latest Information Reported by Device](docs/Getting-Latest-Information-Reported-by-Device.md)
* [Device Information Reporting](docs/Device-Information-Reporting.md)
* [Clearing Control](docs/Clearing-Control.md)
* [Event Reporting and Multi-Event Reporting](docs/Event-Reporting-and-Multi-Event-Reporting.md)
* [Checking for Firmware Update](docs/Checking-for-Firmware-Update.md)
* [Gateway Use Cases](docs/Gateway-Use-Cases.md)
* [Network Configuration Through Bluetooth LLSync](docs/Network-Configuration-Through-Bluetooth-LLSync.md)

## Third-party service connection guide
* [IoT Explorer Face Recognition SDK for Android Development Guide](../explorer-device-face/README.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](docs/SDK-API-and-Parameter-Descriptions.md) in the `docs` directory.

## FAQs
For the FAQs, please see [FAQs for Android](docs/FAQs-for-Android.md) in the `docs` directory.
