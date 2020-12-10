package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;

/**
 * 该类负责序列化MqttMessage相关信息
 */

public class TXMqttMessage implements Parcelable {
    private byte[] mPayload;
    private int mQos = 1;
    private boolean mRetained = false;
    private int mMessageId;

    protected TXMqttMessage(Parcel in) {
        mPayload = in.createByteArray();
        mQos = in.readInt();
        mRetained = in.readByte() != 0;
        mMessageId = in.readInt();
    }

    public TXMqttMessage() {
        setPayload(new byte[]{});
    }

    public TXMqttMessage(MqttMessage mqttMessage) {
        mPayload = mqttMessage.getPayload();
        mQos = mqttMessage.getQos();
        mRetained = mqttMessage.isRetained();
        mMessageId = mqttMessage.getId();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(mPayload);
        parcel.writeInt(mQos);
        parcel.writeByte((byte) (mRetained ? 1 : 0));
        parcel.writeInt(mMessageId);
    }

    public void readFromParcel(Parcel in) {
        mPayload = in.createByteArray();
        mQos = in.readInt();
        mRetained = in.readByte() != 0;
        mMessageId = in.readInt();
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
