简体中文 | [English](docs/en)

* [IoT Explorer TME Android SDK开发指南](#IoT-Explorer-TME-Android-SDK开发指南)
  * [引用方式](#引用方式)
  * [API说明](#API说明)
     *  [explorer-device-tme SDK 关键接口](#explorer-device-tme-SDK-关键接口)
     *  [explorer-device-android SDK 设计说明](#explorer-device-android-SDK-设计说明)
     *  [explorer-device-tme SDK 设计说明](#explorer-device-tme-SDK-设计说明)
     *  [explorer-device-tme SDK 回调callback 设计说明](#explorer-device-tme-SDK-回调callback-设计说明)

# IoT Explorer TME Android SDK开发指南

本文主要描述腾讯云物联网开发平台设备端IoT Explorer Android-SDK中接入TME(腾讯音乐娱乐集团)的酷狗全生态方案SDK的开发指南 。

## 引用方式

1、集成 SDK 方式
 - 依赖本地sdk源码 构建
    修改应用模块的 **[build.gradle](../device-android-demo/build.gradle)**，使应用模块依赖 [explorer-device-tme](../explorer-device-tme)源码，示例如下：

    ```gr
    dependencies {
        implementation project(':explorer:explorer-device-tme')
    }
    ```
    注：**需要集成该SDK请线下联系**，Demo示例工程使用的是 依赖本地 explorer-device-tme 的 sdk源码 构建方式。

2、集成 SDK 时的注意点

（1）在build.gradle文件中添加如下配置：
```
//目前sdk仅提供armeabi-v7a和x86两种so,加上abiFilters以防某些情况下出现加载不了so库的问题
android {
    ndk {
        abiFilters 'armeabi-v7a', 'x86'
    }
    if (findProject(':explorer:explorer-device-tme') != null) {
        api project(':explorer:explorer-device-tme')
    }

    implementation 'io.reactivex.rxjava2:rxjava:2.2.10'
    implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
    implementation 'com.squareup.retrofit2:retrofit:2.6.0'
    implementation 'com.squareup.retrofit2:adapter-rxjava2:2.6.0'
    def room_version = "2.2.5"
    implementation "androidx.room:room-runtime:$room_version"
    annotationProcessor "androidx.room:room-compiler:$room_version"
    implementation "androidx.room:room-rxjava2:$room_version"
}
```

（2）在Android6.0以上系统时，以下需求权限需要动态申请，请确保已授权
```
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

（3）在Application#onCreate里调用UltimateTv#onApplicationCreate()
```
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //调用sdk的onAppcationCreate
        UltimateTv.getInstance().onAppcationCreate(this);
        ...
    }
    ...
}
```

## API说明

### explorer-device-tme SDK 关键接口

1、获取专辑信息 UltimateSongApi#getAlbumInfoList(String albumId, int page, int size)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| albumId | String | 专辑id |
| page | int | 页码 |
| size | int | 页面数量 |

| 返回值 | 描述 |
|:-|:-|
| Observable<Response<AlbumInfo>> | 专辑信息列表 |

2、获取歌单内歌曲列表 UltimateSongApi#getSongList(String playlistId, int page, int size)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| playlistId | String | 歌单id |
| page | int | 页码 |
| size | int | 页面数量 |

| 返回值 | 描述 |
|:-|:-|
| Observable<Response<SongList>> | 歌单内歌曲列表 |

3、获取每日推荐 UltimateSongApi.getDailyRecommendList()

| 返回值 | 描述 |
|:-|:-|
| Observable<Response<SongList>> | 每日推荐列表 |

4、获取首发新歌 UltimateSongApi.getFirstPublishSongList(int page, int size, int topId)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| page | int | 页码 |
| size | int | 页面数量 |
| topId| int | 榜单id 1:华语，2:欧美，3:韩语，4:日语 |

| 返回值 | 描述 |
|:-|:-|
| Observable<Response<SongList>> | 歌曲列表 |


5、歌曲播放 UltimateSongPlayer#play(List<Song> songs, int position, boolean autoStart);

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| songs | List<Song> | 要播放的歌曲列表 |
| position | int | 要播放列表里的第几首 |
| autoStart | boolean | 是否自动播放 |

6、歌曲暂停 UltimateSongPlayer.getInstance().pause()

7、切歌上一首 UltimateSongPlayer.getInstance().previous()

8、切歌下一首 UltimateSongPlayer.getInstance().next()

9、设置播放模式 UltimateSongPlayer#setPlayMode(int playMode)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| playMode | int | 播放模式 顺序播放PLAY_MODE_CYCLE、单曲循环PLAY_MODE_SINGLE、随机播放PLAY_MODE_RANDOM|

10、设置播放音质 UltimateSongPlayer#changeQuality(int songQuality)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| songQuality | int | 播放音质 标准QUALITY_STANDARD、高清QUALITY_HIGH、超清QUALITY_SUPER |

11、调整播放进度 UltimateSongPlayer#seekTo(int positionMs)

| 参数 | 类型 | 描述 |
|:-|:-|:-|
| positionMs | int | 播放进度，单位毫秒 |

**使用详情可参见** [IoT Explorer Demo](../device-android-demo/src/main/java/com/tencent/iot/explorer/device/tme/TmeMainActivity.java)

### explorer-device-android SDK 设计说明

explorer-device-android 请参考 [SDK API及参数说明.md](../explorer-device-android/docs/zh/SDK%20API及参数说明.md)

### explorer-device-tme SDK 设计说明

| 类名                     | 功能                                         |
| ----------------------- | -------------------------------------------- |
| TmeDataTemplate      | 酷狗全生态方案数据模板基本功能                   |
| TmeTemplateClient    | 实现直连设备根据酷狗全生态方案数据模板连接物联网开发平台|

#### TmeDataTemplate

TmeDataTemplate 继承自 TXDataTemplate类

```
/**
 * 通过IoT Explorer后台获取酷狗SDK的User Info
 */
public Status requestUserInfo()
```

#### TmeTemplateClient

TmeTemplateClient 继承自 TXMqttConnection 类

```
/**
 * 是否已经连接物联网开发平台
 * @return 是 、 否
 */
public boolean isConnected()

/**
 * 订阅数据模板相关主题
 * @param topicId 主题ID
 * @param qos QOS等级
 * @return 发送请求成功时返回Status.OK;
 */
public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos)

/**
 * 取消订阅数据模板相关主题
 * @param topicId 主题ID
 * @return 发送请求成功时返回Status.OK;
 */
public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId)

/**
 * 属性上报
 * @param property 属性的json
 * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyReport(JSONObject property, JSONObject metadata)

/**
 * 获取状态
 * @param type 类型
 * @param showmeta 是否携带showmeta
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyGetStatus(String type, boolean showmeta)

/**
 * 设备基本信息上报
 * @param params 参数
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyReportInfo(JSONObject params)

/**
 * 清理控制信息
 * @return 发送请求成功时返回Status.OK;
 */
public Status propertyClearControl()

/**
 * 单个事件上报
 * @param eventId 事件ID
 * @param type 事件类型
 * @param params 参数
 * @return 发送请求成功时返回Status.OK;
 */
public Status eventSinglePost(String eventId, String type, JSONObject params)

/**
 * 多个事件上报
 * @param events 事件集合
 * @return 发送请求成功时返回Status.OK;
 */
public Status eventsPost(JSONArray events)
```

### explorer-device-tme SDK 回调callback 设计说明

AuthCallback SDK认证回调callback说明如下：

| 回调接口 | 功能 |
| ----------------------- | ---------- |
| expired()                 | token 过期 |
| refreshed()               | token 刷新 |

SongPlayStateListener 歌曲播放状态变化的监听

| 回调接口 | 功能 |
| ----------------------- | ---------- |
| onBufferingEnd()                 | 缓冲结束 |
| onBufferingStart()               | 缓冲开始 |
| onBufferingUpdate()              | 缓冲进度变化 |
| onCompletion()                   | 播放结束 |
| onError(int what, String msg)    | 播放出错 |
| onPause()                        | 暂停 |
| onPlay()                         | 开始播放 |
| onPrepared()                     | 准备完成 |
| onSeekComplete()                 | seek完成 |

## 设备与用户绑定说明

Android设备通常具备丰富的人机交互界面（屏幕/键盘），用户可以直接输入 SSID/PSW 进行连接入网。

可使用`连连APP/小程序`扫描由以下接口生成的二维码，建立用户与设备之间的绑定关系。

```
explorer-device-android TXMqttConnection 类 的接口
/**
 * 生成绑定设备的二维码字符串
 * @return 生成的绑定设备的二维码字符串;
 */
public String generateDeviceQRCodeContent()

/**
 * 生成支持微信扫一扫跳转连连小程序的绑定设备的二维码字符串
 * @return 生成的绑定设备的二维码字符串;
 */
public String generateDeviceWechatScanQRCodeContent()
```