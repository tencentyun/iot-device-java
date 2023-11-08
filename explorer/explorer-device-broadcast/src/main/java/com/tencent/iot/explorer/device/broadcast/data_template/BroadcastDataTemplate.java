package com.tencent.iot.explorer.device.broadcast.data_template;

import static com.tencent.iot.explorer.device.broadcast.data_template.model.BroadcastDataTemplateConstants.ACTION_SYS_TRTC_JOIN_BROADCAST;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_ACTION;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.broadcast.data_template.callback.BroadcastCallback;
import com.tencent.iot.explorer.device.broadcast.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class BroadcastDataTemplate extends TXDataTemplate {
    private String TAG = BroadcastDataTemplate.class.getSimpleName();
    public BroadcastCallback mBroadcastCallBack = null;

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     * @param broadcastCallBack
     */
    public BroadcastDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, BroadcastCallback broadcastCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mBroadcastCallBack = broadcastCallBack;
    }

    /**
     * TRTC行为下行消息处理
     *
     * @param message 消息内容
     */
    public void onActionMessageArrivedCallBack(MqttMessage message) {
        TXLog.d(TAG, "action down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_ACTION) && jsonObj.has("actionId")) { //
                String actionId = jsonObj.getString("actionId");
                if (actionId.equals(ACTION_SYS_TRTC_JOIN_BROADCAST)) { // 设备加房间 行为
                    JSONObject params = jsonObj.getJSONObject("params");
                    if (mBroadcastCallBack != null) {

                        Integer SdkAppId = params.getInt("SdkAppId");
                        String UserId = params.getString("UserId");
                        String UserSig = params.getString("UserSig");
                        String StrRoomId = params.getString("StrRoomId");
                        RoomKey room = new RoomKey();
                        room.setAppId(SdkAppId);
                        room.setUserId(UserId);
                        room.setUserSig(UserSig);
                        room.setRoomId(StrRoomId);
                        mBroadcastCallBack.joinBroadcast(room);
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid message: " + message);
        }
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        if (topic.equals(TOPIC_ACTION_DOWN_PREFIX + mProductId + "/" + mDeviceName)) {
            onActionMessageArrivedCallBack(message);
        }
    }
}
