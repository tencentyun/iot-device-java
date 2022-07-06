package com.tencent.iot.hub.device.java.core.mqtt;

/**
 * 连接状态枚举
 */
public enum ConnectionState {
    /**
     * 连接中
     */
    CONNECTING(0),
    /**
     * 连接上/上线
     */
    CONNECTED(1),
    /**
     * 网络波动造成的掉线（被动触发）
     */
    CONNECTION_LOST(2),
    /**
     * 断开连接中（主动触发）
     */
    DISCONNECTING(3),
    /**
     * 断开连接（主动触发）
     */
    DISCONNECTED(4);

    private int value;

    ConnectionState(int value) {
        this.value = value;
    }
}
