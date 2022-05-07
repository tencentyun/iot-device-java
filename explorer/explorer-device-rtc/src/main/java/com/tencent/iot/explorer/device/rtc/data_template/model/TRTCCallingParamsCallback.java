package com.tencent.iot.explorer.device.rtc.data_template.model;

public interface TRTCCallingParamsCallback {

    void joinRoom(Integer callingType, String deviceId, RoomKey roomKey);

    void refuseEnterRoom();
}
