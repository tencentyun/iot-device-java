package com.tencent.iot.explorer.device.broadcast.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.broadcast.data_template.callback.BroadcastCallback;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

public class BroadcastTemplateClient extends TXMqttConnection {
    //数据模板
    public BroadcastDataTemplate mDataTemplate;
    //属性下行topic
    public String mPropertyDownStreamTopic;

    public BroadcastTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                   MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                   final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, BroadcastCallback broadcastCallback) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new BroadcastDataTemplate(context, this,  productID,  deviceName, jsonFileName, downStreamCallBack, broadcastCallback);
        this.mPropertyDownStreamTopic = mDataTemplate.mPropertyDownStreamTopic;
    }

    public BroadcastTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                   MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return this.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected);
    }

    /**
     * 订阅数据模板相关主题
     * @param topicId 主题ID
     * @param qos QOS等级
     * @return 发送请求成功时返回Status.OK;
     */
    public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos) {
        return  this.mDataTemplate.subscribeTemplateTopic(topicId, qos);
    }

    public Status disConnect(Object userContext) {
        if (mDataTemplate != null) {
            mDataTemplate.destroy();
        }
        return disConnect(0, userContext);
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId) {
        return this.mDataTemplate.unSubscribeTemplateTopic(topicId);
    }

    /**
     * 属性上报
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    public Status propertyReport(JSONObject property, JSONObject metadata) {
        return mDataTemplate.propertyReport(property, metadata);
    }

    /**
     * 获取状态
     * @param type 类型
     * @param showmeta 是否携带showmeta
     * @return 结果
     */
    public Status propertyGetStatus(String type, boolean showmeta) {
        return mDataTemplate.propertyGetStatus(type, showmeta);
    }

    /**
     * 设备基本信息上报
     * @param params 参数
     * @return 结果
     */
    public Status propertyReportInfo(JSONObject params) {
        return mDataTemplate.propertyReportInfo(params);
    }

    /**
     * 清理控制信息
     * @return 结果
     */
    public Status propertyClearControl() {
        return mDataTemplate.propertyClearControl();
    }

    /**
     * 单个事件上报
     * @param eventId 事件ID
     * @param type 事件类型
     * @param params 参数
     * @return 结果
     */
    public Status eventSinglePost(String eventId, String type, JSONObject params) {
        return  mDataTemplate.eventSinglePost(eventId, type, params);
    }

    /**
     * 多个事件上报
     * @param events 事件集合
     * @return 结果
     */
    public Status eventsPost(JSONArray events) {
        return mDataTemplate.eventsPost(events);
    }

    /**
     * 消息到达回调函数
     * @param topic   消息主题
     * @param message 消息内容
     * @throws Exception 异常
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        super.messageArrived(topic, message);
        mDataTemplate.onMessageArrived(topic, message);
    }

    /**
     * mqtt连接成功
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        super.connectComplete(reconnect, serverURI);
    }
}
