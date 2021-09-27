* [Checking for Firmware Update](#Checking-for-Firmware-Update)
  * [Subscribing and publishing to topic for checking for firmware update](#Subscribing-and-publishing-to-topic-for-checking-for-firmware-update)
  * [Updating firmware](#Updating-firmware)

# Checking for Firmware Update

This document describes how to subscribe and publish to the topic for checking for firmware update.

## Subscribing and publishing to topic for checking for firmware update 

Run the demo and click **Connect Device** in the data template module. After the device is connected successfully, click **Report Event** to subscribe to the topic for checking for firmware update `$ota/update/${productID}/${deviceName}` and publish to the topic for checking for firmware update `$ota/report/${productID}/${deviceName}`. Below is the sample code:
```
mMqttConnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
    @Override
    public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) { // Callback for firmware version reporting
        TXLog.e(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
    }
    @Override
    public boolean onLastestFirmwareReady(String url, String md5, String version) {
        return false;
    }
    @Override
    public void onDownloadProgress(int percent, String version) { // Callback for OTA update package download progress
        TXLog.e(TAG, "onDownloadProgress:" + percent);
    }
    @Override
    public void onDownloadCompleted(String outputFile, String version) { // Callback for OTA update package download completion
        TXLog.e(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);
        mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
    }
    @Override
    public void onDownloadFailure(int errCode, String version) { // Callback for OTA update package download failure
        TXLog.e(TAG, "onDownloadFailure:" + errCode);
        mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
    }
});
mMqttConnection.reportCurrentFirmwareVersion("0.0.1");
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $ota/update/LWVUL5SZ2L/light1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"type":"report_version","report":{"version":"0.0.1"}}
D/TXDataTemplateFragment: onSubscribeCompleted, status[OK], topics[[$ota/update/LWVUL5SZ2L/light1]], userContext[], errMsg[subscribe success]
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Received topic: $ota/update/LWVUL5SZ2L/light1, id: 0, message: {"result_code":0,"result_msg":"success","type":"report_version_rsp","version":"0.0.1"}
E/TXDataTemplate: onReportFirmwareVersion:0, version:0.0.1, resultMsg:success
```
The above log represents the process in which the device successfully subscribes and publishes to the topic for checking for firmware update. The currently reported device firmware version number is `0.0.1`.

## Updating firmware

In the firmware update module of the IoT Explorer console, you can upload a new version of the firmware for a product, update the firmware of a specified device, and update the firmware of devices in batches. For more information, please see [Firmware Update](https://cloud.tencent.com/document/product/1081/40296).

After clicking **Update Firmware**, observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Received topic: $ota/update/LWVUL5SZ2L/light1, id: 0, message: {"file_size":511774,"md5sum":"b550fe42dec6fe0bc2d66f23e8074a5d","type":"update_firmware","url":"https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_LWVUL5SZ2L_0.0.2?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603179203%3B1603265603%26q-key-time%3D1603179203%3B1603265603%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3D5324de8e3470bfe325b08009cf2c113357302c03","version":"0.0.2"}
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: fileLength 511774 bytes
I/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: checkServerTrusted OK!!!
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: totalLength 511774 bytes
E/TXDataTemplate: onDownloadProgress:1
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: download 7771 bytes. percent:1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"type":"report_progress","report":{"progress":{"state":"downloading","percent":"1","result_code":"0","result_msg":""},"version":"0.0.2"}}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
...
...
E/TXDataTemplate: onDownloadProgress:100
D/com.tencent.iot.hub.device.java.core.mqtt.TXOTAImpl: download 511774 bytes. percent:100
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"type":"report_progress","report":{"progress":{"state":"downloading","percent":"100","result_code":"0","result_msg":""},"version":"0.0.2"}}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
E/TXDataTemplate: onDownloadCompleted:/storage/emulated/0/b550fe42dec6fe0bc2d66f23e8074a5d, version:0.0.2
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"type":"report_progress","report":{"progress":{"state":"done","result_code":"0","result_msg":"OK"},"version":"0.0.2"}}
D/TXDataTemplateFragment: onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the device receives the firmware version 0.0.2 update message successfully, and the SDK calls back the firmware download progress and reports it.
