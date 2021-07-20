package com.tencent.iot.hub.device.java.core.mqtt;

/**
 * MQTT 常量
 */
public interface TXMqttConstants {

    /**
     * sdk 版本号
     */
    String APPID = "12020126";

    /**
     * 腾讯云密钥认证唯一连接地址前缀
     */
    String PSK_PREFIX = "tcp://";

    /**
     * 腾讯云 tid 认证唯一连接地址前缀
     */
    String TID_PREFIX = "tid://";

    /**
     * 腾讯云证书认证唯一连接地址前缀
     */
    String CER_PREFIX = "ssl://";

    /**
     * 腾讯云连接地址
     */
    String QCLOUD_IOT_MQTT_DIRECT_DOMAIN = ".iotcloud.tencentdevices.com:";

    /**
     * 腾讯云证书认证端口
     */
    String MQTT_SERVER_PORT_CER = "8883";

    /**
     * 腾讯云密钥认证端口
     */
    String MQTT_SERVER_PORT_PSK = "1883";

    /**
     * 腾讯云TID认证端口
     */
    String MQTT_SERVER_PORT_TID = "1884";

    /**
     * 云端保留主题的最大长度
     */
    int MAX_SIZE_OF_CLOUD_TOPIC = 128;

    /**
     * 连接标识字符串最大长度
     */
    int MAX_CONN_ID_LEN = 5;

    /**
     * 当前 MQTT 连接状态
     */
    enum ConnectStatus {

        kConnectIdle,

        kConnecting,

        kConnected,

        kConnectFailed,

        kDisconnected
    }

    /**
     * ping 请求的 action
     */
    String PING_SENDER = TXAlarmPingSender.TAG + ".pingSender.";

    /**
     * 唤醒常量
     */
    String PING_WAKELOCK = TXAlarmPingSender.TAG + ".client.";

    // MQTT 信令类型
    /**
     * 发布信息
     */
    int PUBLISH = 0;
    /**
     * 订阅主题
     */
    int SUBSCRIBE = 1;
    /**
     * 取消订阅主题
     */
    int UNSUBSCRIBE = 2;

    /**
     * QOS等级
     */
    int QOS0 = 0;

    int QOS1 = 1;

    int QOS2 = 2;

    // 消息回调信息成功信息
    /**
     * 发布消息成功
     */
    String PUBLISH_SUCCESS = "publish success";
    /**
     * 订阅主题成功
     */
    String SUBSCRIBE_SUCCESS = "subscribe success";
    /**
     * 订阅主题失败
     */
    String SUBSCRIBE_FAIL = "subscribe fail";
    /**
     * 取消订阅主题成功
     */
    String UNSUBSCRIBE_SUCCESS = "unsubscribe success";
    /**
     * MQTT sdk 版本号
     */
    String MQTT_SDK_VER = "1.1.0";
}
