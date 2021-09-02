package com.tencent.iot.explorer.device.android.app.gateway;

import android.content.Context;
import android.util.Log;

import com.tencent.iot.explorer.device.android.gateway.TXGatewayClient;
import com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.SERVICE_DOWN_STREAM_TOPIC;

public class GatewaySample {
    private static final String TAG = "TXGatewaySample";

    // device info
    private Context mContext;
    private String mDevPSK = "DEVICE-SECRET";
    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";

    //sub dev info
    private String mSubDev1ProductId = "YOUR_SUB_DEV_PRODUCTID";
    private String mSubDev2ProductId = "YOUR_SUB_DEV_PRODUCTID";

    private TXGatewayClient mConnection;
    private static AtomicInteger requestID = new AtomicInteger(0);
    private FirstConnectCompletedCallback mFirstConnectCompletedCallback;

    public GatewaySample(Context context, String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, final String jsonFileName, String subDev1ProductId, String subDev2ProductId) {
        this.mContext = context;
        this.mDevPSK = devPSK;
        this.mSubDev1ProductId = subDev1ProductId;
        this.mSubDev2ProductId = subDev2ProductId;
        this.mDevCertName = devCertName;
        this.mDevKeyName = devKeyName;
        mConnection = new TXGatewayClient( context, brokerURL, productId, devName, devPSK,null,null,
                                new GatewaySampleMqttActionCallBack(), jsonFileName, new GatewaySampleDownStreamCallBack());
    }

    public void setFirstConnectCompletedCallback(FirstConnectCompletedCallback firstConnectCompletedCallback) {
        this.mFirstConnectCompletedCallback = firstConnectCompletedCallback;
    }

    public void online() {
        //初始化连接
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0) {
            TXLog.i(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-android sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mConnection.setBufferOpts(bufferOptions);
    }

    public void offline() {
        if (Status.OK != mConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if (Status.OK != mConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if (Status.OK != mConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if (Status.OK != mConnection.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
        }
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mConnection.disConnect(mqttRequest);
    }

    public Object addSubDev(String productId, String deviceName) {
        if (mConnection.findSubdev(productId, deviceName) != null) {
            return null;
        }

        if(productId.equals(mSubDev1ProductId)) { //通过产品ID表示产品类型
            ProductLight subdev = new ProductLight(mConnection, mContext, productId, deviceName); //创建一个该类产品的设备
            mConnection.addSubdev(subdev.mGatewaySubdev); //添加子设备到网关
            return subdev;
        } else if (productId.equals(mSubDev2ProductId)) {
            ProductAirconditioner subdev = new ProductAirconditioner(mConnection, mContext, productId, deviceName); //创建一个该类产品的设备
            mConnection.addSubdev(subdev.mGatewaySubdev); //添加子设备到网关
            return subdev;
        } else  {
            Log.d(TAG, "Unknown product! Product id is " + productId);
            return null;
        }
    }

    public void delSubDev(String productId, String deviceName) {
        TXGatewaySubdev dev = mConnection.findSubdev(productId, deviceName);
        if (dev != null) {
            dev.destroy();
        }
        mConnection.removeSubdev(productId, deviceName);
    }

    public void onlineSubDev(String productId, String deviceName) {
        mConnection.subdevOnline(productId, deviceName);
    }

    public void offlineSubDev(String productId, String deviceName) {
        mConnection.subdevOffline(productId, deviceName);
    }

    public Status getSubDevStatus(String productId, String deviceName) {
        TXGatewaySubdev subdev = mConnection.findSubdev(productId, deviceName);
        if(null != subdev) {
            return subdev.getSubdevStatus();
        } else {
            return Status.SUBDEV_STAT_NOT_EXIST;
        }
    }

    /**
     * 实现下行消息处理的回调接口
     */
    private class GatewaySampleDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //Just print
            Log.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            Log.d(TAG, "data received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            Log.d(TAG, "msg received : " + msg);
            return null;
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            TXLog.d(TAG, "action [%s] received, input:" + params, actionId);
            return null;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            //用户删除设备的通知消息
            Log.d(TAG, "unbind device received : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            //用户绑定设备的通知消息
            Log.d(TAG, "bind device received : " + msg);
        }
    }

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class GatewaySampleMqttActionCallBack extends TXMqttActionCallBack {
        /**初次连接成功则订阅相关主题*/
        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            if(Status.OK == status && !reconnect) { //初次连接订阅主题,重连后会自动订阅主题
                if (Status.OK != mConnection.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)) {
                    TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
                }
                if (Status.OK != mConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
                    TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
                }
                if (Status.OK != mConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
                    TXLog.e(TAG, "subscribeTopic: subscribe action down stream topic failed!");
                }
                if (Status.OK != mConnection.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)){
                    TXLog.e(TAG, "subscribeTopic: subscribe service down stream topic failed!");
                }
                if (mFirstConnectCompletedCallback != null) {
                    mFirstConnectCompletedCallback.firstConnectCompleted(mConnection.generateDeviceWechatScanQRCodeContent());
                }
            } else {
                String userContextInfo = "";
                if (userContext instanceof TXMqttRequest) {
                    userContextInfo = userContext.toString();
                }
                String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                        status.name(), reconnect, userContextInfo, msg);
                TXLog.d(TAG,logInfo);
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            TXLog.e(TAG,logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            TXLog.d(TAG,logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            TXLog.d(TAG,logInfo);
        }

        /**订阅子设备主题相关主题成功，则调用子设备onSubscribeCompleted*/
        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String topic = Arrays.toString(asyncActionToken.getTopics());
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), topic, userContextInfo, errMsg);

            if (Status.ERROR == status) {
                TXLog.e(TAG,logInfo);
            } else {
                String [] splitStr = topic.split("/");
                String productId = splitStr[3];
                String devName = splitStr[4].substring(0,splitStr[4].length() - 1);
                if(mConnection.mProductId.equals(productId) && mConnection.mDeviceName.equals(devName)) {
                    //订阅相关主题成功
                    TXLog.d(TAG, logInfo);
                } else {
                    TXGatewaySubdev subdev= mConnection.findSubdev(productId, devName);
                    if(null != subdev) {
                        subdev.mActionCallBack.onSubscribeCompleted(status, asyncActionToken, userContext, errMsg);
                    } else {
                        Log.e(TAG, "Sub dev should be added! Product id:" + productId + ", Device Name:" + devName);
                    }
                }
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            TXLog.d(TAG,logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            //do nothing, message will be process in GatewaySampleDownStreamCallBack
        }
    }

    public interface FirstConnectCompletedCallback {
        void firstConnectCompleted(String deviceQRcontent);
    }
}
