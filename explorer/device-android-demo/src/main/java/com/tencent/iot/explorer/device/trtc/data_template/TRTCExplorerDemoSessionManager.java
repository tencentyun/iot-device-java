package com.tencent.iot.explorer.device.trtc.data_template;

import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCSessionManager;

public class TRTCExplorerDemoSessionManager extends TRTCSessionManager {

    private TRTCDataTemplateSample mDataTemplateSample;

    public TRTCExplorerDemoSessionManager(TRTCDataTemplateSample dataTemplateSample) {
        super();
        mDataTemplateSample = dataTemplateSample;
    }

    @Override
    public void joinRoom(Integer callingType, String deviceId) {
        super.joinRoom(callingType, deviceId);
        mDataTemplateSample.reportCallStatusProperty(TRTCCallStatus.TYPE_CALLING, callingType);
    }
}
