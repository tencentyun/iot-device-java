package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * 该类负责序列化 DisconnectedBufferOptions 相关信息
 */
public class TXDisconnectedBufferOptions extends com.tencent.iot.hub.device.java.service.TXDisconnectedBufferOptions implements Parcelable {

    /**
     * 构造函数
     */
    public TXDisconnectedBufferOptions() {
        super();
    }

    protected TXDisconnectedBufferOptions(Parcel in) {
        setBufferSize(in.readInt());
        setBufferEnabled(in.readByte() != 0);
        setPersistBuffer(in.readByte() != 0);
        setDeleteOldestMessages(in.readByte() != 0);
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
        parcel.writeInt(getBufferSize());
        parcel.writeByte((byte) (isBufferEnabled() ? 1 : 0));
        parcel.writeByte((byte) (isPersistBuffer() ? 1 : 0));
        parcel.writeByte((byte) (isDeleteOldestMessages() ? 1 : 0));
    }

    public static final Creator<TXDisconnectedBufferOptions> CREATOR = new Creator<TXDisconnectedBufferOptions>() {
        @Override
        public TXDisconnectedBufferOptions createFromParcel(Parcel in) {
            return new TXDisconnectedBufferOptions(in);
        }

        @Override
        public TXDisconnectedBufferOptions[] newArray(int size) {
            return new TXDisconnectedBufferOptions[size];
        }
    };
}
