* [Gateway Use Cases](#Gateway-Use-Cases)
  * [Creating device in console](#Creating-device-in-console)
     *  [Key authentication for connection](#Key-authentication-for-connection)
     *  [Certificate authentication for connection](#Certificate-authentication-for-connection)
     *  [Data template import in JSON](#Data-template-import-in-JSON)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Running demo for gateway device connection through authenticated MQTT connection](#Running-demo-for-gateway-device-connection-through-authenticated-MQTT-connection)
  * [Disconnecting gateway](#Disconnecting-gateway)
  * [Binding subdevice](#Binding-subdevice)
  * [Unbinding subdevice](#Unbinding-subdevice)
  * [Adding smart light device](#Adding-smart-light-device)
  * [Deleting smart light device](#Deleting-smart-light-device)
  * [Connecting smart light device](#Connecting-smart-light-device)
  * [Disconnecting smart light device](#Disconnecting-smart-light-device)

# Gateway Use Cases

This document describes the use cases of a gateway device in the SDK demo.

## Creating device in console

To use the gateway demo, you need to create a gateway device, a smart light device, and an air conditioner device in the IoT Explorer console. For more information, please see [Gateway Device Connection](https://cloud.tencent.com/document/product/1081/43417). **Note: you need to bind the smart light device and the air conditioner device to the gateway device as subdevices.**

#### Key authentication for connection

Edit the parameter configuration information in the [unit_test_config.json](../../src/test/resources/unit_test_config.json) file in the demo.
```
{
  "TESTGATEWAYSAMPLE_PRODUCT_ID":        "",
  "TESTGATEWAYSAMPLE_DEVICE_NAME":       "",
  "TESTGATEWAYSAMPLE_DEVICE_PSK":        "",
  "TESTGATEWAYSAMPLE_SUB1_PRODUCT_ID":   "",
  "TESTGATEWAYSAMPLE_SUB1_DEV_NAME":     "",
  "TESTGATEWAYSAMPLE_SUB1_DEV_PSK":      "",
  "TESTGATEWAYSAMPLE_SUB2_PRODUCT_ID":   "",
  "TESTGATEWAYSAMPLE_SUB2_DEV_NAME":     "",
}
```
If key authentication is used during gateway device and subdevice creation in the console, you need to enter the `TESTGATEWAYSAMPLE_PRODUCT_ID` (product ID), `TESTGATEWAYSAMPLE_DEVICE_NAME` (device name), and `TESTGATEWAYSAMPLE_DEVICE_PSK` (device key) of the gateway device in `unit_test_config.json`. Key authentication is used in the demo, where `TESTGATEWAYSAMPLE_SUB1_PRODUCT_ID` (product ID of the smart light subdevice), `TESTGATEWAYSAMPLE_SUB1_DEV_NAME` (name of the smart light subdevice), `TESTGATEWAYSAMPLE_SUB1_DEV_PSK` (key of the smart light subdevice, which is required for subdevice binding), `TESTGATEWAYSAMPLE_SUB2_PRODUCT_ID` (product ID of the air conditioner subdevice), and `TESTGATEWAYSAMPLE_SUB2_DEV_NAME` (name of the air conditioner subdevice) are entered.

#### Certificate authentication for connection

Place the certificate and private key in the [resources](../../src/test/resources/) folder.

If certificate authentication is used during gateway device creation in the console, in addition to entering the `TESTGATEWAYSAMPLE_PRODUCT_ID` (product ID) and `TESTGATEWAYSAMPLE_DEVICE_NAME` (device name) in `unit_test_config.json`, you also need to set `mDevPSK` to `null` in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java) and set the `mDevCert(DEVICE_CERT_FILE_NAME)` (device certificate filename) and `mDevPriv(DEVICE_PRIVATE_KEY_FILE_NAME)` (device private key filename).

#### Data template import in JSON

You can set the data template for each product created in the console. The features of the data template are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

For the gateway device and subdevices in the demo, you need to import the JSON files downloaded from the console into the demo to standardize the data verification during attribute and event reporting. Please place the JSON files in the [resources](../../src/test/resources/) folder, set the `mJsonFileName` (JSON file name) in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java) for the gateway device, and set the `mSubDev1JsonFileName` (JSON file name) in the corresponding [ProductLight.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/samples/gateway/ProductLight.java) and [ProductAirconditioner.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/samples/gateway/ProductAirconditioner.java) for the subdevices.

## Running demo for gateway device connection through authenticated MQTT connection 

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java) and call the following code in `testMqttConnect` for authenticated gateway device connection.
```
mGatewaySample = new GatewaySample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mJsonFileName, mSubDev1ProductId, mSubDev2ProductId);// Initialize `mGatewaySample`
mGatewaySample.online();
```

Observe the logcat log.
```
24/02/2021 14:07:46,873 [main] INFO  GatewaySample online 63  - Using PSK
24/02/2021 14:07:47,141 [main] INFO  TXGatewayClient connect 478  - Start connecting to ssl://VOY2UGD9HH.iotcloud.tencentdevices.com:8883
iot.TXAlarmPingSenderRegister alarmreceiver to Context iot.TXAlarmPingSender.pingSender.VOY2UGD9HHgateway1
iot.TXAlarmPingSenderSchedule next alarm at 1614147107382
D/iot.TXAlarmPingSender: Alarm scheule using setExactAndAllowWhileIdle, next: 240000
24/02/2021 14:07:47,382 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXGatewayClient onSuccess 445  - onSuccess!
24/02/2021 14:07:47,383 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: $thing/down/property/VOY2UGD9HH/gateway1
24/02/2021 14:07:47,385 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: $thing/down/event/VOY2UGD9HH/gateway1
24/02/2021 14:07:47,385 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: $thing/down/action/VOY2UGD9HH/gateway1
24/02/2021 14:07:47,385 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection subscribe 674  - Starting subscribe topic: $gateway/operation/result/VOY2UGD9HH/gateway1
24/02/2021 14:07:47,385 [MQTT Call: VOY2UGD9HHgateway1] DEBUG TXGatewayClient onSuccess 452  - Connected, then subscribe the gateway result topic
24/02/2021 14:07:47,385 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection connectComplete 871  - connectComplete. reconnect flag is false
24/02/2021 14:07:47,398 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onSubscribeCompleted 237  - onSubscribeCompleted, status[OK], topics[[$thing/down/action/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[subscribe success]
24/02/2021 14:07:47,440 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onSubscribeCompleted 237  - onSubscribeCompleted, status[OK], topics[[$gateway/operation/result/VOY2UGD9HH/gateway1]], userContext[], errMsg[subscribe success]
24/02/2021 14:07:47,440 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onSubscribeCompleted 237  - onSubscribeCompleted, status[OK], topics[[$thing/down/property/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[subscribe success]
24/02/2021 14:07:47,440 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onSubscribeCompleted 237  - onSubscribeCompleted, status[OK], topics[[$thing/down/event/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```
The above log represents the process in which the gateway device connects to the cloud over MQTT successfully and subscribes to the [data template protocol](https://cloud.tencent.com/document/product/1081/34916) topic associated with it. In the console, you can see that the status of the gateway device has been updated to `online`.

## Disconnecting gateway

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the gateway device is connected successfully and subscribes to the topic, call `gatewayOffline()` to disconnect it from MQTT. Below is the sample code:
```
private static void gatewayOffline() {
    try {
        Thread.sleep(2000);
        mGatewaySample.offline();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

Observe the logcat log.
```
24/02/2021 15:25:35,157 [main] INFO  TXMqttConnection unSubscribe 712  - Starting unSubscribe topic: $thing/down/property/VOY2UGD9HH/gateway1
24/02/2021 15:25:35,157 [main] INFO  TXMqttConnection unSubscribe 712  - Starting unSubscribe topic: $thing/down/event/VOY2UGD9HH/gateway1
24/02/2021 15:25:35,157 [main] INFO  TXMqttConnection unSubscribe 712  - Starting unSubscribe topic: $thing/down/action/VOY2UGD9HH/gateway1
24/02/2021 15:25:35,167 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onUnSubscribeCompleted 262  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/event/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=16}], errMsg[unsubscribe success]
24/02/2021 15:25:35,172 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onUnSubscribeCompleted 262  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/property/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=15}], errMsg[unsubscribe success]
24/02/2021 15:25:35,172 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onUnSubscribeCompleted 262  - onUnSubscribeCompleted, status[OK], topics[[$thing/down/action/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=17}], errMsg[unsubscribe success]
iot.TXAlarmPingSenderUnregister alarmreceiver to Context VOY2UGD9HHgateway1
24/02/2021 15:25:35,173 [MQTT Disc: VOY2UGD9HHgateway1] DEBUG GatewaySample onDisconnectCompleted 209  - onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=1}], msg[disconnected to ssl://VOY2UGD9HH.iotcloud.tencentdevices.com:8883]
```
The above log represents the process in which the gateway device successfully closes its authenticated connection with MQTT and unsubscribes from the [data template protocol](https://cloud.tencent.com/document/product/1081/34916) topic associated with it. In the console, you can see that the status of the gateway device has been updated to `offline`.

## Binding subdevice

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the device is connected successfully and subscribes to the topic, call `gatewayBindSubdev(mSubDev1ProductId,mSubDev1DeviceName,mSubDev1DevicePSK)` to bind the subdevices to the specified gateway device. Below is the sample code:
```
private static void gatewayBindSubdev(String productId, String deviceName, String devicePsk) {
    try {
        Thread.sleep(2000);
        mGatewaySample.gatewayBindSubdev(productId, deviceName, devicePsk);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the gateway device binds a subdevice successfully. You can refresh the subdevices under the gateway device in the console and select the corresponding bound subproduct to view the bound subdevice.
```
24/02/2021 17:06:47,974 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $gateway/operation/VOY2UGD9HH/gateway1 Message: {"payload":{"devices":[{"random":592432,"device_name":"light1","signmethod":"hmacsha256","signature":"IA3zqP2BfedQ8Vb2dtVCRhfrV80u4kBBrhd5Ec2fgjQ=","product_id":"LWVUL5SZ2L","timestamp":1614157607,"authtype":"psk"}]},"type":"bind"}
24/02/2021 17:06:47,987 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onPublishCompleted 228  - onPublishCompleted, status[OK], topics[[$gateway/operation/VOY2UGD9HH/gateway1]],  userContext[], errMsg[publish success]
24/02/2021 17:06:48,014 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection messageArrived 931  - Received topic: $gateway/operation/result/VOY2UGD9HH/gateway1, id: 6, message: {"type":"bind","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1","result":0}]}}
```

## Unbinding subdevice

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the device is connected successfully and subscribes to the topic, call `gatewayUnbindSubdev(mSubDev1ProductId,mSubDev1DeviceName)` to unbind the subdevices from the specified gateway device. Below is the sample code:
```
private static void gatewayUnbindSubdev(String productId, String deviceName) {
    try {
        Thread.sleep(2000);
        mGatewaySample.gatewayUnbindSubdev(productId, deviceName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

The following logcat log represents the process in which the gateway device unbinds a subdevice successfully. Refresh the subdevices under the gateway device in the console, select the corresponding bound subproduct, and you will see that the previously bound subdevice is no longer in the subdevice list, which indicates that it has been unbound successfully.
```
24/02/2021 17:26:47,995 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $gateway/operation/VOY2UGD9HH/gateway1 Message: {"payload":{"devices":[{"device_name":"light1","product_id":"LWVUL5SZ2L"}]},"type":"unbind"}
24/02/2021 17:26:48,003 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onPublishCompleted 228  - onPublishCompleted, status[OK], topics[[$gateway/operation/VOY2UGD9HH/gateway1]],  userContext[], errMsg[publish success]
24/02/2021 17:26:48,034 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection messageArrived 931  - Received topic: $gateway/operation/result/VOY2UGD9HH/gateway1, id: 8, message: {"type":"unbind","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1","result":0}]}}
```

## Adding smart light device

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the device is connected successfully and subscribes to the topic, call `gatewayAddSubDev(mSubDev1ProductId,mSubDev1DeviceName)` to add the smart light device to the subdevice list of the gateway device. Below is the sample code:

```
private static void gatewayAddSubDev(String productId, String deviceName) {
    try {
        Thread.sleep(2000);
        mGatewaySample.addSubDev(productId,deviceName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

Observe the logcat log.
```
24/02/2021 15:25:31,154 [main] DEBUG TXGatewayClient findSubdev 54  - input product id is LWVUL5SZ2L, input device name is light1
24/02/2021 15:25:31,155 [main] DEBUG TXGatewayClient findSubdev 55  - The hashed information is {LWVUL5SZ2Llight1=com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdev@27a5f880}
```
The above log represents the process in which the gateway device adds the smart light device as its subdevice successfully.

## Deleting smart light device

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the device is connected successfully and subscribes to the topic, call `gatewayDelSubDev(mSubDev1ProductId,mSubDev1DeviceName)` to remove the smart light device from the subdevice list of the gateway device. Below is the sample code:

```
private static void gatewayDelSubDev(String productId, String deviceName) {
    try {
        Thread.sleep(2000);
        mGatewaySample.delSubDev(productId,deviceName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

## Connecting smart light device

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the device is connected successfully and subscribes to the topic, call `gatewayOnlineSubDev(mSubDev1ProductId,mSubDev1DeviceName)` to publish the topic message for connecting the smart light. Below is the sample code:

```
private static void gatewayOnlineSubDev(String productId, String deviceName) {
    try {
        Thread.sleep(2000);
        mGatewaySample.onlineSubDev(productId,deviceName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

Observe the logcat log.
```
24/02/2021 17:33:50,015 [main] DEBUG TXGatewayClient subdevOnline 183  - set LWVUL5SZ2L & light1 to Online
24/02/2021 17:33:50,015 [main] DEBUG TXGatewayClient subdevOnline 201  - publish message {"payload":{"devices":[{"device_name":"light1","product_id":"LWVUL5SZ2L"}]},"type":"online"}
24/02/2021 17:33:50,015 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $gateway/operation/VOY2UGD9HH/gateway1 Message: {"payload":{"devices":[{"device_name":"light1","product_id":"LWVUL5SZ2L"}]},"type":"online"}
24/02/2021 17:33:50,016 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection deliveryComplete 955  - deliveryComplete, token.getMessageId:6
24/02/2021 17:33:50,017 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onPublishCompleted 228  - onPublishCompleted, status[OK], topics[[$gateway/operation/VOY2UGD9HH/gateway1]],  userContext[], errMsg[publish success]
24/02/2021 17:33:50,056 [MQTT Call: VOY2UGD9HHgateway1] DEBUG TXGatewayClient messageArrived 388  - message received $gateway/operation/result/VOY2UGD9HH/gateway1
24/02/2021 17:33:50,056 [MQTT Call: VOY2UGD9HHgateway1] DEBUG TXGatewayClient consumeGwOperationMsg 349  - got gate operation messga $gateway/operation/result/VOY2UGD9HH/gateway1{"type":"online","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1","result":0}]}}
```
The above log represents the process in which the gateway device successfully sends a message to the topic for connecting the smart light and receives a message from the topic for connecting the subdevice. For more information on the topic for proxying subdevice connection and disconnection, please see [Proxying Subdevice Connection and Disconnection](https://cloud.tencent.com/document/product/1081/47442).

## Disconnecting smart light device

Run the `testMqttConnect` function in [GatewaySampleTest.java](../../src/test/java/com/tencent/iot/explorer/device/java/core/gateway/GatewaySampleTest.java). After the device is connected successfully and subscribes to the topic, call `gatewayOfflineSubDev(mSubDev1ProductId,mSubDev1DeviceName)` to publish the topic message for disconnecting the smart light. Below is the sample code:

```
private static void gatewayOfflineSubDev(String productId, String deviceName) {
    try {
        Thread.sleep(2000);
        mGatewaySample.offlineSubDev(productId,deviceName);
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}
```

Observe the logcat log.
```
24/02/2021 17:33:52,016 [main] DEBUG TXGatewayClient subdevOffline 135  - Try to find LWVUL5SZ2L & light1
24/02/2021 17:33:52,016 [main] DEBUG TXGatewayClient findSubdev 54  - input product id is LWVUL5SZ2L, input device name is light1
24/02/2021 17:33:52,016 [main] DEBUG TXGatewayClient findSubdev 55  - The hashed information is {LWVUL5SZ2Llight1=com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdev@53f65459}
24/02/2021 17:33:52,016 [main] DEBUG TXGatewayClient subdevOffline 146  - set LWVUL5SZ2L & light1 to offline
24/02/2021 17:33:52,016 [main] DEBUG TXGatewayClient subdevOffline 163  - publish message {"payload":{"devices":[{"device_name":"light1","product_id":"LWVUL5SZ2L"}]},"type":"offline"}
24/02/2021 17:33:52,016 [main] INFO  TXMqttConnection publish 492  - Starting publish topic: $gateway/operation/VOY2UGD9HH/gateway1 Message: {"payload":{"devices":[{"device_name":"light1","product_id":"LWVUL5SZ2L"}]},"type":"offline"}
24/02/2021 17:33:52,017 [MQTT Call: VOY2UGD9HHgateway1] INFO  TXMqttConnection deliveryComplete 955  - deliveryComplete, token.getMessageId:15
24/02/2021 17:33:52,017 [MQTT Call: VOY2UGD9HHgateway1] DEBUG GatewaySample onPublishCompleted 228  - onPublishCompleted, status[OK], topics[[$gateway/operation/VOY2UGD9HH/gateway1]],  userContext[], errMsg[publish success]
24/02/2021 17:33:52,041 [MQTT Call: VOY2UGD9HHgateway1] DEBUG TXGatewayClient messageArrived 388  - message received $gateway/operation/result/VOY2UGD9HH/gateway1
24/02/2021 17:33:52,041 [MQTT Call: VOY2UGD9HHgateway1] DEBUG TXGatewayClient consumeGwOperationMsg 349  - got gate operation messga $gateway/operation/result/VOY2UGD9HH/gateway1{"type":"offline","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1","result":0}]}}
```
The above log represents the process in which the gateway device successfully sends a message to the topic for disconnecting the smart light and receives a message from the topic for disconnecting the subdevice. For more information on the topic for proxying subdevice connection and disconnection, please see [Proxying Subdevice Connection and Disconnection](https://cloud.tencent.com/document/product/1081/47442).
