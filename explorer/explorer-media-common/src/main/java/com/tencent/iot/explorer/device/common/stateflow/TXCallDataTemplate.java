package com.tencent.iot.explorer.device.common.stateflow;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_GET_USER_AVATAR;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_STREAM_TOPIC;

import android.content.Context;
import android.text.TextUtils;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.common.stateflow.entity.TXCallDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TXCallDataTemplate extends TXDataTemplate {
    private String TAG = TXCallDataTemplate.class.getSimpleName();

    //Mqtt 连接
    private TXMqttConnection mConnection;
    public OnCall mTrtcCallBack = null;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private boolean mIsBusy = false; //trtc设备是否空闲
    private String mCurrentCallingUserid = ""; //当前呼叫trtc设备的userId
    private volatile boolean isCallOther = false;
    public String getCurrentCallingUserid() {
        return mCurrentCallingUserid;
    }

    public boolean isCallOther() {
        return isCallOther;
    }

    public void setCallOther(boolean callOther) {
        isCallOther = callOther;
    }

    public void setCurrentCallingUserid(String currentCallingUserid) {
        this.mCurrentCallingUserid = currentCallingUserid;
    }

    public boolean isContainCurrentCallingUserid(String id) {
        if (mCurrentCallingUserid.isEmpty()) {
            return false;
        }
        String[] currentCallingIds = mCurrentCallingUserid.split(";");
        for (String currentCallingId : currentCallingIds) {
            if (currentCallingId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXCallDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, OnCall trtcCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
        this.mTrtcCallBack = trtcCallBack;
    }

    private void checkStatusIsNotIdleReportResetStatus() {
        propertyGetStatus("report", false);
    }

    /**
     * TRTC属性下行消息处理
     * @param message 消息内容
     */
    public void onPropertyMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "property down stream message received " + message);
    }

    public Status reportExtraInfoRejectUserId(String rejectUserId) {
        JSONObject property = new JSONObject();

        if (TextUtils.isEmpty(rejectUserId)) {
            TXLog.e(TAG, "reportExtraInfoRejectUserId rejectUserId empty");
            return Status.PARAMETER_INVALID;
        }

        JSONObject extraInfo = new JSONObject();
        try {
            extraInfo.put(TXCallDataTemplateConstants.PROPERTY_REJECT_USERID, rejectUserId);

            property.put(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO, extraInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Status status = sysPropertyReport(property, null);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        return status;
    }

    public void setBusy(boolean mIsBusy) {
        this.mIsBusy = mIsBusy;
    }

    public boolean isBusy() {
        return mIsBusy;
    }

    /**
     * TRTC行为下行消息处理
     * @param message 消息内容
     */
    public void onActionMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "action down stream message received " + message);
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
            mIsBusy = callStatus != CallState.TYPE_IDLE_OR_REFUSE;
            mCurrentCallingUserid = mIsBusy ? userId : "";
            if (callType == CallingType.TYPE_VIDEO_CALL) { //video
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS,callStatus);
                if (!agent.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT,agent);
                }
                String extraInfo = ""; //预留 额外信息 _sys_extra_info
                if (!extraInfo.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO,extraInfo);
                }

            } else if (callType == CallingType.TYPE_AUDIO_CALL) { //audio
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS,callStatus);
                if (!agent.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT,agent);
                }
                String extraInfo = ""; //预留 额外信息 _sys_extra_info
                if (!extraInfo.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO,extraInfo);
                }
            } else {
                return Status.ERR_JSON_CONSTRUCT;
            }

            if (isCallOther) {  // 主叫以后的请求
                if (!userId.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLED_USERID, userId);
                }
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLER_USERID, String.format("%s/%s", mProductId, mDeviceName));
            } else { // 被叫以后的请求
                if (!userId.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLED_USERID, String.format("%s/%s", mProductId, mDeviceName));
                }
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLER_USERID, userId);
            }

        } catch (JSONException e) {
            TXLog.e(TAG, "Construct property json failed!");
            return Status.ERROR;
        }

        Status status = sysPropertyReport(property, null);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        if (callStatus == CallState.TYPE_IDLE_OR_REFUSE) { //上报呼叫状态0时，防止上报不成功，延迟1秒后查询设备状态，不为0则再次上报状态
            TimerTask task = new TimerTask(){
                public void run(){
                    checkStatusIsNotIdleReportResetStatus();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 1000);
        }
        return status;
    }

    /**
     * 上报实时音视频类设备呼叫属性
     * @param callStatus 呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param callType 邀请类型 1-语音通话，2-视频通话
     * @param userId 被呼叫用户id json字符串
     * @param agent 代理方
     * @return 结果
     */
    public Status reportCallStatusPropertyWithExtra(Integer callStatus, Integer callType, String userId, String agent, JSONObject params) {
        JSONObject property = new JSONObject();

        if (params != null && params.length() != 0) {
            //检查构造是否符合json文件中的定义
            if(Status.OK != checkPropertyJson(property)){
                TXLog.e(TAG, "propertyReport: invalid property json!");
                return Status.PARAMETER_INVALID;
            }
            property = params;
        }
        try {
            mIsBusy = callStatus != CallState.TYPE_IDLE_OR_REFUSE;
            mCurrentCallingUserid = mIsBusy ? userId : "";
            if (callType == CallingType.TYPE_VIDEO_CALL) { //video
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS,callStatus);
                if (!agent.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT,agent);
                }
                String extraInfo = ""; //预留 额外信息 _sys_extra_info
                if (!extraInfo.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO,extraInfo);
                }

            } else if (callType == CallingType.TYPE_AUDIO_CALL) { //audio
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS,callStatus);
                if (!agent.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AGENT,agent);
                }
                String extraInfo = ""; //预留 额外信息 _sys_extra_info
                if (!extraInfo.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_EXTRA_INFO,extraInfo);
                }

            } else {
                return Status.ERR_JSON_CONSTRUCT;
            }

            if (isCallOther) {  // 主叫以后的请求
                if (!userId.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLED_USERID, userId);
                }
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLER_USERID, String.format("%s/%s", mProductId, mDeviceName));
            } else { // 被叫以后的请求
                if (!userId.equals("")) {
                    property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLED_USERID, String.format("%s/%s", mProductId, mDeviceName));
                }
                property.put(TXCallDataTemplateConstants.PROPERTY_SYS_CALLER_USERID, userId);
            }
        } catch (JSONException e) {
            TXLog.e(TAG, "Construct property json failed!");
            return Status.ERROR;
        }

        Status status = sysPropertyReport(property, null);
        if(Status.OK != status) {
            TXLog.e(TAG, "property report failed!");
        }
        if (callStatus == CallState.TYPE_IDLE_OR_REFUSE) { //上报呼叫状态0时，防止上报不成功，延迟1秒后查询设备状态，不为0则再次上报状态
            TimerTask task = new TimerTask(){
                public void run(){
                    checkStatusIsNotIdleReportResetStatus();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 1000);
        }
        return status;
    }

    /**
     * 上报重置设备呼叫属性 为空闲
     * @return 结果
     */
    public Status reportResetCallStatusProperty() {
        JSONObject property = new JSONObject();
        try {
            property.put(TXCallDataTemplateConstants.PROPERTY_SYS_VIDEO_CALL_STATUS, CallState.TYPE_IDLE_OR_REFUSE);
            property.put(TXCallDataTemplateConstants.PROPERTY_SYS_AUDIO_CALL_STATUS, CallState.TYPE_IDLE_OR_REFUSE);
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
        return sysPropertyReport(property, metadata, 0);
    }

    /**
     * 系统属性上报， 不检查构造是否符合json文件中的定义
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    public Status sysPropertyReport(JSONObject property, JSONObject metadata, int qos) {
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
        message.setQos(qos);
        message.setPayload(objectString.getBytes());

        return publishTemplateMessage(clientToken,PROPERTY_UP_STREAM_TOPIC, message);
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
    }

    public CallExtraInfo getCallExtraInfo(JSONObject param) {
        String callerId = null;
        String calledId = null;
        try {
            if (param.has(TXCallDataTemplateConstants.PROPERTY_SYS_CALLER_USERID)) {
                callerId = param.getString(TXCallDataTemplateConstants.PROPERTY_SYS_CALLER_USERID);
            }
            if (param.has(TXCallDataTemplateConstants.PROPERTY_SYS_CALLED_USERID)) {
                calledId = param.getString(TXCallDataTemplateConstants.PROPERTY_SYS_CALLED_USERID);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CallExtraInfo ret = new CallExtraInfo(callerId, calledId);
        return ret;
    }

    /**
     * 获取用户头像
     * @param userIdsArray 要获取哪些头像的用户Id数组
     * @return 获取用户头像，发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status getUserAvatar(JSONArray userIdsArray) {

        //构造发布信息
        JSONObject object = new JSONObject();
        JSONObject params = new JSONObject();
        String clientToken = mProductId + mDeviceName + UUID.randomUUID().toString();
        try {
            object.put("method", METHOD_GET_USER_AVATAR);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
            params.put("user_id_list", userIdsArray);
            object.put("params", params);
        } catch (Exception e) {
            TXLog.e(TAG, "appBindToken: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(1); //qos 1
        message.setPayload(object.toString().getBytes());
        return mConnection.publish(TOPIC_SERVICE_UP_PREFIX + mProductId + "/" + mDeviceName, message, null);
    }
}
