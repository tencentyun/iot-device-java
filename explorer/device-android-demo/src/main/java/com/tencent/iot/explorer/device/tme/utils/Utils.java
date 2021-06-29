package com.tencent.iot.explorer.device.tme.utils;

public class Utils {
    /**
     * 转换成00:00/05:00格式
     *
     * @param cur
     * @param total
     * @return
     */
    public static String toProgress(int cur, int total) {
        StringBuilder pStr = new StringBuilder();
        int min = cur / 60;
        int sec = cur % 60;
        if (min < 10) pStr.append(0);
        pStr.append(min);
        pStr.append(":");
        if (sec < 10) pStr.append(0);
        pStr.append(sec);
        pStr.append("/");

        min = total / 60;
        sec = total % 60;
        if (min < 10) pStr.append(0);
        pStr.append(min);
        pStr.append(":");
        if (sec < 10) pStr.append(0);
        pStr.append(sec);

        return pStr.toString();
    }
}
