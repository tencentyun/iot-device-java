 * 1 [准备开发环境](#准备开发环境)
 * 2 [创建一个新的Android工程](#创建一个新的Android工程)
 * 3 [引用方式](#引用方式)
 * 4 [根据用户业务逻辑开发相关功能](#根据用户业务逻辑开发相关功能)

## 准备开发环境
SDK Demo示例中使用的是Android Studio版本为4.0.1，gradle版本为6.3。
[Android Studio](https://developer.android.com/studio) 可在Android developers官网下载，Android开发教程，请参考Android developers官网文档。

## 创建一个新的Android工程

## 引用方式
- 集成 SDK 方式
 -  依赖 maven 远程构建
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-link-android:1.0.0'
    }
    ```
 -  依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](https://github.com/tencentyun/iot-device-java/blob/master/explorer-device-android/explorer-demo/build.gradle)**，使应用模块依赖 [iot_explorer](https://github.com/tencentyun/iot-device-java/tree/master/explorer-device-android/iot_explorer)源码，示例如下：
     ```gr
    dependencies {
        implementation project(':explorer-device-android:iot_explorer')
    }
     ```

## 根据用户业务逻辑开发相关功能
用户调用各个功能API接口可以参考SDK Demo中对[API的使用示例]()。
