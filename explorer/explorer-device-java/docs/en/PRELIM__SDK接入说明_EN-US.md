 * [How to Import](#How-to-Import)

## How to Import

**How to import**

-  Remotely build a Gradle project through the official SDK

    If you want to use JAR import for project development, you can add the following dependencies in `build.gradle` in the `module` directory. For the specific version number, please see [Latest release](https://github.com/tencentyun/iot-device-java/releases):
    ```
    dependencies {
        ...
        implementation 'com.tencent.iot.explorer:explorer-device-java:x.x.x'
    }
    ```

-  Remotely build a Maven project through the official SDK

    Add in the `pom.xml` in the project root directory. For the specific version number, please see [Latest release](https://github.com/tencentyun/iot-device-java/releases):
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.explorer</groupId>
            <artifactId>explorer-device-java</artifactId>
            <version>x.x.x</version>
        </dependency>
    </dependencies>
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
        implementation 'com.tencent.iot.explorer:explorer-device-java:x.x.x-SNAPSHOT'
    }
    ```

-  Remotely build a Maven project through the snapshot SDK

    > We recommend you use the official SDK, as the snapshot SDK is updated silently and may involve risks.

    Add in the `pom.xml` in the project root directory:
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.explorer</groupId>
            <artifactId>explorer-device-java</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <repositories>
        <repository>
            <id>snapshots</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </repository>
    </repositories>
    ```

**SDK for Java source code**

If you want to develop a project through code integration, you can download the SDK for Java source code from [Github](../../../explorer-device-java).
