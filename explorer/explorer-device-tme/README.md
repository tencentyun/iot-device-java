* [IoT Explorer TME Android SDK开发指南](#IoT-Explorer-TME-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [explorer-device-tme SDK 设计说明](#explorer-device-tme-SDK-设计说明)
     *  [explorer-device-tme SDK 回调callback 设计说明](#explorer-device-tme-SDK-回调callback-设计说明)

# IoT Explorer TME Android SDK开发指南

本文主要描述腾讯云物联网开发平台设备端IoT Explorer Android-SDK中接入TME(腾讯音乐娱乐集团)的酷狗全生态方案SDK的开发指南 。

## 引用方式

- 集成 SDK 方式

如果不需要接入TME SDK，仅需要接入explorer-device-android SDK，请参考 [编译环境及SDK接入说明.md](../explorer-device-android/docs/编译环境及SDK接入说明.md)

 -  gradle 工程 正式版SDK 远程构建

    在应用模块的build.gradle中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本 
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-tme:x.x.x'
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
        implementation 'com.tencent.iot.explorer:explorer-device-tme:x.x.x-SNAPSHOT' //IoT Explorer 与 TME 的依赖
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-tme](../explorer-device-tme)源码，示例如下：
    
    ```gr
    dependencies {
        implementation project(':explorer:explorer-device-tme') //IoT Explorer 与 TME 的依赖，注意添加相互依赖的其他sdk module
    }
    ```

Demo示例工程使用的是 依赖本地 explorer-device-android 和 explorer-device-tme 的 sdk源码 构建方式。

## API说明

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/SDK%20API及参数说明.md)

### explorer-device-tme SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| TmeDataTemplate      | 酷狗全生态方案数据模板基本功能                   |
| TmeTemplateClient    | 实现直连设备根据酷狗全生态方案数据模板连接物联网开发平台|

#### TmeDataTemplate

TmeDataTemplate 继承自 TXDataTemplate类

```
    /**
     * 通过IoT Explorer后台获取酷狗SDK的User Info
     */
    public Status requestUserInfo()
```


#### TmeTemplateClient

TmeTemplateClient 继承自 TXMqttConnection 类

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

### explorer-device-tme SDK 回调callback 设计说明

ExpiredCallback token过期回调callback说明如下：

```
    void expired();
```

## 设备与用户绑定说明

Android设备通常具备丰富的人机交互界面（屏幕/键盘），用户可以直接输入 SSID/PSW 进行连接入网。

可使用`连连APP/小程序`扫描由以下接口生成的二维码，建立用户与设备之间的绑定关系。

```
explorer-device-android TXMqttConnection 类 的接口
    /**
     * 生成绑定设备的二维码字符串
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generateDeviceQRCodeContent()

    /**
     * 生成支持微信扫一扫跳转连连小程序的绑定设备的二维码字符串
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generateDeviceWechatScanQRCodeContent()
```