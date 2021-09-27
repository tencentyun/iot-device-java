 * [Preparing Development Environment](#Preparing-Development-Environment)
 * [Creating Android Project](#Creating-Android-Project)
 * [How to Import](#How-to-Import)

## Preparing Development Environment
Android Studio 4.0.1 and Gradle 6.3 are used in the SDK demo.
You can download Android Studio from [Android Developers](https://developer.android.com/studio). For Android development tutorials, please see the documentation at the Android Developers website.

## Creating Android Project

## How to Import
- SDK integration
 -  Remotely build a Gradle project through the official SDK

    Configure in the `build.gradle` of the application module. For the specific version number, please see [Latest release](https://github.com/tencentyun/iot-device-java/releases). 
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-android:x.x.x'
    }
    ```
 -  Remotely build a Gradle project through the snapshot SDK

    > We recommend you use the official SDK, as the snapshot SDK is updated silently and may involve risks.

    Configure the repository URL in `build.gradle` of the project.
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
    Configure in the `build.gradle` of the application module. For the specific version number, please see [Latest release](https://github.com/tencentyun/iot-device-java/releases) (add 1 to the last digit).
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-android:x.x.x-SNAPSHOT'
    }
    ```
 -  Depend on the local SDK source code for build
    Modify the **[build.gradle](../../device-android-demo/build.gradle)** of the application module to make it dependent on the [explorer-device-android](../../explorer-device-android) source code. Below is the sample code:
    
     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-android')
    }
     ```

Build from the local SDK source code is used in the demo.
