package com.tencent.iot.explorer.device.trtc.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;

public class TXTRTCDataTemplate extends TXDataTemplate {

    private TXTRTCCallBack mTrtcCallBack = null;
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
        mTrtcCallBack = trtcCallBack;
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
                if (params.has("userid") && mTrtcCallBack != null) {
                    if (params.has("video_call_status")) {
                        Integer callStatus = params.getInt("video_call_status");
                        String userid = jsonObj.getString("userid");
                        String username = jsonObj.getString("username");
                        mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, username, 1);
                    } else if (params.has("audio_call_status")) {
                        Integer callStatus = params.getInt("audio_call_status");
                        String userid = jsonObj.getString("userid");
                        String username = jsonObj.getString("username");
                        mTrtcCallBack.onGetCallStatusCallBack(callStatus, userid, username, 2);
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onPropertyMessageArrivedCallBack: invalid message: " + message);
        }
    }

    public Status reportCallStatusProperty(Integer callStatus, Integer callType) {
        JSONObject property = new JSONObject();
        try {
            if (callType == 1) { //video
                property.put("video_call_status",callStatus);
            } else if (callType == 2) { //audio
                property.put("audio_call_status",callStatus);
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
        }
    }
}
