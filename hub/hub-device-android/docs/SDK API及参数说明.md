* [API 接口说明](#API-接口说明)
  * [MQTT 接口](#MQTT-接口)
  * [MQTT 网关接口](#MQTT-网关接口)
  * [设备影子接口](#设备影子接口)
  * [动态注册接口](#动态注册接口)

## API 接口说明

### MQTT 接口
MQTT的相关接口定义在[TXMqttConnection](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/iot_core/src/main/java/com/tencent/iot/hub/device/android/core/mqtt/TXMqttConnection.java)类中，支持发布和订阅等功能，TXMqttConnection类的接口介绍如下：

| 方法名                              | 说明                                             |
| ---------------------------------- | ----------------------------------------------- |
| connect                            | MQTT连接                                         |
| reconnect                          | 重新连接                                          |
| disConnect                         | 断开连接                                          |
| publish                            | 发布MQTT消息                                      |
| subscribe                          | 订阅MQTT主题                                      |
| unSubscribe                        | 取消订阅MQTT主题                                  |
| initOTA                            | 初始化OTA功能                                     |
| reportCurrentFirmwareVersion       | 上报设备当前版本信息到后台服务器                      |
| reportOTAState                     | 上报设备升级状态到后台服务器                         |
| mLog                               | 生成一条设备日志                                   |
| uploadLog                          | 触发一次日志上传                                   |
| subscribeRRPCTopic                 | 订阅RRPC主题                                      |
| getConnectStatus                   | 获取当前连接状态                                   |
| setBufferOpts                      | 设置断连状态buffer缓冲区                           |
| subscribeBroadcastTopic            | 订阅广播主题                                      |

### MQTT 网关接口
对于不具备直接接入以太网网络的设备，可先接入本地网关设备的网络，利用网关设备的通信功能，代理设备接入腾讯云物联网通信IoT Hub平台。对于局域网中加入或退出网络的子设备，需通过平台进行绑定或解绑操作。 注：当子设备发起过上线，后续只要网关链接成功，后台就会显示子设备在线，除非设备发起下线操作。 MQTT网关的相关接口定义在[TXGatewayConnection](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/iot_core/src/main/java/com/tencent/iot/hub/device/android/core/gateway/TXGatewayConnection.java)类中，介绍如下：

| 方法名                              | 说明                                             |
| ---------------------------------- | ----------------------------------------------- |
| connect                            | 网关MQTT连接                                      |
| reconnect                          | 网关MQTT重连                                      |
| disConnect                         | 断开网关MQTT连接                                  |
| publish                            | 发布MQTT消息                                      |
| subscribe                          | 订阅MQTT主题                                      |
| unSubscribe                        | 取消订阅MQTT主题                                  |
| getConnectStatus                   | 获取当前连接状态                                   |
| setBufferOpts                      | 设置断连状态buffer缓冲区                           |
| getSubdevStatus                    | 获取子设备当前连接状态                              |
| setSubdevStatus                    | 设置子设备连接状态                                 |
| gatewaySubdevOffline               | 子设备下线                                        |
| gatewaySubdevOnline                | 子设备上线                                        |
| gatewayBindSubdev                  | 子设备绑定                                        |
| gatewayUnbindSubdev                | 子设备解绑                                        |
| getGatewaySubdevRealtion           | 查询设备拓扑关系                                   |

### 设备影子接口
如果需要支持设备影子功能，需使用[TXShadowConnection](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/iot_core/src/main/java/com/tencent/iot/hub/device/android/core/shadow/TXShadowConnection.java)类中的方法，介绍如下：

| 方法名                              | 说明                                             |
| ---------------------------------- | ----------------------------------------------- |
| connect                            | MQTT连接                                         |
| disConnect                         | 断开连接                                          |
| publish                            | 发布MQTT消息                                      |
| subscribe                          | 订阅MQTT主题                                      |
| unSubscribe                        | 取消订阅MQTT主题                                  |
| getConnectStatus                   | 获取当前连接状态                                   |
| setBufferOpts                      | 设置断连状态buffer缓冲区                           |
| update                             | 更新设备影子文档                                   |
| get                                | 获取设备影子文档                                   |
| reportNullDesiredInfo              | 更新delta信息后，上报空的desired信息                |
| registerProperty                   | 注册当前设备的设备属性                              |
| unRegisterProperty                 | 取消注册当前设备的指定属性                           |

### 动态注册接口
如果需要支持动态注册功能，需使用[TXMqttDynreg](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/iot_core/src/main/java/com/tencent/iot/hub/device/android/core/dynreg/TXMqttDynreg.java)类中的方法，介绍如下：

| 方法名                              | 说明                                             |
| ---------------------------------- | ----------------------------------------------- |
| doDynamicRegister                  | 动态注册接口                                      |

[TXMqttDynregCallback](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/iot_core/src/main/java/com/tencent/iot/hub/device/android/core/dynreg/TXMqttDynregCallback.java)为动态注册回调接口，介绍如下：

| 方法名                              | 说明                                                 |
| ---------------------------------- | ----------------------------------------------------|
| onGetDevicePSK                     | 动态注册认证成功回调 设备密钥                            |
| onGetDeviceCert                    | 动态注册认证成功回调 设备证书内容字符串 和 设备私钥内容字符串 |
| onFailedDynreg                     | 动态注册认证失败回调                                    |
