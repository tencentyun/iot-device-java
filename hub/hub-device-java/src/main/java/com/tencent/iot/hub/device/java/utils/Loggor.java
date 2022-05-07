package com.tencent.iot.hub.device.java.utils;

import com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loggor {

    private static Logger logger = LoggerFactory.getLogger(Loggor.class);

    private static LogCallBack logCallback;

    public static void setLogger(Logger _logger) {
        logger = _logger;
    }

    public static void setLogCallback(LogCallBack callback) {
        logCallback = callback;
    }

    public static void error(String tag, String msg) {
        logger.error(msg);
        if (logCallback != null) {
            logCallback.error(tag, msg);
        }
    }

    public static void warn(String tag, String msg) {
        logger.warn(msg);
        if (logCallback != null) {
            logCallback.warn(tag, msg);
        }
    }

    public static void info(String tag, String msg) {
        logger.info(msg);
        if (logCallback != null) {
            logCallback.info(tag, msg);
        }
    }

    public static void debug(String tag, String msg) {
        logger.debug(msg);
        if (logCallback != null) {
            logCallback.debug(tag, msg);
        }
    }
}
