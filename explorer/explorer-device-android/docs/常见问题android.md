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

#### 远程依赖 explorer-device-android sdk，抛出 Execution failed for task ':app:compileStagDebugJavaWithJavac'.  Could not resolve all files for configuration ':app:stagDebugCompileClasspath'. Failed to transform explorer-device-android-3.3.0.jar如何排查 ？

工程依赖了 implementation 'com.tencent.iot.explorer:explorer-device-android:3.3.0' ，此处却抛出找不到explorer-device-android-3.3.0.jar文件，可能是开发的环境已经混乱，此时建议完全删除下android studio以及相关环境和gradle配置，重新下载android studio以及配置环境信息后，同步gradle添加依赖。

#### 动态注册回调onFailedDynreg 错误信息为{"code":1010,"message":"Check signature failed"如何排查

此问题很可能是填写了错误的ProductSecret，建议和云控制台上设备信息比对检查下调用动态注册时传入的三个参数，productId，deviceName，ProductSecret。

#### 如何保存SDK的日志以及SDK日志的存放路径是什么？

在使用我们的SDK功能API之前调用`TXLogImpl.init(Context context)`方法或者`TXLogImpl.init(Context context, int duration)`方法，其中`TXLogImpl.init(Context context)`方法可将SDK的日志保存至/sdcard/tencent/包名(context对应的包名)/iot_${yyyyMMdd}.log文件中，默认保存近7天的日志文件；`TXLogImpl.init(Context context, int duration, String path)`方法支持自定义保存近${duration}天的日志。

假设应用包名为：com.tencent.iot.explorer.demo，在调用SDK功能API前添加`TXLogImpl.init(this)`，那么SDK日志将会保存在/sdcard/tencent/com/tencent/iot/explorer/demo/iot_${yyyyMMdd}.log文件中
```
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trtc_main);
        TXLogImpl.init(this);
        ... //业务代码
}
```