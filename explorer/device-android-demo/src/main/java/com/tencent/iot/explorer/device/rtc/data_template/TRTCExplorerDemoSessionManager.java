package com.tencent.iot.explorer.device.rtc.data_template;

import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCSessionManager;

public class TRTCExplorerDemoSessionManager extends TRTCSessionManager {

    private TRTCDataTemplateSample mDataTemplateSample;

    public TRTCExplorerDemoSessionManager(TRTCDataTemplateSample dataTemplateSample) {
        super();
        mDataTemplateSample = dataTemplateSample;
    }

    @Override
    public void joinRoom(Integer callingType, String deviceId) {
        super.joinRoom(callingType, deviceId);
        mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_CALLING, callingType, deviceId, null);
    }

    @Override
    public void didExitRoom(Integer callingType, String deviceId) {
        super.didExitRoom(callingType, deviceId);
        //退出房间时需要设置callStatus为0初始状态
        mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_IDLE_OR_REFUSE, callingType, deviceId, null);
    }

    @Override
    public void startOnThePhone(Integer callingType, String deviceId) {
        super.startOnThePhone(callingType, deviceId);
        mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_ON_THE_PHONE, callingType, deviceId, null);
    }
}
