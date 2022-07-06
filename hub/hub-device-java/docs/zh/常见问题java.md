## 常见问题

#### 设备端与云端通信内容，在控制台内容日志中的内容显示base64的编码，为什么没有显示传递的json格式的数据？

检查一下创建产品的时候数据格式是不是设置的自定义格式，如果是自定义格式的产品，数据传输到云端后会进行base编码。如果需要显示json格式的数据在创建产品的时候数据格式请选择json格式。

#### 物联网通信Java SDK采用maven方式引入依赖，报错cannot resolve symbol "TXMqttConnection"，如何解决？

请确认SDK对应的maven依赖地址和版本是否正确，下载下来的jar包是否完整，不完整请清除缓存，重新拉取jar包。如使用 IntelliJ IDEA 编辑器，建议点击 Invalidate and Restart 清除无效缓存并重启。

#### 物联网通信有示例Demo吗

java的示例请参考[功能文档](../../README.md#功能文档)。

#### java的示例中mProductID，mDevName，mDevPSK需要填什么？

mProductID对应填写产品ID，mDevName对应填写设备名称，mDevPSK对应填写设备密钥（使用密钥认证方式）。如果使用证书认证方式请参考 [MqttSampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java)

#### 设备为何一直上下线？

物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作; 断开重连逻辑是自己写的还是用的mqtt的（setAutomaticReconnect(true)），若使用的是mqtt的话无需在onConnectionLost回调里调用connect接口，否则会和mqtt自带重连逻辑冲突；使用<=3.3.12版本的sdk，若传给TXOTAImpl的TXOTACallBack为空，会在OTA升级时发生NullPointer，这种情况也会导致设备频繁上下线的问题。

#### 动态注册的设备认证连接时报无权连接(5)，如何排查？

在SDK中调用动态注册对应注册成功的日志会返回 `I/TXMQTT: Dynamic register OK! onGetDevicePSK, devicePSK[**********************]` 或者 `I/TXMQTT: Dynamic register OK!onGetDeviceCert, deviceCert[**********************] devicePriv[**********************]` 可以检查一下认证连接的时候，传入的密钥或证书是不是对应动态注册的得到的密钥或证书。

#### 是否支持通过API批量注册设备

目前android 和 java 的sdk 还不支持批量动态注册, 可参考[动态注册接口说明](动态注册.md)

#### 使用java1.1.0版本的sdk 不发ping消息导致设备掉线

该问题在新版本sdk已解决，请使用升级到最新的sdk。

#### Android Studio编译抛错UTF-8编码识别为GBK编码，如何修改。

关闭 android studio，打开 android studio 安装目录下的 bin 目录，找到 studio.exe.vmoptions，studio64.exe.vmoptions，使用文本编辑器打开这两个文件，添加如下内容 -Dfile.encoding=UTF-8 保存后，重新打开 Android Studio，就可以正常编译了。


#### 如何设置服务器根证书

如果是接入公有云的话，不需要单独设置CA证书，SDK内部会使用默认的CA证书；如果是接入私有云的话，可以参考[自建服务器接入](自建服务器接入.md)的自定义CA证书部分

#### 填写了设备三元组信息，但是在连接的时候报'代理程序不可用(3)'

如果所创建的产品的认证方式是密钥认证，请检查设备三元组信息（productId/deviceName/devicePsk）是否填写正确，特别注意devicePsk一般均以'=='双等号结尾，复制的时候务必复制完整；若创建的产品是证书认证方式，在设备详情页下载设备证书和设备私钥，并在调用AsymcSslUtils.getSocketFactoryByFile()获取socketFactory时传入正确的证书和私钥路径，同时TXMqttConnection构造时设备psk务必传空值。

#### 如何保存SDK的日志以及SDK日志的存放路径是什么？

保存日志的方法有两种：

1、在使用我们的SDK功能API前调用以下方法即可保存SDK日志：
```
Loggor.saveLogs(String path)
```

以下示例会将SDK日志保存在hub/hub-device-java.log文件中
```
public void doJob() {
    Loggor.saveLogs("hub/hub-device-java.log");
    ... //业务代码
}
```

2、在工程中配置log4j.properties文件

配置文件存放路径：
```
${parent_path}/src/main/resources/log4j.properties
```

配置文件内容示例：
```
log4j.rootLogger = debug,file

log4j.appender.file = com.tencent.iot.hub.device.java.utils.MyDailyRollingFileAppender
log4j.appender.file.File = hub/hub-device-java.log
log4j.appender.file.Append = true
log4j.appender.file.Threshold = DEBUG
log4j.appender.file.layout = org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern = %d{HH:mm:ss,SSS} [%t] %-5p %c{1} %L %x - %m%n
```
以上示例会将SDK日志文件保存在hub/hub-device-java.log文件中

#### 设备连接broker时，报以下异常：MqttException (0) - java.net.UnknownHostException: ${PRODUCT_ID}.iotcloud.tencentdevices.com]是什么问题？

检查设备网络是否正常，该异常是设备没网导致

#### 设备连接broker时，报broker unavailable错误

该错属于broker服务器报错，确认broker服务器是否宕机

#### 动态注册时，报Certificate not valid until ${日期} 错

该错出现的原因是设备本机时间不正确，请检查设备本机的时间是否正确

#### 控制台设备日志显示'FAIL，reach max limit'是什么问题？

后台对设备的连接频率有限制：5s只允许连接一次；可以排查业务代码是否存在频繁连接的操作

#### SDK的断网重连机制是怎样的？

如果重连选项设置为true（options.setAutomaticReconnect(true);），则在连接丢失的情况下，客户端将尝试重新连接服务器。 一开始会等待1秒尝试重新连接，如果重新连接失败，延迟将会双倍直到2分钟，此后重连失败延迟将保持2分钟不变