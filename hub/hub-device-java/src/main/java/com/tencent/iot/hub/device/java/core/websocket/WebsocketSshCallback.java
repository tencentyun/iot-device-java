package com.tencent.iot.hub.device.java.core.websocket;

public interface WebsocketSshCallback {
    /**
     * 设备建立ssh通道
     */
    void localSshCreate();

    /**
     * 透传 SSH 指令
     */
    void websocketSshRawData(String token, String payload, int payloadLen) ;
}
