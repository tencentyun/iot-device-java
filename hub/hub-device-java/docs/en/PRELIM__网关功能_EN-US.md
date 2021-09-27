* [Gateway Feature](#Gateway-Feature)
  * [Overview](#Overview)
  * [Running demo to try out gateway feature](#Running-demo-to-try-out-gateway-feature)
    * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
    * [Trying out connecting subdevice](#Trying-out-connecting-subdevice)
    * [Trying out disconnecting subdevice](#Trying-out-disconnecting-subdevice)

# Gateway Feature
## Overview
In addition to the basic features of general products, a gateway product can also be bound to products that cannot directly access the internet and used to exchange data with IoT Hub on behalf of such products (i.e., subdevices). This document describes how to connect a gateway product to IoT Hub over the MQTT protocol for proxied subdevice connection/disconnection and message sending/receiving.

To try out the gateway feature, you need to create a gateway product in the console and bind a subproduct and a subdevice. For more information, please see [Device Connection Preparations](https://cloud.tencent.com/document/product/634/14442) and [Gateway Product Connection](https://cloud.tencent.com/document/product/634/32740).

## Running demo to try out gateway feature

#### Entering parameters for authenticating device for connection
Edit the parameter configuration information in the [unit_test_config.json](../src/test/resources/unit_test_config.json) file in the demo.
```
{
    "TESTGATEWAYSAMPLE_PRODUCT_ID":        "",
    "TESTGATEWAYSAMPLE_DEVICE_NAME":       "",
    "TESTGATEWAYSAMPLE_DEVICE_PSK":        "",
    "TESTGATEWAYSAMPLE_SUB_PRODUCT_ID":    "",
    "TESTGATEWAYSAMPLE_SUB_DEV_NAME":      "",
    "TESTGATEWAYSAMPLE_SUB_DEV_PSK":       "",
    "TESTGATEWAYSAMPLE_TEST_TOPIC":        "",
}
```
You need to enter the `TESTGATEWAYSAMPLE_PRODUCT_ID` (gateway product ID), `TESTGATEWAYSAMPLE_DEVICE_NAME` (gateway device name), `TESTGATEWAYSAMPLE_DEVICE_PSK` (gateway device key), `TESTGATEWAYSAMPLE_SUB_PRODUCT_ID` (subdevice product ID), `TESTGATEWAYSAMPLE_SUB_DEV_NAME` (subdevice name), and `TESTGATEWAYSAMPLE_SUB_DEV_PSK` (subdevice key) parameters of a key-authenticated device in `unit_test_config.json`.

#### Trying out connecting subdevice

Run the `main` function in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java). After the device is connected, call `gatewaySubdevOnline()` to connect the subdevice. Below is the sample code:
```
private static void gatewaySubdevOnline() {
    try {
        Thread.sleep(2000);
        // set subdev online
        mqttconnection.gatewaySubdevOnline(mSubProductID, mSubDevName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the subdevice is connected successfully. In the console, you can see that the status of the device has been updated to `online`.
```
17/03/2021 09:52:22,726 [main] DEBUG TXGatewayConnection gatewaySubdevOnline 234  - set 9RW4A8OOFK & test_device to Online
17/03/2021 09:52:22,728 [main] INFO  TXMqttConnection publish 567  - Starting publish topic: $gateway/operation/AP9ZLEVFKT/log_test Message: {"payload":{"devices":[{"device_name":"test_device","product_id":"9RW4A8OOFK"}]},"type":"online"}
17/03/2021 09:52:22,729 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onPublishCompleted 347  - onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/log_test]],  userContext[], errMsg[publish success]
17/03/2021 09:52:22,822 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onMessageReceived 375  - receive message, topic[$gateway/operation/result/AP9ZLEVFKT/log_test], message[{"type":"online","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"test_device","result":0}]}}]
```

#### Trying out disconnecting subdevice

Run the `main` function in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java). After both the device and the subdevice are connected, call `gatewaySubdevOffline()` to disconnect the subdevice. Below is the sample code:
```
private static void gatewaySubdevOffline() {
    try {
        Thread.sleep(2000);
        mqttconnection.gatewaySubdevOffline(mSubProductID, mSubDevName);// Disconnect the subdevice
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the subdevice is disconnected successfully. In the console, you can see that the status of the device has been updated to `offline`.
```
17/03/2021 09:58:36,324 [main] DEBUG TXGatewayConnection gatewaySubdevOffline 205  - set 9RW4A8OOFK & test_device to offline
17/03/2021 09:58:36,324 [main] INFO  TXMqttConnection publish 567  - Starting publish topic: $gateway/operation/AP9ZLEVFKT/log_test Message: {"payload":{"devices":[{"device_name":"test_device","product_id":"9RW4A8OOFK"}]},"type":"offline"}
17/03/2021 09:58:36,325 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onPublishCompleted 347  - onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/log_test]],  userContext[], errMsg[publish success]
17/03/2021 09:58:36,345 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onMessageReceived 375  - receive message, topic[$gateway/operation/result/AP9ZLEVFKT/log_test], message[{"type":"offline","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"test_device","result":0}]}}]
```

