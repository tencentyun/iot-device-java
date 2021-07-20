package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;

/**
 * 该类负责序列化 MqttMessage 相关信息
 */
public class TXMqttMessage extends com.tencent.iot.hub.device.java.service.TXMqttMessage implements Parcelable {

    /**
     * 构造函数
     */
    public TXMqttMessage() {
        super();
    }

    /**
     * 构造函数
     *
     * @param mqttMessage {@link MqttMessage}
     */
    public TXMqttMessage(MqttMessage mqttMessage) {
        super(mqttMessage);
    }

    protected TXMqttMessage(Parcel in) {
        setPayload(in.createByteArray());
        setQos(in.readInt());
        setRetained(in.readByte() != 0);
        setMessageId(in.readInt());
    }

    public static final Creator<TXMqttMessage> CREATOR = new Creator<TXMqttMessage>() {
        @Override
        public TXMqttMessage createFromParcel(Parcel in) {
            return new TXMqttMessage(in);
        }

        @Override
        public TXMqttMessage[] newArray(int size) {
            return new TXMqttMessage[size];
        }
    };

    /**
     * 内容描述符
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
        parcel.writeByteArray(getPayload());
        parcel.writeInt(getQos());
        parcel.writeByte((byte) (isRetained() ? 1 : 0));
        parcel.writeInt(getMessageId());
    }

    /**
     * 读取序列化的内容
     *
     * @param in {@link Parcel}
     */
    public void readFromParcel(Parcel in) {
        setPayload(in.createByteArray());
        setQos(in.readInt());
        setRetained(in.readByte() != 0);
        setMessageId(in.readInt());
    }
}
