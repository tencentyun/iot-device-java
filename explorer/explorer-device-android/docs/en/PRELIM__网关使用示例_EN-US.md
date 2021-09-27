* [Gateway Use Cases](#Gateway-Use-Cases)
  * [Creating device in console](#Creating-device-in-console)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Running demo for gateway device connection through authenticated MQTT connection](#Running-demo-for-gateway-device-connection-through-authenticated-MQTT-connection)
  * [Disconnecting gateway](#Disconnecting-gateway)
  * [Adding smart light device](#Adding-smart-light-device)
  * [Deleting smart light device](#Deleting-smart-light-device)
  * [Connecting smart light device](#Connecting-smart-light-device)
  * [Disconnecting smart light device](#Disconnecting-smart-light-device)

# Gateway Use Cases

This document describes the use cases of a gateway device in the SDK demo.

## Creating device in console

To use the gateway demo, you need to create a gateway device, a smart light device, and an air conditioner device in the IoT Explorer console. For more information, please see [Gateway Device Connection](https://cloud.tencent.com/document/product/1081/43417). **Note: you need to bind the smart light device and the air conditioner device to the gateway device as subdevices.**

## Entering parameters for authenticating device for connection 

Edit the parameter configuration information in the [app-config.json](../../../device-android-demo/src/main/assets/app-config.json) file.
```
{
  "PRODUCT_ID":        "",
  "DEVICE_NAME":       "",
  "DEVICE_PSK":        "",
  "SUB_PRODUCT_ID":    "",
  "SUB_DEV_NAME":      "",
  "SUB_DEV_PSK":       "",
  "SUB_PRODUCT_ID2":   "",
  "SUB_DEV_NAME2":     "",
  "SUB_DEV_PSK2":      ""
}
```
If **key authentication** is used during device creation in the console, you need to enter `PRODUCT_ID` (gateway device product ID), `DEVICE_NAME` (gateway device name), `DEVICE_PSK` (gateway device key), `SUB_PRODUCT_ID` (smart light device product ID), `SUB_DEV_NAME` (smart light device name), `SUB_PRODUCT_ID2` (air conditioner device product ID), and `SUB_DEV_NAME2` (air conditioner device name) in `app-config.json`. Key authentication is used in the demo.

If **certificate authentication** is used during device creation in the console, in addition to entering `PRODUCT_ID` (product ID) and `DEVICE_NAME` (device name) and setting `DEVICE_PSK` (device key) to `null` in `app-config.json`, you also need to read the certificate through `AssetManager`, create the `assets` directory in the `explorer/device-android-demo/src/main` path of the demo, place the device certificate and private key in this directory, and configure `mDevCert` (device certificate file name) and `mDevPriv` (device private key file name) in `IoTGatewayFragment.java`.

You can set the data template for each product created in the console. The features of the data template are divided into three categories: attribute, event, and action. For more information on how to use a data template in the console, please see [Data Template](https://cloud.tencent.com/document/product/1081/44921).

For the devices in the demo, you need to import the JSON files downloaded from the console into the demo to standardize the data verification during attribute and event reporting. Please place the JSON files in the `assets` directory and set the `mJsonFileName` and `mSubDev1JsonFileName` (JSON file name) of the corresponding gateway device and subdevices.

## Running demo for gateway device connection through authenticated MQTT connection 

Run the demo and click **Connect Gateway** in the gateway demo module for authenticated gateway device connection. Below is the sample code:
```
mGatewaySample.online();
```

Observe the logcat log.
```
I/TXGatewaySample: Using PSK
I/TXGATEWAYCLIENT: Start connecting to ssl://VOY2UGD9HH.iotcloud.tencentdevices.com:8883
D/iot.TXAlarmPingSender: Register alarmreceiver to Context iot.TXAlarmPingSender.pingSender.VOY2UGD9HHgateway1
D/iot.TXAlarmPingSender: Schedule next alarm at 1603183885475
D/iot.TXAlarmPingSender: Alarm scheule using setExactAndAllowWhileIdle, next: 240000
I/TXGATEWAYCLIENT: onSuccess!
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $thing/down/property/VOY2UGD9HH/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $thing/down/event/VOY2UGD9HH/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $thing/down/action/VOY2UGD9HH/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting subscribe topic: $gateway/operation/result/VOY2UGD9HH/gateway1
D/TXGATEWAYCLIENT: Connected, then subscribe the gateway result topic
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: connectComplete. reconnect flag is false
D/TXGatewaySample: onSubscribeCompleted, status[OK], topics[[$thing/down/property/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=0}], errMsg[subscribe success]
D/TXGatewaySample: onSubscribeCompleted, status[OK], topics[[$thing/down/action/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=2}], errMsg[subscribe success]
D/TXGatewaySample: onSubscribeCompleted, status[OK], topics[[$gateway/operation/result/VOY2UGD9HH/gateway1]], userContext[], errMsg[subscribe success]
D/TXGatewaySample: onSubscribeCompleted, status[OK], topics[[$thing/down/event/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=1}], errMsg[subscribe success]
```
The above log represents the process in which the gateway device connects to the cloud over MQTT successfully and subscribes to the [data template protocol](https://cloud.tencent.com/document/product/1081/34916) topic associated with it. In the console, you can see that the status of the gateway device has been updated to `online`.

## Disconnecting gateway

Run the demo. After the gateway device is connected, click **Disconnect Gateway** in the gateway demo module to disconnect it from MQTT. Below is the sample code:
```
mGatewaySample.offline(); 
```

Observe the logcat log.
```
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $thing/down/property/VOY2UGD9HH/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $thing/down/event/VOY2UGD9HH/gateway1
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting unSubscribe topic: $thing/down/action/VOY2UGD9HH/gateway1
D/TXGatewaySample: onUnSubscribeCompleted, status[OK], topics[[$thing/down/property/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=3}], errMsg[unsubscribe success]
D/TXGatewaySample: onUnSubscribeCompleted, status[OK], topics[[$thing/down/event/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=4}], errMsg[unsubscribe success]
D/TXGatewaySample: onUnSubscribeCompleted, status[OK], topics[[$thing/down/action/VOY2UGD9HH/gateway1]], userContext[MQTTRequest{requestType='subscribeTopic', requestId=5}], errMsg[unsubscribe success]
D/iot.TXAlarmPingSender: Unregister alarmreceiver to Context VOY2UGD9HHgateway1
D/TXGatewaySample: onDisconnectCompleted, status[OK], userContext[MQTTRequest{requestType='disconnect', requestId=1}], msg[disconnected to ssl://VOY2UGD9HH.iotcloud.tencentdevices.com:8883]
```
The above log represents the process in which the gateway device successfully closes its authenticated connection with MQTT and unsubscribes from the [data template protocol](https://cloud.tencent.com/document/product/1081/34916) topic associated with it. In the console, you can see that the status of the gateway device has been updated to `offline`.

## Adding smart light device

Run the demo. After the gateway device is connected, click **Add Smart Light** in the gateway demo module to add the smart light device to the subdevice list of the gateway device. Below is the sample code:

```
Object obj =  mGatewaySample.addSubDev(mSubDev1ProductId, mSubDev1DeviceName);
if(null != obj) {
    mSubDev1 = (ProductLight)obj;
}
```

Observe the logcat log.
```
D/TXGATEWAYCLIENT: input product id is LWVUL5SZ2L, input device name is light1
    The hashed information is {LWVUL5SZ2Llight1=com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev@27282a6}
D/TXGATEWAYCLIENT: input product id is LWVUL5SZ2L, input device name is light1
D/TXGATEWAYCLIENT: The hashed information is {LWVUL5SZ2Llight1=com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev@27282a6}
```
The above log represents the process in which the gateway device adds the smart light device as its subdevice successfully.

## Deleting smart light device

Run the demo. After the gateway device is connected and the smart light is added, click **Delete Smart Light** in the gateway demo module to remove the smart light device from the subdevice list of the gateway device. Below is the sample code:

```
mGatewaySample.delSubDev(mSubDev1ProductId, mSubDev1DeviceName);
mSubDev1 = null;
```

## Connecting smart light device

Run the demo. After the gateway device is connected and the smart light is added, click **Connect** for the smart light in the gateway demo module to publish a message to the topic for connecting the smart light. Below is the sample code:

```
mGatewaySample.onlineSubDev(mSubDev1ProductId, mSubDev1DeviceName);
```

Observe the logcat log.
```
D/TXGATEWAYCLIENT: input product id is LWVUL5SZ2L, input device name is light1
    The hashed information is {LWVUL5SZ2Llight1=com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev@2f548f5}
    set LWVUL5SZ2L & light1 to Online
D/TXGATEWAYCLIENT: publish message {"type":"online","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1"}]}}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/VOY2UGD9HH/gateway1 Message: {"type":"online","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1"}]}}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: deliveryComplete, token.getMessageId:0
D/TXGatewaySample: onPublishCompleted, status[OK], topics[[$gateway/operation/VOY2UGD9HH/gateway1]],  userContext[], errMsg[publish success]
D/TXGATEWAYCLIENT: message received $gateway/operation/result/VOY2UGD9HH/gateway1
D/TXGATEWAYCLIENT: got gate operation messga $gateway/operation/result/VOY2UGD9HH/gateway1{"type":"online","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1","result":0}]}}
    input product id is LWVUL5SZ2L, input device name is light1
D/TXProductLight: dev[light1] online!
```
The above log represents the process in which the gateway device successfully sends a message to the topic for connecting the smart light and receives a message from the topic for connecting the subdevice. For more information on the topic for proxying subdevice connection and disconnection, please see [Proxying Subdevice Connection and Disconnection](https://cloud.tencent.com/document/product/1081/47442).

## Disconnecting smart light device

Run the demo. After the gateway device is connected and the smart light is added, click **Disconnect** for the smart light in the gateway demo module to publish a message to the topic for disconnecting the smart light. Below is the sample code:

```
mGatewaySample.offlineSubDev(mSubDev1ProductId, mSubDev1DeviceName);
```

Observe the logcat log.
```
D/TXGATEWAYCLIENT: Try to find LWVUL5SZ2L & light1
    input product id is LWVUL5SZ2L, input device name is light1
    The hashed information is {LWVUL5SZ2Llight1=com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev@77dca3}
    set LWVUL5SZ2L & light1 to offline
D/TXGATEWAYCLIENT: publish message {"type":"offline","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1"}]}}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: Starting publish topic: $gateway/operation/VOY2UGD9HH/gateway1 Message: {"type":"offline","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1"}]}}
I/com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection: deliveryComplete, token.getMessageId:0
D/TXGatewaySample: onPublishCompleted, status[OK], topics[[$gateway/operation/VOY2UGD9HH/gateway1]],  userContext[], errMsg[publish success]
D/TXGATEWAYCLIENT: message received $gateway/operation/result/VOY2UGD9HH/gateway1
    got gate operation messga $gateway/operation/result/VOY2UGD9HH/gateway1{"type":"offline","payload":{"devices":[{"product_id":"LWVUL5SZ2L","device_name":"light1","result":0}]}}
```
The above log represents the process in which the gateway device successfully sends a message to the topic for disconnecting the smart light and receives a message from the topic for disconnecting the subdevice. For more information on the topic for proxying subdevice connection and disconnection, please see [Proxying Subdevice Connection and Disconnection](https://cloud.tencent.com/document/product/1081/47442).
