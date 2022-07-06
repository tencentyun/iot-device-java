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
Edit the configuration information in the [app-config.json](../../../hub-android-demo/src/main/assets/app-config.json) file.
```
{
  "PRODUCT_ID":        "",
  "DEVICE_NAME":       "",
  "DEVICE_PSK":        "",
  "SUB_PRODUCT_ID":    "",
  "SUB_DEV_NAME":      "",
  "SUB_PRODUCT_KEY":   "",
  "TEST_TOPIC":        "",
  "SHADOW_TEST_TOPIC": "",
  "PRODUCT_KEY":       ""
}
```
You need to enter the `PRODUCT_ID` (gateway product ID), `DEVICE_NAME` (gateway device name), `DEVICE_PSK` (gateway device key), `SUB_PRODUCT_ID` (subproduct ID), `SUB_DEV_NAME` (subdevice name), and `SUB_PRODUCT_KEY` (subdevice key) parameters of a key-authenticated device in `app-config.json`.

#### Trying out connecting subdevice

Please connect the device to MQTT for authenticated connection as instructed in [Device Connection Through MQTT over TCP](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md) first.

Run the demo and click **Connect Subdevice** in the basic feature module to connect the subdevice. Below is the sample code:
```
mMQTTSample.setSubdevOnline(); // Connect the subdevice
```

The following logcat log represents the process in which the subdevice is connected successfully. In the console, you can see that the status of the device has been updated to `online`.
```
D/TXMQTT1.2.3: The hashed information is {9RW4A8OOFKdoor1=com.tencent.iot.hub.device.java.core.gateway.TXGatewaySubdev@9a3c239}
    set 9RW4A8OOFK & door1 to Online
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/AP9ZLEVFKT/gateway1 Message: {"type":"online","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1"}]}} {"type":"online","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1"}]}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
D/TXMQTT: receive command, topic[$gateway/operation/result/AP9ZLEVFKT/gateway1], message[{"type":"online","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1","result":0}]}}]
```

#### Trying out disconnecting subdevice

Please connect the device to MQTT for authenticated connection as instructed in [Device Connection Through MQTT over TCP](../../../hub-device-android/docs/en/PRELIM__基于TCP的MQTT设备接入_EN-US.md) first.

Run the demo and click **Disconnect Subdevice** in the basic feature module to disconnect the subdevice. Below is the sample code:
```
mMQTTSample.setSubDevOffline(); // Disconnect the subdevice
```

The following logcat log represents the process in which the subdevice is disconnected successfully. In the console, you can see that the status of the device has been updated to `offline`.
```
D/TXMQTT1.2.3: Try to find 9RW4A8OOFK & door1
    The hashed information is {9RW4A8OOFKdoor1=com.tencent.iot.hub.device.java.core.gateway.TXGatewaySubdev@27282a6}
    set 9RW4A8OOFK & door1 to offline
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/AP9ZLEVFKT/gateway1 Message: {"type":"offline","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1"}]}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
D/TXMQTT: receive command, topic[$gateway/operation/result/AP9ZLEVFKT/gateway1], message[{"type":"offline","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1","result":0}]}}]
```

