## 常见问题

#### 设备端与云端通信内容，在控制台内容日志中的内容显示base64的编码，为什么没有显示传递的json格式的数据？

检查一下创建产品的时候数据格式是不是设置的自定义格式，如果是自定义格式的产品，数据传输到云端后会进行base编码。如果需要显示json格式的数据在创建产品的时候数据格式请选择json格式。

#### 物联网通信有示例Demo吗

Android的示例Demo在github仓库中 [hub-demo对应的App模块](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-android/hub-demo)。

#### Android的示例Demo中PRODUCT_ID，DEVICE_NAME，DEVICE_PSK需要填什么？

PRODUCT_ID，DEVICE_NAME，DEVICE_PSK对应的填写的参数请参考 [基于TCP的MQTT设备接入介绍填写认证连接设备的参数部分](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/docs/基于TCP的MQTT设备接入.md#填写认证连接设备的参数)

#### android的SDK中使用抛出javax.net.ssl.SSLHandshakeException异常，如何解决？

此异常是证书握手出现异常，请仔细检查传入的证书和密钥是否和设备是对应的，传入的证书和密钥的内容是否正确。

#### 使用 Android SDK 进行 MQTT 连接时，提示“错误的用户名或者密码”，如何解决？

如果确认设备参数（PRODUCT_ID、DEVICE_NAME、DEVICE_PSK）都配置正确的话，即可检查一下测试设备的系统时间是否正确，例如，使用 adb shell date 查看 Android 设备的系统时间。

#### 设备为何一直上下线？

物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作。


