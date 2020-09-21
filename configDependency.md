## SDK依赖方式

### 1、gradle工程配置SDK依赖（以hub-device-java为例）
#### 依赖正式版
在module目录下的build.gradle中添加依赖如下依赖
```
dependencies {
    ...
    implementation 'com.tencent.iot.hub:hub-device-java:1.0.1-SNAPSHOT'
}
```
#### 依赖snapshot版
1、配置Maven仓库地址，在工程根目录的build.gradle中配置url("https://oss.sonatype.org/content/repositories/snapshots")
```
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

2、在module目录下的build.gradle中添加依赖如下依赖
```
dependencies {
    ...
    implementation 'com.tencent.iot.hub:hub-device-java:1.0.1-SNAPSHOT'
}
```

### 2、maven工程配置SDk依赖（以hub-device-java为例）
#### 依赖正式版
在工程根目录的pom.xml中添加：
```
<dependencies>
    <dependency>
        <groupId>com.tencent.iot.hub</groupId>
        <artifactId>hub-device-java</artifactId>
        <version>1.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

#### 依赖snapshot版
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

注：建议使用正式版SDK，SNAPSHOT版本会静默更新，使用存在风险