package com.tencent.iot.device.video.advanced.recorder;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_ACTION;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.common.stateflow.CallState;
import com.tencent.iot.explorer.device.common.stateflow.TXCallDataTemplate;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.common.stateflow.entity.TXCallDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class TXVideoDataTemplate extends TXCallDataTemplate {
    private String TAG = TXVideoDataTemplate.class.getSimpleName();

    // Mqtt 连接
    private TXMqttConnection mConnection;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private TXVideoCallBack videoCallBack;
    protected Set<String> aceeptCallInfo = new CopyOnWriteArraySet<>();

    String TOPIC_VIDEO_UP_PREFIX = "$video/up/service/";
    //上下行消息主题
    private String mVideoServiceUpStreamTopic;

    private volatile String callOtherDeviceClientToken = "";
    private TimerTask callTask = null;

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
        mVideoServiceUpStreamTopic = TOPIC_VIDEO_UP_PREFIX + productId + "/" + deviceName;
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

    /**
     * 呼叫其他设备
     * @param calledProductID 其他设备产品ID
     * @param calledDeviceName 其他设备设备名称
     * @return 发送请求成功时返回Status.OK;
     */
    public Status callOtherDevice(String calledProductID, String calledDeviceName) {
        //构造发布信息
        JSONObject object = new JSONObject();
        JSONObject params = new JSONObject();
        String clientToken =  mProductId + mDeviceName + String.valueOf(requestID.getAndIncrement());
        callOtherDeviceClientToken = clientToken;
        try {
            object.put("method", "callTRTCDevice");
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());

            params.put("productID", calledProductID);
            params.put("deviceName", calledDeviceName);

            object.put("params", params);
        } catch (Exception e) {
            TXLog.e(TAG, "callOtherDevice: failed!" );
            return Status.ERR_JSON_CONSTRUCT;
        }
        String objectString = object.toString();
        objectString = objectString.replace("\\/", "/");

        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(objectString.getBytes());
        checkoutIsCallOtherDeviceTimeout();

        return mConnection.publish(mVideoServiceUpStreamTopic, message, null);
    }

    private void checkoutIsCallOtherDeviceTimeout() {
        if (callTask == null) {
            callTask = new TimerTask(){
                public void run(){
                    if (!callOtherDeviceClientToken.isEmpty() && mTrtcCallBack != null) {
                        videoCallBack.callOtherDeviceFailed(2, "timeout");
                        callOtherDeviceClientToken = "";
                    }
                    callTask = null;
                }
            };
            Timer timer = new Timer();
            timer.schedule(callTask, 3000);
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

    /**
     * TRTC行为下行消息处理
     *
     * @param message 消息内容
     */
    @Override
    public void onActionMessageArrivedCallBack(MqttMessage message) {
        TXLog.d(TAG, "action down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (method.equals(METHOD_ACTION) && jsonObj.has("actionId")) { //
                String actionId = jsonObj.getString("actionId");
                if (actionId.equals(TXVideoDataTemplateConstants.ACTION_TRTC_JOIN_ROOM)) { // 设备加房间 行为
                    JSONObject params = jsonObj.getJSONObject("params");
                    String clientToken = jsonObj.getString("clientToken");
                    if (mTrtcCallBack != null) {
                        if (params.has("SdkAppId")) {
                            Integer SdkAppId = params.getInt("SdkAppId");
                            String UserId = params.getString("UserId");
                            String UserSig = params.getString("UserSig");
                            String StrRoomId = params.getString("StrRoomId");
                            RoomKey room = new RoomKey();
                            room.setAppId(SdkAppId);
                            room.setUserId(UserId);
                            room.setUserSig(UserSig);
                            room.setRoomId(StrRoomId);
                            if (!callOtherDeviceClientToken.isEmpty() && clientToken.endsWith(callOtherDeviceClientToken)) {
                                videoCallBack.callOtherDeviceSuccess(room);
                                callOtherDeviceClientToken = "";
                            } else {
                                videoCallBack.receiveRtcJoinRoomAction(room);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            TXLog.e(TAG, "onActionMessageArrivedCallBack: invalid message: " + message);
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
                        if (params.has(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO)) {
                            String extraInfo = params.getString(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO);
                            String clientToken = jsonObj.getString("clientToken");
                            if (!callOtherDeviceClientToken.isEmpty() && clientToken.endsWith(callOtherDeviceClientToken) && !extraInfo.isEmpty()) {
                                videoCallBack.callOtherDeviceFailed(1, extraInfo);
                                callOtherDeviceClientToken = "";
                            }
                        }
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
