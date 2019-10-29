package com.qcloud.iot_explorer.data_template;

import android.content.Context;
import android.util.Log;
import android.util.SparseLongArray;

import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttConnection;
import com.qcloud.iot_explorer.mqtt.TXMqttRequest;

import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_ACTION_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_EVENT_POST;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_EVENT_REPLY;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CLEAR_CONTROL;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_INFO;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_ACTION_UP_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_EVENT_DOWN_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_EVENT_UP_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_DOWN_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_UP_PREFIX;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.ACTION_UP_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.EVENT_UP_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplateSubTopic;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qcloud.iot_explorer.mqtt.TXMqttConstants.MQTT_SDK_VER;

public class TXDataTemplateClient extends TXMqttConnection {
    public static final String TAG = "TXDATATEMPLATE_" + MQTT_SDK_VER;

    private static AtomicInteger requestID = new AtomicInteger(0);

    private HashMap<String, Long> mEventWaitList;
    private long mEventWaitTimeout = 60*1000; //60s

    private String mPropertyDownStreamTopic;
    private String mPropertyUptreamTopic;
    private String mEventDownStreamTopic;
    private String mEventUptreamTopic;
    private String mActionDownStreamTopic;
    private String mActionUptreamTopic;

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXDataTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        mPropertyDownStreamTopic = TOPIC_PROPERTY_DOWN_PREFIX + mProductId + "/"  + mDeviceName;
        mPropertyUptreamTopic = TOPIC_PROPERTY_UP_PREFIX + mProductId + "/"  + mDeviceName;
        mEventDownStreamTopic = TOPIC_EVENT_DOWN_PREFIX + mProductId + "/"  + mDeviceName;
        mEventUptreamTopic = TOPIC_EVENT_UP_PREFIX + mProductId + "/"  + mDeviceName;
        mActionDownStreamTopic = TOPIC_ACTION_DOWN_PREFIX + mProductId + "/"  + mDeviceName;
        mActionUptreamTopic = TOPIC_ACTION_UP_PREFIX + mProductId + "/"  + mDeviceName;
    }

    /**
     * 订阅数据模板相关主题
     * @param topicId 主题ID
     * @param qos QOS等级
     * @return 发送请求成功时返回Status.OK;
     */
    public Status subscribeTemplateTopic(TemplateSubTopic topicId, final int qos) {
        Status ret;
        String topic;

        TXMqttRequest mqttRequest = new TXMqttRequest("subscribeTopic", requestID.getAndIncrement());

        switch (topicId) {
            case PROPERTY_DOWN_TOPIC:
                topic = mPropertyDownStreamTopic;
                break;
            case EVENT_DOWN_TOPIC:
                topic = mEventDownStreamTopic;
                mEventWaitList = new HashMap<>(100);
                break;
            case ACTION_DOWN_TOPIC:
                topic = mActionDownStreamTopic;
                break;
            default:
                Log.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }

        ret = super.subscribe(topic, qos, mqttRequest);
        if(Status.OK != ret) {
            Log.e(TAG, "subscribeTopic failed! " + topic);
            return ret;
        }
        return Status.OK;
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeTemplateTopic(TemplateSubTopic topicId) {
        Status ret;
        String topic;
        TXMqttRequest mqttRequest = new TXMqttRequest("subscribeTopic", requestID.getAndIncrement());

        switch (topicId) {
            case PROPERTY_DOWN_TOPIC:
                topic = mPropertyDownStreamTopic;
                break;
            case EVENT_DOWN_TOPIC:
                topic = mEventDownStreamTopic;
                break;
            case ACTION_DOWN_TOPIC:
                topic = mActionDownStreamTopic;
                break;
            default:
                Log.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }

        ret = super.unSubscribe(topic, mqttRequest);
        if(Status.OK != ret) {
            Log.e(TAG, "subscribeTopic failed! " + topic);
        }

        return Status.OK;
    }

    /**
     * 发布消息
     * @param topicId 主题ID
     * @param message 消息内容
     * @return 发送请求成功时返回Status.OK;
     */
    private Status publishTemplateMessage(TemplatePubTopic topicId, MqttMessage message) {
        String topic;
        switch (topicId) {
            case PROPERTY_UP_TOPIC:
                topic = mPropertyUptreamTopic;
                break;
            case EVENT_UP_TOPIC:
                topic = mEventUptreamTopic;
                break;
            case ACTION_UP_TOPIC:
                topic = mActionUptreamTopic;
                break;
            default:
                Log.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }
        return super.publish(topic, message, null);
    }

    //属性上报
    public Status propertyReport(String params, String metadata) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_PROPERTY_REPORT);
            object.put("clientToken", requestID.getAndIncrement());
            object.put("timestamp", System.currentTimeMillis());
            object.put("params", params);
            if (null != metadata)
                object.put("metadata", metadata);
        } catch (Exception e) {
            Log.e(TAG, "propertyReport: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(PROPERTY_UP_TOPIC, message);
    }

    //获取状态
    public Status propertyGetStatus(String type, boolean showmeta) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_PROPERTY_GET_STATUS);
            object.put("clientToken", requestID.getAndIncrement());
            object.put("type", type);
            if (showmeta)
                object.put("showmeta", 1);
            else
                object.put("showmeta", 0);
        } catch (Exception e) {
            Log.e(TAG, "propertyGetStatus: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(PROPERTY_UP_TOPIC, message);
    }

    //清理控制信息
    public Status propertyClearControl() {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_PROPERTY_CLEAR_CONTROL);
            object.put("clientToken", requestID.getAndIncrement());
        } catch (Exception e) {
            Log.e(TAG, "propertyClearControl: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(PROPERTY_UP_TOPIC, message);
    }

    //设备基本信息上报
    public Status propertyReportInfo(String params) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_PROPERTY_REPORT_INFO);
            object.put("clientToken", requestID.getAndIncrement());
            object.put("params", params);
        } catch (Exception e) {
            Log.e(TAG, "propertyReportInfo: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(PROPERTY_UP_TOPIC, message);
    }

    //单个事件上报
    public Status eventSinglePost(String eventId, String type, String params) {
        JSONObject object = new JSONObject();
        String clientToken =  String.valueOf(requestID.getAndIncrement());
        long timestamp =  System.currentTimeMillis();
        try {
            object.put("method", METHOD_EVENT_POST);
            object.put("clientToken", clientToken);
            object.put("eventId", eventId);
            object.put("type", type);
            object.put("timestamp", timestamp);
            object.put("params", params);
        } catch (Exception e) {
            Log.e(TAG, "eventSinglePost: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        Status ret = publishTemplateMessage(EVENT_UP_TOPIC, message);
        if(Status.OK == ret) {
            mEventWaitList.put(clientToken,timestamp); //加入到等待列表中
            return Status.OK;
        } else{
            return Status.ERROR;
        }
    }

    //多个事件上报
    public Status eventsPost(String events) {
        JSONObject object = new JSONObject();
        String clientToken =  String.valueOf(requestID.getAndIncrement());
        long timestamp =  System.currentTimeMillis();
        try {
            object.put("method", METHOD_EVENT_POST);
            object.put("clientToken", requestID.getAndIncrement());
            object.put("events", events);
        } catch (Exception e) {
            Log.e(TAG, "eventsPost: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        Status ret = publishTemplateMessage(EVENT_UP_TOPIC, message);
        if(Status.OK == ret) {
            mEventWaitList.put(clientToken,timestamp); //加入到等待列表中
            return Status.OK;
        } else{
            return Status.ERROR;
        }
    }

    //行为执行结果上报
    public Status actionReply(int code, String status, String response) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_ACTION_REPLY);
            object.put("clientToken", requestID.getAndIncrement());
            object.put("code", code);
            object.put("status", status);
            object.put("response", response);
        } catch (Exception e) {
            Log.e(TAG, "actionReply: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(1); //qos 1
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(ACTION_UP_TOPIC, message);
    }

    /**
     * 属性下行消息处理
     * @param message 消息内容
     */
    private void onPropertyMessageArrivedCallBack(MqttMessage message){
        Log.d(TAG, "property down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if(method.equals(METHOD_PROPERTY_REPORT)) {
                Log.d(TAG, "onPropertyCallBack: "+ method);
            } else if(method.equals(METHOD_PROPERTY_CONTROL)) {
                Log.d(TAG, "onPropertyCallBack: "+ method);
            } else if(method.equals(METHOD_PROPERTY_GET_STATUS)) {
                Log.d(TAG, "onPropertyCallBack: "+ method);
            } else if(method.equals(METHOD_PROPERTY_CLEAR_CONTROL)) {
                Log.d(TAG, "onPropertyCallBack: "+ method);
            } else if(method.equals(METHOD_PROPERTY_REPORT_INFO)) {
                Log.d(TAG, "onPropertyCallBack: "+ method);
            } else {
                Log.e(TAG, "onPropertyCallBack: invalid method:" + method);
            }
        } catch (Exception e) {
            Log.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
    }

    /**
     * 事件下行消息处理
     * @param message 消息内容
     */
    private void onEventMessageArrivedCallBack(MqttMessage message){
        Log.d(TAG, "event down stream message received : " + message);
        // 查询列表中的event，并处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if(!method.equals(METHOD_EVENT_REPLY)) {
                Log.e(TAG, "onEventMessageArrivedCallBack: invalid method:" + method);
                return;
            }
            String clientToken = jsonObj.getString("clientToken");
            long timestamp = mEventWaitList.get(clientToken);
            if(timestamp - System.currentTimeMillis() > mEventWaitTimeout) {
                Log.e(TAG, "onEventMessageArrivedCallBack: event reply timeout! ClientToken:" + clientToken);
            } else {
                mEventWaitList.remove(clientToken);
                int code = jsonObj.getInt("code");
                if(0 == code) {
                    Log.d(TAG, "onEventMessageArrivedCallBack: event reply OK! ClientToken:" + clientToken);
                } else {
                    Log.e(TAG, "onEventMessageArrivedCallBack: event reply failed! ClientToken:" + clientToken + ",code:" + code);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onEventMessageArrivedCallBack: invalid message:" + message);
        }
    }

    /**
     * 行为下行消息处理
     * @param message 消息内容
     */
    private void onActionMessageArrivedCallBack(MqttMessage message){
        Log.d(TAG, "action down stream message received : " + message);
        // 查询列表中的action，然后调用相应的回调函数
    }

    /**
     * 消息到达回调函数
     * @param topic   消息主题
     * @param message 消息内容
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if(topic.equals(mPropertyDownStreamTopic)) {
            onPropertyMessageArrivedCallBack(message);
        } else if (topic.equals(mEventDownStreamTopic)) {
            onEventMessageArrivedCallBack(message);
        } else if (topic.equals(mActionDownStreamTopic)) {
            onActionMessageArrivedCallBack(message);
        }
        super.messageArrived(topic, message);
    }
}
