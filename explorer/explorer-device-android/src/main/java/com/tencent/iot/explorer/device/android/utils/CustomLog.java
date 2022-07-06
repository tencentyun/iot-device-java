package com.tencent.iot.explorer.device.android.utils;

import com.tencent.iot.explorer.device.java.utils.ILog;


public class CustomLog implements ILog {

    @Override
    public void debug(String tag, String msg) {
        TXLog.d(tag, msg);
    }

    @Override
    public void info(String tag, String msg) {
        TXLog.i(tag, msg);
    }

    @Override
    public void warn(String tag, String msg) {
        TXLog.w(tag, msg);
    }

    @Override
    public void error(String tag, String msg) {
        TXLog.e(tag, msg);
    }
}
