package com.tencent.iot.explorer.device.face.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.face.resource.TXResourceCallBack;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

public class TXFaceKitTemplateClient extends TXMqttConnection {
    //数据模板
    private TXFaceKitTemplate mDataTemplate;
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
    public TXFaceKitTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                   MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                   final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXAuthCallBack authCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new TXFaceKitTemplate(context, this,  productID,  deviceName, jsonFileName, downStreamCallBack, authCallBack);
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
    public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos) {
        return  mDataTemplate.subscribeTemplateTopic(topicId, qos);
    }

    /**
     * 订阅Service主题
     */
    public Status subscribeServiceTopic() {
        return mDataTemplate.subscribeServiceTopic();
    }

    /**
     * 取消订阅Service主题
     */
    public Status unSubscribeServiceTopic() {
        return mDataTemplate.unSubscribeServiceTopic();
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId) {
        return mDataTemplate.unSubscribeTemplateTopic(topicId);
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
     * 初始化授权
     */
    public void initAuth() {
        mDataTemplate.initAuth();
    }
    /**
     * 初始化资源下载功能。
     *
     * @param storagePath 资源下载文件存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param cosServerCaCrtList 资源下载服务器的CA证书链
     * @param callback 事件回调
     */
    public void initResource(String storagePath, String[] cosServerCaCrtList, TXResourceCallBack callback) {
        mDataTemplate.initResource(storagePath, cosServerCaCrtList, callback);
    }

    /**
     * 初始化资源下载功能。
     *
     * @param storagePath
     *            资源下载文件存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param callback
     *            事件回调
     */
    public void initResource(String storagePath, TXResourceCallBack callback) {
        mDataTemplate.initResource(storagePath, callback);
    }

    /**
     * 上报设备当前资源版本信息到后台服务器。
     *
     * @param resourceList JSONArray
     *            装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentResourceVersion(JSONArray resourceList) {
        return mDataTemplate.reportCurrentResourceVersion(resourceList);
    }

    /**
     * 上报设备资源下载状态到后台服务器。
     *
     * @param state
     *            状态
     * @param resultCode
     *            结果代码。0：表示成功；其它：表示失败；常见错误码：-1: 下载超时;
     *            -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新资源文件失败
     * @param resultMsg
     *            结果描述
     * @param version
     *            版本号
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportResourceState(String resourceName , TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
        return mDataTemplate.reportResourceState(resourceName, state, resultCode, resultMsg, version);
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
