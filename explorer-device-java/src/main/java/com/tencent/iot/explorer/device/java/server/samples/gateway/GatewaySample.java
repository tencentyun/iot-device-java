package com.tencent.iot.explorer.device.java.server.samples.gateway;



import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.gateway.TXGatewayClient;
import com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdev;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.*;


public class GatewaySample {
    private static final String TAG = "TXGatewaySample";
    private static final Logger LOG = LoggerFactory.getLogger(GatewaySample.class);

    // device info

    private String mDevPSK = "DEVICE-SECRET";
    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";

    //sub dev info
    private String mSubDev1ProductId = "DI0D24RRON";
    private String mSubDev2ProductId = "YOUR_SUB_DEV_PRODUCTID";

    private TXGatewayClient mConnection;
    private static AtomicInteger requestID = new AtomicInteger(0);

    public GatewaySample(String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, final String jsonFileName, String subDev1ProductId, String subDev2ProductId) {

        this.mDevPSK = devPSK;
        this.mSubDev1ProductId = subDev1ProductId;
        this.mSubDev2ProductId = subDev2ProductId;
        this.mDevCertName = devCertName;
        this.mDevKeyName = devKeyName;
        mConnection = new TXGatewayClient(brokerURL, productId, devName, devPSK,null,null,
                                new GatewaySampleMqttActionCallBack(), jsonFileName, new GatewaySampleDownStreamCallBack());
    }

    public void online() {
        //初始化连接
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0) {
            LOG.info(TAG, "Using PSK");
            options.setSocketFactory(AsymcSslUtils.getSocketFactory());
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
            LOG.error(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if (Status.OK != mConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)) {
            LOG.error(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if (Status.OK != mConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)) {
            LOG.error(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mConnection.disConnect(mqttRequest);
    }

    public Object addSubDev(String productId, String deviceName) {
        if(productId.equals(mSubDev1ProductId)) { //通过产品ID表示产品类型
            ProductLight subdev = new ProductLight(mConnection, productId, deviceName); //创建一个该类产品的设备
            mConnection.addSubdev(subdev.mGatewaySubdev); //添加子设备到网关
            return subdev;
        } else if (productId.equals(mSubDev2ProductId)) {
            ProductAirconditioner subdev = new ProductAirconditioner(mConnection,  productId, deviceName); //创建一个该类产品的设备
            mConnection.addSubdev(subdev.mGatewaySubdev); //添加子设备到网关
            return subdev;
        } else  {
            LOG.debug(TAG, "Unknown product! Product id is " + productId);
            return null;
        }
    }

    public void delSubDev(String productId, String deviceName) {
        mConnection.removeSubdev(productId, deviceName);
    }

    public void onlineSubDev(String productId, String deviceName) {
        mConnection.subdevOnline(productId, deviceName);
    }

    public void offlineSubDev(String productId, String deviceName) {
        mConnection.subdevOffline(productId, deviceName);
    }

    public void subscribeSubDevTopic(String productId, String deviceName, TXDataTemplateConstants.TemplateSubTopic topicId, final int qos){
        mConnection.subscribeSubDevTopic(productId,deviceName,topicId,qos);
    }

    public void subDevPropertyReport(String subProductID, String subDeviceName,JSONObject property, JSONObject metadata){
        mConnection.subDevPropertyReport(subProductID,subDeviceName,property,metadata);
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
            LOG.debug(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            LOG.debug(TAG, "data received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            LOG.debug(TAG, "msg received : " + msg);
            return null;
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            LOG.debug(TAG, "action [%s] received, input:" + params, actionId);
            return null;
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
                    LOG.error(TAG, "subscribeTopic: subscribe property down stream topic failed!");
                }
                if (Status.OK != mConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
                    LOG.error(TAG, "subscribeTopic: subscribe event down stream topic failed!");
                }
                if (Status.OK != mConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
                    LOG.error(TAG, "subscribeTopic: subscribe event down stream topic failed!");
                }
            } else {
                String userContextInfo = "";
                if (userContext instanceof TXMqttRequest) {
                    userContextInfo = userContext.toString();
                }
                String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                        status.name(), reconnect, userContextInfo, msg);
                LOG.debug(TAG,logInfo);
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            LOG.error(TAG,logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            LOG.debug(TAG,logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            LOG.debug(TAG,logInfo);
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
                LOG.error(TAG,logInfo);
            } else {
                String [] splitStr = topic.split("/");
                String productId = splitStr[3];
                String devName = splitStr[4].substring(0,splitStr[4].length() - 1);
                if(mConnection.mProductId.equals(productId) && mConnection.mDeviceName.equals(devName)) {
                    //订阅相关主题成功
                    LOG.debug(TAG, logInfo);
                } else {
                    TXGatewaySubdev subdev= mConnection.findSubdev(productId, devName);
                    if(null != subdev) {
                        subdev.mActionCallBack.onSubscribeCompleted(status, asyncActionToken, userContext, errMsg);
                    } else {
                        LOG.error(TAG, "Sub dev should be added! Product id:" + productId + ", Device Name:" + devName);
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
            LOG.debug(TAG,logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            //do nothing, message will be process in GatewaySampleDownStreamCallBack
        }
    }
}
