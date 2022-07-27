简体中文 | [English](docs/en)

* [IoT Explorer Central Android SDK开发指南](#IoT-Explorer-Central-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-central SDK 关键接口](#explorer-device-central-SDK-关键接口)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [explorer-device-central SDK 设计说明](#explorer-device-central-SDK-设计说明)
     *  [explorer-device-central SDK 回调callback 设计说明](#explorer-device-central-SDK-回调callback-设计说明)

# IoT Explorer Central Android SDK开发指南

本文主要描述腾讯云物联网开发平台设备端中接入中控屏SDK的开发指南 。

## 引用方式

1、集成 SDK 方式
-  gradle 工程 正式版SDK 远程构建

    在应用模块的build.gradle中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-central:x.x.x'
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
        implementation 'com.tencent.iot.explorer:explorer-device-central:x.x.x-SNAPSHOT'
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-central](../explorer-device-central)源码，示例如下：

     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-central')
    }
     ```

Demo示例工程使用的是 依赖本地 explorer-device-android 和 explorer-device-central 的 sdk源码 构建方式。


## API说明

### explorer-device-central SDK 关键接口

Http请求相关接口 请参考 [HttpRequest.kt](../explorer-device-android/src/main/java/com/tencent/iot/explorer/device/android/http/HttpRequest.kt)

#### Http接口说明

##### 设备管理
| 接口名称                    | Action                                         |    链接 |   备注  |
| ----------------------- | ------------------------------- | --------------------------------------------  |  ----------------|
|获取产品信息|	AppGetProducts|	https://cloud.tencent.com/document/product/1081/48764|	获取物模型、通信类型|
|获取家庭列表|	AppGetFamilyList|	https://cloud.tencent.com/document/product/1081/40811||
|获取房间列表|	AppGetRoomList|	https://cloud.tencent.com/document/product/1081/40816||
|获取用户绑定设备列表	|AppGetFamilyDeviceList|	https://cloud.tencent.com/document/product/1081/40803||
|获取设备当前状态	|AppGetDeviceStatuses|	https://cloud.tencent.com/document/product/1081/40804|	获取设备在线离线状态|
|获取设备详情	|AppGetDeviceInFamily|	https://cloud.tencent.com/document/product/1081/40807|	获取设备名称、别名、icon url、家庭ID、房间ID|
|获取已绑定到家庭下的指定网关的子设备列表	|AppGetFamilySubDeviceList	|https://cloud.tencent.com/document/product/1081/48130	|获取已绑定到家庭下的指定网关的子设备列表|

##### 设备控制
| 接口名称                    | Action                                         |    链接 |   备注  |
| ----------------------- | ------------------------------- | --------------------------------------------  |  ----------------|
|用户控制设备|	AppControlDeviceData|	https://cloud.tencent.com/document/product/1081/40805|	用于用户对绑定的设备发起控制操作|

##### 场景联动
| 接口名称                    | Action                                         |    链接 |   备注  |
| ----------------------- | ------------------------------- | --------------------------------------------  |  ----------------|
|获取手动智能联动列表|	AppGetSceneList|	https://cloud.tencent.com/document/product/1081/50211||
|执行手动智能联动|	AppRunScene|	https://cloud.tencent.com/document/product/1081/50214||
|获取自动智能联动列表|	AppGetAutomationList|	https://cloud.tencent.com/document/product/1081/50216||
|修改自动智能联动状态|	AppModifyAutomationStatus|	https://cloud.tencent.com/document/product/1081/50220||


**使用详情可参见** [IoT Explorer Demo](../device-android-demo/src/main/java/com/tencent/iot/explorer/device/central/CentralMainActivity.java)

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/zh/SDK%20API及参数说明.md)

### explorer-device-central SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| CentralDataTemplate      | 中控屏方案数据模板基本功能                   |
| CentralTemplateClient    | 实现中控屏方案数据模板连接物联网开发平台|

#### CentralDataTemplate

CentralDataTemplate 继承自 TXDataTemplate类

```
/**
 * 传入accessToken（该token是在腾讯连连小程序绑定中控屏设备后，由后台下发给中控设备的，在onControlCallBack回调里可以收到该token），获取和该token对应的设备列表，设备列表通过OnGetDeviceListListener接口进行回调
 */
public Status requestDeviceList(String accessToken)
```

```
/**
 * 当accessToken过期时，可以通过该接口刷新token
 */
public Status refreshToken(String accessToken)
```

#### CentralTemplateClient

CentralTemplateClient 继承自 TXMqttConnection 类

```
/**
 * 是否已经连接物联网开发平台
 * @return 是 、 否
 */
public boolean isConnected()

/**
 * 订阅数据模板相关主题
 * @param topicId 主题ID
 * @param qos QOS等级
 * @return 发送请求成功时返回Status.OK;
 */
public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos)

/**
 * 取消订阅数据模板相关主题
 * @param topicId 主题ID
 * @return 发送请求成功时返回Status.OK;
 */
public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId)

/**
 * 属性上报
 * @param property 属性的json
 * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyReport(JSONObject property, JSONObject metadata)

/**
 * 获取状态
 * @param type 类型
 * @param showmeta 是否携带showmeta
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyGetStatus(String type, boolean showmeta)

/**
 * 设备基本信息上报
 * @param params 参数
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyReportInfo(JSONObject params)

/**
 * 清理控制信息
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyClearControl()

/**
 * 单个事件上报
 * @param eventId 事件ID
 * @param type 事件类型
 * @param params 参数
 * @return 发送请求成功时返回Status.OK;
 */
public Status eventSinglePost(String eventId, String type, JSONObject params)

/**
 * 多个事件上报
 * @param events 事件集合
 * @return 发送请求成功时返回Status.OK;
 */
public Status eventsPost(JSONArray events)
```

### explorer-device-central SDK 回调callback 设计说明

OnGetDeviceListListener 设备列表回调callback说明如下：

| 回调接口 | 功能 |
| ----------------------- | ---------- |
| onGetDeviceList(List<String> devices)                 | 中控屏关联的设备列表 |
