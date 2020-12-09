* [IoT Explorer人脸识别 Android SDK开发指南](#IoT-Explorer人脸识别-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)

# IoT Explorer人脸识别 Android SDK开发指南

本文主要描述物联网开发平台设备端IoT Explorer Android-SDK中接入人脸识别离线 Android-SDK 开发指南 。

## 引用方式

- 集成 SDK 方式
 -  gradle 工程 正式版SDK 远程构建

    在应用模块的build.gradle中配置（如果不需要接入人脸识别离线SDK，请参考 [编译环境及SDK接入说明.md](#https://github.com/tencentyun/iot-device-java/edit/master/explorer-device-android/docs/编译环境及SDK接入说明.md)）
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-android:3.2.0' //IoT Explorer Android-SDK的依赖
        implementation 'com.tencent.iot.explorer:explorer-device-android-face:1.0.0' //IoT Explorer 与 人脸识别离线交互 的依赖
        implementation 'com.tencent.iot.thirdparty.android:ai-face-sdk:6.0.0.140' //人脸识别离线 Android-SDK的依赖
    }
    ```
 -  gradle 工程 snapshot版SDK 远程构建

    > 建议使用正式版SDK，SNAPSHOT版本会静默更新，使用存在风险

    在工程的build.gradle中配置仓库url
    ``` gr
    allprojects {
        repositories {
            google()
            jcenter()
            maven {
                url "https://oss.sonatype.org/content/repositories/snapshots"
            }
        }
    }
    ```
    在应用模块的build.gradle中配置
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-android:3.2.1-SNAPSHOT' //IoT Explorer Android-SDK的依赖
        implementation 'com.tencent.iot.explorer:explorer-device-android-face:1.0.1-SNAPSHOT' //IoT Explorer 与 人脸识别离线交互 的依赖
        implementation 'com.tencent.iot.thirdparty.android:ai-face-sdk:6.0.0.141-SNAPSHOT' //人脸识别离线 Android-SDK的依赖
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/explorer-demo/build.gradle)**，使应用模块依赖 [iot_explorer](https://github.com/tencentyun/iot-device-java/tree/master/explorer-device-android/iot_explorer)源码，示例如下：
     ```gr
    dependencies {
        implementation project(':explorer-device-android:iot_explorer') //IoT Explorer Android-SDK的依赖
        implementation project(':explorer-device-android:iot_face') //IoT Explorer 与 人脸识别离线交互 的依赖
        implementation 'com.tencent.iot.thirdparty.android:ai-face-sdk:6.0.0.140' //人脸识别离线 Android-SDK的依赖
    }
     ```

Demo示例工程使用的是 依赖本地 iot_explorer 和 iot_face 的 sdk源码 构建方式。

## API说明

### iot_explorer SDK 设计说明

iot_explorer 请参考 [SDK API及参数说明.md](https://github.com/tencentyun/iot-device-java/edit/master/explorer-device-android/docs/SDK%20API及参数说明.md)

### iot_face SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| TXFaceKitTemplate       | 实现人脸识别数据模板基本功能                     |
| TXFaceKitTemplateClient | 实现直连设备根据人脸识别数据模板连接物联网开发平台  |
| TXResourceImpl          | 根据物联网开发平台人脸库数据实现下载资源到设备中    |

#### TXFaceKitTemplate

TXFaceKitTemplate 继承自 TXDataTemplate类

| 方法名                         | 说明                     |
| ----------------------------- | ------------------------ |
| initResource                  | 初始化人员库资源下载功能     |
| sysEventSinglePost            | 系统单个事件上报， 不检查构造是否符合json文件中的定义 |
| reportCurrentResourceVersion  | 上报设备当前人员库资源版本信息到后台服务器。  |
| reportResourceState           | 上报设备人员库资源下载状态到后台服务器。     |
| subscribeServiceTopic         | 订阅Service主题             |
| unSubscribeServiceTopic       | 取消订阅Service主题             |
| initAuth                      | 初始化 离线人脸识别SDK 授权             |


#### TXDataTemplateClient

| 方法名                   | 说明                       |
| ------------------------ | -------------------------- |
| isConnected              | 是否已经连接物联网开发平台 |
| subscribeTemplateTopic   | 订阅数据模板相关主题       |
| subscribeServiceTopic    | 订阅Service主题          |
| unSubscribeTemplateTopic | 取消订阅数据模板相关主题   |
| unSubscribeServiceTopic  | 取消订阅Service主题       |
| propertyReport           | 上报属性                   |
| propertyGetStatus        | 更新状态                   |
| propertyReportInfo       | 上报设备信息               |
| propertyClearControl     | 清除控制信息               |
| eventSinglePost          | 上报单个事件               |
| reportSysRetrievalResultEvent | 检索人脸事件上报       |
| eventsPost               | 上报多个事件               |
| initAuth                 | 初始化 离线人脸识别SDK 授权             |
| initResource             | 初始化人员库资源下载功能     |
| reportCurrentResourceVersion  | 上报设备当前人员库资源版本信息到后台服务器。  |