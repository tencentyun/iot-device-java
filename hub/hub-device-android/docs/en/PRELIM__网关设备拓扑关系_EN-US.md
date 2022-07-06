* [Gateway Device Topological Relationship](#Gateway-Device-Topological-Relationship)
  * [Overview](#Overview)
  * [Running demo to try out binding subdevice](#Running-demo-to-try-out-binding-subdevice)
  * [Running demo to try out unbinding subdevice](#Running-demo-to-try-out-unbinding-subdevice)
  * [Running demo to try out querying device topological relationship](#Running-demo-to-try-out-querying-device-topological-relationship)

# Gateway Device Topological Relationship
## Overview
A gateway device can bind and unbind subdevices under it through data communication with the cloud.

To implement this feature, the following two topics will be used:

* Data upstream topic (for publishing): `$gateway/operation/${productid}/${devicename}`
* Data downstream topic (for subscribing): `$gateway/operation/result/${productid}/${devicename}`

For more information on the data formats and parameters used by a gateway device to bind and unbind subdevices, please see [Topological Relationship Management](https://cloud.tencent.com/document/product/634/45960).

You can query the topological relationship of gateway subdevices through the gateway device.

To query gateway subdevices, you also need to use the above two topics with different request data formats as detailed below:

```
{
    "type": "describe_sub_devices"
}
```

## Running demo to try out binding subdevice

You need to enter the corresponding parameters in `app-config.json` as instructed in [Gateway Feature](../../../hub-device-android/docs/en/PRELIM__网关功能_EN-US.md) first to connect the gateway device to MQTT for authenticated connection.

Run the demo and click **Bind Subdevice** in the basic feature module to bind the subdevice to the specified gateway device. Below is the sample code:
```
mMQTTSample.setSubDevBinded(); // Bind the subdevice
```

The following logcat log represents the process in which the gateway device binds a subdevice successfully. You can refresh the subdevices under the gateway device in the console and select the corresponding bound subproduct to view the bound subdevice.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/AP9ZLEVFKT/gateway1 Message: {"type":"bind","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1","random":690831,"timestamp":1603246675,"signmethod":"hmacsha256","authtype":"psk","signature":"U4N6Z6SbO8AZtAB6c63fxC1pbMtPX+\/AJkHG9pAnQKs="}]}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
D/TXMQTT: receive command, topic[$gateway/operation/result/AP9ZLEVFKT/gateway1], message[{"type":"bind","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1","result":0}]}}]
```

## Running demo to try out unbinding subdevice

You need to enter the corresponding parameters in `app-config.json` as instructed in [Gateway Feature](../../../hub-device-android/docs/en/PRELIM__网关功能_EN-US.md) first to connect the gateway device to MQTT for authenticated connection.

Run the demo and click **Unbind Subdevice** in the basic feature module to unbind the subdevice from the specified gateway device. Below is the sample code:
```
mMQTTSample.setSubDevUnbinded(); // Unbind the subdevice
```

The following logcat log represents the process in which the gateway device unbinds a subdevice successfully. Refresh the subdevices under the gateway device in the console, select the corresponding bound subproduct, and you will see that the previously bound subdevice is no longer in the subdevice list, which indicates that it has been unbound successfully.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/AP9ZLEVFKT/gateway1 Message: {"type":"unbind","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1"}]}}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
D/TXMQTT: receive command, topic[$gateway/operation/result/AP9ZLEVFKT/gateway1], message[{"type":"unbind","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1","result":0}]}}]
```

## Running demo to try out querying device topological relationship

You need to enter the corresponding parameters in `app-config.json` as instructed in [Gateway Feature](../../../hub-device-android/docs/en/PRELIM__网关功能_EN-US.md) first to connect the gateway device to MQTT for authenticated connection.

Run the demo and click **Query Device Topological Relationship** in the basic feature module to publish to the topic for querying the gateway device topological relationship. Below is the sample code:
```
mMQTTSample.checkSubdevRelation();// Query the gateway device topological relationship
```

The following logcat log represents the process in which the gateway device topological relationship is queried successfully. As can be seen, there is a `door1` subdevice under the `gateway1` gateway device.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/AP9ZLEVFKT/gateway1 Message: {"type":"describe_sub_devices"}
D/TXMQTT: onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/gateway1]],  userContext[], errMsg[publish success]
D/TXMQTT: receive command, topic[$gateway/operation/result/AP9ZLEVFKT/gateway1], message[{"type":"describe_sub_devices","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"door1"}]}}]
```
