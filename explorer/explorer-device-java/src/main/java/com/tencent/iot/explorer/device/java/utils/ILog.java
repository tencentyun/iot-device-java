package com.tencent.iot.explorer.device.java.utils;

public interface ILog {
    void debug(String tag, String msg);

    void info(String tag, String msg);

    void warn(String tag, String msg);

    void error(String tag, String msg);
}
