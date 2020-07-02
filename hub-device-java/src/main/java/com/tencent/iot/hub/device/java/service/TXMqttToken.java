package com.tencent.iot.hub.device.java.service;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.util.Arrays;

/**
 * 该类负责序列化MqttToken相关信息
 */

public class TXMqttToken {

    private String[] mTopics = null;
    private volatile boolean mCompleted = false;
    private int mMessageID = 0;
    private int[] mGrantedQos = null;
    private boolean mSessionPresent = false;

    public TXMqttToken() {
    }

    public TXMqttToken(String[] topics, boolean mCompleted, int mMessageID, int[] mGrantedQos, boolean mSessionPresent) {
        this.mTopics = topics;
        this.mCompleted = mCompleted;
        this.mMessageID = mMessageID;
        this.mGrantedQos = mGrantedQos;
        this.mSessionPresent = mSessionPresent;
    }

    public TXMqttToken(IMqttToken mqttToken) {
        if (null != mqttToken.getTopics() && mqttToken.getTopics().length > 0) {
            mTopics = new String[mqttToken.getTopics().length];
            System.arraycopy(mqttToken.getTopics(), 0, mTopics, 0, mqttToken.getTopics().length);
        }
        mCompleted = mqttToken.isComplete();
        mMessageID = mqttToken.getMessageId();
        if (null != mqttToken.getGrantedQos() && mqttToken.getGrantedQos().length > 0) {
            mGrantedQos = new int[mqttToken.getGrantedQos().length];
            System.arraycopy(mqttToken.getGrantedQos(), 0, mGrantedQos, 0, mqttToken.getGrantedQos().length);
        }
        mSessionPresent = mqttToken.getSessionPresent();
    }

    public MqttToken transToMqttToken() {
        MqttToken mqttToken = new MqttToken("TXMqttToken");
        mqttToken.internalTok.setTopics(mTopics);
        mqttToken.internalTok.setMessageID(mMessageID);
        return mqttToken;
    }

    @Override
    public String toString() {
        return "TXMqttToken{" +
                "mTopics=" + Arrays.toString(mTopics) +
                ", mCompleted=" + mCompleted +
                ", mMessageID=" + mMessageID +
                ", mGrantedQos=" + Arrays.toString(mGrantedQos) +
                ", mSessionPresent=" + mSessionPresent +
                '}';
    }
}
