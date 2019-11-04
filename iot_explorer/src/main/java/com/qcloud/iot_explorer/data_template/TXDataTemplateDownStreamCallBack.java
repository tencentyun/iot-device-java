package com.qcloud.iot_explorer.data_template;

import org.json.JSONObject;

public abstract class TXDataTemplateDownStreamCallBack {
    public abstract void onReplyCallBack(String msg);

    public abstract void onGetStatusReplyCallBack(JSONObject data);

    public abstract JSONObject onControlCallBack(JSONObject msg);

    public abstract JSONObject onActionCallBack(String actionId, JSONObject params);
}
