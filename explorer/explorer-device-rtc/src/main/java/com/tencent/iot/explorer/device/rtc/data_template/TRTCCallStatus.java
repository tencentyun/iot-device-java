package com.tencent.iot.explorer.device.rtc.data_template;

public abstract class TRTCCallStatus {
    public static final int TYPE_IDLE_OR_REFUSE = 0; //空闲或拒绝
    public static final int TYPE_CALLING = 1; //呼叫中
    public static final int TYPE_ON_THE_PHONE = 2;  //通话中
}
