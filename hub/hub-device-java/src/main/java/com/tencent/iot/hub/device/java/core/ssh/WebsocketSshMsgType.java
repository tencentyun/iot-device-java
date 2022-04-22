package com.tencent.iot.hub.device.java.core.ssh;

public enum WebsocketSshMsgType {

    NEW_SESSION(0),
    NEW_SESSION_RESP(1),
    RELEASE_SESSION(2),
    RELEASE_SESSION_RESP(3),
    CMD_PING(4),
    CMD_PONG(5),
    SSH_RAWDATA(6),
    SSH_RAWDATA_RESP(7),
    VERIFY_DEVICE(8),
    VERIFY_DEVICE_RESP(9);

    private final int value;

    WebsocketSshMsgType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
