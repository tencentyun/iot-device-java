package com.tencent.iot.explorer.device.android.http.utils;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;


public class JsonManager {
    /**
     * json格式转对象   245
     */
    public static <T> T parseJson(String json, Class<T> clazz) {
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            Log.e("ParseJsonEntity", "fastjson解析出错：" + e.getMessage() + "\n原因是：" + e.getCause());
            return null;
        }

    }

    /**
     * json格式转对象   245
     */
    public static <T> List<T> parseJsonArray(String json, Class<T> clazz) {
        try {

            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            Log.e("ParseJsonEntity", "fastjson解析出错：" + e.getMessage() + "\n原因是：" + e.getCause());
            return null;
        }

    }

    /**
     * 功能描述：把JSON数据转换成普通字符串列表
     */
    public static List<String> parseArray(String jsonData) {

        try {
            return JSON.parseArray(jsonData, String.class);
        } catch (Exception e) {
            Log.e("ParseJsonEntity", "fastjson解析出错：" + e.getMessage() + "\n原因是：" + e.getCause());
            return null;

        }
    }

    /**
     * 功能描述：把指定的java对象转为json数据
     */
    public static String toJson(Object clazz) {
        try {
            return JSON.toJSONString(clazz);
        } catch (Exception e) {
            Log.e("ToJsonEntity", "fastjson转换错误：" + e.getMessage() + "\n原因是：" + e.getCause());
            return null;
        }
    }

    /**
     * 功能描述：把JSON数据转换成较为复杂的java对象列表
     */
    public static List<Map<String, Object>> parseObjectArray(String jsonData) {

        try {
            return JSON.parseObject(jsonData, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            Log.e("ParseObjectArray", "fastjson解析错误：" + e.getMessage() + "\n原因是：" + e.getCause());
            return null;
        }
    }

}
