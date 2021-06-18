package com.tencent.iot.hub.device.java.utils;

public interface LogCallBack {

    void debug(String tag, String msg);

    void info(String tag, String msg);

    void warn(String tag, String msg);

    void error(String tag, String msg);
}
