* [LLSync蓝牙辅助配网](#LLSync蓝牙辅助配网)
  * [开启LLSync蓝牙辅助配网GATT服务](#开启LLSync蓝牙辅助配网GATT服务)

# LLSync蓝牙辅助配网

LLSync蓝牙辅助配网主要用于通过 BLE 给同时具有 BLE + Wi-Fi 能力的设备配置网络，通过 BLE 创建指定的 GATT服务，手机连接该 GATT SERVER，利用 BLE 的无线通信能力，将物联网设备连接所需的 SSID、PASSWORD等信息传输给设备端，使设备顺利接入物联网平台，继而完成设备绑定等功能，请参考 [LLSync 协议](https://github.com/tencentyun/qcloud-iot-explorer-BLE-sdk-embedded/blob/master/docs/LLSync%E8%93%9D%E7%89%99%E8%AE%BE%E5%A4%87%E6%8E%A5%E5%85%A5%E5%8D%8F%E8%AE%AE.pdf) 第七章节蓝牙辅助配网 。本文借助示例Demo主要描述如何使用LLSync蓝牙辅助配网功能。

## 开启LLSync蓝牙辅助配网GATT服务

运行示例程序IOT TRTC DEMO部分，点击`LLSYNC配网`按钮，开启设备通过蓝牙创建的GATT服务，广播厂商数据，Company ID是0xFEE7。

示例代码如下：
```
    if (mServer == null) {
        /**
         * 创建一个 LLSyncGattServer 实例.
         *
         * @param context    传入Activity Context
         * @param productId  传入设备productId
         * @param deviceName 传入设备deviceName
         * @param mac        传入设备mac地址
         * @param callback   LLSyncGattServer的回调方法
         */
        mServer = new LLSyncGattServer(TRTCMainActivity.this, mProductID, mDevName, "FF:FF:FF:FF:FF:FF", new LLSyncGattServerCallback() {
             /**
              * 创建 LLSyncGattServer 实例开启GATT服务失败.
              *
              * @param errorMessage    失败原因
              */
            @Override
            public void onFailure(String errorMessage) {
                Log.d(TAG, "LLSyncGattServer onFailure : " + errorMessage);
            }

             /**
              * 回调手机传来的连接WIFI所需的 SSID、PASSWORD等信息.
              *
              * @param ssid        WIFI的ssid
              * @param password    WIFI的密码password
              */
            @Override
            public void requestConnectWifi(String ssid, String password) {
                Log.d(TAG, "LLSyncGattServer requestConnectWifi ssid: " + ssid + "; password: " + password);

                // 需要通过手机传来的WIFI的ssid和password连接上该网络

                WifiUtils.connectWifiApByNameAndPwd(TRTCMainActivity.this, ssid, password, new WifiUtils.WifiConnectCallBack() {
                    @Override
                    public void connectResult(boolean connectResult) {
                        Log.d(TAG, "WifiUtils connectResult connectResult: " + connectResult);
                        TimerTask task = new TimerTask(){
                            public void run(){
                                if (connectResult) {

                                    // 连接WiFi成功后，使设备进行mqtt连接

                                    mConnectBtn.callOnClick(); //连接mqtt
                                }
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(task, 5000);//防止刚切换到wifi时，mqtt连接不上延迟5s
                    }
                });
            }

             /**
              * 回调手机传来的token.
              *
              * @param token        app希望和设备进行绑定的token
              */
            @Override
            public void requestAppBindToken(String token) {
                Log.d(TAG, "LLSyncGattServer requestAppBindToken : " + token);

                // 需要调用mDataTemplateSample中的appBindToken，进行app和设备之间的绑定。

                mDataTemplateSample.appBindToken(token);
            }
        });
    }

    ...

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {

            if (mServer != null) { //当mqtt连接有结果后需要告知手机
                mServer.noticeAppConnectWifiIsSuccess(status == Status.OK);
            }
        }
        ...
    }

```

观察Logcat日志。
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
I/WifiUtils: 切换到指定wifi成功
D/TRTCMainActivity: WifiUtils connectResult connectResult: true
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
以上是LLSync蓝牙辅助配网流程的日志，当收到"method":"app_bind_token_reply"的消息时，"code":0,"status":"success"即手机和设备进行绑定成功。

