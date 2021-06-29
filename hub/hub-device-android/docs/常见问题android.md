## 常见问题

#### 设备端与云端通信内容，在控制台内容日志中的内容显示base64的编码，为什么没有显示传递的json格式的数据？

检查一下创建产品的时候数据格式是不是设置的自定义格式，如果是自定义格式的产品，数据传输到云端后会进行base编码。如果需要显示json格式的数据在创建产品的时候数据格式请选择json格式。

#### 物联网通信有示例Demo吗

Android的示例Demo在github仓库中 [hub-demo对应的App模块](../../hub-android-demo)。

#### Android的示例Demo中PRODUCT_ID，DEVICE_NAME，DEVICE_PSK需要填什么？

PRODUCT_ID，DEVICE_NAME，DEVICE_PSK对应的填写的参数请参考 [基于TCP的MQTT设备接入介绍填写认证连接设备的参数部分](../../hub-device-android/docs/基于TCP的MQTT设备接入.md#填写认证连接设备的参数)

#### android的SDK中使用抛出javax.net.ssl.SSLHandshakeException异常，如何解决？

此异常是证书握手出现异常，请仔细检查传入的证书和密钥是否和设备是对应的，传入的证书和密钥的内容是否正确。

#### 使用 Android SDK 进行 MQTT 连接时，提示“错误的用户名或者密码”，如何解决？

如果确认设备参数（PRODUCT_ID、DEVICE_NAME、DEVICE_PSK）都配置正确的话，即可检查一下测试设备的系统时间是否正确，例如，使用 adb shell date 查看 Android 设备的系统时间。

#### 设备为何一直上下线？

物联网接入层有设备互踢的逻辑，如果是用同一个设备 ID 在不同地方登录，会导致其中一方被另一方踢下线。因此发现设备一直上下线时，需要确认是否有不同的人或者多线程在使用同一个设备 ID 执行登录操作。

#### 频繁的在onConnectionLost回调已断开连接（32109）

onConnectionLost回调已断开连接（32109），表示设备已断开连接，频繁的在onConnectionLost回调抛出已断开连接（32109）错误，是因为同一个设备ID多次登录，被认为其中一方被另一方踢下线，在控制台的云日志中也可以查看到互踢的日志(Device kicked)。因连接connect是异步的，是否连接成功需要检查是否回调了onConnectCompleted，且status为OK，设备下线时成功回调了onDisconnectCompleted后，再去调用connect的方法创建新的一次的连接，使设备上线。

#### 控制台发送消息给设备，设备收不到消息。

可以在控制台的云日志中查看下发送消息的情况，如显示设备断开连接device offline，则检查一下设备是否在线，如显示no subscriber，则是因为设备未订阅发送的Topic。如果发送的是QoS1的消息，设备如果没有回复ack(sdk内部处理)，后台会再次重新发送，但是QoS0的消息如果没有回复ack，后台就不会再重新发送消息。

#### 如果设备离线，之后又重新上线，是否能收到离线中发送的消息。

连接的时候需要设置。MqttConnectOptions中setCleanSession为false，并且发送的消息为QoS1，重新上线订阅过Topic后即可收到累积的消息，且存储消息但设备最多150条，最多存储24小时。

#### 调用sdk连接时，抛出错误代理程序不可用（3） 。

连接MQTT时传入的设备信息不匹配，需要检查一下传入的设备信息（产品ID、设备名称、设备密钥或设备证书私钥）。

#### 编译错误Could not determine java version from '55'。

该错误是sdk中依赖的两个库（bcprov-jdk15on和bcpkix-jdk15on）的版本和开发环境jdk不兼容引起的，可以降低下这两个库的版本。
```
dependencies {
...
    implementation 'org.bouncycastle:bcprov-jdk15on:1.57'
    implementation 'org.bouncycastle:bcpkix-jdk15on:1.57'
    implementation ('com.tencent.iot.hub:hub-device-android-core:x.x.x') {//x.x.x为引入sdk的版本号
        exclude group: 'org.bouncycastle'
    }
...
}
```

#### 如何设置心跳间隔。
options.setKeepAliveInterval(240) 这里240的单位是秒，服务端会取客户端设置的keepalive*1.5最大间隔来判断有没有收到心跳包，是否要断开与客户端的连接。

#### 如何设置是否需要自动重连。
options.setAutomaticReconnect(true);

#### onConnectCompleted回调中status为ERROR连接失败，msg为MqttException (0) - javax.net.ssl.SSLHandshakeException如何排查。
如果密钥认证log中连接的ssl://${ProductId}.iotcloud.tencentdevices.com:8883，需要添加options.setSocketFactory(AsymcSslUtils.getSocketFactory());配置握手设置。

#### 抛出异常MqttClient connect failed 指定的 SocketFactory 类型与代理程序 URI 不匹配 (32105)。
如果密钥认证log中连接的tcp://${ProductId}.iotcloud.tencentdevices.com:1883，去掉options.setSocketFactory(AsymcSslUtils.getSocketFactory());tcp不需要配置握手设置。

#### 动态注册时，返回code:1021,message: Device has been activated。
此错误说明动态注册传入的设备信息对应的设备已经被激活过了，所以不能再动态注册已经激活过的设备了，可以在控制台中删掉该设备后，再重新通过动态注册获取设备密钥或证书的信息。

#### MQTT连接时，报java.net.UnknownHostException
此错误是由于客户使用的v3.2.0 SDK里的服务器域名(iotcloud-mqtt.gz.tencentdevices.com)是旧的，从SDK v3.2.1版本开始，使用了新的域名${ProductId}.iotcloud.tencentdevices.com。解决方法：更新[SDK版本](https://github.com/tencentyun/iot-device-java/releases)；注：从产品经理处获取到"老版本SDK是仍可使用旧域名，对外提供新的域名"，所以该问题也有可能是本地DNS出问题导致host不能被正确解析。

#### SDK需要配置的哪些混淆规则?
在proguard-rules.pro文件里添加如下规则，将SDK相关类加入不混淆名单
```
-keep class org.apache.log4j.** { *; }
-keep class de.mindpipe.android.logging.log4j.** { *; }
-keep class org.eclipse.paho.client.mqttv3.** {*;}
-keepclasseswithmembers class org.eclipse.paho.** {*;}
```

#### 配了混淆规则后报错：java.lang.IllegalArgumentException: no NetworkModule installed for scheme
在项目根目录下的[build.gradle](../../../build.gradle)文件中查看gradle插件的版本号 'com.android.tools.build:gradle:x.x.x'，建议使用3.6以上的版本

