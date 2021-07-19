## 常见问题

#### 设备端与云端通信内容，在控制台内容日志中的内容显示base64的编码，为什么没有显示传递的json格式的数据？

检查一下创建产品的时候数据格式是不是设置的自定义格式，如果是自定义格式的产品，数据传输到云端后会进行base编码。如果需要显示json格式的数据在创建产品的时候数据格式请选择json格式。

#### 物联网通信Java SDK采用maven方式引入依赖，报错cannot resolve symbol "TXMqttConnection"，如何解决？

请确认SDK对应的maven依赖地址和版本是否正确，下载下来的jar包是否完整，不完整请清除缓存，重新拉取jar包。如使用 IntelliJ IDEA 编辑器，建议点击 Invalidate and Restart 清除无效缓存并重启。

#### 物联网通信有示例Demo吗

java的示例请参考[功能文档](../READEME.md#功能文档)。

#### java的示例中mProductID，mDevName，mDevPSK需要填什么？

mProductID对应填写产品ID，mDevName对应填写设备名称，mDevPSK对应填写设备密钥（使用密钥认证方式）。如果使用证书认证方式请参考 [MqttSampleTest.java](../../hub-device-java/src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java)

#### 设备为何一直上下线？

物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作。

#### 动态注册的设备认证连接时报无权连接(5)，如何排查？

在SDK中调用动态注册对应注册成功的日志会返回 `I/TXMQTT: Dynamic register OK! onGetDevicePSK, devicePSK[**********************]` 或者 `I/TXMQTT: Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************]` 可以检查一下认证连接的时候，传入的密钥或证书是不是对应动态注册的得到的密钥或证书。

#### 是否支持通过API批量注册设备

目前android 和 java 的sdk 还不支持批量动态注册, 可参考[动态注册接口说明](https://github.com/tencentyun/iot-device-java/blob/master/hub/hub-device-android/docs/%E5%8A%A8%E6%80%81%E6%B3%A8%E5%86%8C.md)

#### 使用java1.1.0版本的sdk 不发ping消息导致设备掉线

该问题在新版本sdk已解决，请使用升级到最新的sdk。

#### Android Studio编译抛错UTF-8编码识别为GBK编码，如何修改。

关闭 android studio，打开 android studio 安装目录下的 bin 目录，找到 studio.exe.vmoptions，studio64.exe.vmoptions，使用文本编辑器打开这两个文件，添加如下内容 -Dfile.encoding=UTF-8 保存后，重新打开 Android Studio，就可以正常编译了。


#### 如何设置服务器根证书

如果是接入公有云的话，不需要单独设置CA证书，SDK内部会使用默认的CA证书；如果是接入私有云的话，可以参考[自建服务器接入](自建服务器接入.md)的自定义CA证书部分

#### 填写了设备三元组信息，但是在连接的时候报'代理程序不可用(3)'

如果所创建的产品的认证方式是密钥认证，请检查设备三元组信息（productId/deviceName/devicePsk）是否填写正确，特别注意devicePsk一般均以'=='双等号结尾，复制的时候务必复制完整；若创建的产品是证书认证方式，在设备详情页下载设备证书和设备私钥，并在调用AsymcSslUtils.getSocketFactoryByFile()获取socketFactory时传入正确的证书和私钥路径，同时TXMqttConnection构造时设备psk务必传空值。
