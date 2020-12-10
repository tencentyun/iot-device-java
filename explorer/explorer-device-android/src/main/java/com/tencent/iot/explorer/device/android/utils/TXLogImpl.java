package com.tencent.iot.explorer.device.android.utils;

import java.util.concurrent.LinkedBlockingDeque;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TXLogImpl implements TXLog.LogImp {

    private static volatile Context sContext;

    private static String packageName = "";

    static LinkedBlockingDeque<String> logDeque = new LinkedBlockingDeque<String>(15000);

    private static final int[] INTERVAL_RETRY_INIT = new int[]{1, 2, 4, 8, 16, 29}; //重试时间

    private static AtomicInteger retryInitTimes = new AtomicInteger(0);

    public static final SimpleDateFormat timeFormatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    private static String logTime = "";

    private static String logPath = "";

    static String nowUsedFile = "";

    static final ReentrantLock lock = new ReentrantLock();

    protected static Object formatterLock = new Object();

    private static long nextDayTime;

    private static long nextSecondMinuteTime;

    static long lastWriterErrorTime = 0;

    private static FileWriter writer;

    private static Handler retryInitHandler = new Handler(Looper.getMainLooper());

    private int logLevel = TXLog.LEVEL_INFO;

    /**
     * 初始化日志
     */
    public static void init(Context context) {
        sContext = context;
        initRunnable.run();
    }

    /**
     * 将日志写到文件
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

    public static String getLogPath() {
        return logPath;
    }


    /**
     * 初始化日志文件
     */
    static synchronized void initLogFile(long nowCurrentTimeMillis) throws IOException {
        logPath = Environment.getExternalStorageDirectory().getPath() + "/tencent/" + packageName.replace(".", "/")
                + "/";
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

    static void delete7DaysBeforeFiles(long today) {
        logPath = Environment.getExternalStorageDirectory().getPath() + "/tencent/" + packageName.replace(".", "/");
        long day = (long) 1 * 24 * 60 * 60 * 1000;
        File tmpeFile;
        //删除前一个月的
        for (long i = (today - 7 * day); i > today - 37 * day; i = i - day) {
            String date = getDateStr(i);
            nowUsedFile = logPath + getLogFileName(date);
            try {
                tmpeFile = new File(nowUsedFile);
                if (tmpeFile.exists()) {
                    tmpeFile.delete();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 日志初始化Runnable
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
                        delete7DaysBeforeFiles(System.currentTimeMillis());
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

    @Override
    public void logV(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.VERBOSE + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    @Override
    public void logI(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.INFO + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    @Override
    public void logD(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.DEBUG + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    @Override
    public void logW(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.WARN + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    @Override
    public void logE(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.ERROR + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    @Override
    public void logF(String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log) {
        long now = System.currentTimeMillis();
        if (now >= nextSecondMinuteTime) {
            checkNextMinuteTime(now);
        }

        String message = logTime + "|" + LogLevel.OFF + "|" + pid + "|" + tid + "|" + tag + "|" + log + "\n";
        addLogToCache(message);
    }

    @Override
    public int getLogLevel() {
        return logLevel;
    }

    @Override
    public void setLogLevel(int level) {
        logLevel = level;
    }
}

