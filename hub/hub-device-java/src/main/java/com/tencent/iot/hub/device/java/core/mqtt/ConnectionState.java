package com.tencent.iot.hub.device.java.core.mqtt;

public enum ConnectionState {
    CONNECTING(0),      // 连接中
    CONNECTED(1),   // 连接上/上线
    CONNECTION_LOST(2), // 网络波动造成的掉线（被动触发）
    DISCONNECTING(3),   // 断开连接中（主动触发）
    DISCONNECTED(4);    // 断开连接（主动触发）

    private int value;

    ConnectionState(int value) {
        this.value = value;
    }
}
