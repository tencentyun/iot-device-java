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

    /**
     * 构造函数
     */
    public TXMqttMessage() {
        setPayload(new byte[]{});
    }

    /**
     * 构造函数
     *
     * @param mqttMessage {@link MqttMessage}
     */
    public TXMqttMessage(MqttMessage mqttMessage) {
        mPayload = mqttMessage.getPayload();
        mQos = mqttMessage.getQos();
        mRetained = mqttMessage.isRetained();
        mMessageId = mqttMessage.getId();
    }

    /**
     * 转换成 mqtt 的消息
     *
     * @return {@link MqttMessage}
     */
    public MqttMessage transToMqttMessage() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(mQos);
        mqttMessage.setPayload(mPayload);
        mqttMessage.setId(mMessageId);
        mqttMessage.setRetained(mRetained);
        return mqttMessage;
    }

    /**
     * 获取消息内容
     *
     * @return 消息内容
     */
    public byte[] getPayload() {
        return mPayload;
    }

    /**
     * 设置消息内容
     *
     * @param payload 消息内容
     * @return {@link TXMqttMessage}
     */
    public TXMqttMessage setPayload(byte[] payload) {
        this.mPayload = payload;
        return this;
    }

    /**
     * 获取消息 qos
     *
     * @return 消息 qos
     */
    public int getQos() {
        return mQos;
    }

    /**
     * 设置消息 qos
     *
     * @param qos 消息 qos
     * @return {@link TXMqttMessage}
     */
    public TXMqttMessage setQos(int qos) {
        this.mQos = qos;
        return this;
    }

    /**
     * 消息是否持久
     *
     * @return 是否持久
     */
    public boolean isRetained() {
        return mRetained;
    }

    /**
     * 设置消息是否持久
     *
     * @param retained 是否持久
     * @return {@link TXMqttMessage}
     */
    public TXMqttMessage setRetained(boolean retained) {
        this.mRetained = retained;
        return this;
    }

    /**
     * 获取消息 ID
     *
     * @return 消息 ID
     */
    public int getMessageId() {
        return mMessageId;
    }

    /**
     * 设置消息 ID
     *
     * @param messageId 消息 ID
     * @return {@link TXMqttMessage}
     */
    public TXMqttMessage setMessageId(int messageId) {
        this.mMessageId = messageId;
        return this;
    }

    /**
     * 转换成标准格式的字符串内容
     *
     * @return 标准字符串内容
     */
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
