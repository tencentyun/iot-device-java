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
        implementation 'com.tencent.iot.explorer:explorer-device-rtc:3.2.1' //IoT Explorer 与 实时音视频 TRTC 的依赖
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
        implementation 'com.tencent.iot.explorer:explorer-device-rtc:3.2.1-SNAPSHOT' //IoT Explorer 与 实时音视频 TRTC 的依赖
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-rtc](../explorer-device-rtc)源码，示例如下：
    
     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-rtc') //IoT Explorer 与 实时音视频 TRTC 的依赖
    }
     ```

Demo示例工程使用的是 依赖本地 explorer-device-android 和 explorer-device-rtc 的 sdk源码 构建方式。

## API说明

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/SDK%20API及参数说明.md)

### explorer-device-trtc SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| TXTRTCDataTemplate      | 实现实时音视频数据模板基本功能                   |
| TXTRTCTemplateClient    | 实现直连设备根据实时音视频数据模板连接物联网开发平台|

#### TXTRTCDataTemplate

TXTRTCDataTemplate 继承自 TXDataTemplate类

```
    /**
     * 上报实时音视频类设备呼叫属性
     * @param callStatus 呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param callType 邀请类型 1-语音通话，2-视频通话
     * @param userId 被呼叫用户id 多个用户id用";"分割，      也可以为空字符串""，表示群呼，      也可以为字符串"null"，传入params，请做数据转发到您的后台服务，请参考: https://cloud.tencent.com/document/product/1081/40298
     * @param params 用户的物模型属性json
     * @return 发送请求成功时返回Status.OK;。 OK 成功， ERROR 发生错误， ERR_JSON_CONSTRUCT json构造失败， PARAMETER_INVALID Topic无效， MQTT_NO_CONN MQTT未连接
     */
    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId, JSONObject params)
```


#### TXTRTCTemplateClient


```
    /**
     * 是否已经连接物联网开发平台
     * @return 是 、 否
     */
    public boolean isConnected()

    /**
     * 生成绑定设备的二维码字符串
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generalDeviceQRCodeContent()

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
     * 上报实时音视频类设备呼叫属性
     * @param callStatus 呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param callType 邀请类型 1-语音通话，2-视频通话
     * @param userId 被呼叫用户id 多个用户id用";"分割，      也可以为空字符串""，表示群呼，      也可以为字符串"null"，传入params，请做数据转发到您的后台服务，请参考: https://cloud.tencent.com/document/product/1081/40298
     * @param params 用户的物模型属性json
     * @return 发送请求成功时返回Status.OK;
     */
    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId, JSONObject params)

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
