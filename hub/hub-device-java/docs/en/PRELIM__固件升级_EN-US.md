* [OTA Device Firmware Update](#OTA-Device-Firmware-Update)
  * [Overview](#Overview)
  * [Running demo to try out checking for firmware update](#Running-demo-to-try-out-checking-for-firmware-update)

# OTA Device Firmware Update
## Overview
Device firmware update (aka OTA) is an important part of the IoT Hub service. When a device has new features available or vulnerabilities that need to be fixed, firmware update can be quickly performed for it through the OTA service. For more information, please see [Firmware Update](https://cloud.tencent.com/document/product/634/14673).

To try out firmware update, you need to add a new firmware file in the console. For more information, please see [Device Firmware Update](https://cloud.tencent.com/document/product/634/14674).

## Running demo to try out checking for firmware update

Please configure the following information in [OTASampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/ota/OTASampleTest.java) as instructed in [Device Connection Through MQTT over TCP](../../docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md) to connect the device to MQTT for authenticated connection with key or certificate.

```
{
  private static String mProductID = "";
  private static String mDevName = "";
  private static String mDevPSK  = ""; // Set to `null` if certificate authentication is used
  private static String mCertFilePath = "";           // Enter the name of the device certificate file in the `resources` folder
  private static String mPrivKeyFilePath = "";        // Enter the name of the device private key file in the `resources` folder
  private static String mDevCert = "";           // Enter the device certificate file content
  private static String mDevPriv = "";           // Enter the device private key file content
}
```

Run the `main` function in [OTASampleTest.java](../../src/test/java/com/tencent/iot/hub/device/java/core/ota/OTASampleTest.java). After the device is connected, call `checkFirmware()` to report the firmware version number and check for any firmware update. Below is the sample code:
```
private static void checkFirmware() {
    try {
        Thread.sleep(2000);
        String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
        mqttconnection.initOTA(workDir, new TXOTACallBack() {
        	@Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                LOG.error("onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }
            
            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                LOG.error("MQTTSample onLastestFirmwareReady");
                return false;
            }
            
            @Override
            public void onDownloadProgress(int percent, String version) {
                LOG.error("onDownloadProgress:" + percent);
            }
            
            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                LOG.error("onDownloadCompleted:" + outputFile + ", version:" + version);
            
                mqttconnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }
            
            @Override
            public void onDownloadFailure(int errCode, String version) {
                LOG.error("onDownloadFailure:" + errCode);
            
                mqttconnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mqttconnection.reportCurrentFirmwareVersion("0.0.1");
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the device successfully subscribes to the topic for OTA update and reports the current version number. The value passed in for the `version` parameter in the demo is `0.0.1`.
```
17/03/2021 10:12:08,546 [main] INFO  TXMqttConnection subscribe 749  - Starting subscribe topic: $ota/update/AP9ZLEVFKT/gateway2
17/03/2021 10:12:08,556 [MQTT Call: AP9ZLEVFKTgateway2] DEBUG OTASampleTest onSubscribeCompleted 405  - onSubscribeCompleted, status[OK], topics[[$ota/update/AP9ZLEVFKT/gateway2]], userContext[], errMsg[subscribe success]
onSubscribeCompleted status OK
17/03/2021 10:12:08,549 [main] INFO  TXMqttConnection publish 567  - Starting publish topic: $ota/report/AP9ZLEVFKT/gateway2 Message: {"report":{"version":"0.0.1"},"type":"report_version"}
17/03/2021 10:12:08,564 [MQTT Call: AP9ZLEVFKTgateway2] DEBUG OTASampleTest onPublishCompleted 393  - onPublishCompleted, status[OK], topics[[$ota/report/AP9ZLEVFKT/gateway2]],  userContext[], errMsg[publish success]
17/03/2021 10:12:08,578 [MQTT Call: AP9ZLEVFKTgateway2] INFO  TXMqttConnection messageArrived 1129  - Received topic: $ota/update/AP9ZLEVFKT/gateway2, id: 0, message: {"result_code":0,"result_msg":"success","type":"report_version_rsp","version":"0.0.1"}
```

After the firmware update operation is triggered in the console, the device will receive a firmware update message through the subscribed `$ota/update/${productID}/${deviceName}` topic for OTA update.

```
17/03/2021 10:14:05,260 [MQTT Call: AP9ZLEVFKTgateway2] INFO  TXMqttConnection messageArrived 1129  - Received topic: $ota/update/AP9ZLEVFKT/gateway2, id: 0, message: {"file_size":234775,"md5sum":"f2f1b3317d4f1ef7f512bfae5050563b","type":"update_firmware","url":"https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_AP9ZLEVFKT_0.0.2?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1615947245%3B1616033645%26q-key-time%3D1615947245%3B1616033645%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3D47e9850aaf8942711ea3cd9896af1694c1df4d79","version":"0.0.2"}
mStoragePath=/Users/sun/Documents/work_code_from_gerrit/iot-device-android/hub/hub-device-java/src/test/resources/
17/03/2021 10:14:05,262 [Thread-2] DEBUG TXOTAImpl run 513  - fileLength 0 bytes
17/03/2021 10:14:05,262 [Thread-2] DEBUG TXOTAImpl run 522  - connect: https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_AP9ZLEVFKT_0.0.2?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1615947245%3B1616033645%26q-key-time%3D1615947245%3B1616033645%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3D47e9850aaf8942711ea3cd9896af1694c1df4d79
17/03/2021 10:14:08,532 [Thread-2] INFO  TXOTAImpl checkServerTrusted 446  - checkServerTrusted OK!!!
17/03/2021 10:14:08,668 [Thread-2] DEBUG TXOTAImpl run 532  - totalLength 234775 bytes
17/03/2021 10:14:08,669 [Thread-2] DEBUG TXOTAImpl run 555  - download 7789 bytes. percent:3
17/03/2021 10:14:08,705 [Thread-2] DEBUG TXOTAImpl run 555  - download 163301 bytes. percent:69
17/03/2021 10:14:08,713 [Thread-2] DEBUG TXOTAImpl run 555  - download 234775 bytes. percent:100
17/03/2021 10:14:08,717 [Thread-2] INFO  TXMqttConnection publish 567  - Starting publish topic: $ota/report/AP9ZLEVFKT/gateway2 Message: {"report":{"progress":{"result_msg":"OK","result_code":"0","state":"done"},"version":"0.0.2"},"type":"report_progress"}
17/03/2021 10:14:08,718 [MQTT Call: AP9ZLEVFKTgateway2] DEBUG OTASampleTest onPublishCompleted 393  - onPublishCompleted, status[OK], topics[[$ota/report/AP9ZLEVFKT/gateway2]],  userContext[], errMsg[publish success]
```
The above log represents the process in which the device receives a firmware update message, downloads the firmware file, displays the download progress, and reports the latest version number after the new version is downloaded successfully. The latest `version` reported in the demo is `0.0.2`.




