package com.tencent.iot.explorer.device.trtc.data_template.model;

public interface TRTCCallingParamsCallback {

    void joinRoom(Integer callingType, String deviceId, RoomKey roomKey);
}
