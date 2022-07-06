package com.tencent.iot.explorer.device.video.recorder;

import com.tencent.iot.explorer.device.common.stateflow.OnCall;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;

public abstract class TXVideoCallBack extends OnCall {
    public abstract void onNewCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
    public abstract void onUserAccept(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
    public abstract void onCallOver(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
    public abstract void onAutoRejectCall(String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);
}
