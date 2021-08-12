package com.tencent.iot.hub.device.android.core.util;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.iot.hub.device.java.utils.Loggor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static com.tencent.iot.hub.device.android.core.util.TXLog.LEVEL_DEBUG;
import static com.tencent.iot.hub.device.android.core.util.TXLog.LEVEL_ERROR;
import static com.tencent.iot.hub.device.android.core.util.TXLog.LEVEL_INFO;
import static com.tencent.iot.hub.device.android.core.util.TXLog.LEVEL_VERBOSE;
import static com.tencent.iot.hub.device.android.core.util.TXLog.LEVEL_WARNING;

public class TXLogImpl implements TXLog.LogImp {

    private static volatile Context sContext;

    private static String packageName = "";

    static LinkedBlockingDeque<String> logDeque = new LinkedBlockingDeque<String>(15000);

    private static final int[] INTERVAL_RETRY_INIT = new int[]{1, 2, 4, 8, 16, 29}; //重试时间

    private static AtomicInteger retryInitTimes = new AtomicInteger(0);

    /**
     * 日期格式
     */
    public static final SimpleDateFormat timeFormatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    private static String logTime = "";

    private static String logPath = "";

    private static int logDuration = 7;

    static String nowUsedFile = "";

    static final ReentrantLock lock = new ReentrantLock();

    protected static Object formatterLock = new Object();

    private static long nextDayTime;

    private static long nextSecondMinuteTime;

    static long lastWriterErrorTime = 0;

    private static FileWriter writer;

    private static Handler retryInitHandler = new Handler(Looper.getMainLooper());

    private int logLevel = LEVEL_VERBOSE;

    /**
     * 初始化日志
     *
     * @param context 上下文
     */
    public static void init(Context context) {
        init(context, logDuration, logPath);
    }

    /**
     * 初始化日志
     *
     * @param context 上下文
     * @param duration 保存近${duration}天的日志
     */
    public static void init(Context context, int duration) {
        init(context, duration, logPath);
    }

    /**
     * 初始化日志
     *
     * @param context 上下文
     * @param duration 保存近${duration}天的日志
     * @param _logPath 保存路径
     */
    public static void init(Context context, int duration, String _logPath) {
        sContext = context;
        logDuration = duration;
        logPath = _logPath;
        Loggor.setLogCallback(TXLog.getImpl());
        initRunnable.run();
    }

