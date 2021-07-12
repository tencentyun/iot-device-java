package com.tencent.iot.explorer.device.rtc.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCUIManager;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.Base64;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TXTRTCTemplateClient extends TXMqttConnection {
    //数据模板
    private TXTRTCDataTemplate mDataTemplate;
    //属性下行topic
    public String mPropertyDownStreamTopic;

    public TXTRTCTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXTRTCCallBack trtcCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new TXTRTCDataTemplate(context, this,  productID,  deviceName, jsonFileName, downStreamCallBack, trtcCallBack);
        this.mPropertyDownStreamTopic = mDataTemplate.mPropertyDownStreamTopic;
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return this.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected);
    }

    /**
     * 设备绑定App下发的token
     * @return 生成的绑定设备的二维码字符串;
     */
    public Status appBindToken(String token) {
        return this.appBindToken(token);
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
     * 上报实时音视频类设备呼叫属性
     * @param callStatus 呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param callType 邀请类型 1-语音通话，2-视频通话
     * @param userId 被呼叫用户id json字符串
     * @param agent 代理方
     * @return 结果
     */
    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId, String agent, JSONObject params) {
        return mDataTemplate.reportCallStatusProperty(callStatus, callType, userId, agent, params);
    }

    /**
     * 上报重置设备呼叫属性 为空闲
     * @return 结果
     */
    private Status reportResetCallStatusProperty() {
        return mDataTemplate.reportResetCallStatusProperty();
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
        if (!TRTCUIManager.getInstance().isCalling) {
            reportResetCallStatusProperty();
        }
    }
}
