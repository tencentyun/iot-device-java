package com.tencent.iot.hub.device.java.service;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.util.Arrays;

/**
 * 该类负责序列化 MqttToken 相关信息
 */
public class TXMqttToken {

    private String[] mTopics = null;
    private volatile boolean mCompleted = false;
    private int mMessageID = 0;
    private int[] mGrantedQos = null;
    private boolean mSessionPresent = false;

    /**
     * 构造函数
     */
    public TXMqttToken() {
    }

    /**
     * 构造函数
     *
     * @param topics 主题
     * @param completed 完成标记
     * @param messageID 消息 ID
     * @param grantedQos 消息 qos
     * @param sessionPresent 体显 session
     */
    public TXMqttToken(String[] topics, boolean completed, int messageID, int[] grantedQos, boolean sessionPresent) {
        this.mTopics = topics;
        this.mCompleted = completed;
        this.mMessageID = messageID;
        this.mGrantedQos = grantedQos;
        this.mSessionPresent = sessionPresent;
    }

    /**
     * 构造函数
     *
     * @param mqttToken {@link IMqttToken}
     */
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

    /**
     * 转换 MqttToken {@link MqttToken}
     *
     * @return {@link MqttToken}
     */
    public MqttToken transToMqttToken() {
        MqttToken mqttToken = new MqttToken("TXMqttToken");
        mqttToken.internalTok.setTopics(mTopics);
        mqttToken.internalTok.setMessageID(mMessageID);
        return mqttToken;
    }

    /**
     * 转换成标准格式的字符串内容
     *
     * @return 标准字符串内容
     */
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

    /**
     * 获取主题
     *
     * @return 主题数组
     */
    public String[] getTopics() {
        return mTopics;
    }

    /**
     * 设置主题
     *
     * @param topics 主题数组
     */
    public void setTopics(String[] topics) {
        this.mTopics = topics;
    }

    /**
     * 是否完成标记
     *
     * @return 完成标记
     */
    public boolean isCompleted() {
        return mCompleted;
    }

    /**
     * 设置完成标记
     *
     * @param completed 完成标记
     */
    public void setCompleted(boolean completed) {
        this.mCompleted = completed;
    }

    /**
     * 获取消息 ID
     *
     * @return 消息 ID
     */
    public int getMessageID() {
        return mMessageID;
    }

    /**
     * 设置消息 ID
     *
     * @param messageID 消息 ID
     */
    public void setMessageID(int messageID) {
        this.mMessageID = messageID;
    }

    /**
     * 获取消息 qos
     *
     * @return 消息 qos
     */
    public int[] getGrantedQos() {
        return mGrantedQos;
    }

    /**
     * 设置消息 qos
     * @param grantedQos 消息 qos
     */
    public void setGrantedQos(int[] grantedQos) {
        this.mGrantedQos = grantedQos;
    }

    /**
     * 是否体现 session
     *
     * @return 体显 session 的标记
     */
    public boolean isSessionPresent() {
        return mSessionPresent;
    }

    /**
     * 设置是否体现 session
     *
     * @param sessionPresent 体显 session 的标记
     */
    public void setSessionPresent(boolean sessionPresent) {
        this.mSessionPresent = sessionPresent;
    }
}
