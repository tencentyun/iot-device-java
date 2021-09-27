* [OTA Device Firmware Update](#OTA-Device-Firmware-Update)
  * [Overview](#Overview)
  * [Running demo to try out checking for firmware update](#Running-demo-to-try-out-checking-for-firmware-update)

# OTA Device Firmware Update
## Overview
Device firmware update (aka OTA) is an important part of the IoT Hub service. When a device has new features available or vulnerabilities that need to be fixed, firmware update can be quickly performed for it through the OTA service. For more information, please see [Firmware Update](https://cloud.tencent.com/document/product/634/14673).

To try out firmware update, you need to add a new firmware file in the console. For more information, please see [Device Firmware Update](https://cloud.tencent.com/document/product/634/14674).

## Running demo to try out checking for firmware update

Please connect the device to MQTT for authenticated connection as instructed in [Device Connection Through MQTT over TCP](../../hub-device-android/docs/Device-Connection-Through-MQTT-over-TCP.md) first.

Run the demo and click **Check for Firmware Update** in the basic feature module to report the firmware version number and check for firmware update. Below is the sample code:
```
mMQTTSample.checkFirmware(); // Check for firmware update
```

The following logcat log represents the process in which the device successfully subscribes to the topic for OTA update and reports the current version number. The value passed in for the `version` parameter in the demo is `0.0.1`.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $ota/update/AP9ZLEVFKT/gateway1
D/TXMQTT: onSubscribeCompleted, status[OK], topics[[$ota/update/AP9ZLEVFKT/gateway1]], userContext[], errMsg[subscribe success]
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/AP9ZLEVFKT/gateway1 Message: {"type":"report_version","report":{"version":"0.0.1"}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$ota/report/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
```

After the firmware update operation is triggered in the console, the device will receive a firmware update message through the subscribed `$ota/update/${productID}/${deviceName}` topic for OTA update.

```
I/TXMQTT_1.2.3: Received topic: $ota/update/AP9ZLEVFKT/gateway1, id: 0, message: {"file_size":11821,"md5sum":"1d40bfcfc9d7d21ca16b5515c0f6a340","type":"update_firmware","url":"https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_AP9ZLEVFKT_0.0.2?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603246098%3B1603332498%26q-key-time%3D1603246098%3B1603332498%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Df960bd07ea5fd16b1bbef33689b9647a5eccbea9","version":"0.0.2"}
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: fileLength 11821 bytes
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: connect: https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_AP9ZLEVFKT_0.0.2?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603246098%3B1603332498%26q-key-time%3D1603246098%3B1603332498%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Df960bd07ea5fd16b1bbef33689b9647a5eccbea9
I/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: checkServerTrusted OK!!!
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: totalLength 11821 bytes
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: download 7771 bytes. percent:65
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: download 11821 bytes. percent:100
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/AP9ZLEVFKT/gateway1 Message: {"type":"report_progress","report":{"progress":{"state":"done","result_code":"0","result_msg":"OK"},"version":"0.0.2"}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$ota/report/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the device receives a firmware update message, downloads the firmware file, displays the download progress, and reports the latest version number after the new version is downloaded successfully. The latest `version` reported in the demo is `0.0.2`.




