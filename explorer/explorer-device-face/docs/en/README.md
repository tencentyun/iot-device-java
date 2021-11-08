[简体中文](././README.md) | English

* [IoT Explorer Face Recognition SDK for Android Development Guide](#IoT-Explorer-Face-Recognition-SDK-for-Android-Development-Guide)
  * [How to import](#How-to-import)
  * [API description](#API-description)
     *  [explorer-device-android SDK design description](#explorer-device-android-SDK-design-description)
     *  [explorer-device-face SDK design description](#explorer-device-face-SDK-design-description)
     *  [explorer-device-face SDK callback design description](#explorer-device-face-SDK-callback-design-description)

# IoT Explorer Face Recognition SDK for Android Development Guide

This document describes how to connect the Offline Face Recognition SDK for Android to the IoT Explorer device SDK for Android.

## How to import

- SDK integration

If you don't need to connect the Offline Face Recognition SDK and only need to connect the `explorer-device-android` SDK, please see [Compilation Environment and SDK Connection Description.md](../explorer-device-android/docs/en/PRELIM__编译环境及SDK接入说明_EN-US.md).

 -  Remotely build a Gradle project through the official SDK

    Configure in the `build.gradle` of the application module. For the specific version number, please see [Latest release](https://github.com/tencentyun/iot-device-java/releases).
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-face:x.x.x' // Dependencies of IoT Explorer and Offline Face Recognition
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
        implementation 'com.tencent.iot.explorer:explorer-device-face:x.x.x-SNAPSHOT' // Dependencies of IoT Explorer and Offline Face Recognition
    }
    ```
 -  Depend on the local SDK source code for build
    Modify the **[build.gradle](../device-android-demo/build.gradle)** of the application module to make it dependent on the [explorer-device-face](../explorer-device-face) source code. Below is the sample code:
    
     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-face') // Dependencies of IoT Explorer and Offline Face Recognition
    }
     ```

Build from the local `explorer-device-android` and `explorer-device-face` SDK source code is used in the demo.

## API description

### explorer-device-android SDK design description

For more information on explorer-device-android SDK, please see [SDK API and Parameter Descriptions.md](../explorer-device-android/docs/en/PRELIM__SDK%20API及参数说明_EN-US.md).

### explorer-device-face SDK design description

| Class                     | Feature                                         |
| ----------------------- | -------------------------------------------- |
| TXFaceKitTemplate       | Implements the basic features of Face Recognition data template                     |
| TXFaceKitTemplateClient | Connects directly connected device to IoT Explorer based on Face Recognition data template  |
| TXResourceImpl          | Downloads resource to device based on the data from IoT Explorer's person library    |

#### TXFaceKitTemplate

`TXFaceKitTemplate` is inherited from the `TXDataTemplate` class.

| Method                         | Description                                            |
| ----------------------------- | ---------------------------------------------- |
| initResource                  | Initializes download feature for person library resource                           |
| sysEventSinglePost            | Reports one event without checking whether the construction complies with the definition in the JSON file |
| reportCurrentResourceVersion  | Reports the current version information of person library resource on device to backend server           |
| reportResourceState           | Reports the download status of person library resource on device to backend server               |
| subscribeServiceTopic         | Subscribes to `Service` topic                                 |
| unSubscribeServiceTopic       | Unsubscribes from `Service` topic                              |
| initAuth                      | Initializes the authorization of Offline Face Recognition SDK                       |


#### TXDataTemplateClient

| Method                         | Description                                  |
| ----------------------------- | ------------------------------------ |
| isConnected                   | Checks connection to IoT Explorer               |
| subscribeTemplateTopic   | Subscribes to topic related to data template     |
| subscribeServiceTopic         | Subscribes to `Service` topic                                 |
| unSubscribeTemplateTopic      | Unsubscribes from topic related to data template                |
| unSubscribeServiceTopic       | Unsubscribes from `Service` topic                              |
| propertyReport                | Reports attribute                              |
| propertyGetStatus             | Updates status                              |
| propertyReportInfo            | Reports device information                           |
| propertyClearControl          | Clears control information                           |
| eventSinglePost               | Reports one event                           |
| reportSysRetrievalResultEvent | Reports face search event                       |
| eventsPost                    | Reports multiple events                           |
| initAuth                      | Initializes the authorization of Offline Face Recognition SDK                       |
| initResource                  | Initializes download feature for person library resource                |
| reportCurrentResourceVersion  | Reports the current version information of person library resource on device to backend server |

### explorer-device-face SDK callback design description

`TXAuthCallBack` authorization callbacks are as described below:

```
    /**
     * Callback for Face Recognition SDK authentication success
     */
    public abstract void onSuccess();
    /**
     * Callback for Face Recognition SDK authentication failure
     * @param code          Authentication status code
     * @param status        Result
     */
    public abstract void onFailure(Integer code, String status);
```

`TXResourceCallBack` person library resource callbacks are as described below:

```
    /**
     * Callback for person library resource version information reporting
     * @param resultCode  Reporting result code. 0: success; others: failure
     * @param resourceList  `JSONArray` is internally loaded with the `JSONObject` in the format of `{"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"}`
     * @param resultMsg  Reporting result code description
     */
    void onReportResourceVersion(int resultCode, JSONArray resourceList, String resultMsg);

    /**
     * The person library resource file has a new version available for update
     * @param url File URL, which is used to download the latest version
     * @param md5  MD5 value, which is used for verification
     * @param version  Latest version number
     */
    boolean onLastestResourceReady(String url, String md5, String version);

    /**
     * Callback for person library resource file download progress
     * @param resourceName  Face library resource file name or resource file name without the path and with the `featureId` and file format;
     * @param percent  Download progress (0–100);
     * @param version  Version;
     */
    void onDownloadProgress(String resourceName, int percent, String version);

    /**
     * Callback for person library resource file download completion
     * @param outputFile  Name of downloaded resource file (with the entire path);
     * @param version  Version;
     */
    void onDownloadCompleted(String outputFile, String version);

    /**
     * Callback for person library resource file download failure
     * @param resourceName  Face library resource file name or resource file name without the path and with the `featureId` and file format;
     * @param errCode  Failure error code. -1: download timed out; -2: the file does not exist; -3: the signature expired; -4: verification error; -5: firmware update failed
     * @param version  Version;
     */
    void onDownloadFailure(String resourceName, int errCode, String version);

    /**
     * Callback for person library feature deletion
     * @param featureId     Feature ID (featureId)
     * @param resourceName  Resource file name without the path and with the `featureId` and file format
     */
    void onFeatureDelete(String featureId, String resourceName);

    /**
     * Callback for person library deletion
     * @param version       Face library resource file version number
     * @param resourceName  Face library resource file name
     */
    void onFaceLibDelete(String version, String resourceName);

    /**
     * Callback for offline search event saving
     * @param feature_id    Feature ID, corresponding to the person ID in the console
     * @param score         Search score
     * @param sim           Similarity to feature
     * @param timestamp     Timestamp
     */
    void onOfflineRetrievalResultEventSave(String feature_id, float score, float sim, int timestamp);
```


