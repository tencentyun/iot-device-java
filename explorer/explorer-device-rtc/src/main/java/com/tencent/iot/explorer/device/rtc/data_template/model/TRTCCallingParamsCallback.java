package com.tencent.iot.explorer.device.rtc.data_template.model;

import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;

public interface TRTCCallingParamsCallback {

    void joinRoom(Integer callingType, String deviceId, RoomKey roomKey);

    void refuseEnterRoom();
}
