* [腾讯云物联网开发平台 Android-SDK](#腾讯云物联网开发平台-Android-SDK)
  * [SDK接入指南](#SDK接入指南)
  * [控制设备上下线](#控制设备上下线)
  * [订阅与取消订阅 Topic 主题](#订阅与取消订阅-Topic-主题)
  * [属性上报](#属性上报)
  * [获取设备最新上报信息](#获取设备最新上报信息)
  * [设备信息上报](#设备信息上报)
  * [清除控制](#清除控制)
  * [事件上报以及多事件上报](#事件上报以及多事件上报)
  * [检查固件更新](#检查固件更新)
  * [网关使用示例](#网关使用示例)
  * [SDK API及参数说明](#SDK-API及参数说明)

# 腾讯云物联网开发平台 Android-SDK
腾讯云物联网开发平台 Android-SDK 配合平台对设备数据模板化的定义，实现和云端基于数据模板协议的数据交互框架，开发者基于IoT_Explorer Android SDK数据模板框架，快速实现设备和平台、设备和应用之间的数据交互。

## SDK接入指南
SDK支持远程maven依赖，以及本地源码依赖，详细接入步骤请参考 [编译环境及SDK接入说明](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/编译环境及SDK接入说明.md)

## 控制设备上下线
请参考 [控制设备上下线.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/控制设备上下线.md) 文档，介绍如何在腾讯云物联网通信开发平台控制台创建设备, 并结合 SDK Demo 快速体验设备端通过 MQTT 协议连接到腾讯云端，和断开 MQTT 连接使设备下线。

## 订阅与取消订阅 Topic 主题
请参考 [订阅与取消订阅 Topic 主题.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/订阅与取消订阅%20Topic%20主题.md) 文档，订阅表示设备可以订阅 Topic 主题以获取消息，订阅和取消订阅指令都需由设备发起。设备发送订阅某个Topic后，该订阅永久生效；仅在设备发起取消订阅该Topic后，订阅才会被取消。

## 属性上报
请参考 [属性上报.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/属性上报.md) 文档，介绍设备端将定义的属性根据设备端的业务逻辑向云端上报。

## 获取设备最新上报信息
请参考 [获取设备最新上报信息.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/获取设备最新上报信息.md) 文档，设备从云端接收最新消息，获取设备最新上报的信息。

## 设备信息上报
请参考 [设备信息上报.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/设备信息上报.md) 文档，进行设备基本信息的上报。

## 清除控制
请参考 [清除控制.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/清除控制.md) 文档，删除数据模板属性控制消息。

## 事件上报以及多事件上报
请参考 [事件上报以及多事件上报.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/事件上报以及多事件上报.md) 文档，介绍单个事件上报以及多事件上报的使用。

## 检查固件更新
请参考 [检查固件更新.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/检查固件更新.md) 文档，介绍固件升级功能，并结合 SDK Demo 展示固件升级的流程和功能。

## 网关使用示例
请参考 [网关使用示例.md](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/网关使用示例.md) 文档，介绍在物联网开发平台控制台申请网关设备并绑定子设备, 并结合 SDK Demo 快速体验网关设备通过 MQTT 协议连接到云端, 代理子设备上下线，发送和接收消息。

## SDK API及参数说明
SDK API接口及参数说明请参考docs目录下的[SDK API及参数说明](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/docs/SDK%20API及参数说明.md)
