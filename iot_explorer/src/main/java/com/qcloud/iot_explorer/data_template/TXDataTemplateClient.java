package com.qcloud.iot_explorer.data_template;

import android.content.Context;
import android.util.Log;

import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttConnection;
import com.qcloud.iot_explorer.mqtt.TXMqttRequest;
import com.qcloud.iot_explorer.utils.TXLog;

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
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.ACTION_UP_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.EVENT_UP_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_TOPIC;
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
    public static final String TAG = "TXDATATEMPLATE_" + MQTT_SDK_VER;

    private static AtomicInteger requestID = new AtomicInteger(0);

    //等待下行回复
    private class replyWaitNode{
        public long mTimestamp;
        public TXDataTemplateDownCallBack mCallBack;
        public replyWaitNode(long timestamp, TXDataTemplateDownCallBack callBack) {
            mTimestamp = timestamp;
            mCallBack = callBack;
        }
    }
    private ConcurrentHashMap<String, replyWaitNode> mReplyWaitList = null;
    private long mReplyWaitTimeout = 60*1000; //60s

    //上下行消息主题
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
        mReplyWaitList = new ConcurrentHashMap<>();
        new checkReplyTimeoutThread().start();
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
                break;
            case ACTION_DOWN_TOPIC:
                topic = mActionDownStreamTopic;
                break;
            default:
                TXLog.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }

        ret = super.subscribe(topic, qos, mqttRequest);
        if(Status.OK != ret) {
            TXLog.e(TAG, "subscribeTopic failed! " + topic);
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
                TXLog.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }

        ret = super.unSubscribe(topic, mqttRequest);
        if(Status.OK != ret) {
            TXLog.e(TAG, "subscribeTopic failed! " + topic);
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
                TXLog.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }
        return super.publish(topic, message, null);
    }

    /**property*/
    //属性上报
    public Status propertyReport(String params, String metadata, TXDataTemplateDownCallBack reportReplyCallBack) {
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_PROPERTY_REPORT);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
            JSONObject paramsJson = new JSONObject(params);
            object.put("params", paramsJson);
            if (null != metadata)
                object.put("metadata", metadata);
        } catch (Exception e) {
            TXLog.e(TAG, "propertyReport: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        Status ret =  publishTemplateMessage(PROPERTY_UP_TOPIC, message);
        if(Status.OK == ret) {
            mReplyWaitList.put(clientToken,new replyWaitNode(System.currentTimeMillis(), reportReplyCallBack)); //加入到等待列表中
            return Status.OK;
        } else{
            return Status.ERROR;
        }
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
            TXLog.e(TAG, "propertyGetStatus: failed!");
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
            TXLog.e(TAG, "propertyClearControl: failed!" );
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
            TXLog.e(TAG, "propertyReportInfo: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(PROPERTY_UP_TOPIC, message);
    }

    /**event*/
    //单个事件上报
    public Status eventSinglePost(String eventId, String type, String params, TXDataTemplateDownCallBack eventReplyCallBack) {
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        long timestamp =  System.currentTimeMillis();
        try {
            object.put("method", METHOD_EVENT_POST);
            object.put("clientToken", clientToken);
            object.put("eventId", eventId);
            object.put("type", type);
            object.put("timestamp", timestamp);
            JSONObject paramsJson = new JSONObject(params);
            object.put("params", paramsJson);
        } catch (Exception e) {
            TXLog.e(TAG, "eventSinglePost: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        Status ret = publishTemplateMessage(EVENT_UP_TOPIC, message);
        if(Status.OK == ret) {
            mReplyWaitList.put(clientToken,new replyWaitNode(System.currentTimeMillis(), eventReplyCallBack)); //加入到等待列表中
            return Status.OK;
        } else{
            return Status.ERROR;
        }
    }

    //多个事件上报
    public Status eventsPost(String events, TXDataTemplateDownCallBack eventReplyCallBack) {
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_EVENTS_POST);
            object.put("clientToken", clientToken);
            JSONArray  eventsJson = new JSONArray(events);
            object.put("events", eventsJson);
        } catch (Exception e) {
            TXLog.e(TAG, "eventsPost: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        Status ret = publishTemplateMessage(EVENT_UP_TOPIC, message);
        if(Status.OK == ret) {
            mReplyWaitList.put(clientToken,new replyWaitNode(System.currentTimeMillis(), eventReplyCallBack)); //加入到等待列表中
            return Status.OK;
        } else{
            return Status.ERROR;
        }
    }

    /**action*/
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
            TXLog.e(TAG, "actionReply: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(1); //qos 1
        message.setPayload(object.toString().getBytes());

        return  publishTemplateMessage(ACTION_UP_TOPIC, message);
    }

    //检查超时线程
    private class checkReplyTimeoutThread extends Thread {
        public void run() {
            while(true) {
                Iterator<Map.Entry<String, replyWaitNode>> entries = mReplyWaitList.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, replyWaitNode> entry = entries.next();
                    if (System.currentTimeMillis() - entry.getValue().mTimestamp > mReplyWaitTimeout) {
                        TXLog.e(TAG, "Event reply timeout. Client token:" + entry.getKey());
                        mReplyWaitList.remove(entry.getKey());
                    }
                }
                try {
                    Thread.sleep(mReplyWaitTimeout);
                } catch (InterruptedException e) {
                    TXLog.e(TAG, "The thread has been interrupted");
                }
            }
        }
    }

    private void handleReply(MqttMessage message) {
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String clientToken = jsonObj.getString("clientToken");
            replyWaitNode node = mReplyWaitList.get(clientToken);
            if (null == node) {
                TXLog.e(TAG, "handleReply: client token [%s] not found!", clientToken);
                return;
            }
            if (System.currentTimeMillis() - node.mTimestamp > mReplyWaitTimeout) {
                TXLog.e(TAG, "handle_reply: reply timeout! ClientToken:" + clientToken);
            } else {
                int code = jsonObj.getInt("code");
                if (0 == code) {
                    TXLog.d(TAG, "handle_reply: reply OK! ClientToken:" + clientToken);
                } else {
                    TXLog.e(TAG, "handle_reply: reply failed! ClientToken:" + clientToken + ",code:" + code);
                }
            }
            if (null != node.mCallBack) {
                node.mCallBack.onDownStreamCallBack(new String(message.getPayload()));
            }
            mReplyWaitList.remove(clientToken);
        } catch (JSONException e) {
            TXLog.e(TAG, "handle_reply: failed! Message[ %s ] is not vaild!", message);
        }
    }

    /**
     * 属性下行消息处理
     * @param message 消息内容
     */
    private void onPropertyMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "property down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");

            if (!method.equals(METHOD_PROPERTY_REPORT_REPLY) && !method.equals(METHOD_PROPERTY_CONTROL_REPLY)
                &&  !method.equals(METHOD_PROPERTY_GET_STATUS_REPLY) && !method.equals(METHOD_PROPERTY_CLEAR_CONTROL_REPLY)
                &&  !method.equals(METHOD_PROPERTY_REPORT_INFO_REPLY)) {
                TXLog.e(TAG, "onPropertyCallBack: invalid method:" + method);
                return;
            }

            handleReply(message);
        } catch (Exception e) {
            TXLog.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
    }

    /**
     * 事件下行消息处理
     * @param message 消息内容
     */
    private void onEventMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "event down stream message received : " + message);
        // 查询列表中的event，并处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if(!method.equals(METHOD_EVENT_REPLY) && !method.equals(METHOD_EVENTS_REPLY)) {
                TXLog.e(TAG, "onEventMessageArrivedCallBack: invalid method:" + method);
                return;
            }
            handleReply(message);
        } catch (Exception e) {
            TXLog.e(TAG, "onEventMessageArrivedCallBack: invalid message:" + message);
        }
    }

    /**
     * 行为下行消息处理
     * @param message 消息内容
     */
    private void onActionMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "action down stream message received : " + message);
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
