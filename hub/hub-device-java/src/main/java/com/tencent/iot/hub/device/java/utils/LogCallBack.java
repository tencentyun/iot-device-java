package com.tencent.iot.hub.device.java.utils;

/**
 * 日志打印接口类
 */
public interface LogCallBack {

    /**
     * debug 级别方法
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    void debug(String tag, String msg);

    /**
     * info 级别方法
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    void info(String tag, String msg);

    /**
     * warn 级别方法
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    void warn(String tag, String msg);

    /**
     * error 级别方法
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    void error(String tag, String msg);
}
