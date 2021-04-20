package com.tencent.iot.explorer.device.java.data_template;

import com.tencent.iot.hub.device.java.core.common.Status;

import org.json.JSONArray;
import org.json.JSONObject;

public class ArrayCheckUtils {

    public static Status checkArrayValues(JSONObject valuesTypeData, Object value) {
        if (value instanceof JSONArray) {
            JSONArray json = (JSONArray) value;

            if (valuesTypeData.getString("type").equals("int")) {
                for (int i = 0; i < json.length(); i++) {
                    if (json.getInt(i) < valuesTypeData.getInt("min") ||
                            json.getInt(i) > valuesTypeData.getInt("max")) {
                        return Status.PARAMETER_INVALID;
                    }
                }
            } else if (valuesTypeData.getString("type").equals("string")) {
                for (int i = 0; i < json.length(); i++) {
                    if (json.getString(i).length() < valuesTypeData.getInt("min") ||
                            json.getString(i).length() > valuesTypeData.getInt("max")) {
                        return Status.PARAMETER_INVALID;
                    }
                }
            } else if (valuesTypeData.getString("type").equals("float")) {
                for (int i = 0; i < json.length(); i++) {
                    if (json.getFloat(i) < valuesTypeData.getFloat("min") ||
                            json.getFloat(i) > valuesTypeData.getFloat("max")) {
                        return Status.PARAMETER_INVALID;
                    }
                }
            } else if (valuesTypeData.getString("type").equals("struct")) {
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
