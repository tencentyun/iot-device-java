# 腾讯物联网开发平台Android SDK
腾讯物联开发平台 Android SDK 配合平台对设备数据模板化的定义，实现和云端基于数据模板协议的数据交互框架，开发者基于IoT_Explorer Android SDK数据模板框架，快速实现设备和平台、设备和应用之间的数据交互。

# 快速开始
本节将讲述如何在腾讯物联网开发平台控制台申请设备, 并结合本 SDK 快速体验设备如何接入腾讯物联网开发平台。

## 一. 控制台创建设备

#### 1. 注册/登录腾讯云账号
访问[腾讯云登录页面](https://cloud.tencent.com/login?s_url=https%3A%2F%2Fcloud.tencent.com%2F), 点击[立即注册](https://cloud.tencent.com/register?s_url=https%3A%2F%2Fcloud.tencent.com%2F), 免费获取腾讯云账号，若您已有账号，可直接登录。

#### 2. 访问物联云控制台
登录后点击右上角控制台，进入控制台后, 鼠标悬停在云产品上, 弹出层叠菜单。

![](https://main.qcloudimg.com/raw/ec85d26d1dbef9c90c4a2462f3204403.jpg
)

搜索框中输入物联网开发平台，或直接访问[物联网开发平台控制台](https://console.cloud.tencent.com/iotexplorer)。

#### 3. 创建产品和设备

3.1 创建项目![](https://main.qcloudimg.com/raw/68b086b04d78339d336db8494b1b1033.jpg)

3.2.创建并选择和产品比较相近的模板产品，此处示例创建3种产品（网关，灯和空调，其中灯和空调作为子设备），更多产品请参阅[产品定义](https://cloud.tencent.com/document/product/1081/34739?!preview&!editLang=zh)。

![](https://main.qcloudimg.com/raw/82d66efc51c9b5d598231c198eed28aa.jpg)

![](https://main.qcloudimg.com/raw/6ec31ad280be850aaf4f5c8308647141.jpg)

![](https://main.qcloudimg.com/raw/694bbe63521ef4f4730eb89d151ed164.jpg)

![](https://main.qcloudimg.com/raw/7d1da93fffe9e0aa5f72a8b48e18b710.jpg)

3.3 定义产品的数据和事件模板，参阅[数据模板创建](https://cloud.tencent.com/document/product/1081/34739?!preview&!editLang=zh#.E6.95.B0.E6.8D.AE.E6.A8.A1.E6.9D.BF)，数据模板的说明参见[数据模板协议](https://cloud.tencent.com/document/product/1081/34916?!preview&!editLang=zh)。（**此处使用的默认产品，数据模板已建好，示例中略过该步**）

3.4 完成产品创建和数据模板定义后，创建设备，则每一个创建的设备都具备这个产品下的数据模板属性，如下图示。

![](https://main.qcloudimg.com/raw/7d05e54fdaf8520c481f375456298257.jpg)

![](https://main.qcloudimg.com/raw/2adee7ee3b7dba94326b96316020755f.jpg)

![](https://main.qcloudimg.com/raw/43201a6ceba57ce8e21381a340c0abfc.jpg)

3.5 查询产品和设备信息，除了子设备不需要设备密钥外，设备连接物联网开发平台需要三元组信息包括设备名称、设备密钥和产品ID

![](https://main.qcloudimg.com/raw/a4f19b2bcaef9f348b24bef35dcab8dc.jpg)

![](https://main.qcloudimg.com/raw/c49b49888af552f29820a3ae1381cf9d.jpg)

3.6 导出数据模板json文件，如果有子设备也需要导出
![](https://main.qcloudimg.com/raw/b0a65a222d1911d71c5893755ede611b.jpg)

#### 4. 添加子设备（网关示例下需要该步骤）

4.1 添加子产品

![](https://main.qcloudimg.com/raw/f0caff57f2ada4bcbd0593344c2b8edd.jpg)

![](https://main.qcloudimg.com/raw/8448010dceb40e792c3fa89d00171448.png)

4.2 添加子设备

![](https://main.qcloudimg.com/raw/782b89420533a3f132304c6e36f1fd56.jpg)

## 二. 编译运行示例程序

#### 1. 使用Android Studio打开工程
使用 Android Studio 导入 qcloud-iot-explorer-sdk-android/build.gradle 从而打开工程

#### 2. 填入设备信息

SDK提供了三种示例，分别对应数据模板基本功能示例（使用灯产品下的设备），非网关设备示例（使用灯产品下的设备），网关设备示例（使用网关产品下的设备以及绑定的产品子设备）。

| 示例对应文件                 | 简介                 | 所需填入设备信息                                             |
| ---------------------------- | -------------------- | ------------------------------------------------------------ |
| IoTDataTemplateFragment.java | 实现数据模板基本功能 | 设备ID，设备名称，设备密钥                                   |
| IoTLightFragment.java        | 灯产品例子           | 设备ID，设备名称，设备密钥                                   |
| IoTGatewayFragment.java      | 网关产品例子         | 网关设备：设备ID，设备名称，设备密钥<br />子设备：设备ID，设备名称 |

IoTDataTemplateFragment.java  和IoTLightFragment.java对应代码段：

![](https://main.qcloudimg.com/raw/6933b83d0a7af12558ca749922a3d0b7.jpg)

IoTGatewayFragment.java对应代码段

![](https://main.qcloudimg.com/raw/fcbf3f04b24e0bdee9c1238365770ecb.jpg)

#### 4. 运行
点击 Android Studio Run 'app' 按钮安装 Demo。

#### 5. 数据模板基本功能

点击`设备上线`，然后`订阅主题`，可以进行包括`属性上报`，`状态更新`等基本功能的测试，并且可以通过相应设备的控制台进行在线调试。测试完，需要`设备下线`。

![](https://main.qcloudimg.com/raw/f39f0086c460e6a69e31dff799699e8a.jpg)

![](https://main.qcloudimg.com/raw/a2563026883b1af1555668c3e196d1af.jpg)

#### 6.灯示例

![](https://main.qcloudimg.com/raw/f39f0086c460e6a69e31dff799699e8a.jpg)

![](https://main.qcloudimg.com/raw/a2563026883b1af1555668c3e196d1af.jpg)

#### 6.灯示例

点击`设备上线`，然后通过相应设备的控制台进行在线调试，可以看到属性变化。测试完，需要`下线设备`。

![](https://main.qcloudimg.com/raw/bfb53d1431e30ad4eac419b1348fb119.jpg)

#### 7.网关示例

点击`网关上线`，然后通过添加相应的子设备，点击相应设备`上线`可以通过对应设备的控制台在线调试

可以看到属性变化。测试完，需要`下线`相应设备，并`网关下线`。

![](https://main.qcloudimg.com/raw/5aff99be6b1fdadaf2d57380eaf926f3.jpg)