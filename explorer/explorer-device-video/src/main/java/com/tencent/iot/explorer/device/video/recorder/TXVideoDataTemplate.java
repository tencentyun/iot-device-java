package com.tencent.iot.explorer.device.video.recorder;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.common.stateflow.CallState;
import com.tencent.iot.explorer.device.common.stateflow.TXCallDataTemplate;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.common.stateflow.entity.TXCallDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class TXVideoDataTemplate extends TXCallDataTemplate {
    private String TAG = TXVideoDataTemplate.class.getSimpleName();

    // Mqtt 连接
    private TXMqttConnection mConnection;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private TXVideoCallBack videoCallBack;
    protected Set<String> aceeptCallInfo = new CopyOnWriteArraySet<>();

    /**
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param connection 连接实例
     * @param productId 产品名
     * @param deviceName 设备名，唯一
     * @param jsonFileName 数据模板描述文件
     */
    public TXVideoDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName,
                               TXDataTemplateDownStreamCallBack downStreamCallBack, TXVideoCallBack videoCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack, videoCallBack);
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

        Status status = sysPropertyReport(property, null, 1);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        return status;
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        if (topic.equals(mPropertyDownStreamTopic)) {
            onPropertyMessageArrivedCallBack(message);
        } else if (topic.equals(TOPIC_ACTION_DOWN_PREFIX + mProductId + "/" + mDeviceName)) {

        }
    }

    @Override
    public void onPropertyMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "property down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_PROPERTY_CONTROL)){ //
                JSONObject params = jsonObj.getJSONObject("params");
                if (videoCallBack != null) {
                    if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS);
                        String userid = "";
                        if (!isBusy() && callStatus == CallState.TYPE_CALLING) {
                            setCallOther(false);
                        }
                        CallExtraInfo tmp = getCallExtraInfo(params);
                        if (tmp != null && !TextUtils.isEmpty(tmp.getCallerId())) { // 一呼全用户，called 是空字符串
                            if (isCallOther()) {
                                userid = tmp.getCalledId();
                            } else {
                                userid = tmp.getCallerId();
                            }
                        }

                        String userAgent = "";
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT)) {
                            userAgent = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT);
                        }
                        if (isBusy() && !getCurrentCallingUserid().equals(userid)) { //非当前设备的通话用户的请求忽略
                            if (callStatus != CallState.TYPE_IDLE_OR_REFUSE) {reportExtraInfoRejectUserId(userid);}
                            return;
                        }
                        convertData2Callback(callStatus, userid, userAgent, CallingType.TYPE_VIDEO_CALL, params);
                    } else if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS)) {
                        Integer callStatus = params.getInt(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS);

                        String userid = "";
                        if (!isBusy() && callStatus == CallState.TYPE_CALLING) {
                            setCallOther(false);
                        }
                        CallExtraInfo tmp = getCallExtraInfo(params);
                        if (tmp != null && !TextUtils.isEmpty(tmp.getCallerId())) { // 一呼全用户，called 是空字符串
                            if (isCallOther()) {
                                userid = tmp.getCalledId();
                            } else {
                                userid = tmp.getCallerId();
                            }
                        }

                        String userAgent = "";
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT)) {
                            userAgent = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT);
                        }
                        if (isBusy() && !getCurrentCallingUserid().equals(userid)) { //非当前设备的通话用户的请求忽略
                            if (callStatus != CallState.TYPE_IDLE_OR_REFUSE) {reportExtraInfoRejectUserId(userid);}
                            return;
                        }
                        convertData2Callback(callStatus, userid, userAgent, CallingType.TYPE_AUDIO_CALL, params);
                    } else if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_CALL_USERLIST)) {
                        //上报下接收到的 userlist
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

    private void convertData2Callback(int callStatus, String userid, String userAgent, int callType, JSONObject params) {
        Log.e(TAG, "convertData2Callback callStatus " + callStatus + ", userid " + userid + ", userAgent " + userAgent + ", callType " + callType);
        if (isBusy() && callStatus == CallState.TYPE_CALLING && getCurrentCallingUserid().equals(userid)) {  // 对方接受了通话请求
            Log.e(TAG, "accpet call userid " + userid);
            String userTag = "" + isBusy() + callStatus + userid + userAgent;
            Log.e(TAG, "accpet call userTag " + userTag);
            if (aceeptCallInfo.contains(userTag)) return; // 完全相同的通话接听事件，没有挂断之前只触发一次
            Log.e(TAG, "accpet call userTag convertData2Callback");
            videoCallBack.onUserAccept(userid, userAgent, callType, getCallExtraInfo(params));
            setBusy(true);
            setCurrentCallingUserid(userid);  // 接受当前通话才会替换新的 userId
            aceeptCallInfo.add(userTag);

        } else if (!isBusy() && callStatus == CallState.TYPE_CALLING && !getCurrentCallingUserid().equals(userid)) { // 来了一通新的通话求
            Log.e(TAG, "a new call userid " + userid);
            videoCallBack.onNewCall(userid, userAgent, callType, getCallExtraInfo(params));
            setCurrentCallingUserid(userid);
            setBusy(true); // 新的通话不替换 userId

        } else if (isBusy() && callStatus == CallState.TYPE_IDLE_OR_REFUSE && getCurrentCallingUserid().equals(userid)) { // 当前通话结束
            Log.e(TAG, "call over userid " + userid);
            videoCallBack.onCallOver(userid, userAgent, callType, getCallExtraInfo(params));
            aceeptCallInfo.clear(); // 清理对应的通话事件

        } else if (isBusy() && callStatus == CallState.TYPE_CALLING && !getCurrentCallingUserid().equals(userid)) {
            Log.e(TAG, "auto reject call " + userid);
            reportExtraInfoRejectUserId(userid);
            videoCallBack.onAutoRejectCall(userid, userAgent, callType, getCallExtraInfo(params));
        }

        if (callStatus == CallState.TYPE_IDLE_OR_REFUSE) {
            setBusy(false);
            setCurrentCallingUserid("");
        }
    }

    @Override
    public Status reportCallStatusPropertyWithExtra(Integer callStatus, Integer callType, String userId, String agent, JSONObject params) {
        if (callStatus == CallState.TYPE_CALLING) setCallOther(true);  // 主动呼叫对方，设置为主叫
        return super.reportCallStatusPropertyWithExtra(callStatus, callType, userId, agent, params);
    }
}
