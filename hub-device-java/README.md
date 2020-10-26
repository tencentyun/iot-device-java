* [腾讯云物联网通信设备端 IoT Hub Java-SDK](#腾讯云物联网通信设备端-IoT-Hub-Java-SDK)
  * [前提条件](#前提条件)
  * [工程配置](#工程配置)
  * [下载IoT Hub Java-SDK Demo示例代码](#下载IoT-Hub-Java-SDK-Demo示例代码)
  * [认证连接](#认证连接)
  * [子设备管理](#子设备管理)
  * [设备影子](#设备影子)
  * [API接口说明](#API接口说明)
  * [Demo示例体验-设备互通](#Demo示例体验-设备互通)
  * [Demo示例体验-设备状态上报与状态设置](#Demo示例体验-设备状态上报与状态设置)
  * [Demo示例体验-网关子设备固件升级](#Demo示例体验-网关子设备固件升级)
  * [常见问题](#常见问题)

# 腾讯云物联网通信设备端 IoT Hub Java-SDK
欢迎使用腾讯云物联网通信设备端 IoT Hub Java-SDK 。

腾讯云物联网通信设备端 IoT Hub Java-SDK， 依靠安全且性能强大的数据通道，为物联网领域开发人员提供终端(如传感器, 执行器, 嵌入式设备或智能家电等等)和云端的双向通信能力。此文档将介绍如何获取 IoT Hub Java-SDK 并开始调用。 如果您在使用 IoT Hub Java-SDK 的过程中遇到任何问题，[欢迎在当前 GitHub 提交 Issues](https://github.com/tencentyun/iot-device-java/issues/new)。

## 前提条件
* 您需要创建一个腾讯云账号，在腾讯云控制台中开通物联网通信产品。
* 在控制台上创建物联网产品和设备，获取产品ID、设备名称、设备证书（证书认证）、设备私钥（证书认证）、设备密钥（密钥认证），设备与云端认证连接时需要用到以上信息。具体步骤请参考官网 [控制台使用手册-设备接入准备](https://cloud.tencent.com/document/product/634/14442)。
* 理解Topic权限，当在控制台中成功创建产品后，该产品默认有三条权限。订阅：${productId}/${deviceName}/control，订阅和发布：${productId}/${deviceName}/data，发布：${productId}/${deviceName}/event。请参考官网 [控制台使用手册-权限列表](https://cloud.tencent.com/document/product/634/14444) 操作Topic权限。

## 工程配置

**引用方式**

-  gradle 工程 正式版SDK 远程构建

    如果您想通过jar引用方式进行项目开发，可在module目录下的build.gradle中添加依赖，如下依赖：
    ```
    dependencies {
        ...
        implementation 'com.tencent.iot.hub:hub-device-java:1.0.0'
    }
    ```

-  maven 工程 正式版SDK 远程构建

    在工程根目录的pom.xml中添加：
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.hub</groupId>
            <artifactId>hub-device-java</artifactId>
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
        implementation 'com.tencent.iot.hub:hub-device-java:1.0.1-SNAPSHOT'
    }
    ```

-  maven 工程 snapshot版SDK 远程构建

    > 建议使用正式版SDK，SNAPSHOT版本会静默更新，使用存在风险

    在工程根目录的pom.xml中添加：
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.hub</groupId>
            <artifactId>hub-device-java</artifactId>
            <version>1.0.1-SNAPSHOT</version>
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

如果您想通过代码集成方式进行项目开发，可访问[Github](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-java)下载Java Sdk源码。

## 下载IoT Hub Java-SDK Demo示例代码
下载[仓库](https://github.com/tencentyun/iot-device-java)下完整代码，IoT Hub Java-SDK Demo示例代码在 [hub-device-java](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-java) 的module下。


## 认证连接
设备的身份认证支持两种方法，密钥认证和证书认证：
- 若使用密钥认证方式，需ProductID，DevName和DevPSK；
- 若使用证书认证方式，需ProductID，DevName，CertFile和PrivateKeyFile通过输入流解析构造双向认证SSLSocketFactory；
```
    String mProductID = "YOUR_PRODUCT_ID";
    String mDevName = "YOUR_DEVICE_NAME";
    String mDevPSK = "YOUR_DEV_PSK";
    String mSubProductID = "YOUR_SUB_PRODUCT_ID";
    String mSubDevName = "YOUR_SUB_DEV_NAME";
    String mSubDevProductKey = "YOUR_SUB_DEV_PSK";
    String mTestTopic = "YOUR_TEST_TOPIC";
    String mCertFilePath = null;
    String mPrivKeyFilePath = null;

    TXMqttConnection mqttconnection = new TXMqttConnection(mProductID, mDevName, mDevPSK, new callBack());
    mqttconnection.connect(options, null);
    try {
            Thread.sleep(20000);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    mqttconnection.disConnect(null);
```

## 子设备管理
如果当前设备是一个网关，且该网关下的子设备需接入平台从而可以通过平台对子设备进行控制与管理，此时需要使用子设备管理功能。
网关子设备管理提供了绑定子设备、解绑子设备、子设备上线、子设备下线、代理子设备数据上下行的能力。

```
  mqttconnection.gatewayBindSubdev(mSubProductID, mSubDevName, mDevPSK);
  //mqttconnection.gatewayUnbindSubdev(mSubProductID, mSubDevName);

  mqttconnection.gatewaySubdevOnline(mSubProductID, mSubDevName);
  //mqttconnection.gatewaySubdevOffline(mSubProductID, mSubDevName);
```

## 设备影子
为降低设备功耗和通信流量，设备通过设备影子功能，在平台缓存设备状态信息，方便其他业务使用；同时平台可通过设备影子离线配置设备，当设备上线后会更新设备影子中数据给设备，实现对设备进行配置。
```
  //mShadowConnection.get(null);
  //mShadowConnection.reportNullDesiredInfo();

  DeviceProperty deviceProperty = new DeviceProperty("temperature", "27", TXShadowConstants.JSONDataType.INT);
  List<DeviceProperty> devicePropertyList = new ArrayList<>() ;
  devicePropertyList.add(deviceProperty);
  mShadowConnection.update(devicePropertyList, null);
```

## API接口说明

### MQTT接口 ###
MQTT的相关接口定义在[TXMqttConnection](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/src/main/java/com/tencent/iot/hub/device/java/core/mqtt/TXMqttConnection.java)类中，支持发布和订阅功能；如果需支持设备影子功能，则需使用TXShadowConnection类及其方法，TXMqttConnection类的接口介绍如下：

| 方法名               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| connect     | MQTT连接                                         |
| reconnect | MQTT重连                               |
| disConnect     | 断开MQTT连接                                   |
| publish      | 发布MQTT消息                        |
| subscribe           | 订阅MQTT主题                                  |
| unSubscribe   | 取消订阅MQTT主题 |
| getConnectStatus               | 获取MQTT连接状态                      |
| setBufferOpts      | 设置断连状态buffer缓冲区                                 |
| initOTA      | 初始化OTA功能                                 |
| reportCurrentFirmwareVersion      | 上报设备当前版本信息到后台服务器                      |
| reportOTAState      | 上报设备升级状态到后台服务器                      |
                              

### MQTT网关接口 ###

对于不具备直接接入以太网网络的设备，可先接入本地网关设备的网络，利用网关设备的通信功能，代理设备接入腾讯云物联网通信IoT Hub平台。对于局域网中加入或退出网络的子设备，需通过平台进行绑定或解绑操作。
注：当子设备发起过上线，后续只要网关链接成功，后台就会显示子设备在线，除非设备发起下线操作。
MQTT网关的相关接口定义在[TXGatewayConnection](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/src/main/java/com/tencent/iot/hub/device/java/core/gateway/TXGatewayConnection.java)类中，介绍如下：

| 方法名               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| connect     | 网关MQTT连接                                         |
| reconnect | 网关MQTT重连                               |
| disConnect     | 断开网关MQTT连接                                   |
| publish      | 发布MQTT消息                        |
| subscribe           | 订阅MQTT主题                                  |
| unSubscribe   | 取消订阅MQTT主题 |
| getConnectStatus               | 获取MQTT连接状态                      |
| setBufferOpts      | 设置断连状态buffer缓冲区                                 |
| gatewaySubdevOffline           | 子设备下线                                 |
| gatewaySubdevOnline   | 子设备上线 |
| gatewayBindSubdev               | 子设备绑定                      |
| gatewayUnbindSubdev      | 子设备解绑                                 |      
| getSubdevStatus      | 获取子设备状态                                 |       
| setSubdevStatus      | 设置子设备状态                                 |      
| gatewayGetSubdevRelation      | 获取网关拓扑关系                                 |                

### 设备影子接口 ###

如果需要支持设备影子功能，需使用[TXShadowConnection](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/src/main/java/com/tencent/iot/hub/device/java/core/shadow/TXShadowConnection.java)类中的方法，介绍如下：

| 方法名               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| connect     | MQTT连接                                         |
| disConnect     | 断开MQTT连接                                   |
| publish      | 发布MQTT消息                        |
| subscribe           | 订阅MQTT主题                                  |
| update               | 更新设备影子文档                     |
| get      | 获取设备影子文档                                 |
| reportNullDesiredInfo           | 更新delta信息后，上报空的desired信息                                 |
| setBufferOpts   | 设置断连状态buffer缓冲区 |
| getMqttConnection               | 获取TXMqttConnection实例                      |
| getConnectStatus      | 获取mqtt连接状态                                 |  
| registerProperty      | 注册当前设备的设备属性                                 |  
| unRegisterProperty      | 取消注册当前设备的指定属性                                 |  


## Demo示例体验-设备互通
官网上假设的一个示例场景，请参考 [设备互通.md](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/docs/设备互通.md)

## Demo示例体验-设备状态上报与状态设置
官网上假设的一个示例场景，请参考 [设备状态上报与状态设置.md](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/docs/设备状态上报与状态设置.md)

## Demo示例体验-网关子设备固件升级
网关子设备固件升级Demo，请参考 [网关子设备固件升级.md](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/docs/网关子设备固件升级.md)

## 常见问题

常见问题请参考docs目录下的[常见问题java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/docs/常见问题java.md)
