package com.tencent.iot.device.video.advanced.recorder;

import android.content.Context;
import android.util.Log;

import com.tencent.iot.explorer.device.common.stateflow.CallState;
import com.tencent.iot.explorer.device.common.stateflow.TXCallTemplateClient;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class TXVideoTemplateClient extends TXCallTemplateClient {
    private String TAG = TXVideoTemplateClient.class.getSimpleName();
    //属性下行topic
    public String mPropertyDownStreamTopic;

    public TXVideoTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                 MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                 final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXVideoCallBack trtcCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new TXVideoDataTemplate(context, this,  productID,  deviceName, jsonFileName, null, trtcCallBack);
        this.mPropertyDownStreamTopic = mDataTemplate.mPropertyDownStreamTopic;
    }

    public TXVideoTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, String jsonFileName, DisconnectedBufferOptions bufferOpts,
                                 MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack, TXDataTemplateDownStreamCallBack downStreamCallBack, TXVideoCallBack trtcCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new TXVideoDataTemplate(context, this,  productID,  deviceName, jsonFileName, downStreamCallBack, trtcCallBack);
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return this.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected);
    }

    /**
     * 订阅数据模板相关主题
     * @param topicId 主题ID
     * @param qos QOS等级
     * @return 发送请求成功时返回Status.OK;
     */
    public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos) {
        return  this.mDataTemplate.subscribeTemplateTopic(topicId, qos);
    }

    public Status disConnect(Object userContext) {
        if (mDataTemplate != null) {
            mDataTemplate.destroy();
        }
        return disConnect(0, userContext);
    }

    public Status reportCallStatusProperty(Integer callStatus, Integer callType, String userId, String agent, JSONObject params) {
        if (params == null) {
            params = new JSONObject();
        }
        try {
            params.put(TXVideoDataTemplateConstants.PROPERTY_SYS_CALLER_ID, mDataTemplate.mProductId + "/" + mDataTemplate.mDeviceName);
            params.put(TXVideoDataTemplateConstants.PROPERTY_SYS_CALLED_ID, userId);
        } catch (JSONException e) {
            e.printStackTrace();
            return Status.PARAMETER_INVALID;
        }

        Status ret = mDataTemplate.reportCallStatusPropertyWithExtra(callStatus, callType, userId, agent, params);
        if (callStatus == CallState.TYPE_IDLE_OR_REFUSE && mDataTemplate instanceof TXVideoDataTemplate) {
            Log.e(TAG, "CallState: TYPE_IDLE_OR_REFUSE, clear aceeptCallInfo");
            ((TXVideoDataTemplate) mDataTemplate).aceeptCallInfo.clear(); // 主动挂断电话，清理已接听电话的标记
        }
        return ret;
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId) {
        return this.mDataTemplate.unSubscribeTemplateTopic(topicId);
    }


    public Status reportXp2pInfo(String p2pInfo) {
        Log.e(TAG, "reportXp2pInfo p2pInfo " + p2pInfo);
        if (mDataTemplate instanceof TXVideoDataTemplate) {
            return ((TXVideoDataTemplate)mDataTemplate).reportXp2pInfo(p2pInfo);
        }
        return Status.ERROR;
    }

    /**
     * 呼叫其他设备
     * @param calledProductID 其他设备产品ID
     * @param calledDeviceName 其他设备设备名称
     * @return 发送请求成功时返回Status.OK;
     */
    public Status callOtherDevice(String calledProductID, String calledDeviceName) {
        Log.e(TAG, "callOtherDevice calledProductID: " + calledProductID + ", calledDeviceName: " + calledDeviceName);
        if (mDataTemplate instanceof TXVideoDataTemplate) {
            return ((TXVideoDataTemplate)mDataTemplate).callOtherDevice(calledProductID, calledDeviceName);
        }
        return Status.ERROR;
    }

    /**
     * 消息到达回调函数
     * @param topic   消息主题
     * @param message 消息内容
     * @throws Exception 异常
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        mDataTemplate.onMessageArrived(topic, message);
    }

    /**
     * mqtt连接成功
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        super.connectComplete(reconnect, serverURI);
        Log.e(TAG, "----- connectComplete ");
    }
}
