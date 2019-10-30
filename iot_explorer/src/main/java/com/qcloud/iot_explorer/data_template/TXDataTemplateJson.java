package com.qcloud.iot_explorer.data_template;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.utils.TXLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.qcloud.iot_explorer.mqtt.TXMqttConstants.MQTT_SDK_VER;

class TXDataTemplateJson {
    public static final String TAG = "TX_TEMPLATE_JSON_" + MQTT_SDK_VER;
    private JSONArray mPropertyJson = null;
    private JSONArray mEventJson = null;
    private JSONArray mActionJson = null;

    public TXDataTemplateJson(Context context, final String jsonFileName) {
        if (Status.OK != registerDataTemplateJson(context, jsonFileName)) {
            Log.e(TAG, "TXDataTemplateJson: construct json failed!");
        }
    }

    /**
     * 读取文件内容到字符串
     * @param inputStream 文件输入流
     * @return
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
     * 注册从官网下载的json文件
     * @param context Android上下文，可使用进程上下文/Activity
     * @param jsonFileName assets中json文件名
     * @return
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
            mPropertyJson = json.getJSONArray("properties");
            mEventJson = json.getJSONArray("events");
            mActionJson = json.getJSONArray("actions");
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
     *
     */






}
