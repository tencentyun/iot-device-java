package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.util.Arrays;

/**
 * 该类负责序列化 MqttToken 相关信息
 */
public class TXMqttToken extends com.tencent.iot.hub.device.java.service.TXMqttToken implements Parcelable {

    protected TXMqttToken(Parcel in) {
        setTopics(in.createStringArray());
        setCompleted(in.readByte() != 0);
        setMessageID(in.readInt());
        setGrantedQos(in.createIntArray());
        setSessionPresent(in.readByte() != 0);
    }

    /**
     * 构造函数
     * @param topics 主题
     * @param completed 完成标记
     * @param messageID 消息 ID
     * @param grantedQos 消息 qos
     * @param sessionPresent 体显 session 的标记
     */
    public TXMqttToken(String[] topics, boolean completed, int messageID, int[] grantedQos, boolean sessionPresent) {
        super(topics, completed, messageID, grantedQos, sessionPresent);
    }

    /**
     * 构造函数
     *
     * @param mqttToken {@link IMqttToken}
     */
    public TXMqttToken(IMqttToken mqttToken) {
        super(mqttToken);
    }

    public static final Creator<TXMqttToken> CREATOR = new Creator<TXMqttToken>() {
        @Override
        public TXMqttToken createFromParcel(Parcel in) {
            return new TXMqttToken(in);
        }

        @Override
        public TXMqttToken[] newArray(int size) {
            return new TXMqttToken[size];
        }
    };

    /**
     * 获取内容描述符
     *
     * @return 描述符
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 序列化
     *
     * @param parcel {@link Parcel}
     * @param i 标记
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(getTopics());
        parcel.writeByte((byte) (isCompleted() ? 1 : 0));
        parcel.writeInt(getMessageID());
        parcel.writeIntArray(getGrantedQos());
        parcel.writeByte((byte) (isSessionPresent() ? 1 : 0));
    }

    /**
     * 读取序列话的内容
     *
     * @param in {@link Parcel}
     */
    public void readFromParcel(Parcel in) {
        setTopics(in.createStringArray());
        setCompleted(in.readByte() != 0);
        setMessageID(in.readInt());
        setGrantedQos(in.createIntArray());
        setSessionPresent(in.readByte() != 0);
    }
}
