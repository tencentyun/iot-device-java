package com.tencent.iot.explorer.device.android.app.utils;

import java.util.Locale;

public class CommonUtils {
    public static String formatedDurationMilli(long duration){
        if (duration >= 1000) {
            return String.format(Locale.US, "%.2f sec", (duration*1.000) / 1000);
        } else {
            return String.format(Locale.US, "%d msec", duration);
        }
    }

    public static String formatedSpeed(long bytes, long elapsed_milli) {
        if (elapsed_milli <= 0) {
            return "0 B/s";
        }
        if (bytes <= 0) {
            return "0 B/s";
        }
        double bytes_per_sec = (bytes * 1.000) * 1000f / (elapsed_milli*1.000);
        if (bytes_per_sec >= 1000 * 1000) {
            return String.format(Locale.US, "%.2f MB/s", bytes_per_sec / 1000 / 1000);
        } else if (bytes_per_sec >= 1000) {
            return String.format(Locale.US, "%.1f KB/s", bytes_per_sec / 1000);
        } else {
            return String.format(Locale.US, "%d B/s", bytes_per_sec);
        }
    }

    public static String formatedSize(long bytes) {
        if (bytes >= 100 * 1000) {
            return String.format(Locale.US, "%.2f MB", (bytes * 1.000) / 1000 / 1000);
        } else if (bytes >= 100) {
            return String.format(Locale.US, "%.1f KB", (bytes * 1.000) / 1000);
        } else {
            return String.format(Locale.US, "%d B", bytes);
        }
    }
}
