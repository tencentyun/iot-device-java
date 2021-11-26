package com.tencent.iot.explorer.device.video.recorder;

import com.tencent.iot.explorer.device.rtc.data_template.TXTRTCCallBack;

public abstract class TXVideoCallBack extends TXTRTCCallBack {
    public abstract void onNewCall(String userid, String agent, Integer callType);
    public abstract void onUserAccept(String userid, String agent, Integer callType);
    public abstract void onCallOver(String userid, String agent, Integer callType);
}
