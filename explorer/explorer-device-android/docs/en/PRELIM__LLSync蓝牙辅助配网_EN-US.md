* [Network Configuration Through Bluetooth LLSync](#Network-Configuration-Through-Bluetooth-LLSync)
  * [Enabling GATT service for network configuration through Bluetooth LLSync](#Enabling-GATT-service-for-network-configuration-through-Bluetooth-LLSync)

# Network Configuration Through Bluetooth LLSync

Network configuration through Bluetooth LLSync is mainly used to configure a network for devices that are both Bluetooth and Wi-Fi capable through Bluetooth. It creates a specified GATT service through Bluetooth, connects a mobile phone to the GATT server, and uses Bluetooth's wireless communication capabilities to transfer the information required for connecting IoT device such as SSID and password to the device, so as to connect the device to IoT Hub and implement features such as device binding. For more information, please see Section 7 Network Configuration Through Bluetooth in [LLSync Protocol](https://github.com/tencentyun/qcloud-iot-explorer-BLE-sdk-embedded/blob/master/docs/LLSync%E8%93%9D%E7%89%99%E8%AE%BE%E5%A4%87%E6%8E%A5%E5%85%A5%E5%8D%8F%E8%AE%AE.pdf). This document describes how to use this feature with the aid of the demo.

## Enabling GATT service for network configuration through Bluetooth LLSync

Run the IoT TRTC demo. Click **Configure Network Through LLSync**, enable the GATT service created by the device through Bluetooth, and broadcast the vendor data. The `Company ID` is `0xFEE7`.

Below is the sample code:
```
    if (mServer == null) {
        /**
         * Create an `LLSyncGattServer` instance.
         *
         * @param context    Pass in `Activity Context`
         * @param productId  Pass in the `productId` of the device
         * @param deviceName Pass in the `deviceName` of the device
         * @param mac        Pass in the MAC address of the device
         * @param callback   `LLSyncGattServer` callback method
         */
        mServer = new LLSyncGattServer(TRTCMainActivity.this, mProductID, mDevName, "FF:FF:FF:FF:FF:FF", new LLSyncGattServerCallback() {
             /**
              * Failed to create the `LLSyncGattServer` instance and enable the GATT service
              *
              * @param errorMessage    Failure cause
              */
            @Override
            public void onFailure(String errorMessage) {
                Log.d(TAG, "LLSyncGattServer onFailure : " + errorMessage);
            }

             /**
              * Call back the information transferred from the mobile phone, which is required for establishing a Wi-Fi connection, such as SSID and password
              *
              * @param ssid        Wi-Fi SSID
              * @param password    Wi-Fi password
              */
            @Override
            public void requestConnectWifi(String ssid, String password) {
                Log.d(TAG, "LLSyncGattServer requestConnectWifi ssid: " + ssid + "; password: " + password);

                // The Wi-Fi SSID and password transferred from the mobile phone are required for connection to the network

                WifiUtils.connectWifiApByNameAndPwd(TRTCMainActivity.this, ssid, password, new WifiUtils.WifiConnectCallBack() {
                    @Override
                    public void connnectResult(boolean connectResult) {
                        Log.d(TAG, "WifiUtils connnectResult connectResult: " + connectResult);
                        TimerTask task = new TimerTask(){
                            public void run(){
                                if (connectResult) {

                                    // Connect the device to MQTT after successful Wi-Fi connection

                                    mConnectBtn.callOnClick(); // Connect to MQTT
                                }
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(task, 5000);// Set a delay of 5s to avoid the situation where MQTT cannot be connected to right after the Wi-Fi network switch
                    }
                });
            }

             /**
              * Call back the token transferred from the mobile phone
              *
              * @param token        The token for the application to bind with the device
              */
            @Override
            public void requestAppBindToken(String token) {
                Log.d(TAG, "LLSyncGattServer requestAppBindToken : " + token);

                // The `appBindToken` in `mDataTemplateSample` needs to be called to bind the application to the device

                mDataTemplateSample.appBindToken(token);
            }
        });
    }

    ...

    /**
     * Implement the `TXMqttActionCallBack` callback API
     */
    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {

            if (mServer != null) { // The mobile phone needs to be notified when there is a result of the MQTT connection
                mServer.noticeAppConnectWifiIsSuccess(status == Status.OK);
            }
        }
        ...
    }

```

Observe the logcat log.
```
D/LLSyncGattServer: Bluetooth enabled...starting services
D/BluetoothGattServer: registerCallback()
D/BluetoothGattServer: registerCallback() - UUID=06cb2a16-26c0-4248-87e3-ac410c182c54
D/BluetoothGattServer: onServerRegistered() - status=0 serverIf=6
D/BluetoothGattServer: addService() - service: 0000fff0-65d0-4e20-b56a-e493541ba4e2
D/BluetoothGattServer: onServiceAdded() - handle=40 uuid=0000fff0-65d0-4e20-b56a-e493541ba4e2 status=0
I/LLSyncGattServer: LE Advertise Started.
D/BluetoothGattServer: onServerConnectionState() - status=0 serverIf=6 device=56:99:D0:E6:9A:1B
I/LLSyncGattServer: BluetoothDevice CONNECTED: 56:99:D0:E6:9A:1B
D/BluetoothGattServer: onPhyUpdate() - device=56:99:D0:E6:9A:1B, txPHy=2, rxPHy=2
I/LLSyncGattServer: onPhyUpdate56:99:D0:E6:9A:1B
D/BluetoothGattServer: onMtuChanged() - device=56:99:D0:E6:9A:1B, mtu=517
I/LLSyncGattServer: onMtuChanged56:99:D0:E6:9A:1B
D/LLSyncGattServer: Subscribe device to notifications: 56:99:D0:E6:9A:1B
I/LLSyncGattServer: Write Device Info e0
I/LLSyncGattServer: notify getDeviceInfo 0800130282050f6c6c73796e635f776966695f303031
I/LLSyncGattServer: Write Device Info 090000
I/LLSyncGattServer: notify setMTUSuccess 0b00020205
I/LLSyncGattServer: Write Device Info e101
I/LLSyncGattServer: notify setWifiModeSuccess e0000100
I/LLSyncGattServer: Write Device Info e2000d054c2d30303408696f743230323124
I/LLSyncGattServer: ssid: L-004; pwd: iot2021$
D/TRTCMainActivity: LLSyncGattServer requestConnectWifi ssid: L-004; password: iot123456
I/WifiUtils: newNetworkId is:8
I/WifiUtils: switched to the specified Wi-Fi network successfully
D/TRTCMainActivity: WifiUtils connnectResult connectResult: true
I/TXMQTT_1.1.0: Start connecting to tcp://Z52SO5W5OT.iotcloud.tencentdevices.com:1883
I/TRTCMainActivity: onConnectCompleted, status[OK], reconnect[false], userContext[MQTTRequest{requestType='connect', requestId=0}], msg[connected to tcp://Z52SO5W5OT.iotcloud.tencentdevices.com:1883]
I/LLSyncGattServer: notify getWifiInfoSuccess e1000100
I/LLSyncGattServer: Write Device Info e3
I/LLSyncGattServer: notify connectWifiIsSuccess e20009010000054c2d303034
I/LLSyncGattServer: Write Device Info e400203434396238323133353435316465333934396466613666646664376438623831
D/TRTCMainActivity: LLSyncGattServer requestAppBindToken : 449b82135451de3949dfa6fdfd7d8b81
I/LLSyncGattServer: notify bindAppSuccess e3000100
D/TRTCMainActivity: onPublishCompleted, status[OK], topics[[$thing/up/service/Z52SO5W5OT/llsync_wifi_001]],  userContext[], errMsg[publish success]
D/TRTCMainActivity: receive command, topic[$thing/down/service/Z52SO5W5OT/llsync_wifi_001], message[{"method":"app_bind_token_reply","clientToken":"Z52SO5W5OTllsync_wifi_00105889184-3d98-4853-9b40-d89901c1a7d9","code":0,"status":"success"}]
```
The above log represents the process of network configuration through Bluetooth LLSync. When the `"method":"app_bind_token_reply"` message is received, `"code":0,"status":"success"` indicates that the mobile phone is bound to the device successfully.

