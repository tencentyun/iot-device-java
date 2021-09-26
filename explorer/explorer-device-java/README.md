* [腾讯云物联网开发平台设备端 IoT Explorer Java-SDK](#腾讯云物联网开发平台设备端-IoT-Explorer-Java-SDK)
  * [前提条件](#前提条件)
  * [工程配置](#工程配置)
  * [下载IoT Explorer Java-SDK Demo示例代码](#下载IoT-Explorer-Java-SDK-Demo示例代码)
  * [功能文档](#功能文档)
  * [SDK API 说明](#SDK-API-说明)

# 腾讯云物联网开发平台设备端 IoT Explorer Java-SDK
欢迎使用腾讯云物联网开发平台设备端 IoT Explorer Java-SDK 。

腾讯云物联网开发平台设备端IoT Explorer Java-SDK， 配合平台对设备数据模板化进行定义，基于数据模板协议实现设备和云端的数据交互框架。开发者基于IoT Explorer Java-SDK数据模板框架，可快速实现设备和平台、设备和应用之间的数据交互。此文档将介绍如何获取 IoT Explorer Java-SDK 并开始调用。 如果您在使用 IoT Explorer Java-SDK 的过程中遇到任何问题，[欢迎在当前 GitHub 提交 Issues](https://github.com/tencentyun/iot-device-java/issues/new)。

## 前提条件
* 您需要创建一个腾讯云账号，在腾讯云控制台中开通物联网开发平台产品。
* 在控制台上创建项目产品设备，获取产品ID、设备名称、设备证书（证书认证）、设备私钥（证书认证）、设备密钥（密钥认证），设备与云端认证连接时需要用到以上信息。具体步骤请参考官网 [用户指南-项目管理](https://cloud.tencent.com/document/product/1081/40290)、 [用户指南-产品定义](https://cloud.tencent.com/document/product/1081/34739)、 [用户指南-设备调试](https://cloud.tencent.com/document/product/1081/34741)。

## 工程配置
SDK支持远程maven依赖，以及本地源码依赖，详细接入步骤请参考 [SDK接入说明](docs/zh/SDK接入说明.md)

## 下载IoT Explorer Java-SDK Demo示例代码
下载[仓库](../..)下完整代码，IoT Explorer Java-SDK Demo示例代码在 [explorer-device-java/src/test/java](../explorer-device-java/src/test/java) 目录下。

## 功能文档
调用API接口可以参考以下对应功能示例Demo的使用。

* [控制设备上下线](docs/zh/控制设备上下线.md)
* [动态注册](docs/zh/动态注册.md)
* [订阅与取消订阅 Topic 主题](docs/zh/订阅与取消订阅%20Topic%20主题.md)
* [属性上报](docs/zh/属性上报.md)
* [获取设备最新上报信息](docs/zh/获取设备最新上报信息.md)
* [设备信息上报](docs/zh/设备信息上报.md)
* [清除控制](docs/zh/清除控制.md)
* [事件上报以及多事件上报](docs/zh/事件上报以及多事件上报.md)
* [检查固件更新](docs/zh/检查固件更新.md)
* [网关使用示例](docs/zh/网关使用示例.md)

## SDK API及参数说明
SDK API接口及参数说明请参考docs目录下的[SDK API及参数说明](docs/zh/SDK%20API及参数说明.md)

