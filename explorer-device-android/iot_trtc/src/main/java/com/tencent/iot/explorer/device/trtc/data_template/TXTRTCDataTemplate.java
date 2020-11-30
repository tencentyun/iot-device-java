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
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_UP_PREFIX;

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
                    if (params.has("_sys_video_call_status")) {
                        Integer callStatus = params.getInt("_sys_video_call_status");
                        String userid = "";
                        if (params.has("_sys_userid")) {
                            params.getString("_sys_userid");
                        }
                        String username = "";
                        if (params.has("username")) {
                            params.getString("username");
                        }
                        mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, username, 1);
                    } else if (params.has("audio_call_status")) {
                        Integer callStatus = params.getInt("audio_call_status");
                        String userid = "";
                        if (params.has("_sys_userid")) {
                            params.getString("_sys_userid");
                        }
                        String username = "";
                        if (params.has("username")) {
                            params.getString("username");
                        }
                        mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, username, 2);
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
                if (actionId.equals("_sys_trtc_join_room")) { // 设备加房间 行为
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

    public Status reportCallStatusProperty(Integer callStatus, Integer callType) {
        JSONObject property = new JSONObject();
        try {
            if (callType == 1) { //video
                property.put("_sys_video_call_status",callStatus);
//                property.put("_sys_video_call_status",0);
                property.put("_sys_userid",String.format("%s/%s", mProductId, mDeviceName));
            } else if (callType == 2) { //audio
                property.put("_sys_audio_call_status",callStatus);
//                property.put("_sys_audio_call_status",0);
                property.put("_sys_userid",String.format("%s/%s", mProductId, mDeviceName));
            } else {
                return Status.ERR_JSON_CONSTRUCT;
            }
        } catch (JSONException e) {
            TXLog.e(TAG, "Construct property json failed!");
            return Status.ERROR;
        }

        Status status = propertyReport(property, null);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        return status;
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
