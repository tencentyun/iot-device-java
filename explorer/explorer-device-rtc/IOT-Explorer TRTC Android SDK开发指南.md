* [IoT Explorer TRTC Android SDK开发指南](#IoT-Explorer-TRTC-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [explorer-device-trtc SDK 设计说明](#explorer-device-trtc-SDK-设计说明)
     *  [explorer-device-trtc SDK 回调callback 设计说明](#explorer-device-trtc-SDK-回调callback-设计说明)

# IoT Explorer TRTC Android SDK开发指南

本文主要描述物联网开发平台设备端IoT Explorer Android-SDK中接入实时音视频 TRTC Android-SDK 开发指南 。

## 引用方式

- 集成 SDK 方式

如果不需要接入实时音视频 TRTC SDK，仅需要接入explorer-device-android SDK，请参考 [编译环境及SDK接入说明.md](../explorer-device-android/docs/编译环境及SDK接入说明.md)

 -  gradle 工程 正式版SDK 远程构建

    在应用模块的build.gradle中配置
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-trtc:3.2.1' //IoT Explorer 与 实时音视频 TRTC 的依赖
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
        implementation 'com.tencent.iot.explorer:explorer-device-trtc:3.2.1-SNAPSHOT' //IoT Explorer 与 实时音视频 TRTC 的依赖
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-trtc](../explorer-device-rtc)源码，示例如下：
    
     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-trtc') //IoT Explorer 与 实时音视频 TRTC 的依赖
    }
     ```

Demo示例工程使用的是 依赖本地 explorer-device-android 和 explorer-device-trtc 的 sdk源码 构建方式。

## API说明

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/SDK%20API及参数说明.md)

### explorer-device-trtc SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| TXTRTCDataTemplate      | 实现人脸识别数据模板基本功能                     |
| TXTRTCTemplateClient    | 实现直连设备根据人脸识别数据模板连接物联网开发平台  |

#### TXTRTCDataTemplate

TXTRTCDataTemplate 继承自 TXDataTemplate类

| 方法名                         | 说明                                            |
| ----------------------------- | ---------------------------------------------- |
| reportCallStatusProperty      | 上报TRTC音频视频呼叫状态                          |
| sysPropertyReport             | 系统单个属性上报， 不检查构造是否符合json文件中的定义 |


#### TXTRTCTemplateClient

| 方法名                         | 说明                                  |
| ----------------------------- | ------------------------------------ |
| isConnected                   | 是否已经连接物联网开发平台               |
| generalDeviceQRCodeContent    | 生成绑定设备的二维码字符串               |
| subscribeTemplateTopic        | 订阅数据模板相关主题                    |
| unSubscribeTemplateTopic      | 取消订阅数据模板相关主题                |
| propertyReport                | 上报属性                              |
| reportCallStatusProperty      | 上报实时音视频类设备呼叫属性             |
| propertyGetStatus             | 更新状态                              |
| propertyReportInfo            | 上报设备信息                           |
| propertyClearControl          | 清除控制信息                           |
| eventSinglePost               | 上报单个事件                           |
| eventsPost                    | 上报多个事件                           |

### explorer-device-trtc SDK 回调callback 设计说明

TXTRTCCallBack 授权回调callback说明如下：

```
    /**
     * 获取TRTC属性呼叫状态
     *
     * @param callStatus            呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param userid                用户id
     * @param callType              邀请类型 1-语音通话，2-视频通话
     */
    public abstract void onGetCallStatusCallBack(Integer callStatus, String userid, Integer callType);

    /**
     * 获取trtc进入房间所需参数模型
     *
     * @param room
     */
    public abstract void trtcJoinRoomCallBack(RoomKey room);
```
