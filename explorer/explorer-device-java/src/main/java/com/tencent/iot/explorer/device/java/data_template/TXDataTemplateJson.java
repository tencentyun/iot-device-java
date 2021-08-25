package com.tencent.iot.explorer.device.java.data_template;


import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.explorer.device.java.utils.CustomLog;
import com.tencent.iot.explorer.device.java.utils.ILog;
import com.tencent.iot.explorer.device.java.utils.ReadFile;
import com.tencent.iot.hub.device.java.core.common.Status;

import java.io.File;


class TXDataTemplateJson extends DataTemplateJson {

    private static final Logger LOG = LoggerFactory.getLogger(TXDataTemplateJson.class);
    private static final CustomLog CUSTOM_LOG = new CustomLog(LOG);

    TXDataTemplateJson(final String jsonFileName) {
        super(CUSTOM_LOG);
        if (Status.OK != registerDataTemplateJson( jsonFileName, null)) {
            LOG.info("TXDataTemplateJson: construct json failed!");
        }
    }

    TXDataTemplateJson(final String jsonFileName, final String jsonFilePath) {
        super(CUSTOM_LOG);
        if (Status.OK != registerDataTemplateJson( jsonFileName, jsonFilePath)) {
            LOG.info("TXDataTemplateJson: construct json failed!");
        }
    }

    /**
     * 注册从控制台界面下载的json文件
     *
     * @param jsonFileName assets中json文件名
     * @return 检查结果
     */
    private Status registerDataTemplateJson(final String jsonFileName, final String jsonFilePath) {
        File file;
        if (jsonFilePath == null || jsonFilePath.length() == 0) {
            file = new File(System.getProperty("user.dir") + "/src/test/resources/" + jsonFileName);
        } else {
            file = new File(jsonFilePath + jsonFileName);
        }
        System.out.println(file.getAbsolutePath());
        if (file.exists()) {
            try {
                String s = ReadFile.readJsonFile(file.getAbsolutePath());
                JSONObject json = new JSONObject(s);
                this.mPropertyJson = json.getJSONArray("properties");
                this.mEventJson = json.getJSONArray("events");
                this.mActionJson = json.getJSONArray("actions");
                LOG.info("registerDataTemplateJson: propertyJson" + mPropertyJson);
                LOG.info("registerDataTemplateJson: eventJson" + mEventJson);
                LOG.info("registerDataTemplateJson: actionJson" + mActionJson);
            } catch (JSONException t) {
                LOG.error("Json file format is invalid!." + t);
                return Status.ERROR;
            }
        } else {
            LOG.error("Cannot open Json Files.");
            return Status.ERROR;
        }
        return Status.OK;
    }
}
