* [OTA Gateway Subdevice Firmware Update](#OTA-Gateway-Subdevice-Firmware-Update)
  * [Overview](#Overview)
  * [Running demo to try out checking for OTA gateway subdevice firmware update](#Running-demo-to-try-out-checking-for-OTA-gateway-subdevice-firmware-update)

# OTA Gateway Subdevice Firmware Update
## Overview

As subdevices cannot connect directly to the cloud, you can use the device OTA update method to update their firmware through a gateway device. You can update one or multiple subdevices at a time.

For more information on how to use the device firmware update feature, please see [Firmware Update](../../hub-device-android/docs/Firmware-Update.md).

## Running demo to try out checking for OTA gateway subdevice firmware update

Please enter the corresponding information of the gateway device and the subdevice as instructed in [Gateway Feature](../../hub-device-android/docs/Gateway-Feature.md) first to connect the gateway device to MQTT for authenticated connection.

Run the demo and click **Report Subdevice Version** in the basic feature module to report the subdevice firmware version number and check for firmware update. Below is the sample code:
```
mMQTTSample.initOTA();  // Initialize the OTA service and start to listen on pushes from the platform
mMQTTSample.reportSubDevVersion("0.0"); 
```

The following logcat log represents the process in which the gateway device and the subdevice successfully subscribe to the topic for OTA update and the subdevice reports the current version number. The value passed in for the `version` parameter in the demo is `0.0`.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $ota/update/AP9ZLEVFKT/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $ota/update/9RW4A8OOFK/door1
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[$ota/update/AP9ZLEVFKT/gateway1]], userContext[], errMsg[subscribe success]
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[$ota/update/9RW4A8OOFK/door1]], userContext[], errMsg[subscribe success]
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/9RW4A8OOFK/door1 Message: {"type":"report_version","report":{"version":"0.0"}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$ota/report/9RW4A8OOFK/door1]],  userContext[], errMsg[publish success]
```

After the subdevice firmware update operation is triggered in the console, click **Report Subdevice Version** again, and the device will receive a firmware update message through the subscribed `$ota/update/${productID}/${deviceName}` topic for subdevice OTA update.

```
I/TXMQTT_1.2.3: Received topic: $ota/update/9RW4A8OOFK/door1, id: 0, message: {"file_size":234775,"md5sum":"f2f1b3317d4f1ef7f512bfae5050563b","type":"update_firmware","url":"https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_9RW4A8OOFK_0.0.1?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603678277%3B1603764677%26q-key-time%3D1603678277%3B1603764677%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Dc79061bf0366e368ba5f9582895ee2074a457910%00","version":"0.0.1"}
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: fileLength 234775 bytes
    connect: https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_9RW4A8OOFK_0.0.1?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603678277%3B1603764677%26q-key-time%3D1603678277%3B1603764677%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Dc79061bf0366e368ba5f9582895ee2074a457910%00
I/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: checkServerTrusted OK!!!
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: totalLength 234775 bytes
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: download 7772 bytes. percent:3
...
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: download 234775 bytes. percent:100
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/9RW4A8OOFK/door1 Message: {"type":"report_progress","report":{"progress":{"state":"done","result_code":"0","result_msg":""},"version":"0.0.1"}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$ota/report/9RW4A8OOFK/door1]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the subdevice receives a firmware update message, downloads the firmware, displays the download progress, and reports the latest version number after the new version is downloaded successfully. The latest `version` reported in the demo is `0.0.1`.




