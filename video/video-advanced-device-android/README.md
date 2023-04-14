简体中文 | [English](docs/en)

* [IoT Video Advanced Device SDK开发指南](#IoT-Video-Advanced-Device-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [iot-video-advanced-device-android SDK 设计说明](#iot-video-advanced-device-android-SDK-设计说明)
     *  [iot-video-advanced-device-android SDK 回调callback 设计说明](#iot-video-advanced-device-android-SDK-回调callback-设计说明)

# IoT Video Advanced Device SDK开发指南

本文主要介绍腾讯云物联网智能视频服务（消费版）设备端IoT Video Advanced Device Android SDK的开发指南 。

## 引用方式

1、集成 SDK 方式
 - 依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](/build.gradle)**，使应用模块依赖 [video-advanced-device-android]()源码，示例如下：

    ```gr
    dependencies {
        implementation project(':video:video-advanced-device-android')
    }
    ```
    注：Demo示例工程使用的是 依赖本地 video-advanced-device-android 的 sdk源码 构建方式。

 -  gradle工程集成正式版SDK
     在module目录下的build.gradle中添加如下依赖，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本：
     ```
     dependencies {
         ...
         implementation 'com.tencent.iot.video:video-advanced-device-android:x.x.x'
     }
     ```

 -  gradle工程集成snapshot版SDK

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

     在应用模块的build.gradle中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本，末位+1
     ``` gr
     dependencies {
         implementation 'com.tencent.iot.video:video-advanced-device-android:x.x.x-SNAPSHOT'
     }
     ```


## API说明

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../../explorer/explorer-device-android/docs/zh/SDK%20API及参数说明.md)

### iot-device-video-advanced SDK 设计说明

#### com.tencent.iot.device.video.advanced.recorder.TXVideoTemplateClient

1、呼叫某个设备 TXVideoTemplateClient#callOtherDevice(String calledProductID, String calledDeviceName)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| calledProductID | String | 呼叫设备的产品ID |
| calledDeviceName | String | 呼叫设备的房间名称 |

#### com.tencent.iot.device.video.advanced.recorder.TXVideoCallBack

TXVideoTemplateClient初始化中TXVideoCallBack 回调callback主要接口说明如下：

| 回调接口 | 功能 |
| ----------------------- | ---------- |
| receiveRtcJoinRoomAction(RoomKey room) | 收到其他app用户或设备呼叫的rtc链接房间action以及所需参数， room 是 VideoNativeInteface中initWithDevice所需链接房间参数 |
| callOtherDeviceSuccess(RoomKey room) | 呼叫其他设备成功得到rtc链接房间action以及所需参数， room 是 VideoNativeInteface中initWithDevice所需链接房间参数 |
| callOtherDeviceFailed(int code, String reason) | 呼叫其他设备失败， code 是 错误码， reason是失败原因 |



#### com.tencent.iot.device.video.advanced.recorder.rtc.VideoNativeInteface

1、初始化 VideoNativeInteface#initWithDevice(Context context)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| context | Context | 上下文 |

| 返回值 | 描述 |
|:-|:-|
| VideoNativeInteface | VideoNativeInteface实例 |

2、开始进房 VideoNativeInteface#enterRoom(RoomKey roomKey)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| roomKey | RoomKey | 链接房间参数 |

3、设置回调 VideoNativeInteface#setCallback(XP2PCallback callback)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| callback | XP2PCallback | 回调 |

4、释放链接 VideoNativeInteface#release()

5、发送信令 VideoNativeInteface#sendMsgToPeer(String msg)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| msg | String | 信令消息 |

| 返回值 | 描述 |
|:-|:-|
| boolean | 发送是否成功 |

6、打开摄像头预览 VideoNativeInteface#openCamera(boolean isFrontCamera, TXCloudVideoView txCloudVideoView)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| isFrontCamera | boolean | 是否是前置摄像头 |
| txCloudVideoView | TXCloudVideoView | 承载视频画面的控件 |

7、开始推流 VideoNativeInteface#sendStreamToServer()

8、绑定远端视频渲染控件 VideoNativeInteface#startRemoteView(String userId, TXCloudVideoView txCloudVideoView)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| userId | String | 远端用户id |
| txCloudVideoView | TXCloudVideoView | 承载视频画面的控件 |

9、切换摄像头 VideoNativeInteface#switchCamera(boolean isFrontCamera)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| isFrontCamera | boolean | 是否是前置摄像头 |

10、设置麦克风是否静音 VideoNativeInteface#setMicMute(boolean isMute)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| isMute | boolean | 是否静音 |

11、设置是否免提 VideoNativeInteface#setHandsFree(boolean isHandsFree)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| isHandsFree | boolean | 是否免提 |

12、关闭摄像头预览 VideoNativeInteface#closeCamera()


### iot-device-video-advanced SDK 回调callback 设计说明

com.tencent.iot.device.video.advanced.recorder.rtc.XP2PCallback 回调callback说明如下：

| 回调接口 | 功能 |
| ----------------------- | ---------- |
| onError(int code, String msg) | sdk内部发生了错误， code 错误码， msg 错误消息 |
| onConnect(long result)  | 链接成功与否的事件回调， result 如果加入成功，回调 result 会是一个正数（result > 0），代表链接所消耗的时间，单位是毫秒（ms），如果链接失败，回调 result 会是一个负数（result < 0），代表失败原因的错误码。|
| onRelease(int reason) | 释放链接的事件回调， reason 释放链接的原因，0：主动调用 release 释放链接；1、2：被服务器释放链接；|
| onUserEnter(String rtc_uid) | 如果有用户同意进入通话，那么会收到此回调， rtc_uid 进入通话的用户 |
| onUserLeave(String rtc_uid) | 如果有用户同意离开通话，那么会收到此回调， rtc_uid 离开通话的用户 |
| onUserVideoAvailable(String rtc_uid, boolean isVideoAvailable) | 远端用户开启/关闭了摄像头， rtc_uid 远端用户ID，isVideoAvailable true:远端用户打开摄像头  false:远端用户关闭摄像头 |
| onUserVoiceVolume(Map<String, Integer> volumeMap) | 用户说话音量回调， volumeMap 音量表，根据每个userid可以获取对应的音量大小，音量最小值0，音量最大值100 |
| onRecvCustomCmdMsg(String rtc_uid, String message) | 收到自定义消息的事件回调， rtc_uid 用户标识，message 消息数据 |
| onFirstVideoFrame(String rtc_uid, int streamType, int width, int height) | SDK 开始渲染自己本地或远端用户的首帧画面， rtc_uid 用户标识，width 画面的宽度，height 画面的高度 |
