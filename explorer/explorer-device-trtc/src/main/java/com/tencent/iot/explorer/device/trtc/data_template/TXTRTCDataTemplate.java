package com.tencent.iot.explorer.device.trtc.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.trtc.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCalling;
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
import static com.tencent.iot.explorer.device.trtc.data_template.model.TXTRTCDataTemplateConstants.ACTION_SYS_TRTC_JOIN_ROOM;
import static com.tencent.iot.explorer.device.trtc.data_template.model.TXTRTCDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS;
import static com.tencent.iot.explorer.device.trtc.data_template.model.TXTRTCDataTemplateConstants.PROPERTY_SYS_USERID;
import static com.tencent.iot.explorer.device.trtc.data_template.model.TXTRTCDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS;

public class TXTRTCDataTemplate extends TXDataTemplate {

    //Mqtt 连接
    private TXMqttConnection mConnection;

    private TXTRTCCallBack mTrtcCallBack = null;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private String mUserId = "";

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
        this.mUserId = productId + "/"  + deviceName;
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
                    if (params.has(PROPERTY_SYS_VIDEO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(PROPERTY_SYS_VIDEO_CALL_STATUS);
                        String userid = "";
                        if (params.has(PROPERTY_SYS_USERID)) {
                            userid = params.getString(PROPERTY_SYS_USERID);
                        }
                        mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, TRTCCalling.TYPE_VIDEO_CALL);
                    } else if (params.has(PROPERTY_SYS_AUDIO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(PROPERTY_SYS_AUDIO_CALL_STATUS);
                        String userid = "";
                        if (params.has(PROPERTY_SYS_USERID)) {
                            userid = params.getString(PROPERTY_SYS_USERID);
                        }
                        mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, TRTCCalling.TYPE_AUDIO_CALL);
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
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
                if (actionId.equals(ACTION_SYS_TRTC_JOIN_ROOM)) { // 设备加房间 行为
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
                        mTrtcCallBack.trtcJoinRoomCallBack(room);
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
     * @return 结果
     */
    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId) {
        JSONObject property = new JSONObject();
        try {
            if (callType == TRTCCalling.TYPE_VIDEO_CALL) { //video
                property.put(PROPERTY_SYS_VIDEO_CALL_STATUS,callStatus);
                if (!userId.equals("")) {
                    property.put("_sys_userid",userId);
                }
            } else if (callType == TRTCCalling.TYPE_AUDIO_CALL) { //audio
                property.put(PROPERTY_SYS_AUDIO_CALL_STATUS,callStatus);
                if (!userId.equals("")) {
                    property.put("_sys_userid",userId);
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
    public Status sysPropertyReport(JSONObject property, JSONObject metadata) {
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
