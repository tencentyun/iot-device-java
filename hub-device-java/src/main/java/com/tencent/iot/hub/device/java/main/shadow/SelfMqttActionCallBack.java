package com.tencent.iot.hub.device.java.main.shadow;

import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.main.mqtt.MQTTRequest;

public class SelfMqttActionCallBack extends TXMqttActionCallBack {
	private static final String TAG = "TXMQTT";
	private static final Logger LOG = LoggerFactory.getLogger(TXMqttConnection.class);
    @Override
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
        String userContextInfo = "";
        if (userContext instanceof MQTTRequest) {
            userContextInfo = userContext.toString();
        }
        String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                status.name(), reconnect, userContextInfo, msg);
        System.out.println("***************** OnConnectCompleted " + logInfo);
        LOG.info("{}", logInfo);
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
        LOG.info("{}", logInfo);
    }

    @Override
    public void onDisconnectCompleted(Status status, Object userContext, String msg) {
        String userContextInfo = "";
        if (userContext instanceof MQTTRequest) {
            userContextInfo = userContext.toString();
        }
        String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
        LOG.info("{}", logInfo);
    }

    @Override
    public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
        String userContextInfo = "";
        if (userContext instanceof MQTTRequest) {
            userContextInfo = userContext.toString();
        }
        String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
        LOG.info("{}", logInfo);
    }

    @Override
    public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
        String userContextInfo = "";
        System.out.println("***************** OnSub " + status);
        if (userContext instanceof MQTTRequest) {
            userContextInfo = userContext.toString();
        }
        String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
        if (Status.ERROR == status) {
            LOG.info("{}", logInfo);
        } else {
            LOG.info("{}", logInfo);
        }
    }

    @Override
    public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
        String userContextInfo = "";
        if (userContext instanceof MQTTRequest) {
            userContextInfo = userContext.toString();
        }
        String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
        LOG.info("{}", logInfo);
    }

    @Override
    public void onMessageReceived(final String topic, final MqttMessage message) {
        String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
        System.out.println("***************** OnMsgReceived " + logInfo);
        LOG.info("{}", logInfo);
    }

}
