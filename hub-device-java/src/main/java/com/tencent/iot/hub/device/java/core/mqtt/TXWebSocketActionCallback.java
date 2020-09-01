package com.tencent.iot.hub.device.java.core.mqtt;

public interface TXWebSocketActionCallback {

    // 上线回调
    void onConnected();

    void onMessage(String message);

    // 掉线回调
    void onDisconnect(boolean remote);
}
