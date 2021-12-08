package com.tencent.iot.explorer.device.common.stateflow.entity;

public class CallExtraInfo {
    private String calledId;
    private String callerId;

    public CallExtraInfo(String callerId, String calledId) {
        this.callerId = callerId;
        this.calledId = calledId;
    }

    public String getCalledId() {
        return calledId;
    }

    public void setCalledId(String calledId) {
        this.calledId = calledId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }
}
