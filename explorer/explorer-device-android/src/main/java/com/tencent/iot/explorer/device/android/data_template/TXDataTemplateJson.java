package com.tencent.iot.explorer.device.android.data_template;

import android.content.Context;
import android.content.res.AssetManager;

import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.DataTemplateJson;
import com.tencent.iot.explorer.device.java.utils.Log;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

class TXDataTemplateJson extends DataTemplateJson {

    public static final String TAG = "TX_TEMPLATE_JSON_" + MQTT_SDK_VER;
    private static final CustomLog CUSTOM_LOG = new CustomLog();

    TXDataTemplateJson(Context context, final String jsonFileName) {
        super(CUSTOM_LOG);
        if (Status.OK != registerDataTemplateJson(context, jsonFileName)) {
            TXLog.e(TAG, "TXDataTemplateJson: construct json failed!");
        }
    }

    /**
     * 注册从控制台界面下载的json文件
     *
     * @param context      Android上下文，可使用进程上下文/Activity
     * @param jsonFileName assets中json文件名
     * @return 检查结果
     */
    private Status registerDataTemplateJson(Context context, final String jsonFileName) {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return Status.OK;
    }

    private static class CustomLog implements Log {

        @Override
        public void debug(String msg) {
            TXLog.d(TAG, msg);
        }

        @Override
        public void info(String msg) {
            TXLog.i(TAG, msg);
        }

        @Override
        public void warn(String msg) {
            TXLog.w(TAG, msg);
        }

        @Override
        public void error(String msg) {
            TXLog.e(TAG, msg);
        }
    }
}