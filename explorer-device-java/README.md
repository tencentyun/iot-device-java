* [腾讯云物联网开发平台设备端 IoT Explorer Java-SDK](#腾讯云物联网开发平台设备端-IoT-Explorer-Java-SDK)
  * [前提条件](#前提条件)
  * [工程配置](#工程配置)
  * [下载IoT Explorer Java-SDK Demo示例代码](#下载IoT-Explorer-Java-SDK-Demo示例代码)
  * [设备认证说明](#设备认证说明)
  * [子设备管理](#子设备管理)
  * [SDK设计说明](#SDK设计说明)
  * [SDK API 说明](#SDK-API-说明)

# 腾讯云物联网开发平台设备端 IoT Explorer Java-SDK
欢迎使用腾讯云物联网开发平台设备端 IoT Explorer Java-SDK 。

腾讯云物联网开发平台设备端IoT Explorer Java-SDK， 配合平台对设备数据模板化进行定义，基于数据模板协议实现设备和云端的数据交互框架。开发者基于IoT Explorer Java-SDK数据模板框架，可快速实现设备和平台、设备和应用之间的数据交互。此文档将介绍如何获取 IoT Explorer Java-SDK 并开始调用。 如果您在使用 IoT Explorer Java-SDK 的过程中遇到任何问题，[欢迎在当前 GitHub 提交 Issues](https://github.com/tencentyun/iot-device-java/issues/new)。

## 前提条件
* 您需要创建一个腾讯云账号，在腾讯云控制台中开通物联网开发平台产品。
* 在控制台上创建项目产品设备，获取产品ID、设备名称、设备证书（证书认证）、设备私钥（证书认证）、设备密钥（密钥认证），设备与云端认证连接时需要用到以上信息。具体步骤请参考官网 [用户指南-项目管理](https://cloud.tencent.com/document/product/1081/40290)、 [用户指南-产品定义](https://cloud.tencent.com/document/product/1081/34739)、 [用户指南-设备调试](https://cloud.tencent.com/document/product/1081/34741)。

## 工程配置

**引用方式**

-  gradle 工程 正式版SDK 远程构建

    如果您想通过引用jar的方式进行项目开发，可在module目录下的build.gradle中添加如下依赖：
    ```
    dependencies {
        ...
        implementation 'com.tencent.iot.explorer:explorer-device-java:1.0.0'
    }
    ```

-  maven 工程 正式版SDK 远程构建

    在工程根目录的pom.xml中添加：
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.explorer</groupId>
            <artifactId>explorer-device-java</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
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
        implementation 'com.tencent.iot.explorer:explorer-device-java:1.0.0-SNAPSHOT'
    }
    ```

-  maven 工程 snapshot版SDK 远程构建

    > 建议使用正式版SDK，SNAPSHOT版本会静默更新，使用存在风险

    在工程根目录的pom.xml中添加：
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.explorer</groupId>
            <artifactId>explorer-device-java</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <repositories>
        <repository>
            <id>snapshots</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </repository>
    </repositories>
    ```

**Java Sdk源码**

如果您想通过代码集成方式进行项目开发，可访问[Github](https://github.com/tencentyun/iot-device-java/tree/master/explorer-device-java)下载Java Sdk源码。

## 下载IoT Explorer Java-SDK Demo示例代码
下载[仓库](https://github.com/tencentyun/iot-device-java)下完整代码，IoT Explorer Java-SDK Demo示例代码在 [explorer-device-java](https://github.com/tencentyun/iot-device-java/tree/master/explorer-device-java) 的module下。

## 设备认证说明

IoT Explorer物联网开发平台支持设备以密钥认证和证书认证，两种认证方式接入。

#### 密钥认证接入

示例中编辑 [IoTDataTemplate.java](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-java/src/main/java/com/tencent/iot/explorer/device/java/test/IoTDataTemplate.java) 文件中的参数配置信息
```
{
    private static String mProductID = "";
    private static String mDevName = "";
    private static String mDevPSK  = ""; //若使用证书验证，设为null
}
```
如果控制台创建设备使用的是密钥认证方式，需要在 IoTDataTemplate.java 填写 mProductID（产品ID）、mDevName（设备名称）、mDevPSK（设备密钥），示例中使用的是密钥认证。

#### 证书认证接入

将证书放到resources文件夹中，将psk设置null，然后将证书名称填写入指定区域

如果控制台创建设备使用的是证书认证方式，除了需要在 IoTDataTemplate.java 填写 mProductID（产品ID）、mDevName（设备名称），mDevPSK（设备密钥）设置为null，还需修改 DataTemplateSample 初始化方法为
```
public DataTemplateSample(String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack, final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack)
```
将证书放到resources文件夹中，在 DataTemplateSample 初始化时传入 devCertName（设备证书文件名称）devKeyName（设备私钥文件名称）。

## 子设备管理
有些设备必须依托网关设备才可与物联网开发平台进行通信，这类设备我们称之为子设备。网关类型的设备通过与云端进行数据通信，可代理其下的子设备进行上下线，以及提供添加/删除子设备的能力。
示例代码如下：
```
  mGatewaySample.addSubDev(mSubDev1ProductId,mSubDev1DeviceName);
  //mGatewaySample.delSubDev(mSubDev1ProductId,mSubDev1DeviceName);
  
  mGatewaySample.onlineSubDev(mSubDev1ProductId,mSubDev1DeviceName);
  //mGatewaySample.offlineSubDev(mSubDev1ProductId,mSubDev1DeviceName);
```

## SDK设计说明

| 类名                 | 功能                                         |
| -------------------- | -------------------------------------------- |
| TXMqttConnection     | 连接物联网开发平台                           |
| TXDataTemplate       | 实现数据模板基本功能                         |
| TXDataTemplateClient | 实现直连设备根据数据模板连接物联网开发平台   |
| TXGatewayClient      | 实现网关设备根据数据模板连接物联网开发平台   |
| TXGatewaySubdev      | 实现网关子设备根据数据模板连接物联网开发平台 |

![](https://main.qcloudimg.com/raw/ea345acb67bd0f9ef20a7336704bd070.jpg)

## SDK API 说明

#### TXMqttConnection

| 方法名                        | 说明                                |
| ---------------------------- | -----------------------------------|
| connect                      |  MQTT 连接                          |
| reconnect                    |  MQTT 重连                          |
| disConnect                   |  断开 MQTT连接                       |
| publish                      |  发布 MQTT 消息                      |
| subscribe                    |  订阅 MQTT 主题                      |
| unSubscribe                  |  取消订阅 MQTT 主题                   |
| getConnectStatus             |  获取 MQTT 连接状态                   |
| setBufferOpts                |  设置断连状态 buffer 缓冲区              |
| initOTA                      |  初始化 OTA 功能                        |
| reportCurrentFirmwareVersion |  上报设备当前版本信息到后台服务器        |
| reportOTAState               |  上报设备升级状态到后台服务器            |

#### TXDataTemplate

| 方法名                   | 说明                     |
| ------------------------ | ------------------------ |
| subscribeTemplateTopic   | 订阅数据模板相关主题     |
| unSubscribeTemplateTopic | 取消订阅数据模板相关主题 |
| propertyReport           | 上报属性                 |
| propertyGetStatus        | 更新状态                 |
| propertyReportInfo       | 上报设备信息             |
| propertyClearControl     | 清除控制信息             |
| eventSinglePost          | 上报单个事件             |
| eventsPost               | 上报多个事件             |

#### TXDataTemplateClient

| 方法名                   | 说明                       |
| ------------------------ | -------------------------- |
| isConnected              | 是否已经连接物联网开发平台 |
| subscribeTemplateTopic   | 订阅数据模板相关主题       |
| unSubscribeTemplateTopic | 取消订阅数据模板相关主题   |
| propertyReport           | 上报属性                   |
| propertyGetStatus        | 更新状态                   |
| propertyReportInfo       | 上报设备信息               |
| propertyClearControl     | 清除控制信息               |
| eventSinglePost          | 上报单个事件               |
| eventsPost               | 上报多个事件               |

#### TXGatewayClient

| 方法名        | 说明                               |
| ------------- | ---------------------------------- |
| findSubdev    | 查找子设备（根据产品ID和设备名称） |
| removeSubdev  | 删除子设备                         |
| addSubdev     | 添加子设备                         |
| subdevOffline | 上线子设备                         |
| subdevOnline  | 下线子设备                         |
| setSubdevStatus  | 设置子设备状态             |
| subscribeSubDevTopic  | 订阅数据模板相关主题         |
| unSubscribeSubDevTopic  | 取消订阅数据模板相关主题         |
| subDevPropertyReport  | 属性上报                          |

#### TXGatewaySubdev

| 方法名          | 说明               |
| --------------- | ------------------ |
| getSubdevStatus | 获取子设备连接状态 |
| setSubdevStatus | 设置子设备连接状态 |

