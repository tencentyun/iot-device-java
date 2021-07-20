package com.tencent.iot.hub.device.java.utils;

import com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志打印类
 */
public class Loggor {

    private static Logger logger = LoggerFactory.getLogger(Loggor.class);

    private static LogCallBack logCallback;

    /**
     * 设置日志打印的对象
     * @param _logger 打印日志的实体
     */
    public static void setLogger(Logger _logger) {
        logger = _logger;
    }

    /**
     * 设置日志接口对象，回调打印过过程
     *
     * @param callback 日志接口对象
     */
    public static void setLogCallback(LogCallBack callback) {
        logCallback = callback;
    }

    /**
     * 打印 error 级别日志
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    public static void error(String tag, String msg) {
        logger.error(msg);
        if (logCallback != null) {
            logCallback.error(tag, msg);
        }
    }

    /**
     * 打印 warn 级别日志
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    public static void warn(String tag, String msg) {
        logger.warn(msg);
        if (logCallback != null) {
            logCallback.warn(tag, msg);
        }
    }

    /**
     * 打印 info 级别日志
     *
     * @param tag 日志标记
     * @param msg 日志内容
     */
    public static void info(String tag, String msg) {
        logger.info(msg);
        if (logCallback != null) {
            logCallback.info(tag, msg);
        }
    }

    /**
     * 打印 debug 级别日志
     * @param tag 日志标记
     * @param msg 日志内容
     */
    public static void debug(String tag, String msg) {
        logger.debug(msg);
        if (logCallback != null) {
            logCallback.debug(tag, msg);
        }
    }
}
