package com.tencent.iot.explorer.device.java.data_template;

import com.tencent.iot.hub.device.java.core.common.Status;

import org.json.JSONArray;
import org.json.JSONObject;

public class ArrayCheckUtils {

    public static Status checkArrayValues(JSONObject valuesTypeData, Object value) {
        if (value instanceof JSONArray) {
            JSONArray json = (JSONArray) value;

            if (valuesTypeData.getString("type").equals(TXDataTemplateJson.TYPE_INT)) {
                for (int i = 0; i < json.length(); i++) {
                    if (!(json.get(i) instanceof Integer) || json.getInt(i) < valuesTypeData.getInt("min") ||
                            json.getInt(i) > valuesTypeData.getInt("max")) {
                        return Status.PARAMETER_INVALID;
                    }
                }
            } else if (valuesTypeData.getString("type").equals(TXDataTemplateJson.TYPE_STRING)) {
                for (int i = 0; i < json.length(); i++) {
                    if (!(json.get(i) instanceof String) || json.getString(i).length() < valuesTypeData.getInt("min") ||
                            json.getString(i).length() > valuesTypeData.getInt("max")) {
                        return Status.PARAMETER_INVALID;
                    }
                }
            } else if (valuesTypeData.getString("type").equals(TXDataTemplateJson.TYPE_FLOAT)) {
                for (int i = 0; i < json.length(); i++) {
                    float currentNum = Float.valueOf(json.get(i).toString());
                    float min = Float.valueOf(valuesTypeData.getString("min"));
                    float max = Float.valueOf(valuesTypeData.getString("max"));
                    if (!(json.get(i) instanceof Double) || currentNum < min || currentNum > max) {
                        return Status.PARAMETER_INVALID;
                    }
                }
            } else if (valuesTypeData.getString("type").equals(TXDataTemplateJson.TYPE_STRUCT)) {
                for (int i = 0; i < json.length(); i++) {
                    JSONObject ele = json.getJSONObject(i);
                    JSONArray valuesType = valuesTypeData.getJSONArray("specs");
                    return StrcutCheckUtils.checkStructValues(valuesType, ele);
                }
            } else {
                return Status.PARAMETER_INVALID;
            }
        }

        return Status.OK;
    }
}
