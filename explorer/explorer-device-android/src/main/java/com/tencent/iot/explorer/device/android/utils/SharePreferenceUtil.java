package com.tencent.iot.explorer.device.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地数据存储与读取（共享参数）
 */
public class SharePreferenceUtil {

    /**
     * 保存单个字段的设置
     *
     * @param context
     * @param fileName
     * @param key
     * @param value
     */
    public static boolean saveInt(Context context, String fileName, String key, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit();
        Map<String, Integer> map = new HashMap<>();
        map.put(key, value);
        editor.putInt(key, value);
        return editor.commit();
    }

    /**
     * 保存单个字段的设置
     *
     * @param context
     * @param fileName
     * @param key
     * @param value
     */
    public static boolean saveLong(Context context, String fileName, String key, long value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit();
        Map<String, Long> map = new HashMap<>();
        map.put(key, value);
        editor.putLong(key, value);
        return editor.commit();
    }

    /**
     * 保存单个字段的设置
     *
     * @param context
     * @param fileName
     * @param key
     * @param value
     */
    public static boolean saveString(Context context, String fileName, String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return saveString(context, fileName, map);
    }

    /**
     * 清理单个字段的设置
     *
     * @param context
     * @param fileName
     * @param key
     */
    public static boolean clearString(Context context, String fileName, String key) {
        Map<String, String> map = new HashMap<>();
        map.put(key, null);
        return saveString(context, fileName, map);
    }

    /**
     * 保存多个字段的偏好设置文件
     *
     * @param context
     * @param fileName
     * @param params
     */
    public static boolean saveString(Context context, String fileName, Map<String, String> params) {
        SharedPreferences.Editor editor = context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit();
        for (String key : params.keySet()) {
            String value = params.get(key);
            editor.putString(key, value);
        }
        return editor.commit();
    }

    /**
     * 从偏好设置文件中读取单个字段的数据
     *
     * @param context
     * @param fileName
     * @param key
     */
    public static int getInt(Context context, String fileName, String key) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return sp.getInt(key, 0);
    }

    /**
     * 从偏好设置文件中读取单个字段的数据
     *
     * @param context
     * @param fileName
     * @param key
     */
    public static long getLong(Context context, String fileName, String key) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return sp.getLong(key, 0);
    }

    /**
     * 从偏好设置文件中读取单个字段的数据
     *
     * @param context
     * @param fileName
     * @param key
     */
    public static String getString(Context context, String fileName, String key) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    /**
     * 从偏好设置文件中读取多个字段的数据
     *
     * @param context
     * @param fileName
     * @param keys
     */
    public static Map<String, String> getString(Context context, String fileName, String... keys) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Map<String, String> params = new HashMap<>();
        for (String key : keys) {
            String value = sp.getString(key, "");
            params.put(key, value);
        }
        return params;
    }
}
