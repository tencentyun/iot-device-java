## 常见问题

#### 设备端与云端通信内容，在控制台内容日志中的内容显示base64的编码，为什么没有显示传递的json格式的数据？

检查一下创建产品的时候数据格式是不是设置的自定义格式，如果是自定义格式的产品，数据传输到云端后会进行base编码。如果需要显示json格式的数据在创建产品的时候数据格式请选择json格式。

#### 物联网通信Java SDK采用maven方式引入依赖，报错cannot resolve symbol "TXMqttConnection"，如何解决？

请确认SDK对应的maven依赖地址和版本是否正确，下载下来的jar包是否完整，不完整请清除缓存，重新拉取jar包。如使用 IntelliJ IDEA 编辑器，建议点击 Invalidate and Restart 清除无效缓存并重启。

#### 物联网通信有示例Demo吗

java的示例对应可以运行 [App.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/src/main/java/com/tencent/iot/hub/device/java/App.java) 和 [ShadowApp.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/src/main/java/com/tencent/iot/hub/device/java/ShadowApp.java) 的main函数去查看。

#### java的示例中mProductID，mDevName，mDevPSK需要填什么？

mProductID对应填写产品ID，mDevName对应填写设备名称，mDevPSK对应填写设备密钥（使用密钥认证方式）。如果使用证书认证方式请参考 [java认证连接部分](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-java/README.md#认证连接)

#### 设备为何一直上下线？

物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作。

#### 动态注册的设备认证连接时报无权连接(5)，如何排查？

在SDK中调用动态注册对应注册成功的日志会返回 `I/TXMQTT: Dynamic register OK! onGetDevicePSK, devicePSK[**********************]` 或者 `I/TXMQTT: Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************]` 可以检查一下认证连接的时候，传入的密钥或证书是不是对应动态注册的得到的密钥或证书。


