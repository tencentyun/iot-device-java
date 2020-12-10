package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.util.Arrays;

/**
 * 该类负责序列化MqttToken相关信息
 */

public class TXMqttToken implements Parcelable {

    private String[] mTopics = null;
    private volatile boolean mCompleted = false;
    private int mMessageID = 0;
    private int[] mGrantedQos = null;
    private boolean mSessionPresent = false;

    protected TXMqttToken(Parcel in) {
        mTopics = in.createStringArray();
        mCompleted = in.readByte() != 0;
        mMessageID = in.readInt();
        mGrantedQos = in.createIntArray();
        mSessionPresent = in.readByte() != 0;
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(mTopics);
        parcel.writeByte((byte) (mCompleted ? 1 : 0));
        parcel.writeInt(mMessageID);
        parcel.writeIntArray(mGrantedQos);
        parcel.writeByte((byte) (mSessionPresent ? 1 : 0));
    }

    public void readFromParcel(Parcel in) {
        mTopics = in.createStringArray();
        mCompleted = in.readByte() != 0;
        mMessageID = in.readInt();
        mGrantedQos = in.createIntArray();
        mSessionPresent = in.readByte() != 0;
    }
}
