* [Device Log Reporting](#Device-Log-Reporting)
  * [Overview](#Overview)
  * [Running demo to try out log testing](#Running-demo-to-try-out-log-testing)
  * [Running demo to try out log upload](#Running-demo-to-try-out-log-upload)

# Device Log Reporting
## Overview
The device log reporting feature can report device logs to the cloud over HTTP and display them in the console for you to remotely debug, diagnose, and monitor the device status. For more information on how to view device logs, please see [Cloud Log](https://cloud.tencent.com/document/product/634/14445).

To try out the device log reporting feature, you need to enable **Device Log** and set the corresponding log level in **Device Information** > **Device Log Configuration** in the console.

## Running demo to try out log testing

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java), call `deviceLog()` to get the log level of the device, and the corresponding testing logs will be generated for higher log levels. Below is the sample code:
```
private static void deviceLog() {
    try {
        Thread.sleep(2000);
        mqttconnection.mLog(TXMqttLogConstants.LEVEL_ERROR,TAG,"Error level log for test!!!");  // Generate a `Level1` ERROR device log
        mqttconnection.mLog(TXMqttLogConstants.LEVEL_WARN,TAG,"Warning level log for test!!!"); // Generate a `Level2` WARN device log
        mqttconnection.mLog(TXMqttLogConstants.LEVEL_INFO,TAG,"Info level log for test!!!");    // Generate a `Level3` INFO device log
        mqttconnection.mLog(TXMqttLogConstants.LEVEL_DEBUG,TAG,"Debug level log for test!!!");  // Generate a `Level4` DEBUG device log
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which a testing log is generated and added to the log queue successfully for upload to the device logs in **Cloud Log** in the console. The log level of the device set in the console obtained at this point is a `Level4` DEBUG log.
```
18/03/2021 00:04:38,818 [MQTT Call: 9RW4A8OOFKtest_device] DEBUG TXMqttConnection messageArrived 1164  - ******Set mqttLogLevel to 4
18/03/2021 00:04:43,221 [main] ERROR MqttSampleTest printDebug 553  - Add log to log Deque! ERR|2021-03-18 00:04:43|TXMQTT|Error level log for test!!!
18/03/2021 00:04:44,418 [main] ERROR MqttSampleTest printDebug 553  - Add log to log Deque! WRN|2021-03-18 00:04:44|TXMQTT|Warning level log for test!!!
18/03/2021 00:04:44,960 [main] ERROR MqttSampleTest printDebug 553  - Add log to log Deque! INF|2021-03-18 00:04:44|TXMQTT|Info level log for test!!!
18/03/2021 00:04:45,569 [main] ERROR MqttSampleTest printDebug 553  - Add log to log Deque! DBG|2021-03-18 00:04:45|TXMQTT|Debug level log for test!!!
```


## Running demo to try out log upload

Run the `main` function in [MqttSampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/mqtt/MqttSampleTest.java) and call `uploadLog()` to upload the logs in the log queue to the device logs in **Cloud Log** in the console. Below is the sample code:
```
private static void uploadLog() {
    try {
        Thread.sleep(2000);
        mqttconnection.uploadLog();// Upload the device log
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which a log is uploaded successfully.
```
18/03/2021 00:04:57,866 [Thread-2] ERROR MqttSampleTest printDebug 553  - Upload log to http://devicelog.iot.cloud.tencent.com:80/cgi-bin/report-log success!
```
