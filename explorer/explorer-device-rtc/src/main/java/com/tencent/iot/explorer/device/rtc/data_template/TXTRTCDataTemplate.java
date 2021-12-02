package com.tencent.iot.explorer.device.rtc.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.common.stateflow.CallState;
import com.tencent.iot.explorer.device.common.stateflow.OnCall;
import com.tencent.iot.explorer.device.common.stateflow.TXCallDataTemplate;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.common.stateflow.entity.TXCallDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCUIManager;
import com.tencent.iot.explorer.device.rtc.data_template.model.TXTRTCDataTemplateConstants;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_ACTION;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;

public class TXTRTCDataTemplate extends TXCallDataTemplate {
    private String TAG = TXTRTCDataTemplate.class.getSimpleName();

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     * @param trtcCallBack
     */
    public TXTRTCDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, OnCall trtcCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack, trtcCallBack);
    }

    /**
     * TRTC行为下行消息处理
     * @param message 消息内容
     */
    @Override
    public void onActionMessageArrivedCallBack(MqttMessage message){
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
     * TRTC属性下行消息处理
     * @param message 消息内容
     */
    @Override
    public void onPropertyMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "property down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_PROPERTY_CONTROL)){ //
                JSONObject params = jsonObj.getJSONObject("params");
                if (mTrtcCallBack != null) {
                    if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS);
                        String userid = "";
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_USERID)) {
                            userid = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_USERID);
                        }
                        String userAgent = "";
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT)) {
                            userAgent = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT);
                        }
                        if (isBusy() && !getCurrentCallingUserid().equals(userid)) { //非当前设备的通话用户的请求忽略
                            if (callStatus != CallState.TYPE_IDLE_OR_REFUSE) {reportExtraInfoRejectUserId(userid);}
                            return;
                        }
                        if (!isBusy() || callStatus != CallState.TYPE_CALLING) {
                            mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, userAgent, CallingType.TYPE_VIDEO_CALL);
                        }
                        if (callStatus == CallState.TYPE_IDLE_OR_REFUSE) {
                            setBusy(false);
                            setCurrentCallingUserid("");
                        } else {
                            setBusy(true);
                            setCurrentCallingUserid(userid);
                        }
                    } else if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS);
                        String userid = "";
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_USERID)) {
                            userid = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_USERID);
                        }
                        String userAgent = "";
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT)) {
                            userAgent = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT);
                        }
                        if (isBusy() && !getCurrentCallingUserid().equals(userid)) { //非当前设备的通话用户的请求忽略
                            if (callStatus != CallState.TYPE_IDLE_OR_REFUSE) {reportExtraInfoRejectUserId(userid);}
                            return;
                        }
                        if (!isBusy() || callStatus != CallState.TYPE_CALLING) {
                            mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, userAgent, CallingType.TYPE_AUDIO_CALL);
                        }
                        if (callStatus == CallState.TYPE_IDLE_OR_REFUSE) {
                            setBusy(false);
                            setCurrentCallingUserid("");
                        } else {
                            setBusy(true);
                            setCurrentCallingUserid(userid);
                        }
                    } else if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_CALL_USERLIST)) {
                        //上报下接收到的userlist
                        Status status = sysPropertyReport(params, null);
                        if(Status.OK != status) {
                            TXLog.e(TAG, "property report failed!");
                        }
                    }
                }
            } else if (method.equals(METHOD_PROPERTY_GET_STATUS_REPLY)) {
                JSONObject data = jsonObj.getJSONObject("data").getJSONObject("reported");
                if (data.has(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS) && data.has(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS)) {
                    Integer videoCallStatus = data.getInt(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS);
                    Integer audioCallStatus = data.getInt(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS);
                    if (!isBusy() &&(videoCallStatus != CallState.TYPE_IDLE_OR_REFUSE || audioCallStatus != CallState.TYPE_IDLE_OR_REFUSE)) {
                        // 不在通话中，并且status状态不对  重置video和audio的status状态为0
                        reportResetCallStatusProperty();
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        if (topic.equals(mPropertyDownStreamTopic)) {
            onPropertyMessageArrivedCallBack(message);
        } else if (topic.equals(TOPIC_ACTION_DOWN_PREFIX + mProductId + "/" + mDeviceName)) {
            onActionMessageArrivedCallBack(message);
        }
    }
}
