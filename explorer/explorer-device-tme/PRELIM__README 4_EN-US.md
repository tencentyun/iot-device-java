* [IoT Explorer TME SDK for Android Development Guide](#IoT-Explorer-TME-SDK-for-Android-Development-Guide)
  * [How to import](#How-to-import)
  * [API description](#API-description)
     *  [Key APIs of explorer-device-tme SDK](#Key-APIs-of-explorer-device-tme-SDK)
     *  [explorer-device-android SDK design description](#explorer-device-android-SDK-design-description)
     *  [explorer-device-tme SDK design description](#explorer-device-tme-SDK-design-description)
     *  [explorer-device-tme SDK callback design description](#explorer-device-tme-SDK-callback-design-description)

# IoT Explorer TME SDK for Android Development Guide

This document describes how to connect the KuGou ecosystem solution SDK of Tencent Music Entertainment (TME) Group to the IoT Explorer device SDK for Android.

## How to import

1. SDK integration
 - Depend on the local SDK source code for build
    Modify the **[build.gradle](../device-android-demo/build.gradle)** of the application module to make it dependent on the [explorer-device-tme](../explorer-device-tme) source code. Below is the sample code:

    ```gr
    dependencies {
        implementation project(':explorer:explorer-device-tme')
    }
    ```
    Note: **if you need to integrate the SDK, please contact us.** Build from the local explorer-device-tme SDK source code is used in the demo.

2. Considerations for SDK integration

(1) Add the following configuration to the `build.gradle` file:
```
// Currently, the SDK provides only `armeabi-v7a` and `x86`, as well as `abiFilters` in case where .so libraries cannot be loaded
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

(2) On Android 6.0 or above, you need to dynamically apply for the following required permissions. Please make sure that they are granted.
```
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

(3) Call `UltimateTv#onApplicationCreate()` in `Application#onCreate`.
```
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Call `onAppcationCreate` in the SDK
        UltimateTv.getInstance().onAppcationCreate(this);
        ...
    }
    ...
}
```

## API description

### Key APIs of explorer-device-tme SDK

1. Get album information: UltimateSongApi#getAlbumInfoList(String albumId, int page, int size)

| Parameter | Type | Description |
|:-|:-|:-|
| albumId | String | Album ID |
| page | int | Page number |
| size | int | Number of pages |

| Returned Value | Description |
|:-|:-|
| Observable<Response<AlbumInfo>> | Album information list |

2. Get the list of songs in playlist: UltimateSongApi#getSongList(String playlistId, int page, int size)

| Parameter | Type | Description |
|:-|:-|:-|
| playlistId | String | Playlist ID |
| page | int | Page number |
| size | int | Number of pages |

| Returned Value | Description |
|:-|:-|
| Observable<Response<SongList>> | List of songs in playlist |

3. Get daily recommendations: UltimateSongApi.getDailyRecommendList()

| Returned Value | Description |
|:-|:-|
| Observable<Response<SongList>> | List of daily recommendations |

4. Get newly released songs: UltimateSongApi.getFirstPublishSongList(int page, int size, int topId)

| Parameter | Type | Description |
|:-|:-|:-|
| page | int | Page number |
| size | int | Number of pages |
| topId| int | Chart ID. 1: Chinese; 2: European and US; 3: Korean; 4: Japanese |

| Returned Value | Description |
|:-|:-|
| Observable<Response<SongList>> | List of songs |


5. Play song: UltimateSongPlayer#play(List<Song> songs, int position, boolean autoStart);

| Parameter | Type | Description |
|:-|:-|:-|
| songs | List<Song> | List of songs to be played |
| position | int | Place of the song to be played in the playlist |
| autoStart | boolean | Whether to play automatically |

6. Pause song: UltimateSongPlayer.getInstance().pause()

7. Go to previous song: UltimateSongPlayer.getInstance().previous()

8. Go to next song: UltimateSongPlayer.getInstance().next()

9. Set playback mode: UltimateSongPlayer#setPlayMode(int playMode)

| Parameter | Type | Description |
|:-|:-|:-|
| playMode | int | Playback mode. PLAY_MODE_CYCLE: loop; PLAY_MODE_SINGLE: repeat; PLAY_MODE_RANDOM: shuffle |

10. Set playback sound quality: UltimateSongPlayer#changeQuality(int songQuality)

| Parameter | Type | Description |
|:-|:-|:-|
| songQuality | int | Playback sound quality. QUALITY_STANDARD: SD; QUALITY_HIGH: HD; QUALITY_SUPER: FHD |

11. Adjust playback progress: UltimateSongPlayer#seekTo(int positionMs)

| Parameter | Type | Description |
|:-|:-|:-|
| positionMs | int | Playback progress in milliseconds |

**For more information, please see **[IoT Explorer Demo](../device-android-demo/src/main/java/com/tencent/iot/explorer/device/tme/TmeMainActivity.java).

### explorer-device-android SDK design description

For more information on explorer-device-android SDK, please see [SDK API and Parameter Descriptions.md](../explorer-device-android/docs/SDK-API-and-Parameter-Descriptions.md).

### explorer-device-tme SDK design description

| Class                     | Feature                                         |
| ----------------------- | -------------------------------------------- |
| TmeDataTemplate      | Implements the basic features of the KuGou ecosystem solution data template                   |
| TmeTemplateClient    | Connects directly connected device to IoT Explorer based on KuGou ecosystem solution data template |

#### TmeDataTemplate

`TmeDataTemplate` is inherited from the `TXDataTemplate` class.

```
/**
 * Get the `User Info` of the KuGou SDK through the IoT Explorer backend
 */
public Status requestUserInfo()
```

#### TmeTemplateClient

`TmeTemplateClient` is inherited from the `TXMqttConnection` class.

```
/**
 * Check connection to IoT Explorer
 * @return Yes or no
 */
public boolean isConnected()

/**
 * Subscribe to topic related to data template
 * @param topicId Topic ID
 * @param qos QoS level
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos)

/**
 * Unsubscribe from topic related to data template
 * @param topicId Topic ID
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId)

/**
 * Report attribute
 * @param property Attribute JSON
 * @param metadata Attribute metadata. Currently, it contains only the corresponding timestamps of the attributes
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status propertyReport(JSONObject property, JSONObject metadata)

/**
 * Get status
 * @param type Type
 * @param showmeta Whether to carry `showmeta`
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status propertyGetStatus(String type, boolean showmeta)

/**
 * Report basic device information
 * @param params Parameter
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status propertyReportInfo(JSONObject params)

/**
 * Clear control information
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status propertyClearControl()

/**
 * Report one event
 * @param eventId Event ID
 * @param type Event type
 * @param params Parameter
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status eventSinglePost(String eventId, String type, JSONObject params)

/**
 * Report multiple events
 * @param events Event set
 * @return `Status.OK` will be returned when the request is sent successfully;
 */
public Status eventsPost(JSONArray events)
```

### explorer-device-tme SDK callback design description

`AuthCallback` SDK authentication callbacks are as described below:

| Callback API | Feature |
| ----------------------- | ---------- |
| expired()                 | Token expired |
| refreshed()               | Token refreshed |

`SongPlayStateListener` is used to listen on song playback status changes

| Callback API | Feature |
| ----------------------- | ---------- |
| onBufferingEnd()                 | Buffering start |
| onBufferingStart()               | Buffering end |
| onBufferingUpdate()              | Buffering progress change |
| onCompletion()                   | Playback end |
| onError(int what, String msg)    | Playback error |
| onPause()                        | Pause |
| onPlay()                         | Play |
| onPrepared()                     | Preparations completion |
| onSeekComplete()                 | Seek completion |

## Device-User binding description

Android devices usually offer multiple interfaces for human-machine interaction (such as screen and keyboard), allowing users to access the internet simply by entering the SSID and password.

The **Tencent IoT Link application/mini program** can be used to scan the QR code generated by the following APIs to bind a user to a device.

```
`TXMqttConnection` class APIs of explorer-device-android
/**
 * Generate the QR code string used for binding device
 * @return The generated QR code string used for binding device;
 */
public String generateDeviceQRCodeContent()

/**
 * Generate the QR code string that can be scanned with WeChat for redirection to the Tencent IoT Link mini program for device binding
 * @return The generated QR code string used for binding device;
 */
public String generateDeviceWechatScanQRCodeContent()
```