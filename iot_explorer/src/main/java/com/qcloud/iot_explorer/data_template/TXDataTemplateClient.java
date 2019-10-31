package com.qcloud.iot_explorer.data_template;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.JsonReader;
import android.util.Log;

import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttConnection;
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
    public static final String TAG = "TXDATATEMPLATE_" + MQTT_SDK_VER;

    private static AtomicInteger requestID = new AtomicInteger(0);

    //等待下行回复
    private ConcurrentHashMap<String, Long> mReplyWaitList = null;
    private long mReplyWaitTimeout = 60*1000; //60s

    //上下行消息主题
    private String mPropertyDownStreamTopic;
    private String mPropertyUptreamTopic;
    private String mEventDownStreamTopic;
    private String mEventUptreamTopic;
    private String mActionDownStreamTopic;
    private String mActionUptreamTopic;

    //下行消息回调函数
    TXDataTemplateDownStreamCallBack mDownStreamCallBack = null;

    private TXDataTemplateJson mDataTemplateJson;

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXDataTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        mPropertyDownStreamTopic = TOPIC_PROPERTY_DOWN_PREFIX + mProductId + "/"  + mDeviceName;
        mPropertyUptreamTopic = TOPIC_PROPERTY_UP_PREFIX + mProductId + "/"  + mDeviceName;
        mEventDownStreamTopic = TOPIC_EVENT_DOWN_PREFIX + mProductId + "/"  + mDeviceName;
        mEventUptreamTopic = TOPIC_EVENT_UP_PREFIX + mProductId + "/"  + mDeviceName;
        mActionDownStreamTopic = TOPIC_ACTION_DOWN_PREFIX + mProductId + "/"  + mDeviceName;
        mActionUptreamTopic = TOPIC_ACTION_UP_PREFIX + mProductId + "/"  + mDeviceName;

        mDataTemplateJson = new TXDataTemplateJson (context,jsonFileName);
        mDownStreamCallBack = downStreamCallBack;
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
            case PROPERTY_DOWN_STREAM_TOPIC:
                topic = mPropertyDownStreamTopic;
                break;
            case EVENT_DOWN_STREAM_TOPIC:
                topic = mEventDownStreamTopic;
                break;
            case ACTION_DOWN_STREAM_TOPIC:
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
            case PROPERTY_DOWN_STREAM_TOPIC:
                topic = mPropertyDownStreamTopic;
                break;
            case EVENT_DOWN_STREAM_TOPIC:
                topic = mEventDownStreamTopic;
                break;
            case ACTION_DOWN_STREAM_TOPIC:
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
    private Status publishTemplateMessage( String clientToken,TemplatePubTopic topicId, MqttMessage message) {
        String topic;
        switch (topicId) {
            case PROPERTY_UP_STREAM_TOPIC:
                topic = mPropertyUptreamTopic;
                break;
            case EVENT_UP_STREAM_TOPIC:
                topic = mEventUptreamTopic;
                break;
            case ACTION_UP_STREAM_TOPIC:
                topic = mActionUptreamTopic;
                break;
            default:
                TXLog.e(TAG, "subscribeTemplateTopic: topic id invalid!" + topicId );
                return Status.PARAMETER_INVALID;
        }
        Status ret = super.publish(topic, message, null);
        if(Status.OK == ret) {
            //加入到等待回复列表中
            if(null != clientToken) {
                mReplyWaitList.put(clientToken, System.currentTimeMillis());
            }
            return Status.OK;
        }
        return ret;
    }

    /**
     * 属性上报
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    public Status propertyReport(JSONObject property, JSONObject metadata) {
        //检查构造是否符合json文件中的定义
        if(Status.OK != mDataTemplateJson.checkPropertyJson(property)){
            TXLog.e(TAG, "propertyReport: invalid property json!");
            return Status.PARAMETER_INVALID;
        }

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

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

       return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }

    /**
     * 获取状态
     * @param type 类型
     * @param showmeta 是否携带showmeta
     * @return 结果
     */
    public Status propertyGetStatus(String type, boolean showmeta) {
        if (!type.equals("report") && !type.equals("control")) {
            TXLog.e(TAG, "propertyGetStatus: invalid type[%s]!", type);
            return Status.PARAMETER_INVALID;
        }
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_PROPERTY_GET_STATUS);
            object.put("clientToken", clientToken);
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

        return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }

    /**
     * 设备基本信息上报
     * @param params 参数
     * @return 结果
     */
    public Status propertyReportInfo(JSONObject params) {
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_PROPERTY_REPORT_INFO);
            object.put("clientToken", clientToken);
            object.put("params", params);
        } catch (Exception e) {
            TXLog.e(TAG, "propertyReportInfo: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }

    /**
     * 清理控制信息
     * @return 结果
     */
    public Status propertyClearControl() {
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_PROPERTY_CLEAR_CONTROL);
            object.put("clientToken", clientToken);
        } catch (Exception e) {
            TXLog.e(TAG, "propertyClearControl: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }

    /**
     * 单个事件上报
     * @param eventId 事件ID
     * @param type 事件类型
     * @param params 参数
     * @return 结果
     */
    public Status eventSinglePost(String eventId, String type, JSONObject params) {
        //检查构造是否符合json文件中的定义
        if(Status.OK != mDataTemplateJson.checkEventJson(eventId, type, params)){
            TXLog.e(TAG, "eventSinglePost: invalid parameters!");
            return Status.PARAMETER_INVALID;
        }

        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        long timestamp =  System.currentTimeMillis();
        try {
            object.put("method", METHOD_EVENT_POST);
            object.put("clientToken", clientToken);
            object.put("eventId", eventId);
            object.put("type", type);
            object.put("timestamp", timestamp);
            object.put("params", params);
        } catch (Exception e) {
            TXLog.e(TAG, "eventSinglePost: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return publishTemplateMessage(clientToken,EVENT_UP_STREAM_TOPIC, message);
    }

    /**
     * 多个事件上报
     * @param events 事件集合
     * @return 结果
     */
    public Status eventsPost(JSONArray events) {
        //检查构造是否符合json文件中的定义
        if(Status.OK != mDataTemplateJson.checkEventsJson(events)){
            TXLog.e(TAG, "eventsPost: invalid parameters!");
            return Status.PARAMETER_INVALID;
        }

        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_EVENTS_POST);
            object.put("clientToken", clientToken);
            object.put("events", events);
        } catch (Exception e) {
            TXLog.e(TAG, "eventsPost: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(object.toString().getBytes());

        return publishTemplateMessage(clientToken,EVENT_UP_STREAM_TOPIC, message);
    }

    /**
     * 回复属性下发控制消息
     * @param clientToken 接受到的client token
     * @param code 结果码
     * @param status 状态信息
     * @return 结果
     */
    private Status controlReply(String clientToken, int code, String status) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_PROPERTY_CONTROL_REPLY);
            object.put("clientToken",clientToken);
            object.put("code", code);
            object.put("status", status);
        } catch (Exception e) {
            TXLog.e(TAG, "actionReply: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(1); //qos 1
        message.setPayload(object.toString().getBytes());

        return publishTemplateMessage(null, PROPERTY_UP_STREAM_TOPIC, message);
    }

    /**
     * 回复属性下发控制消息
     * @param clientToken 接受到的client token
     * @param code 结果码
     * @param status 状态信息
     * @return 结果
     */
    private Status actionReply(String clientToken,int code, String status, JSONObject response) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", METHOD_ACTION_REPLY);
            object.put("clientToken", clientToken);
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

        return  publishTemplateMessage(null, ACTION_UP_STREAM_TOPIC, message);
    }

    /**
     * 检查回复是否超时
     */
    private class checkReplyTimeoutThread extends Thread {
        public void run() {
            while(true) {
                Iterator<Map.Entry<String, Long>> entries = mReplyWaitList.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, Long> entry = entries.next();
                    if (System.currentTimeMillis() - entry.getValue() > mReplyWaitTimeout) {
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

    /**
     * 处理Reply回复
     * @param message 回复消息
     */
    private void handleReply(MqttMessage message) {
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String clientToken = jsonObj.getString("clientToken");
            Long timestamp = mReplyWaitList.get(clientToken);
            if (null == timestamp) {
                TXLog.e(TAG, "handleReply: client token [%s] not found!", clientToken);
                return;
            }
            if (System.currentTimeMillis() - timestamp > mReplyWaitTimeout) {
                TXLog.e(TAG, "handle_reply: reply timeout! ClientToken:" + clientToken);
            } else {
                int code = jsonObj.getInt("code");
                if (0 == code) {
                    TXLog.d(TAG, "handle_reply: reply OK! ClientToken:" + clientToken);
                } else {
                    TXLog.e(TAG, "handle_reply: reply failed! ClientToken:" + clientToken + ",code:" + code);
                }
            }
            if (null != mDownStreamCallBack) {
                mDownStreamCallBack.onReplyCallBack(new String(message.getPayload()));
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
            if (!method.equals(METHOD_PROPERTY_REPORT_REPLY)  &&
                !method.equals(METHOD_PROPERTY_CONTROL) &&
                !method.equals(METHOD_PROPERTY_GET_STATUS_REPLY) &&
                !method.equals(METHOD_PROPERTY_CLEAR_CONTROL_REPLY) &&
                !method.equals(METHOD_PROPERTY_REPORT_INFO_REPLY)) {
                TXLog.e(TAG, "onPropertyCallBack: invalid method:" + method);
                return;
            }
            //控制下发消息处理
            if (method.equals(METHOD_PROPERTY_CONTROL)) {
                if(null != mDownStreamCallBack) {
                    JSONObject result = mDownStreamCallBack.onControlCallBack(jsonObj.getJSONObject("params"));
                    if (Status.OK != controlReply(jsonObj.getString("clientToken"), result.getInt("code"), result.getString("status"))) {
                        TXLog.e(TAG, "control reply failed!");
                    }
                }
            } else {
                handleReply(message);
            }
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
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if(!method.equals(METHOD_ACTION)) {
                TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid method:" + method);
                return;
            }
            if(null != mDownStreamCallBack) {
                String actionId = jsonObj.getString("actionId");
                JSONObject params = jsonObj.getJSONObject("params");
                //check action
                if (Status.OK !=mDataTemplateJson.checkActionJson(actionId, params)) {
                    TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid action message:" + message);
                    return;
                }
                //callback
                JSONObject result = mDownStreamCallBack.onActionCallBack(jsonObj.getString("actionId"), jsonObj.getJSONObject("params"));
                //check reply
                JSONObject response = result.getJSONObject("response");
                if (Status.OK !=mDataTemplateJson.checkActionReplyJson(actionId, response)) {
                    TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid action reply message:" +  response);
                    return;
                }
                if (Status.OK != actionReply(jsonObj.getString("clientToken"), result.getInt("code"), result.getString("status"), response)) {
                    TXLog.e(TAG, "action reply failed!");
                }
            }
        } catch (Exception e) {
            TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid message:" + message);
        }
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
        if(topic.equals(mPropertyDownStreamTopic)) {
            onPropertyMessageArrivedCallBack(message);
        } else if (topic.equals(mEventDownStreamTopic)) {
            onEventMessageArrivedCallBack(message);
        } else if (topic.equals(mActionDownStreamTopic)) {
            onActionMessageArrivedCallBack(message);
        }
    }
}
