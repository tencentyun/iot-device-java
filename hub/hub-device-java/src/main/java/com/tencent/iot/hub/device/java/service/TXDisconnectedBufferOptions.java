package com.tencent.iot.hub.device.java.service;



import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;

/**
 * 该类负责序列化 DisconnectedBufferOptions 相关信息
 */
public class TXDisconnectedBufferOptions extends DisconnectedBufferOptions{
    private int bufferSize = DISCONNECTED_BUFFER_SIZE_DEFAULT;
    private boolean bufferEnabled = DISCONNECTED_BUFFER_ENABLED_DEFAULT;
    private boolean persistBuffer = PERSIST_DISCONNECTED_BUFFER_DEFAULT;
    private boolean deleteOldestMessages = DELETE_OLDEST_MESSAGES_DEFAULT;

    /**
     * 构造函数
     */
    public TXDisconnectedBufferOptions() {
        super();
    }

    /**
     * 转换为 DisconnectedBufferOptions
     *
     * @return {@link DisconnectedBufferOptions}
     */
    public DisconnectedBufferOptions transToDisconnectedBufferOptions() {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferSize(bufferSize);
        disconnectedBufferOptions.setBufferEnabled(bufferEnabled);
        disconnectedBufferOptions.setPersistBuffer(persistBuffer);
        disconnectedBufferOptions.setDeleteOldestMessages(deleteOldestMessages);
        return disconnectedBufferOptions;
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * 设置缓存大小
     *
     * @param bufferSize 缓存大小
     */
    public void setBufferSize(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException();
        }
        this.bufferSize = bufferSize;
    }

    /**
     * 是否支持缓存
     * @return 是否支持缓存
     */
    public boolean isBufferEnabled() {
        return bufferEnabled;
    }

    /**
     * 设置是否支持缓存
     * @param bufferEnabled 是否支持缓存
     */
    public void setBufferEnabled(boolean bufferEnabled) {
        this.bufferEnabled = bufferEnabled;
    }

    /**
     * 是否持久缓存
     *
     * @return 是否持久缓存
     */
    public boolean isPersistBuffer() {
        return persistBuffer;
    }

    /**
     * 设置是否持久缓存
     *
     * @param persistBuffer 是否持久缓存
     */
    public void setPersistBuffer(boolean persistBuffer) {
        this.persistBuffer = persistBuffer;
    }

    /**
     * 是否清理最老的消息
     *
     * @return 是否清理最老的消息
     */
    public boolean isDeleteOldestMessages() {
        return deleteOldestMessages;
    }

    /**
     * 设置是否清理最老的消息
     *
     * @param deleteOldestMessages 是否清理最老的消息
     */
    public void setDeleteOldestMessages(boolean deleteOldestMessages) {
        this.deleteOldestMessages = deleteOldestMessages;
    }

    /**
     * 转换成标准格式的字符串内容
     *
     * @return 标准字符串内容
     */
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
