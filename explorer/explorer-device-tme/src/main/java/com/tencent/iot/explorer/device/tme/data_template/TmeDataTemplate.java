package com.tencent.iot.explorer.device.tme.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;

public class TmeDataTemplate extends TXDataTemplate {

    private static final AtomicInteger REQUEST_ID = new AtomicInteger(0);

    private static final String METHOD_KUGOU_QUERY_PID = "kugou_query_pid";

    private TXMqttConnection mConnection;

    private String mServiceDownStreamTopic;

    private String mServiceUptreamTopic;

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
        this.mServiceDownStreamTopic = TOPIC_SERVICE_DOWN_PREFIX + productId + "/" + deviceName;
        this.mServiceUptreamTopic = TOPIC_SERVICE_UP_PREFIX + productId + "/" + deviceName;
    }

    public void requestPidAndPkey() {
        //构造发布信息
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + REQUEST_ID.getAndIncrement();
        try {
            object.put("method", METHOD_KUGOU_QUERY_PID);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(object.toString().getBytes());
        mConnection.publish(mServiceUptreamTopic, message, null);
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        TXLog.d(TAG, message.toString());
    }
}
