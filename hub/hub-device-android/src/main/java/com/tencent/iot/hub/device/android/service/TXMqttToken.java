package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.util.Arrays;

/**
 * 该类负责序列化MqttToken相关信息
 */

public class TXMqttToken extends com.tencent.iot.hub.device.java.service.TXMqttToken implements Parcelable {

    protected TXMqttToken(Parcel in) {
        setTopics(in.createStringArray());
        setCompleted(in.readByte() != 0);
        setMessageID(in.readInt());
        setGrantedQos(in.createIntArray());
        setSessionPresent(in.readByte() != 0);
    }

    public TXMqttToken() {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(getTopics());
        parcel.writeByte((byte) (isCompleted() ? 1 : 0));
        parcel.writeInt(getMessageID());
        parcel.writeIntArray(getGrantedQos());
        parcel.writeByte((byte) (isSessionPresent() ? 1 : 0));
    }

    public void readFromParcel(Parcel in) {
        setTopics(in.createStringArray());
        setCompleted(in.readByte() != 0);
        setMessageID(in.readInt());
        setGrantedQos(in.createIntArray());
        setSessionPresent(in.readByte() != 0);
    }
}
