package com.tencent.iot.explorer.device.java.data_template;

import com.tencent.iot.explorer.device.java.utils.Log;
import com.tencent.iot.explorer.device.java.utils.ReadFile;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

public class DataTemplateJson {
    private static final String TAG = "TX_TEMPLATE_JSON_" + MQTT_SDK_VER;
    protected JSONArray mPropertyJson = null;
    protected JSONArray mEventJson = null;
    protected JSONArray mActionJson = null;

    //value type
    public static final String TYPE_BOOL = "bool";
    public static final String TYPE_INT = "int";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_ENUM = "enum";
    public static final String TYPE_TIMESTAMP = "timestamp";
    public static final String TYPE_STRUCT = "struct";
    public static final String TYPE_ARRAY = "array";
    private final Log log;

    public DataTemplateJson(Log log) {
        this.log = log;
    }

    /**
     * 读取文件内容到字符串
     *
     * @param inputStream 文件输入流
     * @return 检查结果
     */
    protected String readInputStream(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 检查value是否符合定义
     *
     * @param valueDescribeJson 注册的模板中关于参数的描述
     * @param value             实际的值
     * @return 检查结果
     */
    private Status checkParamsValue(JSONObject valueDescribeJson, Object value) {
        try {
            String type = valueDescribeJson.getString("type");
            if (type.equals(TYPE_BOOL)) { //BOOL类型取值0或者1
                if (value instanceof Integer &&
                        (((Integer) value).intValue() == 0 || ((Integer) value).intValue() == 1)) {
                    return Status.OK;
                }
            } else if (type.equals(TYPE_INT)) { //INT类型取值符合范围
                if (value instanceof Integer &&
                        ((Integer) value).intValue() >= Integer.parseInt(valueDescribeJson.getString("min")) &&
                        ((Integer) value).intValue() <= Integer.parseInt(valueDescribeJson.getString("max"))) {
                    return Status.OK;
                }
            } else if (type.equals(TYPE_FLOAT)) { //FLOAT类型取值符合范围
                if (value instanceof Float &&
                        ((Float) value).floatValue() >= Float.parseFloat(valueDescribeJson.getString("min")) &&
                        ((Float) value).floatValue() <= Float.parseFloat(valueDescribeJson.getString("max"))) {
                    return Status.OK;
                } else if (value instanceof Double &&
                        ((Double) value).doubleValue() >= Double.parseDouble(valueDescribeJson.getString("min")) &&
                        ((Double) value).doubleValue() <= Double.parseDouble(valueDescribeJson.getString("max"))) {
                    return Status.OK;
                }
            } else if (type.equals(TYPE_STRING)) { //字符串类型长度不能超过最大长度
                if (value instanceof String &&
                        ((String) value).length() <= Integer.parseInt(valueDescribeJson.getString("max"))) {
                    return Status.OK;
                }
            } else if (type.equals(TYPE_ENUM)) { //枚举类型取值在范围内
                if (value instanceof Integer) {
                    JSONObject mapping = valueDescribeJson.getJSONObject("mapping");
                    Iterator<String> it = mapping.keys();
                    while (it.hasNext()) {
                        if (((Integer) value).intValue() == Integer.parseInt(it.next())) {
                            return Status.OK;
                        }
                    }
                }
            } else if (type.equals(TYPE_TIMESTAMP)) { //时间类型为无符号整数 也有可能是长整数
                if (value instanceof Integer || value instanceof Long) {
                    return Status.OK;
                }
            } else if (type.equals(TYPE_STRUCT)) {
                JSONArray valuesType = valueDescribeJson.getJSONArray("specs");
                return StrcutCheckUtils.checkStructValues(valuesType, value);
            } else if (type.equals(TYPE_ARRAY)) {
                JSONObject arrayInfoJson = valueDescribeJson.getJSONObject("arrayInfo");
                return ArrayCheckUtils.checkArrayValues(arrayInfoJson, value);
            } else {
                log.error("Invalid Data Template Json, please check and replace it!");
            }
            log.error("Invalid Value, excepted " + valueDescribeJson);
        } catch (JSONException e) {
            log.error("Invalid Data Template Json, please check and replace it!");
        }
        return Status.PARAMETER_INVALID;
    }

    /**
     * 检查params是否符合定义
     *
     * @param paramsDescribeJson 注册的模板
     * @param paramsJson         用户构造的参数Json
     * @return 检查结果
     */
    private Status checkParamsJson(JSONArray paramsDescribeJson, JSONObject paramsJson) {
        if (null == paramsJson || null == paramsDescribeJson) {
            log.error("checkParamsJson: json is null!");
            return Status.PARAMETER_INVALID;
        }
        int i;

        //found property in json array
        try {
            Iterator<String> it = paramsJson.keys();
            while (it.hasNext()) {
                String key = it.next();
                for (i = 0; i < paramsDescribeJson.length(); i++) {
                    JSONObject jsonNode = paramsDescribeJson.getJSONObject(i);
                    if (jsonNode.get("id").equals(key)) {
                        if (Status.OK != checkParamsValue(jsonNode.getJSONObject("define"), paramsJson.get(key))) {
                            log.error("checkParamsJson: paramete [" + key + "] with invalid value (may be string): " + paramsJson.get(key));
                            return Status.PARAMETER_INVALID;
                        }
                        break;
                    }
                }
                if (i == paramsDescribeJson.length()) { //property not found
                    log.error("checkParamsJson: no such param:" + key);
                    return Status.PARAMETER_INVALID;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Status.OK;
    }

    /**
     * 检查Action的参数是否符合定义
     *
     * @param paramsDescribeJson 参数模板
     * @param paramsJson         参数描述json
     * @return 检查结果
     */
    private Status checkActionParamsJson(JSONArray paramsDescribeJson, JSONObject paramsJson) {
        System.out.append("paramsDescribeJson " + paramsDescribeJson.toString());
        System.out.append("paramsJson " + paramsJson.toString());
        if (null == paramsJson || null == paramsDescribeJson) {
            log.error("checkParamsJson: json is null!");
            return Status.PARAMETER_INVALID;
        }
        int i;
        boolean isActionExit = false;
        // found all params in params json and check value
        try {
            for (i = 0; i < paramsDescribeJson.length(); i++) {
                JSONObject jsonNode = paramsDescribeJson.getJSONObject(i);
                Iterator<String> it = paramsJson.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    if (jsonNode.get("id").equals(key)) {
                        if (Status.OK != checkParamsValue(jsonNode.getJSONObject("define"), paramsJson.get(key))) {
                            log.error("checkParamsJson: parameter [" + key + "] with invalid value (may be string): " + paramsJson.get(key));
                            return Status.PARAMETER_INVALID;
                        }
                        isActionExit = true;
                        break;
                    }
                }
                if (!isActionExit) {
                    log.error("checkActionParamsJson: params [" + jsonNode.get("id") + "] not found, check the data template json on cloud console!");
                    return Status.ERROR;
                }
                isActionExit = false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Status.OK;
    }

    /**
     * 检查property是否符合定义
     *
     * @param property 用户构造的property json
     * @return 检查结果
     */
    public Status checkPropertyJson(JSONObject property) {
        return checkParamsJson(mPropertyJson, property);
    }

    /**
     * 检查单个event是否符合定义
     *
     * @param eventId 事件ID
     * @param type    事件类型
     * @param params  事件参数
     * @return 结果
     */
    public Status checkEventJson(String eventId, String type, JSONObject params) {
        if (null == eventId || null == type || null == params) {
            log.error("checkEventJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }
        int i;
        //found property in json array
        try {
            for (i = 0; i < mEventJson.length(); i++) {
                JSONObject jsonNode = mEventJson.getJSONObject(i);
                if (jsonNode.get("id").equals(eventId)) {
                    if (jsonNode.get("type").equals(type)) {
                        return checkParamsJson(jsonNode.getJSONArray("params"), params);
                    } else {
                        log.error("checkEventJson: type [" + type + "] is not matched, excepted[" + jsonNode.get("type") + "]!");
                        return Status.PARAMETER_INVALID;
                    }
                }
            }
            if (i == mEventJson.length()) { //property not found
                log.error("checkEventJson: no such event:" + eventId);
                return Status.PARAMETER_INVALID;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Status.OK;
    }

    /**
     * 检查多个event是否符合定义
     *
     * @param events 事件集合
     * @return 结果
     */
    public Status checkEventsJson(JSONArray events) {
        if (null == events) {
            log.error("checkEventsJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }

        //check event in events json array
        try {
            for (int i = 0; i < events.length(); i++) {
                JSONObject jsonNode = events.getJSONObject(i);
                if (!(jsonNode.get("timestamp") instanceof Long)) {
                    log.error("checkEventsJson: timestamp invalid! EventId:[" + jsonNode.getString("eventId") + "].");
                    return Status.PARAMETER_INVALID;
                }
                if (Status.OK != checkEventJson(jsonNode.getString("eventId"), jsonNode.getString("type"), jsonNode.getJSONObject("params"))) {
                    log.error("checkEventsJson: events invalid!");
                    return Status.PARAMETER_INVALID;
                }
            }
        } catch (JSONException e) {
            log.error("checkEventsJson: events invalid!");
            e.printStackTrace();
        }
        return Status.OK;
    }

    /**
     * 检查action是否符合定义
     *
     * @param actionId 动作ID
     * @param params   输入参数
     * @return 结果
     */
    public Status checkActionJson(String actionId, JSONObject params) {
        if (null == params || null == actionId) {
            log.error("checkActionJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }

        //check aciton in actions json array
        int i;
        try {
            for (i = 0; i < mActionJson.length(); i++) {
                JSONObject jsonNode = mActionJson.getJSONObject(i);
                if (jsonNode.get("id").equals(actionId)) {
                    if (Status.OK != checkActionParamsJson(jsonNode.getJSONArray("input"), params)) {
                        log.error("checkActionJson: action [" + actionId + "] with invalid parameter, check the data template json on cloud console!");
                        return Status.PARAMETER_INVALID;
                    }
                    break;
                }
            }
            if (i == mActionJson.length()) { //action not found
                log.error("checkActionJson: no such action id [" + actionId + "], check the data template json on cloud console!");
                return Status.PARAMETER_INVALID;
            }
        } catch (JSONException e) {
            log.error("checkActionJson: action message invalid!");
            e.printStackTrace();
        }
        return Status.OK;
    }

    /**
     * 检查action reply
     *
     * @param actionId 动作ID
     * @param response 回复参数
     * @return 结果
     */
    public Status checkActionReplyJson(String actionId, JSONObject response) {
        if (null == response || null == actionId) {
            log.error("checkActionJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }

        //check aciton in actions json array
        int i;
        try {
            for (i = 0; i < mActionJson.length(); i++) {
                JSONObject jsonNode = mActionJson.getJSONObject(i);
                if (jsonNode.get("id").equals(actionId)) {
                    if (Status.OK != checkActionParamsJson(jsonNode.getJSONArray("output"), response)) {
                        log.error("checkActionReplyJson: action [" + actionId + "] with invalid parameter:" + response);
                        return Status.PARAMETER_INVALID;
                    }
                    break;
                }
            }
            if (i == mActionJson.length()) { //action not found
                log.error("checkActionReplyJson: no such action id :" + actionId);
                return Status.PARAMETER_INVALID;
            }
        } catch (JSONException e) {
            log.error("checkActionReplyJson: events invalid!");
            e.printStackTrace();
        }
        return Status.OK;
    }
}
