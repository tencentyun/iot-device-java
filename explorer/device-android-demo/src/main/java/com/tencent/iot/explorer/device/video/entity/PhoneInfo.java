package com.tencent.iot.explorer.device.video.entity;

import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;

public class PhoneInfo {
    public static String TAG = PhoneInfo.class.getSimpleName();

    private int callType = CallingType.TYPE_UNKNOWN;
    private String userid = "";
    private String agent = "";

    public int getCallType() {
        return callType;
    }

    public String getAgent() {
        return agent;
    }

    public String getUserid() {
        return userid;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
