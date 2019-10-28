package com.qcloud.iot_explorer.mqtt;


public interface TXMqttConstants {

    /**
     * sdk版本号
     */
    String APPID = "12020126";

    /**
     * 腾讯云唯一连接地址
     */
    String DEFAULT_SERVER_URI = "ssl://iotcloud.tencentdevices.com:8883";

    /**
     * 云端保留主题的最大长度
     */
    int MAX_SIZE_OF_CLOUD_TOPIC = 128;

    /**
     * 连接标识字符串最大长度
     */
    int MAX_CONN_ID_LEN = 5;

    /**
     * 当前MQTT连接状态
     */
    enum ConnectStatus {

        kConnectIdle,

        kConnecting,

        kConnected,

        kConnectFailed,

        kDisconnected
    }

    /**
     * PingReq alarm action name
     */
    String PING_SENDER = TXAlarmPingSender.TAG + ".pingSender.";

    /**
     * Constant for wakelock
     */
    String PING_WAKELOCK = TXAlarmPingSender.TAG + ".client.";

    /**
     * MQTT Command Type
     */
    int PUBLISH = 0;

    int SUBSCRIBE = 1;

    int UNSUBSCRIBE = 2;

    /**
     * QOS等级
     */
    int QOS0 = 0;

    int QOS1 = 1;

    int QOS2 = 2;

    /**
     * 消息回调信息成功信息
     */
    String PUBLISH_SUCCESS = "publish success";

    String SUBSCRIBE_SUCCESS = "subscribe success";

    String SUBSCRIBE_FAIL = "subscribe fail";

    String UNSUBSCRIBE_SUCCESS = "unsubscribe success";

    String MQTT_SDK_VER = "1.3.0";
}
