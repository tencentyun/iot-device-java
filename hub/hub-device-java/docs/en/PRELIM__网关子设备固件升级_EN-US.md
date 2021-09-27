* [OTA Gateway Subdevice Firmware Update](#OTA-Gateway-Subdevice-Firmware-Update)
  * [Overview](#Overview)
  * [Compiling and running demo](#Compiling-and-running-demo)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Running demo for authenticated connection and subdevice firmware update](#Running-demo-for-authenticated-connection-and-subdevice-firmware-update)

# OTA Gateway Subdevice Firmware Update
## Overview

Device firmware update (aka OTA) is an important part of the IoT Hub service. When a device has new features available or vulnerabilities that need to be fixed, firmware update can be quickly performed for it through the OTA service. For more information, please see [Firmware Update](https://cloud.tencent.com/document/product/634/14673).

To try out the gateway subdevice firmware update feature, you need to add a new firmware file in the console. For more information, please see [Device Firmware Update](https://cloud.tencent.com/document/product/634/14674).

As subdevices cannot connect directly to the cloud, you can use the device OTA update method to update their firmware through a gateway device. You can update one or multiple subdevices at a time.

## Compiling and running demo

#### Downloading the sample code of IoT Hub SDK for Java demo

The sample code and source code are in the [hub-device-java](../../hub-device-java) module.

#### Preparing development environment

The development environment used in this demo is as follows:

* OS: macOS
* JDK version: [JDK13](https://www.oracle.com/java/technologies/javase-jdk13-downloads.html)
* IDE: [IntelliJ IDEA CE](https://www.jetbrains.com/idea/)

#### Checking SDK dependencies

Source code dependencies are used in the demo. You can also add dependencies in Maven. For more information, please see [Project configuration](../../hub-device-java/README.md#Project-configuration).

## Entering parameters for authenticating device for connection

Please enter the required parameters in the [GatewaySampleTest.java](../../hub-device-java/src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java) file.
```
public class GatewaySampleTest {
    private static String mProductID = "YOUR_PRODUCT_ID"; // Gateway product ID
	private static String mDevName = "YOUR_DEVICE_NAME"; // Gateway device name
	private static String mDevPSK = "YOUR_DEV_PSK"; // Gateway device key (for key authentication)
	private static String mSubProductID = "YOUR_SUB_PRODUCT_ID"; // Product ID of the subdevice
	private static String mSubDevName = "YOUR_SUB_DEV_NAME"; // Subdevice name
	private static String mSubDevProductKey = "YOUR_SUB_DEV_PSK"; // Subdevice key (for key authentication)
}
```

## Running demo for authenticated connection and subdevice firmware update

When the MQTT authentication method is key authentication, you don't need to add the SSL configuration to `MqttConnectOptions`; instead, you can use TCP. When you use SDK v3.3.0 or below, key authentication requires adding the SSL configuration `options.setSocketFactory(AsymcSslUtils.getSocketFactory());` to `MqttOptions`.

When the MQTT authentication method is certificate authentication, you need to add the SSL configuration `options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));` to `MqttConnectOptions`.

Run [GatewaySampleTest.java](../../hub-device-java/src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java) and connect the gateway device to MQTT through authenticated connection. After it is authenticated and connected, initialize OTA and subscribe to the `$ota/update/${productId}/${deviceName}` topic for both the gateway device and the subdevice. Below is the sample code:

```
public static void main(String[] args) {
    mqttconnection = new TXMqttConnection(mProductID, mDevName, mDevPSK, new callBack());
    mqttconnection.setSubDevName(mSubDevName);  // Set the subdevice name
    mqttconnection.setSubDevProductKey(mSubDevProductKey);  // Set the subdevice key (for key authentication)
    mqttconnection.setSubProductID(mSubProductID);  // Set the product ID of the subdevice
    mqttconnection.connect(options, null);  // Establish an authenticated connection to MQTT
}

public static class callBack extends TXMqttActionCallBack {

    @Override
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {  // Callback for authenticated MQTT connection
        mqttconnection.initOTA(path2Store, oTACallBack);  // Initialize OTA
    }

    private TXOTACallBack oTACallBack = new TXOTACallBack() {

        @Override
        public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) { // Callback for firmware version reporting
        }

        @Override
        public boolean onLastestFirmwareReady(String url, String md5, String version) {
        	System.out.println("onLastestFirmwareReady url=" + url + " version " + version);
        	mqttconnection.gatewayDownSubdevApp(url, path2Store + "/" + md5, md5, version);
        	return true; // `false` indicates to automatically trigger update file download, while `true` indicates to manually trigger
        }

        @Override
        public void onDownloadProgress(int percent, String version) { // Callback for OTA update package download progress
        	mqttconnection.gatewaySubdevReportProgress(percent, version);
        }

        @Override
        public void onDownloadCompleted(String outputFile, String version) { // Callback for OTA update package download completion
        	mqttconnection.gatewaySubdevReportStart(version);
        	mqttconnection.gatewaySubdevReportSuccess(version);
        }

        @Override
        public void onDownloadFailure(int errCode, String version) { // Callback for OTA update package download failure
        	mqttconnection.gatewaySubdevReportFail(errCode, "", version);
        }
    };
    @Override
    public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
        for (String topicEls : asyncActionToken.getTopics()) {
            if (topicEls.startsWith("$ota/update/")) {
                mqttconnection.gatewaySubdevReportVer("0.0"); // The reported current device firmware version number is `0.0`.
            }
        }
    ...
}
```

The following IDE console log represents the process in which the subdevice successfully subscribes to the topic for OTA update and reports the current version number. The value passed in for the `version` parameter in the demo is `0.0`.

```
TXMqttConnection connect 297  - Start connecting to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883
TXMqttConnection onSuccess 268  - onSuccess!
ffffffffffff msg connected to ssl://AP9ZLEVFKT.iotcloud.tencentdevices.com:8883
TXMqttConnection subscribe 633  - Starting subscribe topic: $ota/update/AP9ZLEVFKT/gateway1
TXMqttConnection subscribe 633  - Starting subscribe topic: $ota/update/9RW4A8OOFK/door1
onSubscribeCompleted, status[OK], topics[[$ota/update/AP9ZLEVFKT/gateway1]], userContext[], errMsg[subscribe success]
onSubscribeCompleted, status[OK], topics[[$ota/update/9RW4A8OOFK/door1]], userContext[], errMsg[subscribe success]
TXMqttConnection publish 451  - Starting publish topic: $ota/report/9RW4A8OOFK/door1 Message: {"report":{"version":"0.0"},"type":"report_version"}
onPublishCompleted, status[OK], topics[[$ota/report/9RW4A8OOFK/door1]],   errMsg[publish success]
```

After the firmware update operation is triggered in the console, the device will receive a firmware update message through the subscribed `$ota/update/${productID}/${deviceName}` topic for OTA update.

```
TXMqttConnection messageArrived 879  - Received topic: $ota/update/9RW4A8OOFK/door1, id: 0, message: {"file_size":234775,"md5sum":"f2f1b3317d4f1ef7f512bfae5050563b","type":"update_firmware","url":"https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_9RW4A8OOFK_0.0.1?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603710048%3B1603796448%26q-key-time%3D1603710048%3B1603796448%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Deb248051f6bef7756462b7f833f0608c81281cc1","version":"0.0.1"}
TXOTAImpl run 501  - fileLength 234775 bytes
TXOTAImpl run 510  - connect: https://ota-1255858890.cos.ap-guangzhou.myqcloud.com/100012619289_9RW4A8OOFK_0.0.1?sign=q-sign-algorithm%3Dsha1%26q-ak%3DAKIDdO8ldrUa0Uts4H5Gzx6FZ9nfedjpiCd7%26q-sign-time%3D1603710048%3B1603796448%26q-key-time%3D1603710048%3B1603796448%26q-header-list%3D%26q-url-param-list%3D%26q-signature%3Deb248051f6bef7756462b7f833f0608c81281cc1
TXOTAImpl checkServerTrusted 434  - checkServerTrusted OK!!
TXOTAImpl run 520  - totalLength 234775 bytes
TXOTAImpl run 543  - download 7789 bytes. percent:3
TXMqttConnection publish 451  - Starting publish topic: $ota/report/AP9ZLEVFKT/gateway1 Message: {"report":{"progress":{"result_msg":"","result_code":"0","state":"downloading","percent":"3"},"version":"0.0.1"},"type":"report_progress"}
onPublishCompleted, status[OK], topics[[$ota/report/9RW4A8OOFK/door1]],   errMsg[publish success]
...
TXOTAImpl run 543  - download 234775 bytes. percent:100
TXMqttConnection publish 451  - Starting publish topic: $ota/report/9RW4A8OOFK/door1 Message: {"report":{"progress":{"result_msg":"","result_code":"0","state":"done"},"version":"0.0.1"},"type":"report_progress"}
onPublishCompleted, status[OK], topics[[$ota/report/9RW4A8OOFK/door1]],   errMsg[publish success]
```
The above log represents the process in which the device receives a firmware update message, downloads the firmware file, displays the download progress, and reports the latest version number after the new version is downloaded successfully. The latest `version` reported in the demo is `0.0.1`.