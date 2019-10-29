package com.qcloud.iot_explorer.data_template;

public abstract class TXDataTemplateDownCallBack {
    /**
     * 接受到下行消息的回调函数
     *
     * @param msg          回复消息
     */
    public abstract void onDownStreamCallBack(String msg);
}
