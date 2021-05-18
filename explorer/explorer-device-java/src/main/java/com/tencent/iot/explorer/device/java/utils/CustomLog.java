package com.tencent.iot.explorer.device.java.utils;

import org.slf4j.Logger;

public class CustomLog implements ILog {

    private Logger logger;

    public CustomLog(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }
}
