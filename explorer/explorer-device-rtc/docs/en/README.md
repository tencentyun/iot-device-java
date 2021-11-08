[简体中文](././README.md) | English

* [IoT Explorer RTC SDK for Android Development Guide](#IoT-Explorer-RTC-SDK-for-Android-Development-Guide)
  * [How to import](#How-to-import)
  * [API description](#API-description)
     *  [explorer-device-android SDK design description](#explorer-device-android-SDK-design-description)
     *  [explorer-device-rtc SDK design description](#explorer-device-rtc-SDK-design-description)
     *  [explorer-device-rtc SDK callback design description](#explorer-device-rtc-SDK-callback-design-description)
  * [Call process](#Call-process)

# IoT Explorer RTC SDK for Android Development Guide

This document describes how to connect the TRTC SDK for Android to the IoT Explorer device SDK for Android.

## How to import

- SDK integration

If you don't need to connect the TRTC SDK and only need to connect the `explorer-device-android` SDK, please see [Compilation Environment and SDK Connection Description.md](../explorer-device-android/docs/en/PRELIM__编译环境及SDK接入说明_EN-US.md).

 -  Remotely build a Gradle project through the official SDK

    Configure in the `build.gradle` of the application module. For the specific version number, please see [Latest release](https://github.com/tencentyun/iot-device-java/releases). 
    ``` gr
    dependencies {
        implementation 'com.tencent.iot.explorer:explorer-device-rtc:x.x.x' // Dependencies of IoT Explorer and TRTC
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
        implementation 'com.tencent.iot.explorer:explorer-device-rtc:x.x.x-SNAPSHOT' // Dependencies of IoT Explorer and TRTC
    }
    ```
 -  Depend on the local SDK source code for build
    Modify the **[build.gradle](../device-android-demo/build.gradle)** of the application module to make it dependent on the [explorer-device-rtc](../explorer-device-rtc) source code. Below is the sample code:
    
     ```gr
    dependencies {
        implementation project(':explorer:explorer-device-rtc') // Dependencies of IoT Explorer and TRTC. Be sure to add other SDK modules that depend on each other
    }
     ```

Build from the local `explorer-device-android` and `explorer-device-rtc` SDK source code is used in the demo.

## API description

### explorer-device-android SDK design description

For more information on explorer-device-android SDK, please see [SDK API and Parameter Descriptions.md](../explorer-device-android/docs/en/PRELIM__SDK%20API及参数说明_EN-US.md).

### explorer-device-rtc SDK design description

| Class                     | Feature                                         |
| ----------------------- | -------------------------------------------- |
| TXTRTCDataTemplate      | Implements the basic features of TRTC data template                   |
| TXTRTCTemplateClient    | Connects directly connected device to IoT Explorer based on TRTC data template |

#### TXTRTCDataTemplate

`TXTRTCDataTemplate` is inherited from the `TXDataTemplate` class.

```
    /**
     * Report TRTC device call attribute
     * @param callStatus Call status. 0: idle or rejected; 1: calling; 2: busy
     * @param callType Call type. 1: audio; 2: video
     * @param userId ID of the called user. If there are multiple user IDs, separate them by ";". This parameter can also be an empty string "" indicating a group call or the string "null". Please pass in `params` and forward the data to your backend service. For more information, please visit https://cloud.tencent.com/document/product/1081/40298
     * @param agent Agent, which identifies the party making the call. You can pass in an empty string, and if you do so, `agent` will not be reported.
     * @param params User's thing model attribute JSON
     * @return `Status.OK` will be returned when the request is sent successfully. OK: succeeded; ERROR: an error occurred; ERR_JSON_CONSTRUCT: JSON construction failed; PARAMETER_INVALID: invalid topic; MQTT_NO_CONN: MQTT is not connected
     */
    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId, String agent, JSONObject params)
```


#### TXTRTCTemplateClient

`TXTRTCTemplateClient` is inherited from the `TXMqttConnection` class.

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
     * Report TRTC device call attribute
     * @param callStatus Call status. 0: idle or rejected; 1: calling; 2: busy
     * @param callType Call type. 1: audio; 2: video
     * @param userId ID of the called user. If there are multiple user IDs, separate them by ";". This parameter can also be an empty string "" indicating a group call or the string "null". Please pass in `params` and forward the data to your backend service. For more information, please visit https://cloud.tencent.com/document/product/1081/40298
     * @param agent Agent, which identifies the party making the call. You can pass in an empty string, and if you do so, `agent` will not be reported.
     * @param params User's thing model attribute JSON
     * @return `Status.OK` will be returned when the request is sent successfully;
     */
    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId, String agent, JSONObject params)

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

### explorer-device-rtc SDK callback design description

`TXTRTCCallBack` authorization callbacks are as described below:

```
    /**
     * Get the call status of RTC attribute
     *
     * @param callStatus            Call status. 0: idle or rejected; 1: calling; 2: busy
     * @param userid                User ID
     * @param agent                 Agent, which identifies the party making the call
     * @param callType              Call type. 1: audio; 2: video
     */
    public abstract void onGetCallStatusCallBack(Integer callStatus, String userid, String agent, Integer callType);

    /**
     * Get the parameter model required for `rtc` to join room
     *
     * @param room
     */
    public abstract void trtcJoinRoomCallBack(RoomKey room);
```

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

## Call process

### Video call from IoT Link application/mini program to Android device

Sequence diagram:

![UserCallDeviceUML](media/UserCallDeviceUML.png)

1. On the IoT Link application/mini program, the caller clicks **Make Video Call** in the control panel.

2. The cloud service forwards the call request from the IoT Link application/mini program over MQTT, which triggers the `onGetCallStatusCallBack` callback in `TXTRTCCallBack` on the device, where
> * The callback parameter `callStatus` is `1` (calling)
> * `userid` is the ID of the user of the IoT Link application/mini program who makes the call
> * `agent` is the agent, which identifies the party making the call
> * `callType` is the type of the call passed in step 1

After this message is received, the following APIs need to be called:
```
TRTCUIManager.getInstance().setSessionManager(new TRTCExplorerDemoSessionManager(mDataTemplateSample)); // Report the device status on the page
TRTCVideoCallActivity.startBeingCall(TRTCMainActivity.this, new RoomKey(), userid, agent);// Redirect to the corresponding video call page
```

3. When the callee clicks **Answer** on the device, the following API needs to be called:
``` 
TRTCUIManager.getInstance().didAcceptJoinRoom(TRTCCalling.TYPE_VIDEO_CALL, mSponsorUserInfo.getUserId(), mSponsorUserInfo.getAgent());
```
This notifies the caller on the IoT Link application/mini program that the callee on the device has accepted the call request.

4. The cloud service forwards the call request from the caller on the IoT Link application/mini program accepted by the callee on the device over WebSocket, and the IoT Link application/mini program continues to request the room entry parameters and enters the corresponding video room.

5. The cloud service forwards the room entry by the IoT Link application/mini program over MQTT, which triggers the `trtcJoinRoomCallBack` callback in `TXTRTCCallBack` on the device, where
> * The callback parameter `room` is the corresponding room parameter.

After this message is received, the following API needs to be called:
``` 
TRTCUIManager.getInstance().joinRoom(mCallType, "", room); // Enter the room and update the video call attribute to `busy`.
``` 

6. The caller on the IoT Link application/mini program and the callee on the device make the video call.

7. After the caller hangs up or receives the ``` public void onUserLeave(final String userId) ``` callback for hangup by the callee, the current audio/video page will be exited, and the following API will be called: 
``` 
TRTCUIManager.getInstance().didExitRoom(TRTCCalling.TYPE_VIDEO_CALL, mSponsorUserInfo.getUserId()); // Update the video call attribute to `idle`
``` 

**The process in which the caller on the IoT Link application/mini program makes an audio call to the callee on an Android device is similar to the above video call process, except that the corresponding call type should be modified.**

### Video call from Android device to IoT Link application/mini program

1. The caller on the device clicks **Make Video Call**, and the following APIs need to be called:
```
mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_CALLING, TRTCCalling.TYPE_VIDEO_CALL, userId, agent, null); // Update the video call attribute to `calling`.
TRTCUIManager.getInstance().setSessionManager(new TRTCExplorerDemoSessionManager(mDataTemplateSample)); // Report the device status on the page
TRTCVideoCallActivity.startCallSomeone(TRTCMainActivity.this, agent, userId);// Redirect to the corresponding video call page  
```

2. The cloud service forwards the call request from the caller on the device over MQTT, and the callee on the IoT Link application/mini program is redirected to the video call page. After the callee clicks **Answer**, the IoT Link application/mini program continues to request the room entry parameters and enters the corresponding video room.

3. The cloud service forwards the room entry by the IoT Link application/mini program over MQTT, which triggers the `trtcJoinRoomCallBack` callback in `TXTRTCCallBack` on the device, where
> * The callback parameter `room` is the corresponding room parameter.

After this message is received, the following APIs need to be called:
``` 
TRTCUIManager.getInstance().joinRoom(mCallType, "", room); // Enter the room and update the video call attribute to `busy`.
``` 

4. The callee on the IoT Link application/mini program and the caller on the device make the video call.

5. After the caller hangs up or receives the ``` public void onUserLeave(final String userId) ``` callback for hangup by the callee, the current audio/video page will be exited, and the following API will be called: 
``` 
TRTCUIManager.getInstance().didExitRoom(TRTCCalling.TYPE_VIDEO_CALL, mSponsorUserInfo.getUserId()); // Update the video call attribute to `idle`
``` 

**The process in which the caller on an Android device makes an audio call to the callee on the IoT Link application/mini program is similar to the above video call process, except that the corresponding call type should be modified.**