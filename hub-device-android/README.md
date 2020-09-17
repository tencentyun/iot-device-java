# 腾讯物联云SDK
腾讯物联云 SDK 依靠安全且性能强大的数据通道，为物联网领域开发人员提供终端(如传感器, 执行器, 嵌入式设备或智能家电等等)和云端的双向通信能力。

## SDK接入指南
SDK支持远程maven依赖，以及本地源码依赖，详细接入步骤请参考 [编译环境及SDK接入说明]()

## Demo示例体验
hub-demo 工程 module 即为 hub-device-android SDK 的示例工程，关于示例中各功能的详细说明，请参考 [hub-device-android示例Demo目录索引]()

## SDK API及参数说明
SDK API接口及参数说明请参考docs目录下的[SDK API及参数说明]()

## 常见问题

* 使用 Android SDK 进行 MQTT 连接时，提示“错误的用户名或者密码”。
如果确认设备参数（ProductId、DeviceName、DevPsk）都配置正确的话，即可检查一下测试设备的系统时间是否正确，例如，使用 adb shell date 查看 Android 设备的系统时间。

* 使用 Android SDK 进行 MQTT 连接时，日志抛出javax.net.ssl.SSLHandshakeException:异常
请求还没有连通，使用证书认证时证书校验出了问题，用户可以检查一下证书内容以及是否有正确传入。

* 设备为何一直上下线？
物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作。
