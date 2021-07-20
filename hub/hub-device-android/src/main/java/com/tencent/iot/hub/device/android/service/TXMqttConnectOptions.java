package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * 该类负责序列化 MqttConnectOptions 相关信息
 */
public class TXMqttConnectOptions extends com.tencent.iot.hub.device.java.service.TXMqttConnectOptions implements Parcelable {

    public static final Creator<TXMqttConnectOptions> CREATOR = new Creator<TXMqttConnectOptions>() {
        @Override
        public TXMqttConnectOptions createFromParcel(Parcel in) {
            return new TXMqttConnectOptions(in);
        }

        @Override
        public TXMqttConnectOptions[] newArray(int size) {
            return new TXMqttConnectOptions[size];
        }
    };

    /**
     * 构造函数
     */
    public TXMqttConnectOptions() {
    }

    protected TXMqttConnectOptions(Parcel in) {
        setConnectionTimeout(in.readInt());
        setKeepAliveInterval(in.readInt());
        setDeviceCertName(in.readString());
        setDeviceKeyName(in.readString());
        setSecretKey(in.readString());
        boolean[] booleanArray = new boolean[4];
        in.readBooleanArray(booleanArray);
        setAutomaticReconnect(booleanArray[0]);
        setUseShadow(booleanArray[1]);
        setCleanSession(booleanArray[2]);
        setAsymcEncryption(booleanArray[3]);
    }

    /**
     * 内容描述符
     * @return 描述符
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 序列化
     * @param out {@link Parcel}
     * @param flag 标记
     */
    @Override
    public void writeToParcel(Parcel out, int flag) {
        out.writeInt(getConnectionTimeout());
        out.writeInt(getKeepAliveInterval());
        out.writeString(getDeviceCertName());
        out.writeString(getDeviceKeyName());
        out.writeString(getSecretKey());
        boolean[] booleanArray = new boolean[]{isAutomaticReconnect(), isUseShadow(), isCleanSession(), isAsymcEncryption()};
        out.writeBooleanArray(booleanArray);
    }
}
