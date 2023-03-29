package com.tencent.iot.device.video.advanced.recorder;

import com.tencent.iot.explorer.device.common.stateflow.OnCall;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;

public abstract class TXVideoCallBack extends OnCall {
    public abstract void onNewCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
    public abstract void onUserAccept(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
    public abstract void onCallOver(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
    public abstract void onAutoRejectCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);

    /**
     * 收到rtc链接房间action以及所需参数
     *
     * @param room
     */
    public abstract void receiveRtcJoinRoomAction(RoomKey room);

    /**
     * 请求呼叫其他设备成功
     *
     * @param room
     */
    public abstract void callOtherDeviceSuccess(RoomKey room);

    /**
     * 请求呼叫其他设备失败
     *
     * @param code
     * @param reason
     */
    public abstract void callOtherDeviceFailed(int code, String reason);
}
