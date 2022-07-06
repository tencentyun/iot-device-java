package com.tencent.iot.hub.device.java.core.common;


/**
 * 状态码
 */
public enum Status {
    /**
     * 操作成功
     */
    OK,

    /**
     * 操作失败
     */
    ERROR,

    /**
     * 表示结果可能是异步返回
     */
    NO_RESULT,

    /**
     * 参数无效
     */
    PARAMETER_INVALID,

    /**
     * 正在连接中
     */
    MQTT_CONNECT_IN_PROGRESS,


    /**
     * 未建立连接
     */
    MQTT_NO_CONN,

    /**
     * 主题未订阅
     */
    ERROR_TOPIC_UNSUBSCRIBED,

    /**
     * 表示超过JSON文档中的最大TOKEN数
     */
    ERR_MAX_JSON_TOKEN,

    /**
     * 表示文档请求数超并发
     */
    ERR_MAX_APPENDING_REQUEST,

    /**
     * 表示JSON构造错误
     */
    ERR_JSON_CONSTRUCT,

    /**
     *  子设备初始状态
     */
    SUBDEV_STAT_INIT,

    /**
     *  子设备在线状态
     */
    SUBDEV_STAT_ONLINE,

    /**
     *  子设备掉线状态
     */
    SUBDEV_STAT_OFFLINE,

    /**
     *  子设备不存在
     */
    SUBDEV_STAT_NOT_EXIST
}
