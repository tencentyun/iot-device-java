package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * mqtt 远程客户端选项
 */
public class TXMqttClientOptions extends com.tencent.iot.hub.device.java.service.TXMqttClientOptions implements Parcelable {

    public static final Creator<TXMqttClientOptions> CREATOR = new Creator<TXMqttClientOptions>() {
        @Override
        public TXMqttClientOptions createFromParcel(Parcel in) {
            return new TXMqttClientOptions(in);
        }

        @Override
        public TXMqttClientOptions[] newArray(int size) {
            return new TXMqttClientOptions[size];
        }
    };

    /**
     * 构造函数
     */
    public TXMqttClientOptions() {
    }

    protected TXMqttClientOptions(Parcel in) {
        serverURI(in.readString());
        productId(in.readString());
        deviceName(in.readString());
        secretKey(in.readString());
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
     * @param parcel {@link Parcel}
     * @param i 标记
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getServerURI());
        parcel.writeString(getProductId());
        parcel.writeString(getDeviceName());
        parcel.writeString(getSecretKey());
    }
}
