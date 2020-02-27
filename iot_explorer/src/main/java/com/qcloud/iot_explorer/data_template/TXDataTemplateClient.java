package com.qcloud.iot_explorer.data_template;

import android.content.Context;

import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttConnection;
import com.qcloud.iot_explorer.mqtt.TXMqttConstants;
import com.qcloud.iot_explorer.mqtt.TXMqttRequest;
import com.qcloud.iot_explorer.utils.TXLog;

import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_ACTION;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_ACTION_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_EVENTS_POST;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_EVENTS_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_EVENT_POST;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_EVENT_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CLEAR_CONTROL;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CLEAR_CONTROL_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_INFO;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_INFO_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_ACTION_UP_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_EVENT_DOWN_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_EVENT_UP_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_DOWN_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_UP_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.ACTION_UP_STREAM_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.EVENT_UP_STREAM_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_STREAM_TOPIC;

import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplateSubTopic;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qcloud.iot_explorer.mqtt.TXMqttConstants.MQTT_SDK_VER;

public class TXDataTemplateClient extends TXMqttConnection {
    //数据模板
    private  TXDataTemplate mDataTemplate;
    //属性下行topic
    public String mPropertyDownStreamTopic;

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     * @param jsonFileName      数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXDataTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new TXDataTemplate(context, this,  productID,  deviceName, jsonFileName, downStreamCallBack);
        this.mPropertyDownStreamTopic = mDataTemplate.mPropertyDownStreamTopic;
    }

    public boolean isConnected() {
        return this.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected);
    }

    /**
     * 订阅数据模板相关主题
     * @param topicId 主题ID
     * @param qos QOS等级
     * @return 发送请求成功时返回Status.OK;
     */
    public Status subscribeTemplateTopic(TemplateSubTopic topicId, final int qos) {
      return  this.mDataTemplate.subscribeTemplateTopic(topicId, qos);
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeTemplateTopic(TemplateSubTopic topicId) {
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
}
