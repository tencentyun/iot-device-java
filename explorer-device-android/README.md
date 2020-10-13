* [腾讯云物联网开发平台设备端 IoT Explorer Android-SDK](#腾讯云物联网开发平台设备端-IoT-Explorer-Android-SDK)
  * [前提条件](#前提条件)
  * [SDK接入指南](#SDK接入指南)
  * [下载IoT Explorer Android-SDK Demo示例代码](#下载IoT-Explorer-Android-SDK-Demo示例代码)
  * [功能文档](#功能文档)
  * [SDK API及参数说明](#SDK-API及参数说明)

# 腾讯云物联网开发平台设备端 IoT Explorer Android-SDK
欢迎使用腾讯云物联网开发平台设备端 IoT Explorer Android-SDK 。

腾讯云物联网开发平台设备端 IoT Explorer Android-SDK， 配合平台对设备数据模板化的定义，实现和云端基于数据模板协议的数据交互框架，开发者基于IoT Explorer Android-SDK数据模板框架，快速实现设备和平台、设备和应用之间的数据交互。此文档将介绍如何获取 IoT Explorer Android-SDK 并开始调用。 如果您在使用 IoT Explorer Android-SDK 的过程中遇到任何问题，[欢迎在当前 GitHub 提交 Issues](https://github.com/tencentyun/iot-device-java/issues/new)。

## 前提条件
* 您需要创建一个腾讯云账号，在腾讯云控制台中开通物联网开发平台产品。
* 在控制台上创建项目产品设备，具体步骤请参考官网 [用户指南-项目管理](https://cloud.tencent.com/document/product/1081/40290)、 [用户指南-产品定义](https://cloud.tencent.com/document/product/1081/34739)、 [用户指南-设备调试](https://cloud.tencent.com/document/product/1081/34741)。

## SDK接入指南
SDK支持远程maven依赖，以及本地源码依赖，详细接入步骤请参考 [编译环境及SDK接入说明](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/编译环境及SDK接入说明.md)

## 下载IoT Explorer Android-SDK Demo示例代码
下载[仓库](https://github.com/tencentyun/iot-device-java)下完整代码，IoT Explorer Android-SDK Demo示例代码在 [explorer-demo](https://github.com/tencentyun/iot-device-java/tree/master/explorer-device-android/explorer-demo) 的module下。

[Demo apk安装包下载](https://github.com/tencentyun/iot-device-android/wiki/下载安装)

## 功能文档
调用API接口可以参考以下对应功能示例Demo的使用。

* [控制设备上下线](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/控制设备上下线.md)
* [订阅与取消订阅 Topic 主题](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/订阅与取消订阅%20Topic%20主题.md)
* [属性上报](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/属性上报.md)
* [获取设备最新上报信息](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/获取设备最新上报信息.md)
* [设备信息上报](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/设备信息上报.md)
* [清除控制](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/清除控制.md)
* [事件上报以及多事件上报](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/事件上报以及多事件上报.md)
* [检查固件更新](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/检查固件更新.md)
* [网关使用示例](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/网关使用示例.md)

## SDK API及参数说明
SDK API接口及参数说明请参考docs目录下的[SDK API及参数说明](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/SDK%20API及参数说明.md)
