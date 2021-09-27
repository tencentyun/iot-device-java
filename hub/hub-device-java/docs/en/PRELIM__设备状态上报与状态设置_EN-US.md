* [Device Status Reporting and Setting](#Device-Status-Reporting-and-Setting)
  * [Overview](#Overview)
  * [Compiling and running demo](#Compiling-and-running-demo)
  * [Entering parameters for authenticating device for connection](#Entering-parameters-for-authenticating-device-for-connection)
  * [Reporting device status information](#Reporting-device-status-information)
  * [Setting device target temperature](#Setting-device-target-temperature)
  * [Getting device shadow document](#Getting-device-shadow-document)

# Device Status Reporting and Setting
## Overview
This document describes how to try out updating the device attribute information and getting the device shadow document in a smart home scenario with the aid of the IoT Hub device SDK for Java. For more information, please see [Scenario 2: Device Status Reporting and Setting](https://cloud.tencent.com/document/product/634/11914).

To try out the features, you need to create products and devices as instructed in [Device Connection Preparations](https://cloud.tencent.com/document/product/634/14442).

## Compiling and running demo

Download the sample code of the IoT Hub SDK for Java demo, prepare the development environment, and check the dependencies of the SDK. For more information on how to develop a device shadow, please see [Device Interconnection](../../../hub-device-java/docs/en/PRELIM__设备互通_EN-US.md#Compiling-and-running-demo), [Device Shadow](https://cloud.tencent.com/document/product/634/11918), and [Device Shadow Data Flow](https://cloud.tencent.com/document/product/634/14072).

## Entering parameters for authenticating device for connection

Please enter the required parameters in the [ShadowSampleTest.java](../../../hub-device-java/src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java) file.

```
public class ShadowSampleTest {
    private static String testProductIDString = "YOUR_PRODUCT_ID"; // Product ID
    private static String testDeviceNameString = "YOUR_DEVICE_NAME"; // Device name
    private static String testPSKString = "YOUR_PSK"; // Device key (for key authentication)
}
```
Key authentication is used as an example here.

## Reporting device status information

Run [ShadowSampleTest.java](../../../hub-device-java/src/test/java/com/tencent/iot/hub/device/java/core/shadow/ShadowSampleTest.java), initialize `mShadowConnection`, and connect the device shadow to MQTT through authenticated connection. The device shadow updates the device attribute information by calling the `update` API, subscribing to its own `$shadow/operation/result/${productId}/${deviceName}` topic first, and then publishing the attribute information to be reported through the `$shadow/operation/${productId}/${deviceName}` topic. Below is the sample code:

```
public static void main(String[] args) {
...
    mShadowConnection = new TXShadowConnection(testProductIDString, testDeviceNameString, testPSKString, new callback());
    mShadowConnection.connect(options, null);

    while(pubCount < testCnt) {
        pubCount += 1;
        Thread.sleep(20000);
        if (pubCount < 3) {  // Update the device shadow
            List<DeviceProperty>  mDevicePropertyList = new ArrayList<>();
            DeviceProperty deviceProperty1 = new DeviceProperty();
            deviceProperty1.key("updateCount").data(String.valueOf(pubCount)).dataType(TXShadowConstants.JSONDataType.INT);
            mShadowConnection.registerProperty(deviceProperty1);
            DeviceProperty deviceProperty2 = new DeviceProperty();
            deviceProperty2.key("energyConsumption").data(String.valueOf(10+pubCount)).dataType(TXShadowConstants.JSONDataType.INT);
            mShadowConnection.registerProperty(deviceProperty2);
            DeviceProperty deviceProperty3 = new DeviceProperty();
            deviceProperty3.key("temperatureDesire").data(String.valueOf(25)).dataType(TXShadowConstants.JSONDataType.INT);
            mShadowConnection.registerProperty(deviceProperty3);
            mDevicePropertyList.add(deviceProperty1);
            mDevicePropertyList.add(deviceProperty2);
            mDevicePropertyList.add(deviceProperty3);
            mShadowConnection.update(mDevicePropertyList, null);
        }
...
    }
}
public static class callback extends TXShadowActionCallBack{
    /**
     * Callback API for device attribute update
     * @param propertyJSONDocument Original device attribute document in JSON format received from the cloud
     * @param propertyList   Updated device attribute set
     */
    public void onDevicePropertyCallback(String propertyJSONDocument, List<DeviceProperty> propertyList) {
        System.out.println("onDevicePropertyCallback " +propertyList);
    }
}
```

Observe the logcat log.
```
TXMqttConnection connect 297  - Start connecting to ssl://XMN6AZ4M0Y.iotcloud.tencentdevices.com:8883
TXShadowConnection onConnectCompleted 633  - onConnectCompleted, status[OK], reconnect[false], msg[connected to ssl://XMN6AZ4M0Y.iotcloud.tencentdevices.com:8883]
TXMqttConnection subscribe 633  - Starting subscribe topic: $shadow/operation/result/XMN6AZ4M0Y/airConditioner1
TXMqttConnection subscribe 633  - Starting subscribe topic: XMN6AZ4M0Y/airConditioner1/data
TXShadowConnection onSubscribeCompleted 675  - onSubscribeCompleted, status[OK], errMsg[subscribe success], topics[[$shadow/operation/result/XMN6AZ4M0Y/airConditioner1]]
TXShadowConnection onSubscribeCompleted 675  - onSubscribeCompleted, status[OK], errMsg[subscribe success], topics[[XMN6AZ4M0Y/airConditioner1/data]]
TXMqttConnection publish 451  - Starting publish topic: $shadow/operation/XMN6AZ4M0Y/airConditioner1 Message: {"clientToken":"XMN6AZ4M0YairConditioner1-0","state":{"reported":{"energyConsumption":11,"updateCount":1,"temperatureDesire":25}},"type":"update","version":0}
```
The above log represents the process in which the device shadow is connected to MQTT successfully, publishes to its own topic successfully, and carries the relevant attribute information. In the console, you can see that the status of the created device has been changed to `online`, and the device shadow has reported the latest attribute information.

## Setting device target temperature

Prerequisites: the device has been authenticated and connected and has subscribed to the `$shadow/operation/result/${productId}/${deviceName}` topic. Call the [UpdateDeviceShadow](https://console.cloud.tencent.com/api/explorer?Product=iotcloud&Version=2018-06-14&Action=UpdateDeviceShadow&SignVersion=) RESTful API to simulate publishing the target temperature configuration on the home appliance management backend. The RESTful API request parameters such as `deviceName=airConditioner1`, `state={"desired" : {"temperatureDesire": 10}}`, `ProductId=XMN6AZ4M0Y`, and `ShadowVersion` should match the version in the latest device shadow in the console. The desired temperature is 10°C.

Observe the logcat log.
```
TXMqttConnection messageArrived 879  - Received topic: $shadow/operation/result/XMN6AZ4M0Y/airConditioner1, id: 0, message: {"clientToken":"XMN6AZ4M0YairConditioner1-2","payload":{"state":{"delta":{"temperatureDesire":10},"desired":{"temperatureDesire":10},"reported":{"energyConsumption":12,"status":1,"temperatureDesire":25,"updateCount":2}},"timestamp":1603269222843,"version":25},"result":0,"timestamp":1603269269,"type":"get"}
onDevicePropertyCallback [DeviceProperty{mKey='temperatureDesire', mData='10', mDataType=INT}]
```
The above log represents the process in which the device receives the `delta` message delivered by the cloud. The attribute to be updated is `temperatureDesire`, and the target temperature to be set is 10°C. After the `delta` information is updated, the empty `desired` information is reported.

## Getting device shadow document

The device publishes the `get` message to the `$shadow/operation/${productId}/${deviceName}` topic. Below is the sample code:

```
mShadowConnection.get(null);
```

Observe the logcat log.
```
TXMqttConnection publish 451  - Starting publish topic: $shadow/operation/XMN6AZ4M0Y/airConditioner1 Message: {"clientToken":"XMN6AZ4M0YairConditioner1-2","type":"get"}
onRequestCallback{"state":{"desired":{"temperatureDesire":10},"delta":{"temperatureDesire":10},"reported":{"energyConsumption":12,"updateCount":2,"temperatureDesire":25,"status":1}},"version":32,"timestamp":1603269797600}
```
The above log represents the process in which a topic message is successfully published with the `type` being `get`. In the console, you can see that the latest device shadow document is the same as the pulled document.
