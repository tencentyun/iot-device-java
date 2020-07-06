package com.tencent.iot.hub.device.android.core.log;

public interface TXMqttLogConstants {
    /**
     * 请求日志等级相关
     */
    String TYPE = "type";
    String LOG = "log";
    String LOG_LEVEL= "log_level";
    String GET_LOG_LEVEL = "get_log_level";
    String CLIENT_TOKEN = "clientToken";

    /**
     * 日志等级
     */
    int LEVEL_FATAL = 0;
    int LEVEL_ERROR = 1;
    int LEVEL_WARN  = 2;
    int LEVEL_INFO  = 3;
    int LEVEL_DEBUG = 4;
}
