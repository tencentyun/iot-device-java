package com.tencent.iot.explorer.device.rtc.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.rtc.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCUIManager;
import com.tencent.iot.explorer.device.rtc.data_template.model.TXTRTCDataTemplateConstants;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_ACTION;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_STREAM_TOPIC;

public class TXTRTCDataTemplate extends TXDataTemplate {

    //Mqtt 连接
    private TXMqttConnection mConnection;

    private TXTRTCCallBack mTrtcCallBack = null;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private boolean mIsBusy = false; //trtc设备是否空闲

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXTRTCDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXTRTCCallBack trtcCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
        this.mTrtcCallBack = trtcCallBack;
    }

    /**
     * TRTC属性下行消息处理
     * @param message 消息内容
     */
    private void onPropertyMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "property down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_PROPERTY_CONTROL)){ //
                JSONObject params = jsonObj.getJSONObject("params");
                if (mTrtcCallBack != null) {
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
                        if (!mIsBusy || callStatus != 1) {
                            mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, userAgent, TRTCCalling.TYPE_VIDEO_CALL);
                        }
                        if (mIsBusy && callStatus == 1) { //接收到其他用户呼叫请求
                            reportExtraInfoRejectUserId(userid);
                            return;
                        }
                        if (callStatus == 0) {
                            mIsBusy = false;
                        } else {
                            mIsBusy = true;
                        }
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
                        if (!mIsBusy || callStatus != 1) {
                            mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, userAgent, TRTCCalling.TYPE_AUDIO_CALL);
                        }
                        if (mIsBusy && callStatus == 1) { //接收到其他用户呼叫请求
                            reportExtraInfoRejectUserId(userid);
                            return;
                        }
                        if (callStatus == 0) {
                            mIsBusy = false;
                        } else {
                            mIsBusy = true;
                        }
                    } else if (params.has(TXTRTCDataTemplateConstants.PROPERTY_SYS_CALL_USERLIST)) {
                        //上报下接收到的userlist
                        Status status = sysPropertyReport(params, null);
                        if(Status.OK != status) {
                            TXLog.e(TAG, "property report failed!");
                        }
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
    }

    private Status reportExtraInfoRejectUserId(String rejectUserId) {
        JSONObject property = new JSONObject();

        if (rejectUserId == null && rejectUserId.length() == 0) {
            TXLog.e(TAG, "reportExtraInfoRejectUserId rejectUserId empty");
            return Status.PARAMETER_INVALID;
        }

        JSONObject extraInfo = new JSONObject();
        try {
            extraInfo.put(TXTRTCDataTemplateConstants.PROPERTY_REJECT_USERID, rejectUserId);
            String extraInfoString = extraInfo.toString();

            property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO, extraInfoString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Status status = sysPropertyReport(property, null);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        return status;
    }

    /**
     * TRTC行为下行消息处理
     * @param message 消息内容
     */
    private void onActionMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "action down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_ACTION) && jsonObj.has("actionId")){ //
                String actionId = jsonObj.getString("actionId");
                if (actionId.equals(TXTRTCDataTemplateConstants.ACTION_SYS_TRTC_JOIN_ROOM)) { // 设备加房间 行为
                    JSONObject params = jsonObj.getJSONObject("params");
                    if (mTrtcCallBack != null) {

                        Integer SdkAppId = params.getInt("SdkAppId");
                        String UserId = params.getString("UserId");
                        String UserSig = params.getString("UserSig");
                        String StrRoomId = params.getString("StrRoomId");
                        RoomKey room = new RoomKey();
                        room.setAppId(SdkAppId);
                        room.setUserId(UserId);
                        room.setUserSig(UserSig);
                        room.setRoomId(StrRoomId);
                        room.setCallType(TRTCCalling.TYPE_VIDEO_CALL);
                        if (TRTCUIManager.getInstance().callingUserId == "") {
                            mTrtcCallBack.trtcJoinRoomCallBack(room);
                        }
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid message: " + message);
        }
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
        JSONObject property = new JSONObject();

        if (userId.equals("null") && params != null && params.length() != 0) {
            //检查构造是否符合json文件中的定义
            if(Status.OK != checkPropertyJson(property)){
                TXLog.e(TAG, "propertyReport: invalid property json!");
                return Status.PARAMETER_INVALID;
            }
            property = params;
        }
        try {
            mIsBusy = callStatus != 0;
            if (callType == TRTCCalling.TYPE_VIDEO_CALL) { //video
                property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS,callStatus);
                if (!userId.equals("")) {
                    property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID,userId);
                }
                if (!agent.equals("")) {
                    property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_AGENT,agent);
                }
                String extraInfo = ""; //预留 额外信息 _sys_extra_info
                if (!extraInfo.equals("")) {
                    property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO,extraInfo);
                }
            } else if (callType == TRTCCalling.TYPE_AUDIO_CALL) { //audio
                property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS,callStatus);
                if (!userId.equals("")) {
                    property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID,userId);
                }
                if (!agent.equals("")) {
                    property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_AGENT,agent);
                }
                String extraInfo = ""; //预留 额外信息 _sys_extra_info
                if (!extraInfo.equals("")) {
                    property.put(TXTRTCDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO,extraInfo);
                }
            } else {
                return Status.ERR_JSON_CONSTRUCT;
            }
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
     * 系统属性上报， 不检查构造是否符合json文件中的定义
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    private Status sysPropertyReport(JSONObject property, JSONObject metadata) {
        //不检查构造是否符合json文件中的定义

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
        String objectString = object.toString();
        objectString = objectString.replace("\\/", "/");

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(objectString.getBytes());

        return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        if(topic.equals(mPropertyDownStreamTopic)) {
            onPropertyMessageArrivedCallBack(message);
        } else if (topic.equals(TOPIC_ACTION_DOWN_PREFIX + mProductId + "/" + mDeviceName)) {
            onActionMessageArrivedCallBack(message);
        }
    }
}
