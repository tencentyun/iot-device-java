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

You need to enter the corresponding parameters in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java) as instructed in [Gateway Feature](../../hub-device-java/docs/Gateway-Feature.md) first to connect the gateway device to MQTT for authenticated connection.

Run the `main` function in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java). After the device is connected, call `setSubDevBinded()` to bind the subdevice to the specified gateway device. Below is the sample code:
```
private static void setSubDevBinded() {
    try {
        Thread.sleep(2000);
        mqttconnection.gatewayBindSubdev(mSubProductID, mSubDevName, mSubDevPsk);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the gateway device binds a subdevice successfully. You can refresh the subdevices under the gateway device in the console and select the corresponding bound subproduct to view the bound subdevice.
```
12/03/2021 09:42:41,978 [main] INFO  TXMqttConnection publish 557  - Starting publish topic: $gateway/operation/AP9ZLEVFKT/log_test Message: {"payload":{"devices":[{"random":724839,"device_name":"test_device","signmethod":"hmacsha256","signature":"ad7KTCgchgJUMfH8+XNUk/76fCxSnb3r9dtlP9pHFGA=","product_id":"9RW4A8OOFK","timestamp":1615513361,"authtype":"psk"}]},"type":"bind"}
12/03/2021 09:42:41,979 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onPublishCompleted 276  - onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/log_test]],  userContext[], errMsg[publish success]
12/03/2021 09:42:42,014 [MQTT Call: AP9ZLEVFKTlog_test] INFO  TXMqttConnection messageArrived 1119  - Received topic: $gateway/operation/result/AP9ZLEVFKT/log_test, id: 6, message: [{"type":"bind","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"test_device","result":0}]}}]
```

## Running demo to try out unbinding subdevice

You need to enter the corresponding parameters in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java) as instructed in [Gateway Feature](../../hub-device-java/docs/Gateway-Feature.md) first to connect the gateway device to MQTT for authenticated connection.

Run the `main` function in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java). After the device is connected, call `setSubDevUnbinded()` to unbind the subdevice from the specified gateway device. Below is the sample code:
```
private static void setSubDevUnbinded() {
    try {
        Thread.sleep(2000);
        mqttconnection.gatewayUnbindSubdev(mSubProductID, mSubDevName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}

mMQTTSample.setSubDevUnbinded(); // Unbind the subdevice
```

The following logcat log represents the process in which the gateway device unbinds a subdevice successfully. Refresh the subdevices under the gateway device in the console, select the corresponding bound subproduct, and you will see that the previously bound subdevice is no longer in the subdevice list, which indicates that it has been unbound successfully.
```
12/03/2021 09:49:21,833 [main] INFO  TXMqttConnection publish 557  - Starting publish topic: $gateway/operation/AP9ZLEVFKT/log_test Message: {"payload":{"devices":[{"device_name":"test_device","product_id":"9RW4A8OOFK"}]},"type":"unbind"}
12/03/2021 09:49:21,835 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onPublishCompleted 276  - onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/log_test]],  userContext[], errMsg[publish success]
12/03/2021 09:49:21,870 [MQTT Call: AP9ZLEVFKTlog_test] INFO  TXMqttConnection messageArrived 1119  - Received topic: $gateway/operation/result/AP9ZLEVFKT/log_test, id: 8, message: {"type":"unbind","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"test_device","result":0}]}}
```

## Running demo to try out querying device topological relationship

You need to enter the corresponding parameters in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java) as instructed in [Gateway Feature](../../hub-device-java/docs/Gateway-Feature.md) first to connect the gateway device to MQTT for authenticated connection.

Run the `main` function in [GatewaySampleTest.java](../src/test/java/com/tencent/iot/hub/device/java/core/gateway/GatewaySampleTest.java). After the device is connected, call `checkSubdevRelation()` to publish to the topic for querying the gateway device topological relationship. Below is the sample code:
```
private static void checkSubdevRelation() {
    try {
        Thread.sleep(2000);
        mqttconnection.getGatewaySubdevRealtion();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
mMQTTSample.checkSubdevRelation();// Query the gateway device topological relationship
```

The following logcat log represents the process in which the gateway device topological relationship is queried successfully. As can be seen, there is a `door1` subdevice under the `gateway1` gateway device.
```
12/03/2021 09:51:58,160 [main] INFO  TXMqttConnection publish 557  - Starting publish topic: $gateway/operation/AP9ZLEVFKT/log_test Message: {"type":"describe_sub_devices"}
12/03/2021 09:51:58,162 [MQTT Call: AP9ZLEVFKTlog_test] DEBUG GatewaySampleTest onPublishCompleted 276  - onPublishCompleted, status[OK], topics[[$gateway/operation/AP9ZLEVFKT/log_test]],  userContext[], errMsg[publish success]
12/03/2021 09:51:58,188 [MQTT Call: AP9ZLEVFKTlog_test] INFO  TXMqttConnection messageArrived 1119  - Received topic: $gateway/operation/result/AP9ZLEVFKT/log_test, id: 10, message: {"type":"describe_sub_devices","payload":{"devices":[{"product_id":"9RW4A8OOFK","device_name":"test_device1"},{"product_id":"9RW4A8OOFK","device_name":"test_device"}]}}
```
