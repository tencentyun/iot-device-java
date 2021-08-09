package com.tencent.iot.explorer.device.face.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.face.consts.Common;
import com.tencent.iot.explorer.device.face.resource.TXResourceCallBack;
import com.tencent.iot.explorer.device.face.resource.TXResourceImpl;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_EVENT_POST;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.EVENT_UP_STREAM_TOPIC;

public class TXFaceKitTemplate extends TXDataTemplate {

    /**
     * service method
     */
    String METHOD_SERVICE_CALL_SERVICE = "call_service";

    /**
     * service id
     */
    String SERVICE_ID_AI_FACE_LICENSE = "request_ai_face_license";

    //设备服务
    private String mServiceDownStreamTopic;
    private String mServiceUptreamTopic;
    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    //Mqtt 连接
    private TXMqttConnection mConnection;
    protected TXResourceImpl mResourceImpl = null;

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXFaceKitTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;

        this.mServiceDownStreamTopic = TOPIC_SERVICE_DOWN_PREFIX + productId + "/"  + deviceName;
        this.mServiceUptreamTopic = TOPIC_SERVICE_UP_PREFIX + productId + "/"  + deviceName;
    }

    /**
     * 初始化资源下载功能。
     *
     * @param storagePath 资源下载文件存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param cosServerCaCrtList 资源下载服务器的CA证书链
     * @param callback 事件回调
     */
    public void initResource(String storagePath, String[] cosServerCaCrtList, TXResourceCallBack callback) {
        mResourceImpl = new TXResourceImpl(this, mConnection, storagePath, cosServerCaCrtList, callback);
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
        mResourceImpl = new TXResourceImpl(this, mConnection, storagePath, callback);
    }

    /**
     * 系统单个事件上报， 不检查构造是否符合json文件中的定义
     * @param eventId 事件ID
     * @param type 事件类型
     * @param params 参数
     * @return 结果
     */
    public Status sysEventSinglePost(String eventId, String type, JSONObject params) {
        //不检查构造是否符合json文件中的定义

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
        message.setQos(1);
        message.setPayload(object.toString().getBytes());

        return publishTemplateMessage(clientToken,EVENT_UP_STREAM_TOPIC, message);
    }

    /**
     * 单个人脸下载/删除/注册状态的事件上报， 不检查构造是否符合json文件中的定义
     * @param resourceName csv资源文件名
     * @param version 版本
     * @param featureId 特征ID
     * @param result 结果值 1：下载成功，待注册 2：下载失败 3：注册成功 4：注册失败 5：删除成功 6：删除失败
     * @return 返回状态
     */
    public Status faceStatusPost(String resourceName, String version, String featureId, int result) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(Common.PARAMS_RESOURCE_NAME, resourceName);
            obj.put(Common.PARAMS_VERSION, version);
            obj.put(Common.PARAMS_FEATURE_ID, featureId);
            obj.put(Common.PARAMS_UPDATE_RESULT, result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sysEventSinglePost(Common.EVENT_UPDATE_RESULT_REPORT, Common.EVENT_TYPE_INFO, obj);
    }

    /**
     * 上报设备当前资源版本信息到后台服务器。
     *
     * @param resourceList JSONArray
     *            装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentResourceVersion(JSONArray resourceList) {
        if (mResourceImpl != null) {
            return mResourceImpl.reportCurrentFirmwareVersion(resourceList);
        }

        return Status.ERROR;
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
        if (mResourceImpl != null) {
            return mResourceImpl.reportUpdateResourceState(state.toString().toLowerCase(), resourceName, resultCode, resultMsg, version);
        }

        return Status.ERROR;
    }

    /**
     * 订阅Service主题
     */
    public Status subscribeServiceTopic() {
        Status ret = mConnection.subscribe(mServiceDownStreamTopic, TXMqttConstants.QOS1, null);
        if(Status.OK != ret) {
            TXLog.e(TAG, "subscribeServiceTopic failed! " + mServiceDownStreamTopic);
            return ret;
        }
        return Status.OK;
    }

    /**
     * 取消订阅Service主题
     */
    public Status unSubscribeServiceTopic() {
        Status ret = mConnection.unSubscribe(mServiceDownStreamTopic, null);
        if(Status.OK != ret) {
            TXLog.e(TAG, "subscribeServiceTopic failed! " + mServiceDownStreamTopic);
            return ret;
        }
        return Status.OK;
    }

    /**
     * 初始化授权
     */
    public void initAuth(String machineId, String machineInfo) {
        requestAIFaceLicense(machineId, machineInfo);
        //在对应callback中获取到License调用YTCommon中授权方法
    }

    /**
     * 请求AI的License
     * @param machineId 设备序列号
     * @param machineInfo 设备硬件信息
     * @return 结果
     */
    private Status requestAIFaceLicense(String machineId, String machineInfo) {

        //构造发布信息
        JSONObject object = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        try {
            object.put("method", METHOD_SERVICE_CALL_SERVICE);
            object.put("timestamp", System.currentTimeMillis());
            object.put("serviceId", SERVICE_ID_AI_FACE_LICENSE);
            object.put("clientToken", clientToken);

            JSONObject params = new JSONObject();
            params.put("machine_id",machineId);
            params.put("machine_type", "ANDROID");
            params.put("machine_info", machineInfo);
            object.put("params", params);
        } catch (Exception e) {
            TXLog.e(TAG, "requestAIFaceLicense: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(1); //qos 1
        message.setPayload(object.toString().getBytes());
        return mConnection.publish(mServiceUptreamTopic, message, null);
    }

    /**
     * 消息到达回调函数
     * @param topic   消息主题
     * @param message 消息内容
     * @throws Exception 异常
     */
    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        if (mResourceImpl != null ) {
            mResourceImpl.processMessage(topic, message);
        }
    }
}
