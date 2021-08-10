package com.tencent.iot.hub.device.java.utils;

import com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志打印类
 */
public class Loggor {

    private static Logger logger = LoggerFactory.getLogger(Loggor.class);

    private static LogCallBack logCallback;

    /**
     * 开启控制台日志输出
     */
    public static void openConsoleLog() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        ConsoleAppender appender = new ConsoleAppender();
        PatternLayout layout = new PatternLayout();
        String conversionPattern = "%d{yyyy/MM/dd HH:mm:ss} %-5p %c{1} %M %L %x - %m%n";
        layout.setConversionPattern(conversionPattern);
        appender.setLayout(layout);
        appender.setEncoding("UTF-8");
        appender.setThreshold(Level.DEBUG);
        appender.activateOptions();
        rootLogger.addAppender(appender);
    }

    /**
     * 开启日志保存
     * @param path 日志保存路径
     */
    public static void saveLogs(String path) {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        MyDailyRollingFileAppender appender = new MyDailyRollingFileAppender();
        PatternLayout layout = new PatternLayout();
        String conversionPattern = "%d{yyyy/MM/dd HH:mm:ss} %-5p %c{1} %M %L %x - %m%n";
        layout.setConversionPattern(conversionPattern);
        appender.setLayout(layout);
        appender.setFile(path);
        appender.setEncoding("UTF-8");
        appender.setAppend(true);
        appender.setThreshold(Level.DEBUG);
        appender.activateOptions();
        rootLogger.addAppender(appender);
    }


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
