## 常见问题

#### 物联网开发平台有示例Demo吗

Android的示例Demo在github仓库中 [device-android-demo模块](../../device-android-demo)。

#### Android的示例Demo中PRODUCT_ID，DEVICE_NAME，DEVICE_PSK需要填什么？

PRODUCT_ID，DEVICE_NAME，DEVICE_PSK对应的填写的参数请参考 [基于TCP的MQTT设备接入介绍填写认证连接设备的参数部分](../../explorer-device-android/docs/控制设备上下线.md#填写认证连接设备的参数)

#### android的SDK中使用抛出javax.net.ssl.SSLHandshakeException异常，如何解决？

此异常是设备进行证书认证握手出现异常，请仔细检查传入的证书和密钥是否和设备是对应的，传入的证书和密钥的内容是否正确。

#### 使用 Android SDK 进行 MQTT 连接时，提示“错误的用户名或者密码”，如何解决？

如果确认设备参数（PRODUCT_ID、DEVICE_NAME、DEVICE_PSK）都配置正确的话，即可检查一下测试设备的系统时间是否正确，例如，使用 adb shell date 查看 Android 设备的系统时间。

#### 设备为何一直上下线？

物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作。

#### 物联网开发平台控制台上固件升级创建固件升级任务时，升级确认类型中，静默升级和用户确认升级有什么区别？

如果选择了静默升级，创建了升级任务设备就会收到这个固件升级消息。
如果选择用户确认升级，可以通过我们的小程序来提示客户设备有新的固件可以升级，从小程序上确认升级的话，设备就会收到这个固件升级消息了。


