package com.tencent.iot.explorer.device.tme.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;

import java.util.concurrent.atomic.AtomicInteger;

public class TmeDataTemplate extends TXDataTemplate {

    private static final AtomicInteger REQUEST_ID = new AtomicInteger(0);

    private TXMqttConnection mConnection;


    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品ID
     * @param deviceName         设备名
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TmeDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
    }
}
