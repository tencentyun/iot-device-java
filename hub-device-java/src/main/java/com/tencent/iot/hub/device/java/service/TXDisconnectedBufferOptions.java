package com.tencent.iot.hub.device.java.service;



import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;

/**
 * 该类负责序列化DisconnectedBufferOptions相关信息
 */

public class TXDisconnectedBufferOptions extends DisconnectedBufferOptions{
    private int bufferSize = DISCONNECTED_BUFFER_SIZE_DEFAULT;
    private boolean bufferEnabled = DISCONNECTED_BUFFER_ENABLED_DEFAULT;
    private boolean persistBuffer = PERSIST_DISCONNECTED_BUFFER_DEFAULT;
    private boolean deleteOldestMessages = DELETE_OLDEST_MESSAGES_DEFAULT;

    public TXDisconnectedBufferOptions() {
        super();
    }

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
