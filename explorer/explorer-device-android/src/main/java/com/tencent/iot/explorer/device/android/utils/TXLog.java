package com.tencent.iot.explorer.device.android.utils;

import android.content.Context;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import com.tencent.iot.hub.device.java.utils.LogCallBack;


/**
 * 日志类
 */
public class TXLog {
    private static final String TAG = "mars.xlog.log";

    /**
     * 日志级别 VERBOSE
     */
    public static final int LEVEL_VERBOSE = 0;
    /**
     * 日志级别 DEBUG
     */
    public static final int LEVEL_DEBUG = 1;
    /**
     * 日志级别 INFO
     */
    public static final int LEVEL_INFO = 2;
    /**
     * 日志级别 WARNING
     */
    public static final int LEVEL_WARNING = 3;
    /**
     * 日志级别 ERROR
     */
    public static final int LEVEL_ERROR = 4;
    /**
     * 日志级别 FATAL
     */
    public static final int LEVEL_FATAL = 5;
    /**
     * 日志级别 NONE
     */
    public static final int LEVEL_NONE = 6;

    /**
     * 上下文
     */
    public static Context toastSupportContext = null;

    /**
     * 日志接口类
     */
    public interface LogImp extends LogCallBack {

        void logV(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

        void logI(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

        void logD(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

        void logW(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

        void logE(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

        void logF(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

        int getLogLevel();

        void setLogLevel(final int level);

        @Override
        void debug(String tag, String msg);

        @Override
        void info(String tag, String msg);

        @Override
        void warn(String tag, String msg);

        @Override
        void error(String tag, String msg);
    }

    private static LogImp logImp = new TXLogImpl();

    /**
     * 设置日志实例
     *
     * @param imp 日志实例
     */
    public static void setLogImp(LogImp imp) {
        logImp = imp;
    }

    /**
     * 获取日志实例
     *
     * @return 日志实例
     */
    public static LogImp getImpl() {
        return logImp;
    }

    /**
     * 获取日志级别
     *
     * @return 日志级别
     */
    public static int getLogLevel() {
        if (logImp != null) {
            return logImp.getLogLevel();
        }
        return LEVEL_NONE;
    }

    /**
     * 设置日志级别
     *
     * @param level 日志级别
     */
    public static void setLevel(final int level) {
        logImp.setLogLevel(level);
    }

    /**
     * use f(tag, format, obj) instead
     *
     * @param tag
     * @param msg
     */
    public static void f(final String tag, final String msg) {
        f(tag, msg, (Object[]) null);
    }

    /**
     * use e(tag, format, obj) instead
     *
     * @param tag
     * @param msg
     */
    public static void e(final String tag, final String msg) {
        e(tag, msg, (Object[]) null);
    }

    /**
     * 打印日志级别 ERROR 的日志
     *
     * @param tag 日志标记
     * @param msg 日志内容
     * @param tr 异常内容
     */
    public static void e(final String tag, final String msg, Throwable tr) {
        e(tag, tr, msg, (Object[]) null);
    }

    /**
     * use w(tag, format, obj) instead
     *
     * @param tag
     * @param msg
     */
    public static void w(final String tag, final String msg) {
        w(tag, msg, (Object[]) null);
    }

    /**
     * use i(tag, format, obj) instead
     *
     * @param tag
     * @param msg
     */
    public static void i(final String tag, final String msg) {
        i(tag, msg, (Object[]) null);
    }

    /**
     * use d(tag, format, obj) instead
     *
     * @param tag
     * @param msg
     */
    public static void d(final String tag, final String msg) {
        d(tag, msg, (Object[]) null);
    }

    /**
     * use v(tag, format, obj) instead
     *
     * @param tag
     * @param msg
     */
    public static void v(final String tag, final String msg) {
        v(tag, msg, (Object[]) null);
    }

    /**
     * 打印日志级别 FATAL 的日志
     *
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void f(String tag, final String format, final Object... obj) {
        if (logImp != null && LEVEL_FATAL >= logImp.getLogLevel()) {
            final String log = obj == null ? format : String.format(format, obj);
            logImp.logF(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    /**
     * 打印日志级别 ERROR 的日志
     *
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void e(String tag, final String format, final Object... obj) {
        if (logImp != null && LEVEL_ERROR >= logImp.getLogLevel()) {
            String log = obj == null ? format : String.format(format, obj);
            if (log == null) {
                log = "";
            }

            Log.e(tag, log);

            logImp.logE(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    /**
     * 打印日志级别 WARNING 的日志
     *
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void w(String tag, final String format, final Object... obj) {
        if (logImp != null && LEVEL_WARNING >= logImp.getLogLevel()) {
            String log = obj == null ? format : String.format(format, obj);
            if (log == null) {
                log = "";
            }

            Log.w(tag, log);

            logImp.logW(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    /**
     * 打印日志级别 INFO 的日志
     *
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void i(String tag, final String format, final Object... obj) {
        if (logImp != null && LEVEL_INFO >= logImp.getLogLevel()) {
            String log = obj == null ? format : String.format(format, obj);
            if (log == null) {
                log = "";
            }

            Log.i(tag, log);

            logImp.logI(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    /**
     * 打印日志级别 DEBUG 的日志
     *
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void d(String tag, final String format, final Object... obj) {
        if (logImp != null && LEVEL_DEBUG >= logImp.getLogLevel()) {
            String log = obj == null ? format : String.format(format, obj);
            if (log == null) {
                log = "";
            }

            Log.d(tag, log);

            logImp.logD(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    /**
     * 打印日志级别 VERBOSE 的日志
     *
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void v(String tag, final String format, final Object... obj) {
        if (logImp != null && LEVEL_VERBOSE >= logImp.getLogLevel()) {
            String log = obj == null ? format : String.format(format, obj);
            if (log == null) {
                log = "";
            }

            Log.v(tag, log);

            logImp.logV(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    /**
     * 打印日志级别 ERROR 的日志
     *
     * @param tag 日志标记
     * @param tr 异常内容
     * @param format 日志格式
     * @param obj 日志内容
     */
    public static void e(String tag, Throwable tr, final String format, final Object... obj) {
        if (logImp != null && LEVEL_ERROR >= logImp.getLogLevel()) {
            String log = obj == null ? format : String.format(format, obj);
            if (log == null) {
                log = "";
            }

            Log.e(tag, log, tr);

            log += "\n" + Log.getStackTraceString(tr);
            logImp.logE(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), log);
        }
    }

    private static final String SYS_INFO;

    static {
        final StringBuilder sb = new StringBuilder();
        try {
            sb.append("VERSION.RELEASE:[" + android.os.Build.VERSION.RELEASE);
            sb.append("] VERSION.CODENAME:[" + android.os.Build.VERSION.CODENAME);
            sb.append("] VERSION.INCREMENTAL:[" + android.os.Build.VERSION.INCREMENTAL);
            sb.append("] BOARD:[" + android.os.Build.BOARD);
            sb.append("] DEVICE:[" + android.os.Build.DEVICE);
            sb.append("] DISPLAY:[" + android.os.Build.DISPLAY);
            sb.append("] FINGERPRINT:[" + android.os.Build.FINGERPRINT);
            sb.append("] HOST:[" + android.os.Build.HOST);
            sb.append("] MANUFACTURER:[" + android.os.Build.MANUFACTURER);
            sb.append("] MODEL:[" + android.os.Build.MODEL);
            sb.append("] PRODUCT:[" + android.os.Build.PRODUCT);
            sb.append("] TAGS:[" + android.os.Build.TAGS);
            sb.append("] TYPE:[" + android.os.Build.TYPE);
            sb.append("] USER:[" + android.os.Build.USER + "]");
        } catch (Throwable e) {
            e.printStackTrace();
        }

        SYS_INFO = sb.toString();
    }

    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    public static String getSysInfo() {
        return SYS_INFO;
    }
}

