package com.tencent.iot.explorer.device.video.recorder;

import com.tencent.iot.explorer.device.common.stateflow.OnCall;

public abstract class TXVideoCallBack extends OnCall {
    public abstract void onNewCall(String userid, String agent, Integer callType);
    public abstract void onUserAccept(String userid, String agent, Integer callType);
    public abstract void onCallOver(String userid, String agent, Integer callType);
}
