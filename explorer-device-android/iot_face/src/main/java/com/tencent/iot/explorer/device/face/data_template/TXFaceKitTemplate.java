package com.tencent.iot.explorer.device.face.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.face.resource.TXResourceCallBack;
import com.tencent.iot.explorer.device.face.resource.TXResourceImpl;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class TXFaceKitTemplate extends TXDataTemplate {


    String TOPIC_SERVICE_DOWN_PREFIX = "$thing/down/service/";
    String TOPIC_SERVICE_UP_PREFIX = "$thing/up/service/";

    /**
     * service method
     */
    String METHOD_SERVICE_CALL_SERVICE = "call_service";
    String METHOD_SERVICE_CALL_SERVICE_REPLY = "call_service_reply";

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

    private TXAuthCallBack mAuthCallBack = null;

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXFaceKitTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXAuthCallBack authCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
        this.mAuthCallBack = authCallBack;

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
        mResourceImpl = new TXResourceImpl(mConnection, storagePath, cosServerCaCrtList, callback);
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
        mResourceImpl = new TXResourceImpl(mConnection, storagePath, callback);
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
    public void initAuth() {
        String machineId = "";//从YTCommon获取
        String machineInfo = "";//从YTCommon获取
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
            params.put("machine_type", "Android");
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
     * 服务下行消息处理
     * @param message 消息内容
     */
    private void onServiceMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "service down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (!method.equals(METHOD_SERVICE_CALL_SERVICE_REPLY)) {
                TXLog.e(TAG, "onServiceCallBack: invalid method:" + method);
                return;
            }
            //控制下发消息处理
            if (method.equals(METHOD_SERVICE_CALL_SERVICE_REPLY)) {
                if(null != mAuthCallBack) {//onGetAIFaceLicenseCallBack
                    Integer code = jsonObj.getInt("code");
                    String status = jsonObj.getString("status");
                    String license = null;
                    if (code == 0) {//获取AI License成功，解析获取License
                        JSONObject response = jsonObj.getJSONObject("response");
                        license = response.getString("license");
                    }
                    mAuthCallBack.onGetAIFaceLicenseCallBack(code,status,license);
                }
            } else {
//                handleReply(message, false);
            }
        } catch (Exception e) {
            TXLog.e(TAG, "onServiceMessageArrivedCallBack: invalid message: " + message);
        }
    }

    /**
     * 消息到达回调函数
     * @param topic   消息主题
     * @param message 消息内容
     * @throws Exception 异常
     */
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);

        boolean consumed = false;
        if (mResourceImpl != null ) {
            consumed = mResourceImpl.processMessage(topic, message);
        }

        if (!consumed) {
            if (topic.equals(mServiceDownStreamTopic)) {
                onServiceMessageArrivedCallBack(message);
            }
        }
    }
}
