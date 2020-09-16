## 引用方式
- 集成 SDK 方式
  若不需要将 IoT-SDK 运行在 service 组件中，则只需要依赖 [iot_core](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-android/iot_core)
 -  依赖 maven 远程构建
    ``` gr
    dependencies {
        compile 'com.tencent.iot.hub:hub-device-android-core:3.2.0-SNAPSHOT'
        compile 'com.tencent.iot.hub:hub-device-android-service:3.2.0-SNAPSHOT'
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/build.gradle)**，使应用模块依赖 [iot_core](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-android/iot_core) 和[iot_service](https://github.com/tencentyun/iot-device-java/tree/master/hub-device-android/iot_service)源码，示例如下：
     ```gr
    dependencies {
        implementation project(':hub-device-android:iot_core')
        implementation project(':hub-device-android:iot_service')
    }
     ```

## 认证连接
编辑 [app-config.json](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/app-config.json) 文件中的配置信息，可在 [IoTMqttFragment.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) 读取对应以下数据
   ``` gr
    {
      "PRODUCT_ID":        "",
      "DEVICE_NAME":       "",
      "DEVICE_PSK":        "",
      "SUB_PRODUCT_ID":    "",
      "SUB_DEV_NAME":      "",
      "SUB_PRODUCT_KEY":   "",
      "TEST_TOPIC":        "",
      "SHADOW_TEST_TOPIC": "",
      "PRODUCT_KEY":       ""
     }
    ```
SDK提供两种认证方式，一种是PSK，一种是证书的方式。读取设备证书、私钥的接口：一是通过 AssetManager 进行读取，此时需在工程 hub-device-android/hub-demo/src/main 路径下创建 assets 目录并将设备证书、私钥放置在该目录中；二是通过 InputStream 进行读取，此时需传入设备证书、私钥的全路径信息。在[IoTMqttFragment.java](https://github.com/tencentyun/iot-device-java/blob/master/hub-device-android/hub-demo/src/main/java/com/tencent/iot/hub/device/android/app/IoTMqttFragment.java) 中设置mDevCertName证书名称，mDevKeyName私钥名称。
   ``` java
    mMqttConnection = new TXGatewayConnection(mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,mMqttLogFlag, mMqttLogCallBack, mMqttActionCallBack);
    mMqttConnection.connect(options, mqttRequest);
    ```