    /**
     * 将日志写到文件
     *
     * @param log 日志内容
     */
    private synchronized static void writeLogToFile(String log) {
        try {
            // 如果SD卡不可用，则不写日志，以免每次都抛出异常，影响性能
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                System.out.println("writeLogToFile not ready");
                return;
            }

            if (null == writer) {
                System.out.println("can not write log.");
                long now = System.currentTimeMillis();
                if (lastWriterErrorTime == 0) {
                    lastWriterErrorTime = now;
                } else if (now - lastWriterErrorTime > 60 * 1000) {
                    try {
                        initLogFile(System.currentTimeMillis());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    lastWriterErrorTime = now;
                }
            } else {
                long now = System.currentTimeMillis();
                if (now > nextDayTime) {
                    initLogFile(now);
                }
                //加入消息的时候记录时间
                if (lock.tryLock()) {
                    try {
                        writer.write(log);
                        writer.flush();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    if (!insertLogToCacheHead(log)) {
                        System.out.println("insertLogToCacheHead failed!");
                    }
                }
            }

        } catch (Throwable e) {
            if (e instanceof IOException && e.getMessage().contains("ENOSPC")) {
                e.printStackTrace();
            } else {
                try {
                    initLogFile(System.currentTimeMillis());
                } catch (Throwable e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 写日志线程
     */
    static Thread takeThread = new Thread() {
        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    try {
                        String log;
                        log = logDeque.take();
                        if (null != log) {
                            writeLogToFile(log);
                        }
                    } catch (Exception e) {
                        System.out.println("write log file error: " + e.toString());
                    } catch (AssertionError ignore) {
                        System.out.println("--------------");
                    }
                }
            }
        }
    };

    private static String getDateStr(long nowCurrentMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowCurrentMillis);
        SimpleDateFormat logFileFormatter = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        logTime = timeFormatter.format(nowCurrentMillis);
        String thisLogName = logFileFormatter.format(calendar.getTime());
        setNextSecond(calendar);
        setNextHour(calendar);
        return thisLogName;
    }

    private static void setNextHour(Calendar setSecondedCalendar) {
        setSecondedCalendar.add(Calendar.DAY_OF_MONTH, 1);
        nextDayTime = setSecondedCalendar.getTimeInMillis();
    }

    private static void setNextSecond(Calendar calendar) {
        calendar.set(Calendar.MILLISECOND, 0);
        nextSecondMinuteTime = calendar.getTimeInMillis() + 1000;
    }

    /**
     * 获取日志文件名
     *
     * @param dataStr 日期
     * @return 日志文件名
     */
    public static String getLogFileName(String dataStr) {
        return "iot_" + dataStr + ".log";
    }

    private static synchronized void checkNextMinuteTime(long currentTimeMillis) {
        if (currentTimeMillis > nextSecondMinuteTime) {
            synchronized (formatterLock) {
                logTime = timeFormatter.format(currentTimeMillis);
                nextSecondMinuteTime = nextSecondMinuteTime + 1000;
            }
        }
    }

    /**
     * 获取日志路径
     *
     * @return 日志路径
     */
    public static String getLogPath() {
        return logPath;
    }

    /**
     * 初始化日志文件
     */
    static synchronized void initLogFile(long nowCurrentTimeMillis) throws IOException {
        File tmpeFile = new File(logPath);
        if (!tmpeFile.exists()) {
            tmpeFile.mkdirs();
        }
        nowUsedFile = logPath + getLogFileName(getDateStr(nowCurrentTimeMillis));
        try {
            tmpeFile = new File(nowUsedFile);
            if (!tmpeFile.exists()) {
                boolean b = tmpeFile.createNewFile();
                if (null != writer) {
                    writer.write(logTime + "|" + "|D|" + android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE + " create newLogFile " + tmpeFile.getName() + " " + b + "\n");
                    writer.flush();
                }
            } else {
                if (null != writer) {
                    writer.write(logTime + "|" + "|E|" + android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE + "|newLogFile " + tmpeFile.getName() + " is existed.\n");
                    writer.flush();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        writer = new FileWriter(tmpeFile, true);
    }

    static void deleteExpiredLogs(long today) {
        if (TextUtils.isEmpty(logPath)) {
            logPath = Environment.getExternalStorageDirectory().getPath() + "/tencent/" + packageName.replace(".", "/")
                    + "/";
        } else {
            logPath = Environment.getExternalStorageDirectory().getPath() + "/" + logPath;
        }
        long day = (long) 1 * 24 * 60 * 60 * 1000;
        HashSet<String> logNames = new HashSet<>(); // 保存需要保留的日志名字
        for (long i = today; i > today - logDuration* day; i = i - day) {
            String date = getDateStr(i);
            logNames.add(logPath + getLogFileName(date));
        }

        // 删除有效期外的所有日志文件
        File path = new File(logPath);
        File[] files = path.listFiles();
        if (files != null) {
            for (File log : files) {
                if (!logNames.contains(log.getAbsolutePath())) {
                    try {
                        log.delete();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 日志初始化 Runnable
     */
    public static Runnable initRunnable = new Runnable() {
        @Override
        public void run() {
            if (null == sContext) {
                return;
            }

            new Thread("QLogInitThread") {
                @Override
                public void run() {
                    try {
                        try {
                            packageName = sContext.getPackageName();
                        } catch (Exception e) {
                            packageName = "unknown";
                        }
                        deleteExpiredLogs(System.currentTimeMillis());
                        initLogFile(System.currentTimeMillis());

                        takeThread.setName("logWriteThread");
                        takeThread.start();
                        retryInitHandler.removeCallbacks(initRunnable);
                    } catch (Exception e) {
                        int times = retryInitTimes.get();
                        System.out.println("QLogImpl init post retry " + times + " times, interval " + INTERVAL_RETRY_INIT[times]);
                        retryInitHandler.removeCallbacks(initRunnable);
                        retryInitHandler.postDelayed(initRunnable, INTERVAL_RETRY_INIT[times] * 60000);
                        times++;
                        if (times >= INTERVAL_RETRY_INIT.length) {
                            times = 0;
                        }
                        retryInitTimes.set(times);
                    }
                }
            }.start();
        }
    };

    /**
     * 写日志
     *
     * @param level 日志级别
     * @param tag 日志标记
     * @param msg 日志内容
     * @param tr 异常内容
     */
    public static void writeLog(String level, String tag, String msg, Throwable tr) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        long threadId = Thread.currentThread().getId();
        String message = logTime + "|" + level + "|" + String.valueOf(threadId) + "|" + tag + "|" + msg + "\n";
        if (null != tr) {
            message = msg + "\n" + Log.getStackTraceString(tr) + "\n";
        }
        addLogToCache(message);
    }

    /**
     * 添加日志到缓存
     */
    private static boolean addLogToCache(String log) {
        try {
            logDeque.add(log);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加缓冲头部
     */
    private static boolean insertLogToCacheHead(String log) {
        try {
            logDeque.addFirst(log);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    enum LogLevel {

        OFF("OFF"),
        ERROR("ERROR"),
        WARN("WARN"),
        INFO("INFO"),
        DEBUG("DEBUG"),
        VERBOSE("VERBOSE");

        private String descr = "INFO";

        private LogLevel(String descr){
            this.descr = descr;
        }
    }

    /**
     * 打印 VERBOSE 级别的日志
     *
     * @param tag 日志标记
     * @param filename 日志文件名
     * @param funcname 函数名
     * @param line 行号
     * @param pid 进程 ID
     * @param tid 线程 ID
     * @param maintid 主线程 ID
     * @param log 日志内容
     */
    @Override
    public void logV(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.VERBOSE + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    /**
     * 打印 INFO 级别的日志
     *
     * @param tag 日志标记
     * @param filename 日志文件名
     * @param funcname 函数名
     * @param line 行号
     * @param pid 进程 ID
     * @param tid 线程 ID
     * @param maintid 主线程 ID
     * @param log 日志内容
     */
    @Override
    public void logI(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.INFO + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    /**
     * 打印 DEBUG 级别的日志
     *
     * @param tag 日志标记
     * @param filename 日志文件名
     * @param funcname 函数名
     * @param line 行号
     * @param pid 进程 ID
     * @param tid 线程 ID
     * @param maintid 主线程 ID
     * @param log 日志内容
     */
    @Override
    public void logD(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.DEBUG + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    /**
     * 打印 WARNING 级别的日志
     *
     * @param tag 日志标记
     * @param filename 日志文件名
     * @param funcname 函数名
     * @param line 行号
     * @param pid 进程 ID
     * @param tid 线程 ID
     * @param maintid 主线程 ID
     * @param log 日志内容
     */
    @Override
    public void logW(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.WARN + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    /**
     * 打印 ERROR 级别的日志
     *
     * @param tag 日志标记
     * @param filename 日志文件名
     * @param funcname 函数名
     * @param line 行号
     * @param pid 进程 ID
     * @param tid 线程 ID
     * @param maintid 主线程 ID
     * @param log 日志内容
     */
    @Override
    public void logE(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.ERROR + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    /**
     * 打印 FATAL 级别的日志
     *
     * @param tag 日志标记
     * @param filename 日志文件名
     * @param funcname 函数名
     * @param line 行号
     * @param pid 进程 ID
     * @param tid 线程 ID
     * @param maintid 主线程 ID
     * @param log 日志内容
     */
    @Override
    public void logF(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.OFF + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    /**
     * 获取日志级别
     *
     * @return 日志级别
     */
    @Override
    public int getLogLevel() {
        return logLevel;
    }

    /**
     * 设置日志级别
     *
     * @param level 日志级别
     */
    @Override
    public void setLogLevel(int level) {
        logLevel = level;
    }

    /**
     * 打印 DEBUG 级别的日志
     * @param tag 日志标记
     * @param msg 日志内容
     */
    @Override
    public void debug(String tag, String msg) {
        if (LEVEL_DEBUG >= getLogLevel()) {
            logD(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), msg);
        }
    }

    /**
     * 打印 INFO 级别的日志
     * @param tag 日志标记
     * @param msg 日志内容
     */
    @Override
    public void info(String tag, String msg) {
        if (LEVEL_INFO >= getLogLevel()) {
            logI(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), msg);
        }
    }

    /**
     * 打印 WARNING 级别的日志
     * @param tag 日志标记
     * @param msg 日志内容
     */
    @Override
    public void warn(String tag, String msg) {
        if (LEVEL_WARNING >= getLogLevel()) {
            logW(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), msg);
        }
    }

    /**
     * 打印 ERROR 级别的日志
     * @param tag 日志标记
     * @param msg 日志内容
     */
    @Override
    public void error(String tag, String msg) {
        if (LEVEL_ERROR >= getLogLevel()) {
            logE(tag, "", "", 0, Process.myPid(), Thread.currentThread().getId(), Looper.getMainLooper().getThread().getId(), msg);
        }
    }
}
