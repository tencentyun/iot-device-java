package com.tencent.iot.explorer.device.central.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;

public class CentralDataTemplate extends TXDataTemplate {

    private TXMqttConnection mConnection;

    private String mServiceDownStreamTopic;

    private String mServiceUptreamTopic;

    private Context mContext;

    private String mProductID;

    private String mDevName;

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public CentralDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
        this.mServiceDownStreamTopic = TOPIC_SERVICE_DOWN_PREFIX + productId + "/" + deviceName;
        this.mServiceUptreamTopic = TOPIC_SERVICE_UP_PREFIX + productId + "/" + deviceName;
        this.mContext = context;
        this.mProductID = productId;
        this.mDevName = deviceName;
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        TXLog.d(TAG, message.toString());
        if (topic.equals(mServiceDownStreamTopic)) {
            onServiceMessageReceived(message);
        }
    }

    private void onServiceMessageReceived(final MqttMessage message) {

    }
}
