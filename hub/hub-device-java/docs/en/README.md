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

The SDK supports remote Maven dependencies and local source code dependencies. For more information on how to connect, please see [SDK Connection Description](docs/en/PRELIM__SDK接入说明_EN-US.md).

## Downloading the sample code of IoT Hub SDK for Java demo
Download the complete code in the [repository](https://github.com/tencentyun/iot-device-java). The sample code of the IoT Hub SDK for Java demo is in the [hub-device-java](../hub-device-java/src/test) module.


## Feature documentation
For more information on how to call the APIs, please see the demos of the following corresponding features.

* [Device Connection Through MQTT over TCP](docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md)
* [Device Connection Through MQTT over WebSocket](docs/en/PRELIM__基于Websocket的MQTT设备接入_EN-US.md)
* [Dynamic Registration](docs/en/PRELIM__动态注册_EN-US.md)
* [RRPC Sync Communication](docs/en/PRELIM__RRPC同步通信_EN-US.md)
* [Broadcast Communication](docs/en/PRELIM__广播通信_EN-US.md)
* [Gateway Feature](docs/en/PRELIM__网关功能_EN-US.md)
* [Firmware Update](docs/en/PRELIM__固件升级_EN-US.md)
* [Gateway Subdevice Firmware Update](docs/en/PRELIM__网关子设备固件升级_EN-US.md)
* [Device Log Reporting](docs/en/PRELIM__设备日志上报_EN-US.md)
* [Gateway Device Topological Relationship](docs/en/PRELIM__网关设备拓扑关系_EN-US.md)
* [Device Interconnection](docs/en/PRELIM__设备互通_EN-US.md)
* [Device Shadow](docs/en/PRELIM__设备影子_EN-US.md)
* [Self-Built Server Connection](docs/en/PRELIM__自建服务器接入_EN-US.md)
* [Device Status Reporting and Setting](docs/en/PRELIM__设备状态上报与状态设置_EN-US.md)

## SDK API and parameter descriptions
For the SDK API and parameter descriptions, please see [SDK API and Parameter Descriptions](docs/en/PRELIM__SDK%20API及参数说明_EN-US.md) in the `docs` directory.

## FAQs

For the FAQs, please see [FAQs for Java](docs/en/PRELIM__常见问题java_EN-US.md) in the `docs` directory.
