package com.tencent.iot.explorer.device.java.utils;

import com.tencent.iot.hub.device.java.utils.Loggor;

import org.slf4j.Logger;

public class CustomLog implements ILog {

    public CustomLog(Logger logger) {
        Loggor.setLogger(logger);
    }

    @Override
    public void debug(String tag, String msg) {
        Loggor.debug(tag, msg);
    }

    @Override
    public void info(String tag, String msg) {
        Loggor.info(tag, msg);
    }

    @Override
    public void warn(String tag, String msg) {
        Loggor.warn(tag, msg);
    }

    @Override
    public void error(String tag, String msg) {
        Loggor.error(tag, msg);
    }
}
