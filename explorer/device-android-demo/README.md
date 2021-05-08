## 概述
该演示Demo有三个演示入口，其中`IOT DEMO`主要演示[Explorer Device Android SDK](https://github.com/tencentyun/iot-device-java/tree/master/explorer/explorer-device-android) 基础功能和个别使用场景; `IOT AI FACE Demo`
演示了[Explorer Device Face SDK](https://github.com/tencentyun/iot-device-java/tree/master/explorer/explorer-device-face)的简单应用; `IOT TRTC DEMO`演示[Explorer Device RTC SDK](https://github.com/tencentyun/iot-device-java/tree/master/explorer/explorer-device-rtc)的简单应用.
## Demo入口示意图
```
├── Demo
│   ├── IOT DEMO
│   ├── IOT AI FACE DEMO
│   ├── IOT TRTC DEMO
```

## IOT Demo
该演示Demo通过`数据模板` `示例` `网关示例` 三个Tab页演示了Explorer Device Android SDK的基础功能。
### 数据模板
该页面从上至下包含三部分：`参数设置区`、`功能操作区`、`日志输出区`
1. 参数设置区
    * 包含下拉选择参数key和参数value输入框(可以不在此处设置)，在[app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/explorer/device-android-demo/src/main/assets/app-config.json)中设置即可
2. 功能操作区
    * 包含设备上下线、Topic相关(订阅主题/取消订阅主题)、属性上报、状态更新、设备信息上报、清除控制、事件上报、检查固件更新等操作
    * 注意：在操作功能区其他功能的前提是点击`设备上线`且日志输出区打印出`onConnectComplete，status[ok]`字样
3. 日志输出区
    * 功能区的操作在功能区会有对应的日志输出，比如点击了`订阅主题`，日志输出区会打印`onSubscribeCompleted`字样

### 示例
该页面从上至下包含两部分：`功能操作区`、`日志输出区`
1. 功能操作区
    * 包含设备上下线和检查固件更新操作
    * 设备上线后，可以通过[控制台](https://console.cloud.tencent.com/iotexplorer)更改该设备的属性值，比如灯的亮度、颜色、开关，该Demo会实时显示更新后的属性值。

2. 日志输出区
    * 功能区的操作在功能区会有对应的日志输出，比如点击了`设备上线`，日志输出区的Status字段的值会更新为online。

### 网关示例
该页面主要演示在网关设备下添加删除子设备、子设备的上下线以及网关设备本身的上下线功能；该页面从上至下包含两部分：`功能操作区`、`日志输出区`。
1. 功能操作区
    * 包含在网关设备下添加删除子设备、网关上下线和添加子设备以及子设备上下线等操作
    * 注意：点击上下线的前提是已经点击了`添加智能灯`或者`添加空调`

2. 日志输出区
    * 功能区的操作在功能区会有对应的日志输出，比如点击了`上线`，日志输出区的Status字段的值会更新为online。


## IOT AI FACE DEMO
待补充
## IOT TRTC DEMO
该页面主要包含两部分：`设备二维码生成区`、`音视频通话区`，主要演示App与trtc设备间的音视频通话场景
1、设备二维码生成区
    * 填写trtc设备三元组信息（产品ID、设备名称、设备密钥）
    * 点击设备上线即可生成trtc设备的二维码，可通过`腾讯连连App`扫描该二维码进行设备绑定
2、音视频通话区
    * 可以点击`音频呼叫`或`视频呼叫`按钮进行设备与App间通话，前提是App已经绑定了该设备

