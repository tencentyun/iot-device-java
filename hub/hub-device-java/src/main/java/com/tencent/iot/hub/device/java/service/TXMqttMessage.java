package com.tencent.iot.hub.device.java.service;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;

/**
 * 该类负责序列化MqttMessage相关信息
 */

public class TXMqttMessage {
    private byte[] mPayload;
    private int mQos = 1;
    private boolean mRetained = false;
    private int mMessageId;

    public TXMqttMessage() {
        setPayload(new byte[]{});
    }

    public TXMqttMessage(MqttMessage mqttMessage) {
        mPayload = mqttMessage.getPayload();
        mQos = mqttMessage.getQos();
        mRetained = mqttMessage.isRetained();
        mMessageId = mqttMessage.getId();
    }

    public MqttMessage transToMqttMessage() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(mQos);
        mqttMessage.setPayload(mPayload);
        mqttMessage.setId(mMessageId);
        mqttMessage.setRetained(mRetained);
        return mqttMessage;
    }

    public byte[] getPayload() {
        return mPayload;
    }

    public TXMqttMessage setPayload(byte[] payload) {
        this.mPayload = payload;
        return this;
    }

    public int getQos() {
        return mQos;
    }

    public TXMqttMessage setQos(int qos) {
        this.mQos = qos;
        return this;
    }

    public boolean isRetained() {
        return mRetained;
    }

    public TXMqttMessage setRetained(boolean retained) {
        this.mRetained = retained;
        return this;
    }

    public int getMessageId() {
        return mMessageId;
    }

    public TXMqttMessage setMessageId(int messageId) {
        this.mMessageId = messageId;
        return this;
    }

    @Override
    public String toString() {
        return "TXMqttMessage{" +
                ", mPayload=" + Arrays.toString(mPayload) +
                ", mQos=" + mQos +
                ", mRetained=" + mRetained +
                ", mMessageId=" + mMessageId +
                '}';
    }
}
