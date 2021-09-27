* [Checking for Firmware Update](#Checking-for-Firmware-Update)
  * [Subscribing and publishing to topic for checking for firmware update](#Subscribing-and-publishing-to-topic-for-checking-for-firmware-update)
  * [Updating firmware](#Updating-firmware)

# Checking for Firmware Update

This document describes how to subscribe and publish to the topic for checking for firmware update.

## Subscribing and publishing to topic for checking for firmware update 
Edit the parameter configuration information in the [unit_test_config.json](../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTOTASAMPLE_PRODUCT_ID":            "",
  "TESTOTASAMPLE_DEVICE_NAME":           "",
  "TESTOTASAMPLE_DEVICE_PSK":            ""
}
```
`TESTOTASAMPLE_PRODUCT_ID` (product ID), `TESTOTASAMPLE_DEVICE_NAME` (device name), and `TESTOTASAMPLE_DEVICE_PSK` (device key).

Run the `testOTA` function in [OTASampleTest.java](../src/test/java/com/tencent/iot/explorer/device/java/core/ota/OTASampleTest.java). After `connect()` calls back `onConnectCompleted`, call `checkFirmware()` to subscribe to the `$ota/update/${productID}/${deviceName}` topic for checking for firmware update and publish to the `$ota/report/${productID}/${deviceName}` topic for checking for firmware update. Below is the sample code:
```
private static void checkFirmware() {
    mDataTemplateSample.checkFirmware();
}
// The code of `mDataTemplateSample.checkFirmware()` is as follows
public void checkFirmware() {

    mMqttConnection.initOTA(workDir, new TXOTACallBack() { // `workDir` is the download path of the OTA update package
        @Override
        public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) { // Callback for firmware version reporting
            LOG.error(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
        }

        @Override
        public boolean onLastestFirmwareReady(String url, String md5, String version) { // Whether it is ready to download the latest firmware
            return false;
        }

        @Override
        public void onDownloadProgress(int percent, String version) { // Callback for OTA update package download progress
            LOG.error(TAG, "onDownloadProgress:" + percent);
        }

        @Override
        public void onDownloadCompleted(String outputFile, String version) { // Callback for OTA update package download completion
            LOG.error(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

            mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
        }

        @Override
        public void onDownloadFailure(int errCode, String version) { // Callback for OTA update package download failure
            LOG.error(TAG, "onDownloadFailure:" + errCode);

            mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
        }
    });
    mMqttConnection.reportCurrentFirmwareVersion("0.0.1"); // Report the current device firmware version number
}
```

Observe the logcat log.
```
24/02/2021 09:15:36,653 [main] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: $ota/update/LWVUL5SZ2L/light1
24/02/2021 09:15:36,654 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"report":{"version":"0.0.1"},"type":"report_version"}
24/02/2021 09:15:36,671 [MQTT Call: LWVUL5SZ2Llight1] DEBUG OTASampleTest onSubscribeCompleted 333  - onSubscribeCompleted, status[OK], topics[[$ota/update/LWVUL5SZ2L/light1]], userContext[], errMsg[subscribe success]
24/02/2021 09:15:36,666 [MQTT Call: LWVUL5SZ2Llight1] DEBUG OTASampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
24/02/2021 09:15:36,688 [MQTT Call: LWVUL5SZ2Llight1] INFO  TXMqttConnection messageArrived 931  - Received topic: $ota/update/LWVUL5SZ2L/light1, id: 0, message: {"result_code":0,"result_msg":"success","type":"report_version_rsp","version":"0.0.1"}
24/02/2021 09:15:50,329 [MQTT Call: LWVUL5SZ2Llight1] ERROR DataTemplateSample onReportFirmwareVersion 179  - onReportFirmwareVersion:0, version:0.0.1, resultMsg:success
```
The above log represents the process in which the device successfully subscribes and publishes to the topic for checking for firmware update. The currently reported device firmware version number is `0.0.1`.

## Updating firmware

In the firmware update module of the IoT Explorer console, you can upload a new version of the firmware for a product, update the firmware of a specified device, and update the firmware of devices in batches. For more information, please see [Firmware Update](https://cloud.tencent.com/document/product/1081/40296).

After the device is connected and subscribes to the topics related to OTA and the corresponding firmware update task is created in the IoT Explorer console, observe the logcat log.
```
24/02/2021 09:20:43,657 [MQTT Call: LWVUL5SZ2Llight1] INFO  TXMqttConnection messageArrived 931  - Received topic: $ota/update/LWVUL5SZ2L/light1, id: 0, message: {"file_size":234775,"md5sum":"f2f1b3317d4f1ef7f512bfae5050563b","type":"update_firmware","url":"https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_LWVUL5SZ2L_0.0.2?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1614129643%3B1614216043%26q-key-time%3D1614129643%3B1614216043%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Df038c9e4276f8219e1522b7e42d87f99b4d073ac","version":"0.0.2"}
24/02/2021 09:20:57,813 [Thread-3] DEBUG TXOTAImpl run 513  - fileLength 0 bytes
24/02/2021 09:20:57,972 [Thread-3] INFO  TXOTAImpl checkServerTrusted 446  - checkServerTrusted OK!!!
24/02/2021 09:20:58,116 [Thread-3] DEBUG TXOTAImpl run 532  - totalLength 234775 bytes
24/02/2021 09:20:58,117 [Thread-3] ERROR DataTemplateSample onDownloadProgress 189  - onDownloadProgress:6
24/02/2021 09:20:58,117 [Thread-3] DEBUG TXOTAImpl run 555  - download 15964 bytes. percent:6
24/02/2021 09:20:58,117 [Thread-3] INFO  TXMqttConnection publish 492  - Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"report":{"progress":{"result_msg":"","result_code":"0","state":"downloading","percent":"6"},"version":"0.0.2"},"type":"report_progress"}
24/02/2021 09:20:58,118 [MQTT Call: LWVUL5SZ2Llight1] DEBUG OTASampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
...
...
24/02/2021 09:20:58,159 [Thread-3] ERROR DataTemplateSample onDownloadProgress 189  - onDownloadProgress:100
24/02/2021 09:20:58,159 [Thread-3] DEBUG TXOTAImpl run 555  - download 234775 bytes. percent:100
24/02/2021 09:20:58,159 [Thread-3] INFO  TXMqttConnection publish 492  - Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"report":{"progress":{"result_msg":"","result_code":"0","state":"downloading","percent":"100"},"version":"0.0.2"},"type":"report_progress"}
24/02/2021 09:20:58,159 [MQTT Call: LWVUL5SZ2Llight1] DEBUG OTASampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
24/02/2021 09:20:58,169 [Thread-3] ERROR DataTemplateSample onDownloadCompleted 194  - onDownloadCompleted:/Users/sun/Documents/work_code_from_gerrit/iot-device-android/explorer/explorer-device-java/src/test/resources//f2f1b3317d4f1ef7f512bfae5050563b, version:0.0.2
24/02/2021 09:20:58,169 [Thread-3] INFO  TXMqttConnection publish 492  - Starting publish topic: $ota/report/LWVUL5SZ2L/light1 Message: {"report":{"progress":{"result_msg":"OK","result_code":"0","state":"done"},"version":"0.0.2"},"type":"report_progress"}
24/02/2021 09:20:58,170 [MQTT Call: LWVUL5SZ2Llight1] DEBUG OTASampleTest onPublishCompleted 319  - onPublishCompleted, status[OK], topics[[$ota/report/LWVUL5SZ2L/light1]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the device receives the firmware version 0.0.2 update message successfully, and the SDK calls back the firmware download progress and reports it. At this point, you can see that the new firmware OTA update package is already at the download path passed in by ``` public void initOTA(String storagePath, TXOTACallBack callback) ```.
