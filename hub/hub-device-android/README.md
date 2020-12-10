* [腾讯云物联网通信设备端 IoT Hub Android-SDK](#腾讯云物联网通信设备端-IoT-Hub-Android-SDK)
  * [前提条件](#前提条件)
  * [SDK接入指南](#SDK接入指南)
  * [下载IoT Hub Android-SDK Demo示例代码](#下载IoT-Hub-Android-SDK-Demo示例代码)
  * [功能文档](#功能文档)
  * [SDK API及参数说明](#SDK-API及参数说明)
  * [常见问题](#常见问题)

# 腾讯云物联网通信设备端 IoT Hub Android-SDK
欢迎使用腾讯云物联网通信设备端 IoT Hub Android-SDK 。

腾讯云物联网通信设备端 IoT Hub Android-SDK， 依靠安全且性能强大的数据通道，为物联网领域开发人员提供终端(如传感器, 执行器, 嵌入式设备或智能家电等等)和云端的双向通信能力。此文档将介绍如何获取 IoT Hub Android-SDK 并开始调用。 如果您在使用 IoT Hub Android-SDK 的过程中遇到任何问题，[欢迎在当前 GitHub 提交 Issues](https://github.com/tencentyun/iot-device-java/issues/new)。

## 前提条件
* 您需要创建一个腾讯云账号，在腾讯云控制台中开通物联网通信产品。
* 在控制台上创建物联网产品和设备，具体步骤请参考官网 [控制台使用手册-设备接入准备](https://cloud.tencent.com/document/product/634/14442)。

## SDK接入指南
SDK支持远程maven依赖，以及本地源码依赖，详细接入步骤请参考 [编译环境及SDK接入说明](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/编译环境及SDK接入说明.md)

## 下载IoT Hub Android-SDK Demo示例代码
下载[仓库](https://github.com/tencentyun/iot-device-java)下完整代码，IoT Hub Android-SDK Demo示例代码在 [hub-demo](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-android/hub-demo) 的module下。

[Demo apk安装包下载](https://github.com/tencentyun/iot-device-android/wiki/下载安装)

## 功能文档
调用API接口可以参考以下对应功能示例Demo的使用。

* [基于TCP的MQTT设备接入](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/基于TCP的MQTT设备接入.md)
* [基于Websocket的MQTT设备接入](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/基于Websocket的MQTT设备接入.md)
* [动态注册](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/动态注册.md)
* [RRPC同步通信](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/RRPC同步通信.md)
* [广播通信](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/广播通信.md)
* [网关功能](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/网关功能.md)
* [固件升级](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/固件升级.md)
* [网关子设备固件升级](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/网关子设备固件升级.md)
* [设备日志上报](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/设备日志上报.md)
* [网关设备拓扑关系](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/网关设备拓扑关系.md)
* [设备互通](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/设备互通.md)
* [设备影子](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/设备影子.md)
* [自建服务器接入](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/docs/自建服务器接入.md)

## SDK API及参数说明
SDK API接口及参数说明请参考docs目录下的[SDK API及参数说明](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/SDK%20API及参数说明.md)

## 常见问题
常见问题请参考docs目录下的[常见问题android](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/常见问题android.md)
