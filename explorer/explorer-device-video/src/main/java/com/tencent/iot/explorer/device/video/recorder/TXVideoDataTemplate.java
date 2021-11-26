package com.tencent.iot.explorer.device.video.recorder;

import android.content.Context;
import android.util.Log;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.rtc.data_template.TRTCCallStatus;
import com.tencent.iot.explorer.device.rtc.data_template.TXTRTCCallBack;
import com.tencent.iot.explorer.device.rtc.data_template.TXTRTCDataTemplate;
import com.tencent.iot.explorer.device.rtc.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.rtc.data_template.model.TXTRTCDataTemplateConstants;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_STREAM_TOPIC;

public class TXVideoDataTemplate extends TXTRTCDataTemplate {
    private String TAG = TXVideoDataTemplate.class.getSimpleName();

    // Mqtt 连接
    private TXMqttConnection mConnection;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private TXVideoCallBack videoCallBack;

    /**
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param connection 连接实例
     * @param productId 产品名
     * @param deviceName 设备名，唯一
     * @param jsonFileName 数据模板描述文件
     */
    public TXVideoDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXVideoCallBack videoCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, null, videoCallBack);
        this.mConnection = connection;
        this.videoCallBack = videoCallBack;
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
     * 系统属性上报， 不检查构造是否符合 json 文件中的定义
     * @param property 属性的 json
     * @param metadata 属性的 metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    private Status sysPropertyReport(JSONObject property, JSONObject metadata) {
        // 不检查构造是否符合 json 文件中的定义

        // 构造发布信息
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

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        if (topic.equals(mPropertyDownStreamTopic)) {
            onPropertyMessageArrivedCallBack(message);
        } else if (topic.equals(TOPIC_ACTION_DOWN_PREFIX + mProductId + "/" + mDeviceName)) {

        }
    }

    private void onPropertyMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "property down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_PROPERTY_CONTROL)){ //
                JSONObject params = jsonObj.getJSONObject("params");
                if (videoCallBack != null) {
                    if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(TXTRTCDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS);
                        String userid = "";
                        if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID)) {
                            userid = params.getString(TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID);
                        }
                        String userAgent = "";
                        if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_AGENT)) {
                            userAgent = params.getString(TXTRTCDataTemplateConstants.PROPERTY_SYS_AGENT);
                        }
                        if (isBusy() && !getCurrentCallingUserid().equals(userid)) { //非当前设备的通话用户的请求忽略
                            if (callStatus != TRTCCallStatus.TYPE_IDLE_OR_REFUSE) {reportExtraInfoRejectUserId(userid);}
                            return;
                        }
                        convertData2Callback(callStatus, userid, userAgent, TRTCCalling.TYPE_VIDEO_CALL);
                    } else if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(TXTRTCDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS);
                        String userid = "";
                        if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID)) {
                            userid = params.getString(TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID);
                        }
                        String userAgent = "";
                        if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_AGENT)) {
                            userAgent = params.getString(TXTRTCDataTemplateConstants.PROPERTY_SYS_AGENT);
                        }
                        if (isBusy() && !getCurrentCallingUserid().equals(userid)) { //非当前设备的通话用户的请求忽略
                            if (callStatus != TRTCCallStatus.TYPE_IDLE_OR_REFUSE) {reportExtraInfoRejectUserId(userid);}
                            return;
                        }
                        convertData2Callback(callStatus, userid, userAgent, TRTCCalling.TYPE_AUDIO_CALL);
                    } else if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_CALL_USERLIST)) {
                        //上报下接收到的 userlist
                        Status status = sysPropertyReport(params, null);
                        if(Status.OK != status) {
                            TXLog.e(TAG, "property report failed!");
                        }
                    }
                }
            } else if (method.equals(METHOD_PROPERTY_GET_STATUS_REPLY)) {
                JSONObject data = jsonObj.getJSONObject("data").getJSONObject("reported");
                if (data.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS) && data.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS)) {
                    Integer videoCallStatus = data.getInt(TXTRTCDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS);
                    Integer audioCallStatus = data.getInt(TXTRTCDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS);
                    if (!isBusy() &&(videoCallStatus != TRTCCallStatus.TYPE_IDLE_OR_REFUSE || audioCallStatus != TRTCCallStatus.TYPE_IDLE_OR_REFUSE)) {
                        // 不在通话中，并且status状态不对  重置video和audio的status状态为0
                        reportResetCallStatusProperty();
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
    }

    private void convertData2Callback(int callStatus, String userid, String userAgent, int callType) {
        if (isBusy() && callStatus == VideoCallStatus.TYPE_CALLING && getCurrentCallingUserid().equals(userid)) {  // 对方接受了通话请求
            Log.e("XXX", "accpet call");
            videoCallBack.onUserAccept(userid, userAgent, callType);

        } else if (!isBusy() && callStatus == VideoCallStatus.TYPE_CALLING && !getCurrentCallingUserid().equals(userid)) { // 来了一通新的通话求
            Log.e("XXX", "a new call");
            videoCallBack.onNewCall(userid, userAgent, callType);

        } else if (isBusy() && callStatus == VideoCallStatus.TYPE_IDLE_OR_REFUSE && getCurrentCallingUserid().equals(userid)) { // 当前通话结束
            Log.e("XXX", "call over");
            videoCallBack.onCallOver(userid, userAgent, callType);
        }

        if (callStatus == VideoCallStatus.TYPE_IDLE_OR_REFUSE) {
            setBusy(false);
            setCurrentCallingUserid("");
        } else {
            setBusy(true);
            setCurrentCallingUserid(userid);
        }
    }
}
