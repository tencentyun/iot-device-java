package com.tencent.iot.hub.device.android.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;

/**
 * 该类负责序列化DisconnectedBufferOptions相关信息
 */

public class TXDisconnectedBufferOptions extends DisconnectedBufferOptions implements Parcelable {
    private int bufferSize = DISCONNECTED_BUFFER_SIZE_DEFAULT;
    private boolean bufferEnabled = DISCONNECTED_BUFFER_ENABLED_DEFAULT;
    private boolean persistBuffer = PERSIST_DISCONNECTED_BUFFER_DEFAULT;
    private boolean deleteOldestMessages = DELETE_OLDEST_MESSAGES_DEFAULT;

    public TXDisconnectedBufferOptions() {
        super();
    }

    protected TXDisconnectedBufferOptions(Parcel in) {
        bufferSize = in.readInt();
        bufferEnabled = in.readByte() != 0;
        persistBuffer = in.readByte() != 0;
        deleteOldestMessages = in.readByte() != 0;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(bufferSize);
        parcel.writeByte((byte) (bufferEnabled ? 1 : 0));
        parcel.writeByte((byte) (persistBuffer ? 1 : 0));
        parcel.writeByte((byte) (deleteOldestMessages ? 1 : 0));

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

    /**
     * 转换为DisconnectedBufferOptions
     *
     * @return
     */
    public DisconnectedBufferOptions transToDisconnectedBufferOptions() {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferSize(bufferSize);
        disconnectedBufferOptions.setBufferEnabled(bufferEnabled);
        disconnectedBufferOptions.setPersistBuffer(persistBuffer);
        disconnectedBufferOptions.setDeleteOldestMessages(deleteOldestMessages);
        return disconnectedBufferOptions;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException();
        }
        this.bufferSize = bufferSize;
    }

    public boolean isBufferEnabled() {
        return bufferEnabled;
    }

    public void setBufferEnabled(boolean bufferEnabled) {
        this.bufferEnabled = bufferEnabled;
    }

    public boolean isPersistBuffer() {
        return persistBuffer;
    }

    public void setPersistBuffer(boolean persistBuffer) {
        this.persistBuffer = persistBuffer;
    }

    public boolean isDeleteOldestMessages() {
        return deleteOldestMessages;
    }

    public void setDeleteOldestMessages(boolean deleteOldestMessages) {
        this.deleteOldestMessages = deleteOldestMessages;
    }

    @Override
    public String toString() {
        return "TXDisconnectedBufferOptions{" +
                "bufferSize=" + bufferSize +
                ", bufferEnabled=" + bufferEnabled +
                ", persistBuffer=" + persistBuffer +
                ", deleteOldestMessages=" + deleteOldestMessages +
                '}';
    }
}
