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
The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [Compilation Environment and SDK Connection Description](docs/en/PRELIM__编译环境及SDK接入说明_EN-US.md).

## Downloading the sample code of IoT Explorer SDK for Android demo
Download the complete code in the [repository](../..). The sample code of the IoT Explorer SDK for Android demo is in the [device-android-demo](../device-android-demo) module.

Download the [installation package](https://github.com/tencentyun/iot-device-android/wiki/下载安装).

## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Controlling Device Connection and Disconnection](docs/en/PRELIM__控制设备上下线_EN-US.md)
* [Subscribing to and Unsubscribing from Topic](docs/en/PRELIM__订阅与取消订阅%20Topic%20主题_EN-US.md)
* [Attribute Reporting](docs/en/PRELIM__属性上报_EN-US.md)
* [Getting Latest Information Reported by Device](docs/en/PRELIM__获取设备最新上报信息_EN-US.md)
* [Device Information Reporting](docs/en/PRELIM__设备信息上报_EN-US.md)
* [Clearing Control](docs/en/PRELIM__清除控制_EN-US.md)
* [Event Reporting and Multi-Event Reporting](docs/en/PRELIM__事件上报以及多事件上报_EN-US.md)
* [Checking for Firmware Update](docs/en/PRELIM__检查固件更新_EN-US.md)
* [Gateway Use Cases](docs/en/PRELIM__网关使用示例_EN-US.md)
* [Network Configuration Through Bluetooth LLSync](docs/en/PRELIM__LLSync蓝牙辅助配网_EN-US.md)

## Third-party service connection guide
* [IoT Explorer Face Recognition SDK for Android Development Guide](../explorer-device-face/PRELIM__README%202_EN-US.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](docs/en/PRELIM__SDK%20API及参数说明_EN-US.md) in the `docs` directory.

## FAQs
For the FAQs, please see [FAQs for Android](docs/en/PRELIM__常见问题android_EN-US.md) in the `docs` directory.
