package com.tencent.iot.explorer.device.java.data_template;

import com.tencent.iot.hub.device.java.core.common.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class StrcutCheckUtils {

    public static Status checkStructValues(JSONArray valuesTypeData, Object value) {
        if (value instanceof JSONObject) {
            JSONObject json = (JSONObject) value;
            Iterator it = json.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                // 传入的 json key 存在匹配的内容，即认为检查通过
                if (getMatchJsonByKey(valuesTypeData, json, key) == null) {
                    return Status.PARAMETER_INVALID;
                }
            }
        }

        return Status.OK;
    }

    private static JSONObject getMatchJsonByKey(JSONArray valuesType, JSONObject json, String key) {
        for (int i = 0; i < valuesType.length(); i++) {
            try {
                JSONObject valueTypeInfo = valuesType.getJSONObject(i);
                if (valueTypeInfo == null) continue;

                String tagKey = valueTypeInfo.getString("id");
                if (tagKey == null || !tagKey.equals(key)) continue;

                JSONObject dataTypeJson = valueTypeInfo.getJSONObject("dataType");
                if (dataTypeJson == null) continue;
                String type = dataTypeJson.getString("type");
                if (type == null) continue;

                if (type.equals(TXDataTemplateJson.TYPE_STRING) && json.get(key) instanceof String) {
                    String str = json.getString(key);
                    int min = dataTypeJson.getInt("min");
                    int max = dataTypeJson.getInt("max");
                    if (str != null && str.length() >= min && str.length() <= max) {
                        return valueTypeInfo;
                    }

                } else if (type.equals(TXDataTemplateJson.TYPE_FLOAT) && json.get(key) instanceof Double) {
                    double doubleNum = json.getDouble(key);
                    double min = dataTypeJson.getDouble("min");
                    double max = dataTypeJson.getDouble("max");
                    if (doubleNum >= min && doubleNum <= max) {
                        return valueTypeInfo;
                    }

                } else if (((type.equals(TXDataTemplateJson.TYPE_ENUM) && (json.getInt(key) == 0 || json.getInt(key) == 1)) || type.equals(TXDataTemplateJson.TYPE_BOOL)) && json.get(key) instanceof Integer) {
                    int index = json.getInt(key);
                    JSONObject mapping = dataTypeJson.getJSONObject("mapping");
                    Iterator<String> it = mapping.keys();
                    while(it.hasNext()){
                        if (index == Integer.parseInt(it.next())) {
                            return valueTypeInfo;
                        }
                    }
                } else if (type.equals(TXDataTemplateJson.TYPE_TIMESTAMP) && (json.get(key) instanceof Integer || json.get(key) instanceof Long)) {
                    return valueTypeInfo;
                } else if (type.equals(TXDataTemplateJson.TYPE_INT)) {
                    Object obj = json.get(key);
                    if (obj instanceof Integer || obj instanceof Long) {
                        int intValue = json.getInt(key);
                        int min = dataTypeJson.getInt("min");
                        int max = dataTypeJson.getInt("max");
                        if (intValue >= min && intValue <= max) {
                            return valueTypeInfo;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                break;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                break;
            }
        }
        return null;
    }
}
