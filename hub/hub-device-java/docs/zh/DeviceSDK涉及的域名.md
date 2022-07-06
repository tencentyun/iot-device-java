| 域名/API                                                  | 所属模块               | 对应文件                     | PythonSDK对应文件     |
| ------------------------------------------------------- | ---------------------- | --------------------------- | ---------------------|
| ssl://${ProductId}.iotcloud.tencentdevices.com:8883 | MQTT连接核心类| TXMqttConnection | [explorer.py](https://github.com/tencentyun/iot-device-python/blob/master/explorer/explorer.py) |
| wss://${productId}.iotcloud.tencentdevices.com:443 | Websocket-MQTT协议| TXWebSocketManager | [explorer.py](https://github.com/tencentyun/iot-device-python/blob/master/explorer/explorer.py) |
| http://ap-guangzhou.gateway.tencentdevices.com/register/dev| 动态注册 | TXMqttDynreg | [explorer.py](https://github.com/tencentyun/iot-device-python/blob/master/explorer/explorer.py) |
| http://devicelog.iot.cloud.tencent.com:80/cgi-bin/report-log| 日志上报 | TXMqttLogImpl |