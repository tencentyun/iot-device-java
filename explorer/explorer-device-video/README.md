简体中文 | [English](docs/en)

* [IoT Explorer Video Android SDK开发指南](#IoT-Explorer-Video-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [explorer-device-video SDK 设计说明](#explorer-device-video-SDK-设计说明)
     *  [explorer-device-video SDK 回调callback 设计说明](#explorer-device-video-SDK-回调callback-设计说明)
     *  [错误码](#错误码)
  * [设备与App交互流程](#设备与App交互流程])

# IoT Explorer Video Android SDK开发指南

本文主要介绍腾讯云物联网开发平台设备端IoT Explorer Video Android SDK的开发指南 。

## 引用方式

1、集成 SDK 方式
 - 依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-video](../explorer-device-video)源码，示例如下：

    ```gr
    dependencies {
        implementation project(':explorer:explorer-device-video')
    }
    ```
    注：Demo示例工程使用的是 依赖本地 explorer-device-video 的 sdk源码 构建方式。

 -  gradle工程集成正式版SDK
     在module目录下的build.gradle中添加如下依赖，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本：
     ```
     dependencies {
         ...
         implementation 'com.tencent.iot.explorer:explorer-device-video:x.x.x'
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
         implementation 'com.tencent.iot.explorer:explorer-device-video:x.x.x-SNAPSHOT'
     }
     ```


## API说明

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/zh/SDK%20API及参数说明.md)

### explorer-device-video SDK 设计说明

#### VideoNativeInteface

1、使用设备三元组初始化 VideoNativeInteface#initWithDevice(String productid, String devicename, String devicekey)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| productid | String | 产品ID |
| devicename| String | 设备名称 |
| devicekey | String | 设备密钥 |

| 返回值 | 描述 |
|:-|:-|
| int | 错误码 |

2、发送音频数据 VideoNativeInteface#sendAudioData(byte[] dataBytes, long pts, long seq, int visitor)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| dataBytes | byte[] | 音频数据 |
| pts| long | pts |
| seq | long | 音频数据包的序号 |
| visitor | int | 第几路观看者 |

| 返回值 | 描述 |
|:-|:-|
| int | 错误码 |

3、发送视频数据 VideoNativeInteface#sendFrameData(byte[] dataBytes, long pts, long seq, int visitor)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| dataBytes | byte[] | 视频数据 |
| pts| long | pts |
| seq | long | 视频数据包的序号 |
| visitor | int | 第几路观看者 |

| 返回值 | 描述 |
|:-|:-|
| int | 错误码 |

4、获取本端的xp2p info VideoNativeInteface#getXp2pInfo()

| 返回值 | 描述 |
|:-|:-|
| String | xp2p info |

5、释放资源 VideoNativeInteface#release()

| 返回值 | 描述 |
|:-|:-|
| int | 错误码 |

6、设置回调 VideoNativeInteface#setCallback(XP2PCallback callback)

7、发送信令 VideoNativeInteface#sendMsgToPeer(int visitor, String msg, int timeoutMills)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| visitor | int | 第几路观看者 |
| msg | String | 信令消息 |
| timeoutMills | int | 超时时间（毫秒） |

| 返回值 | 描述 |
|:-|:-|
| int | 错误码 0为成功 |

### explorer-device-video SDK 回调callback 设计说明

XP2PCallback 回调callback说明如下：

| 回调接口 | 功能 |
| ----------------------- | ---------- |
| avDataRecvHandle(byte[] data, int len) | 收到对端数据的回调接口 |
| avDataMsgHandle(int type, String msg)  | 事件回调， type: 0 对讲开始，1 对讲结束|


### 错误码

| 名称                          |  取值    | 含义                                   |
| --------                      | -----    | --------------------------------------|
| IV_ERR_NONE                   |    0     |   成功                                 |
| IV_ERR_SYS_INIT_PRM_NULL      |  -100    | 系统模块初始化参数为空                   |
| IV_ERR_SYS_INIT_CB_NULL       |  -101    | 系统模块初始化回调函数为空               |
| IV_ERR_SYS_INIT_PRM_RANGE_xx  |  -102    | 系统模块初始化参数超过范围               |
| IV_ERR_SYS_DEVICE_INFORMATION |  -103    | 系统模块获取设备信息错误                 |
| IV_ERR_DM_INIT_PRM_NULL       |  -200    | 物模型模块初始化参数为空                 |
| IV_ERR_DM_INIT_CB_NULL        |  -201    | 物模型模块初始化回调函数为空               |
| IV_ERR_DM_INIT_PRM_RANGE      |  -202    | 物模型模块参数超过范围               |
| IV_ERR_DM_INIT_ENV            |  -203    | 物模型模块初始化环境错误               |
| IV_ERR_DM_TYPE_NOT_SUPPORT    |  -204    | 物模型模块类型不支持                 |
| IV_ERR_DM_NULL_PTR            |  -205    | 物模型输入参数空指针                 |
| IV_ERR_DM_REPORT_EVENT_FAIL   |  -206    | 事件上报失败                         |
| IV_ERR_DM_REPORT_BUSY         |  -207    | 属性上报忙                           |
| IV_ERR_AVT_INIT_PRM_NULL      |  -300    | 音视频传输和对讲模块初始化参数为空                 |
| IV_ERR_AVT_INIT_CB_NULL       |  -301    | 音视频传输和对讲模块初始化回调函数为空               |
| IV_ERR_AVT_INIT_PRM_RANGE     |  -302    | 音视频传输和对讲模块参数超过范围               |
| IV_ERR_AVT_REQ_CHN_BUSY       |  -303    | 音视频传输和对讲模块请求通道忙               |
| IV_ERR_AVT_SEND_STREAM_TOO_BIG|  -304    | 发送的数据超过初始设置的最大值                 |
| IV_ERR_AVT_CHN_NOT_EXIT       |  -305    | 请求的通道不存在                         |
| IV_ERR_AVT_NEED_IDR_FRAME     |  -306    | 需要关键帧                           |
| IV_ERR_AVT_MALLOC_BUFFER_FAILED|  -307   | 分配的内存失败                           |
| IV_ERR_AVT_FAILED             |  -308    | 音视频传输和对讲模块运行错误                |
| IV_ERR_DEVICE_OFFLINE          |  -901  |  设备处于离线状态              |


## 设备与App交互流程

[App呼叫设备](docs/呼叫流程/App呼设备.md)

[设备呼叫App](docs/呼叫流程/设备呼App.md)
