 * [准备开发环境](#准备开发环境)
 * [创建一个新的Android工程](#创建一个新的Android工程)
 * [引用方式](#引用方式)

## 准备开发环境
SDK Demo 示例中使用的是 Android Studio 版本为 4.0.1，gradle 版本为 6.3。
Android Studio 可在 [Android developers 官网](https://developer.android.com/studio) 下载，Android 开发教程，请参考 Android developers 官网文档。

## 创建一个新的Android工程

## 引用方式
 -  gradle 工程正式版 SDK 远程构建

    在应用模块的 build.gradle 中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本 
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.hub:hub-device-android:x.x.x'
    }
    ```
 -  gradle 工程 snapshot 版 SDK 远程构建

    > 建议使用正式版 SDK，SNAPSHOT 版本会静默更新，使用存在风险

    在工程的 build.gradle 中配置仓库 url
    ``` gr
    allprojects {
        repositories {
            google()
            jcenter()
            maven {
                url "https://oss.sonatype.org/content/repositories/snapshots"
            }
        }
    }
    ```
    在应用模块的 build.gradle 中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本，末位+1
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.hub:hub-device-android:x.x.x-SNAPSHOT'
    }
    ```
 -  依赖本地 sdk 源码构建
    修改应用模块的 **[build.gradle](../../hub-android-demo/build.gradle)**，使应用模块依赖 [hub-device-android](../../hub-device-android) 源码，示例如下：
    
     ```gr
    dependencies {
        implementation project(':hub-device-android')
    }
     ```


Demo 示例工程使用的是依赖本地 sdk 源码构建方式。
