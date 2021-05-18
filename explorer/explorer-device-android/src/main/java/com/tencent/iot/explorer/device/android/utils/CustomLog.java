package com.tencent.iot.explorer.device.android.utils;

import com.tencent.iot.explorer.device.java.utils.ILog;

public class CustomLog implements ILog {

    private String tag;

    public CustomLog(String tag) {
        this.tag = tag;
    }
    @Override
    public void debug(String msg) {
        TXLog.d(tag, msg);
    }

    @Override
    public void info(String msg) {
        TXLog.i(tag, msg);
    }

    @Override
    public void warn(String msg) {
        TXLog.w(tag, msg);
    }

    @Override
    public void error(String msg) {
        TXLog.e(tag, msg);
    }
}
