package com.tencent.iot.explorer.device.common.stateflow;

public abstract class CallState {
    public static final int TYPE_IDLE_OR_REFUSE = 0; //空闲或拒绝
    public static final int TYPE_CALLING = 1; //呼叫中
    public static final int TYPE_ON_THE_PHONE = 2;  //通话中
}
