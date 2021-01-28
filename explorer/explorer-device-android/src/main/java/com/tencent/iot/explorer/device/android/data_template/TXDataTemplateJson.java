package com.tencent.iot.explorer.device.android.data_template;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.StrcutCheckUtils;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

class TXDataTemplateJson {
    public static final String TAG = "TX_TEMPLATE_JSON_" + MQTT_SDK_VER;
    private JSONArray mPropertyJson = null;
    private JSONArray mEventJson = null;
    private JSONArray mActionJson = null;

    //value type
    final static private String TYPE_BOOL = "bool";
    final static private String TYPE_INT = "int";
    final static private String TYPE_FLOAT = "float";
    final static private String TYPE_STRING = "string";
    final static private String TYPE_ENUM = "enum";
    final static private String TYPE_TIMESTAMP = "timestamp";
    final static private String TYPE_STRUCT = "struct";

    TXDataTemplateJson(Context context, final String jsonFileName) {
        if (Status.OK != registerDataTemplateJson(context, jsonFileName)) {
            Log.e(TAG, "TXDataTemplateJson: construct json failed!");
        }
    }

    /**
     * 读取文件内容到字符串
     * @param inputStream 文件输入流
     * @return 检查结果
     */
    private String readInputStream(InputStream inputStream) {
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
     * 注册从控制台界面下载的json文件
     * @param context Android上下文，可使用进程上下文/Activity
     * @param jsonFileName assets中json文件名
     * @return 检查结果
     */
    private Status registerDataTemplateJson(Context context, final String jsonFileName){
        AssetManager assetManager = context.getAssets();
        if (assetManager == null) {
            return null;
        }
        InputStream jsonInputStream = null;
        try {
            jsonInputStream = assetManager.open(jsonFileName);
            String jsonStr = readInputStream(jsonInputStream);
            JSONObject json = new JSONObject(jsonStr);
            this.mPropertyJson = json.getJSONArray("properties");
            this.mEventJson = json.getJSONArray("events");
            this.mActionJson = json.getJSONArray("actions");
            TXLog.d(TAG, "registerDataTemplateJson: propertyJson" + mPropertyJson);
            TXLog.d(TAG, "registerDataTemplateJson: eventJson" + mEventJson);
            TXLog.d(TAG, "registerDataTemplateJson: actionJson" + mActionJson);
        } catch (IOException e) {
            TXLog.e(TAG, "Cannot open Json Files.", e);
            return Status.ERROR;
        } catch (JSONException t) {
            TXLog.e(TAG, "Json file format is invalid!.", t);
            return Status.ERROR;
        } finally {
            if (jsonInputStream != null) {
                try {
                    jsonInputStream.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return Status.OK;
    }

    /**
     * 检查value是否符合定义
     * @param valueDescribeJson 注册的模板中关于参数的描述
     * @param value 实际的值
     * @return 检查结果
     */
    private Status checkParamsValue(JSONObject valueDescribeJson, Object value){
        try {
            String type = valueDescribeJson.getString("type");
            if(type.equals(TYPE_BOOL)){ //BOOL类型取值0或者1
                if(value instanceof Integer &&
                    (((Integer) value).intValue() == 0 || ((Integer) value).intValue() == 1)){
                    return Status.OK;
                }
            } else if (type.equals(TYPE_INT)){ //INT类型取值符合范围
                if (value instanceof Integer &&
                    ((Integer) value).intValue() >= Integer.parseInt(valueDescribeJson.getString("min")) &&
                    ((Integer) value).intValue() <= Integer.parseInt(valueDescribeJson.getString("max"))){
                    return Status.OK;
                }
            } else if (type.equals(TYPE_FLOAT)){ //FLOAT类型取值符合范围
                if (value instanceof Double &&
                    ((Double) value).doubleValue() >= Double.parseDouble(valueDescribeJson.getString("min")) &&
                    ((Double) value).doubleValue() <= Double.parseDouble(valueDescribeJson.getString("max"))){
                    return Status.OK;
                }
            } else if (type.equals(TYPE_STRING)){ //字符串类型长度不能超过最大长度
                if (value instanceof String &&
                    ((String) value).length() <= Integer.parseInt(valueDescribeJson.getString("max"))){
                    return Status.OK;
                }
            } else if (type.equals(TYPE_ENUM)){ //枚举类型取值在范围内
                if (value instanceof Integer){
                    JSONObject mapping = valueDescribeJson.getJSONObject("mapping");
                    Iterator<String> it = mapping.keys();
                    while(it.hasNext()){
                        if( ((Integer) value).intValue() == Integer.parseInt(it.next())) {
                            return Status.OK;
                        }
                    }
                }
            } else if (type.equals(TYPE_TIMESTAMP)){ //时间类型为无符号整数 也有可能是长整数
                if (value instanceof Integer || value instanceof Long){
                    return Status.OK;
                }
            } else if (type.equals(TYPE_STRUCT)) {
                JSONArray valuesType = valueDescribeJson.getJSONArray("specs");
                return StrcutCheckUtils.checkStructValues(valuesType, value);

            } else {
                TXLog.e(TAG, "Invalid Data Template Json, please check and replace it!");
            }
            TXLog.e(TAG, "Invalid Value, excepted " + valueDescribeJson);
        } catch (JSONException e) {
            TXLog.e(TAG, "Invalid Data Template Json, please check and replace it!");
        }
        return Status.PARAMETER_INVALID;
    }

    /**
     * 检查params是否符合定义
     * @param paramsDescribeJson 注册的模板
     * @param paramsJson 用户构造的参数Json
     * @return 检查结果
     */
    private Status checkParamsJson(JSONArray paramsDescribeJson, JSONObject paramsJson){
        if(null == paramsJson || null == paramsDescribeJson) {
            TXLog.e(TAG, "checkParamsJson: json is null!");
            return Status.PARAMETER_INVALID;
        }
        int i;

        //found property in json array
        try {
            Iterator<String> it = paramsJson.keys();
            while(it.hasNext()){
                String key = it.next();
                for(i=0;i < paramsDescribeJson.length();i++) {
                    JSONObject jsonNode = paramsDescribeJson.getJSONObject(i);
                    if (jsonNode.get("id").equals(key)) {
                        if(Status.OK != checkParamsValue(jsonNode.getJSONObject("define"), paramsJson.get(key))) {
                            TXLog.e(TAG, "checkParamsJson: parameter [%s] with invalid value (may be string): " + paramsJson.get(key), key);
                            return Status.PARAMETER_INVALID;
                        }
                        break;
                    }
                }
                if(i == paramsDescribeJson.length()) { //property not found
                    TXLog.e(TAG, "checkParamsJson: no such param:" +  key);
                    return Status.PARAMETER_INVALID;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  Status.OK;
    }

    /**
     * 检查Action的参数是否符合定义
     * @param paramsDescribeJson 参数模板
     * @param paramsJson 参数描述json
     * @return 检查结果
     */
    private Status checkActionParamsJson(JSONArray paramsDescribeJson, JSONObject paramsJson){
        if(null == paramsJson || null == paramsDescribeJson) {
            TXLog.e(TAG, "checkParamsJson: json is null!");
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
                            TXLog.e(TAG, "checkParamsJson: parameter [%s] with invalid value (may be string): " + paramsJson.get(key), key);
                            return Status.PARAMETER_INVALID;
                        }
                        isActionExit = true;
                        break;
                    }
                }
                if(!isActionExit) {
                   TXLog.e(TAG, "checkActionParamsJson: params [%s] not found, check the data template json on cloud console!", jsonNode.get("id"));
                   return Status.ERROR;
                }
                isActionExit = false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  Status.OK;
    }

    /**
     * 检查property是否符合定义
     * @param property 用户构造的property json
     * @return 检查结果
     */
     Status checkPropertyJson(JSONObject property){
        return  checkParamsJson(mPropertyJson, property);
    }

    /**
     * 检查单个event是否符合定义
     * @param eventId 事件ID
     * @param type 事件类型
     * @param params 事件参数
     * @return 结果
     */
     Status checkEventJson(String eventId, String type, JSONObject params){
        if(null == eventId || null == type || null == params) {
            TXLog.e(TAG, "checkEventJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }
        int i;
        //found property in json array
        try {
            for(i=0;i < mEventJson.length();i++) {
                JSONObject jsonNode = mEventJson.getJSONObject(i);
                if (jsonNode.get("id").equals(eventId)) {
                    if(jsonNode.get("type").equals(type)) {
                        return checkParamsJson(jsonNode.getJSONArray("params"), params);
                    } else {
                        TXLog.e(TAG, "checkEventJson: type [%s] is not matched, excepted[%s]!", type, jsonNode.get("type"));
                        return Status.PARAMETER_INVALID;
                    }
                }
            }
            if(i == mEventJson.length()) { //property not found
                TXLog.e(TAG, "checkEventJson: no such event:" +  eventId);
                return Status.PARAMETER_INVALID;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  Status.OK;
    }

    /**
     * 检查多个event是否符合定义
     * @param events 事件集合
     * @return 结果
     */
     Status checkEventsJson(JSONArray events){
        if(null == events) {
            TXLog.e(TAG, "checkEventsJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }

        //check event in events json array
        try {
            for(int i=0;i < events.length();i++) {
                JSONObject jsonNode = events.getJSONObject(i);
                if(!(jsonNode.get("timestamp") instanceof Long)) {
                    TXLog.e(TAG, "checkEventsJson: timestamp invalid! EventId:[%s].", jsonNode.getString("eventId"));
                    return Status.PARAMETER_INVALID;
                }
                if(Status.OK != checkEventJson(jsonNode.getString("eventId"), jsonNode.getString("type"), jsonNode.getJSONObject("params"))) {
                    TXLog.e(TAG, "checkEventsJson: events invalid!");
                    return Status.PARAMETER_INVALID;
                }
            }
        } catch (JSONException e) {
            TXLog.e(TAG, "checkEventsJson: events invalid!");
            e.printStackTrace();
        }
        return  Status.OK;
    }

    /**
     * 检查action是否符合定义
     * @param actionId 动作ID
     * @param params 输入参数
     * @return 结果
     */
     Status checkActionJson(String actionId, JSONObject params){
        if(null == params || null == actionId) {
            TXLog.e(TAG, "checkActionJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }

        //check aciton in actions json array
        int i;
        try {
            for( i=0;i < mActionJson.length();i++) {
                JSONObject jsonNode = mActionJson.getJSONObject(i);
                if (jsonNode.get("id").equals(actionId)) {
                    if(Status.OK != checkActionParamsJson(jsonNode.getJSONArray("input"),params)) {
                        TXLog.e(TAG, "checkActionJson: action [%s] with invalid parameter, check the data template json on cloud console!", actionId);
                        return Status.PARAMETER_INVALID;
                    }
                    break;
                }
            }
            if(i == mActionJson.length()) { //action not found
                TXLog.e(TAG, "checkActionJson: no such action id [%s], check the data template json on cloud console!",actionId );
                return Status.PARAMETER_INVALID;
            }
        } catch (JSONException e) {
            TXLog.e(TAG, "checkActionJson: action message invalid!");
            e.printStackTrace();
        }
        return  Status.OK;
    }

    /**
     * 检查action reply
     * @param actionId 动作ID
     * @param response 回复参数
     * @return 结果
     */
    Status checkActionReplyJson(String actionId, JSONObject response){
        if(null == response || null == actionId) {
            TXLog.e(TAG, "checkActionJson: parameter is null!");
            return Status.PARAMETER_INVALID;
        }

        //check aciton in actions json array
        int i;
        try {
            for( i=0;i < mActionJson.length();i++) {
                JSONObject jsonNode = mActionJson.getJSONObject(i);
                if (jsonNode.get("id").equals(actionId)) {
                    if(Status.OK != checkActionParamsJson(jsonNode.getJSONArray("output"),response)) {
                        TXLog.e(TAG, "checkActionReplyJson: action [%s] with invalid parameter:" + response, actionId);
                        return Status.PARAMETER_INVALID;
                    }
                    break;
                }
            }
            if(i == mActionJson.length()) { //action not found
                TXLog.e(TAG, "checkActionReplyJson: no such action id :" +  actionId);
                return Status.PARAMETER_INVALID;
            }
        } catch (JSONException e) {
            TXLog.e(TAG, "checkActionReplyJson: events invalid!");
            e.printStackTrace();
        }
        return  Status.OK;
    }
}
