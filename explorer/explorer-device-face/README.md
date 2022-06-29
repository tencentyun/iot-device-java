简体中文 | [English](docs/en)

* [IoT Explorer人脸识别 Android SDK开发指南](#IoT-Explorer人脸识别-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [explorer-device-face SDK 设计说明](#explorer-device-face-SDK-设计说明)
     *  [explorer-device-face SDK 回调callback 设计说明](#explorer-device-face-SDK-回调callback-设计说明)

# IoT Explorer人脸识别 Android SDK开发指南

本文主要描述物联网开发平台设备端IoT Explorer Android-SDK中接入人脸识别离线 Android-SDK 开发指南 。

## 引用方式

- 集成 SDK 方式

如果不需要接入人脸识别离线SDK，仅需要接入explorer-device-android SDK，请参考 [编译环境及SDK接入说明.md](../explorer-device-android/docs/zh/编译环境及SDK接入说明.md)

 -  gradle 工程 正式版SDK 远程构建

    在应用模块的build.gradle中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-face:x.x.x' //IoT Explorer 与 人脸识别离线交互 的依赖
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
    在应用模块的build.gradle中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本，末位+1
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-face:x.x.x-SNAPSHOT' //IoT Explorer 与 人脸识别离线交互 的依赖
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-face](../explorer-device-face)源码，示例如下：
    
     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-face') //IoT Explorer 与 人脸识别离线交互 的依赖
    }
     ```

Demo示例工程使用的是 依赖本地 explorer-device-android 和 explorer-device-face 的 sdk源码 构建方式。

## API说明

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/zh/SDK%20API及参数说明.md)

### explorer-device-face SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| TXFaceKitTemplate       | 实现人脸识别数据模板基本功能                     |
| TXFaceKitTemplateClient | 实现直连设备根据人脸识别数据模板连接物联网开发平台  |
| TXResourceImpl          | 根据物联网开发平台人员库数据实现下载资源到设备中    |

#### TXFaceKitTemplate

TXFaceKitTemplate 继承自 TXDataTemplate类

| 方法名                         | 说明                                            |
| ----------------------------- | ---------------------------------------------- |
| initResource                  | 初始化人员库资源下载功能                           |
| sysEventSinglePost            | 系统单个事件上报， 不检查构造是否符合json文件中的定义 |
| reportCurrentResourceVersion  | 上报设备当前人员库资源版本信息到后台服务器           |
| reportResourceState           | 上报设备人员库资源下载状态到后台服务器               |
| subscribeServiceTopic         | 订阅Service主题                                 |
| unSubscribeServiceTopic       | 取消订阅Service主题                              |
| initAuth                      | 初始化 离线人脸识别SDK 授权                       |


#### TXDataTemplateClient

| 方法名                         | 说明                                  |
| ----------------------------- | ------------------------------------ |
| isConnected                   | 是否已经连接物联网开发平台               |
| subscribeTemplateTopic        | 订阅数据模板相关主题                    |
| subscribeServiceTopic         | 订阅Service主题                       |
| unSubscribeTemplateTopic      | 取消订阅数据模板相关主题                |
| unSubscribeServiceTopic       | 取消订阅Service主题                    |
| propertyReport                | 上报属性                              |
| propertyGetStatus             | 更新状态                              |
| propertyReportInfo            | 上报设备信息                           |
| propertyClearControl          | 清除控制信息                           |
| eventSinglePost               | 上报单个事件                           |
| reportSysRetrievalResultEvent | 检索人脸事件上报                       |
| eventsPost                    | 上报多个事件                           |
| initAuth                      | 初始化 离线人脸识别SDK 授权             |
| initResource                  | 初始化人员库资源下载功能                |
| reportCurrentResourceVersion  | 上报设备当前人员库资源版本信息到后台服务器 |

### explorer-device-face SDK 回调callback 设计说明

TXAuthCallBack 授权回调callback说明如下：

```
    /**
     * 人脸识别SDK 鉴权成功回调
     */
    public abstract void onSuccess();
    /**
     * 人脸识别SDK 鉴权失败回调
     * @param code          鉴权状态码
     * @param status        结果
     */
    public abstract void onFailure(Integer code, String status);
```

TXResourceCallBack 人员库资源回调callback说明如下：

```
    /**
     * 上报人员库资源版本信息回调
     * @param resultCode  上报结果码；0：成功；其它：失败
     * @param resourceList  JSONArray内部装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @param resultMsg  上报结果码描述
     */
    void onReportResourceVersion(int resultCode, JSONArray resourceList, String resultMsg);

    /**
     * 人员库资源文件有新的版本可以升级
     * @param url  文件 url 用于下载最新版本
     * @param md5  md5 值用于校验
     * @param version  最新版本号
     */
    boolean onLastestResourceReady(String url, String md5, String version);

    /**
     * 人员库资源文件下载进度回调
     * @param resourceName  人脸库资源文件名称 或 资源文件名称，不含路径，包含featureId和文件格式;
     * @param percent  下载进度（0 ~ 100）;
     * @param version  版本；
     */
    void onDownloadProgress(String resourceName, int percent, String version);

    /**
     * 人员库资源文件下载完成回调
     * @param outputFile  已下载完成的资源文件名（包含全路径）；
     * @param version  版本；
     */
    void onDownloadCompleted(String outputFile, String version);

    /**
     * 人员库资源文件下载失败回调
     * @param resourceName  人脸库资源文件名称 或 资源文件名称，不含路径，包含featureId和文件格式;
     * @param errCode  失败错误码; -1: 下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
     * @param version  版本；
     */
    void onDownloadFailure(String resourceName, int errCode, String version);

    /**
     * 人员库删除特征回调
     * @param featureId     特征Id   featureId
     * @param resourceName  资源文件名称，不含路径，包含featureId和文件格式
     */
    void onFeatureDelete(String featureId, String resourceName);

    /**
     * 人员库删除回调
     * @param version       人脸库资源文件版本号
     * @param resourceName  人脸库资源文件名称
     */
    void onFaceLibDelete(String version, String resourceName);

    /**
     * 离线检索事件需要保存的回调
     * @param feature_id    特征id，对应控制台的人员ID。
     * @param score         检索分数
     * @param sim           检索和特征的相似度
     * @param timestamp     时间戳
     */
    void onOfflineRetrievalResultEventSave(String feature_id, float score, float sim, int timestamp);
```

* [YT-MOBILE-UTILS通用接口](ai-docs/zh/优图移动端通用函数文档.md)
* [优图人脸追踪 FACE-TRACKER](ai-docs/zh/优图人脸追踪文档.md)
* [优图人脸精确配准文档](ai-docs/zh/优图人脸精确配准文档.md)
* [优图人脸质量文档](ai-docs/zh/优图人脸质量文档.md)
* [优图人脸归隐质量文档 FACE-QUALITY-PRO](ai-docs/zh/优图人脸归因质量文档.md)
* [优图人脸彩色活文档 FACE-LIVE-COLOR](ai-docs/zh/优图人脸彩色活体文档.md)
* [优图人脸特征文档 FACE-FEATURE](ai-docs/zh/优图人脸特征文档.md)
* [优图人脸检索 YT-FACE-RETRIEVAL](ai-docs/zh/优图人脸检索文档.md)
* [Demo 架构设计简介](ai-docs/zh/Demo架构设计简介.md)

