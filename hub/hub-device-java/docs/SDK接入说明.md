 * [引用方式](#引用方式)

## 引用方式

**引用方式**

-  gradle 工程 正式版SDK 远程构建

    如果您想通过jar引用方式进行项目开发，可在module目录下的build.gradle中添加依赖，如下依赖，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本：
    ```
    dependencies {
        ...
        implementation 'com.tencent.iot.hub:hub-device-java:x.x.x'
    }
    ```

-  maven 工程 正式版SDK 远程构建

    在工程根目录的pom.xml中添加，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本：
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.hub</groupId>
            <artifactId>hub-device-java</artifactId>
            <version>x.x.x</version>
        </dependency>
    </dependencies>
    ```

-  gradle 工程 snapshot版SDK 远程构建

    > 建议使用正式版SDK，SNAPSHOT版本会静默更新，使用存在风险

    在工程的build.gradle中配置仓库url
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
    在应用模块的build.gradle中配置，具体版本号可参考 [Latest release](https://github.com/tencentyun/iot-device-java/releases) 版本，中间位+1
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.hub:hub-device-java:x.x.x-SNAPSHOT'
    }
    ```

-  maven 工程 snapshot版SDK 远程构建

    > 建议使用正式版SDK，SNAPSHOT版本会静默更新，使用存在风险

    在工程根目录的pom.xml中添加：
    ```
    <dependencies>
        <dependency>
            <groupId>com.tencent.iot.hub</groupId>
            <artifactId>hub-device-java</artifactId>
            <version>1.0.1-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <repositories>
        <repository>
            <id>snapshots</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </repository>
    </repositories>
    ```

**Java Sdk源码**

如果您想通过代码集成方式进行项目开发，可访问[Github](../hub-device-java)下载Java Sdk源码。
