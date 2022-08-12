简体中文 | [English](docs/en)

* [腾讯云物联网通信设备端 IoT Hub Java-SDK](#腾讯云物联网通信设备端-IoT-Hub-Java-SDK)
  * [前提条件](#前提条件)
  * [工程配置](#工程配置)
  * [下载IoT Hub Java-SDK Demo示例代码](#下载IoT-Hub-Java-SDK-Demo示例代码)
  * [功能文档](#功能文档)
  * [SDK API及参数说明](#SDK-API及参数说明)
  * [常见问题](#常见问题)

# 腾讯云物联网通信设备端 IoT Hub Java-SDK
欢迎使用腾讯云物联网通信设备端 IoT Hub Java-SDK 。

腾讯云物联网通信设备端 IoT Hub Java-SDK， 依靠安全且性能强大的数据通道，为物联网领域开发人员提供终端(如传感器, 执行器, 嵌入式设备或智能家电等等)和云端的双向通信能力。此文档将介绍如何获取 IoT Hub Java-SDK 并开始调用。 如果您在使用 IoT Hub Java-SDK 的过程中遇到任何问题，[欢迎在当前 GitHub 提交 Issues](https://github.com/tencentyun/iot-device-java/issues/new)。

## 前提条件
* 您需要创建一个腾讯云账号，在腾讯云控制台中开通物联网通信产品。
* 在控制台上创建物联网产品和设备，获取产品ID、设备名称、设备证书（证书认证）、设备私钥（证书认证）、设备密钥（密钥认证），设备与云端认证连接时需要用到以上信息。具体步骤请参考官网 [控制台使用手册-设备接入准备](https://cloud.tencent.com/document/product/634/14442)。
* 理解Topic权限，当在控制台中成功创建产品后，该产品默认有三条权限。订阅：${productId}/${deviceName}/control，订阅和发布：${productId}/${deviceName}/data，发布：${productId}/${deviceName}/event。请参考官网 [控制台使用手册-权限列表](https://cloud.tencent.com/document/product/634/14444) 操作Topic权限。

## 工程配置

SDK支持远程maven依赖，以及本地源码依赖，详细接入步骤请参考 [SDK接入说明](docs/zh/SDK接入说明.md)

## 下载IoT Hub Java-SDK Demo示例代码
下载[仓库](https://github.com/tencentyun/iot-device-java)下完整代码，IoT Hub Java-SDK Demo示例代码在 [hub-device-java](../hub-device-java/src/test) 的module下。


## 功能文档
调用API接口可以参考以下对应功能示例Demo的使用。

* [基于TCP的MQTT设备接入](docs/zh/基于TCP的MQTT设备接入.md)
* [基于Websocket的MQTT设备接入](docs/zh/基于Websocket的MQTT设备接入.md)
* [动态注册](docs/zh/动态注册.md)
* [RRPC同步通信](docs/zh/RRPC同步通信.md)
* [广播通信](docs/zh/广播通信.md)
* [网关功能](docs/zh/网关功能.md)
* [固件升级](docs/zh/固件升级.md)
* [网关子设备固件升级](docs/zh/网关子设备固件升级.md)
* [设备日志上报](docs/zh/设备日志上报.md)
* [网关设备拓扑关系](docs/zh/网关设备拓扑关系.md)
* [远程登录](docs/zh/远程登录.md)
* [设备互通](docs/zh/设备互通.md)
* [设备影子](docs/zh/设备影子.md)
* [自建服务器接入](docs/zh/自建服务器接入.md)
* [设备状态上报与状态设置](docs/zh/设备状态上报与状态设置.md)

## SDK API及参数说明
SDK API接口及参数说明请参考docs目录下的[SDK API及参数说明](docs/zh/SDK%20API及参数说明.md)

## 常见问题

常见问题请参考docs目录下的[常见问题java](docs/zh/常见问题java.md)
