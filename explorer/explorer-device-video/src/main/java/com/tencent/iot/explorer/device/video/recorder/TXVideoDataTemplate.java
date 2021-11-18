package com.tencent.iot.explorer.device.video.recorder;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.rtc.data_template.TXTRTCDataTemplate;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_STREAM_TOPIC;

public class TXVideoDataTemplate extends TXTRTCDataTemplate {

    //Mqtt 连接
    private TXMqttConnection mConnection;
    private static AtomicInteger requestID = new AtomicInteger(0);

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     */
    public TXVideoDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName) {
        super(context, connection, productId, deviceName, jsonFileName, null, null);
        this.mConnection = connection;
    }

    /**
     * 上报重置设备呼叫属性 为空闲
     * @return 结果
     */
    public Status reportXp2pInfo(String p2pInfo) {
        JSONObject property = new JSONObject();
        try {
            property.put(TXVideoDataTemplateConstants.PROPERTY_SYS_VIDEO_P2P_INFO, p2pInfo);
        } catch (JSONException e) {
            TXLog.e(TAG, "Construct property json failed!");
            return Status.ERROR;
        }

        Status status = sysPropertyReport(property, null);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        return status;
    }

    /**
     * 系统属性上报， 不检查构造是否符合json文件中的定义
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    private Status sysPropertyReport(JSONObject property, JSONObject metadata) {
        //不检查构造是否符合json文件中的定义

        //构造发布信息
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_PROPERTY_REPORT);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
            object.put("params", property);
            if (null != metadata)
                object.put("metadata", metadata);
        } catch (Exception e) {
            TXLog.e(TAG, "propertyReport: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }
        String objectString = object.toString();
        objectString = objectString.replace("\\/", "/");

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(objectString.getBytes());

        return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }
}
