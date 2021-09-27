* [Device Log Reporting](#Device-Log-Reporting)
  * [Overview](#Overview)
  * [Running demo to try out log testing](#Running-demo-to-try-out-log-testing)
  * [Running demo to try out log upload](#Running-demo-to-try-out-log-upload)

# Device Log Reporting
## Overview
The device log reporting feature can report device logs to the cloud over HTTP and display them in the console for you to remotely debug, diagnose, and monitor the device status. For more information on how to view device logs, please see [Cloud Log](https://cloud.tencent.com/document/product/634/14445).

To try out the device log reporting feature, you need to enable **Device Log** and set the corresponding log level in **Device Information** > **Device Log Configuration** in the console.

## Running demo to try out log testing

Run the demo, click **Test Log** in the basic feature module to get the log level of the device, and the corresponding testing logs will be generated for higher log levels. Below is the sample code:
```
mMQTTSample.mLog(TXMqttLogConstants.LEVEL_ERROR,TAG,"Error level log for test!!!");// Generate a `Level1` ERROR device log
mMQTTSample.mLog(TXMqttLogConstants.LEVEL_WARN,TAG,"Warning level log for test!!!");// Generate a `Level2` WARN device log
mMQTTSample.mLog(TXMqttLogConstants.LEVEL_INFO,TAG,"Info level log for test!!!");// Generate a `Level3` INFO device log
mMQTTSample.mLog(TXMqttLogConstants.LEVEL_DEBUG,TAG,"Debug level log for test!!!"); // Generate a `Level4` DEBUG device log
```

The following logcat log represents the process in which a testing log is generated and added to the log queue successfully for upload to the device logs in **Cloud Log** in the console. The log level of the device set in the console obtained at this point is a `Level4` DEBUG log.
```
D/TXMQTT_1.2.3: ******Set mqttLogLevel to 4
D/TXMQTT: Add log to log Deque! ERR|2020-10-21 10:13:10|TXMQTT|Error level log for test!!!
D/TXMQTT: Add log to log Deque! WRN|2020-10-21 10:13:10|TXMQTT|Warning level log for test!!!
D/TXMQTT: Add log to log Deque! INF|2020-10-21 10:13:10|TXMQTT|Info level log for test!!!
D/TXMQTT: Add log to log Deque! DBG|2020-10-21 10:13:10|TXMQTT|Debug level log for test!!!
```


## Running demo to try out log upload

Run the demo and click **Upload Log** in the basic feature module to upload the logs in the log queue to the device logs in **Cloud Log** in the console. Below is the sample code:
```
mMQTTSample.uploadLog();// Upload the device log
```

The following logcat log represents the process in which a log is uploaded successfully.
```
D/TXMQTT: Upload log to http://devicelog.iot.cloud.tencent.com:80/cgi-bin/report-log success!
```
