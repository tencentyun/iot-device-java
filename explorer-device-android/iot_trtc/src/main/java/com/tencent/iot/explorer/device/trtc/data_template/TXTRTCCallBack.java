package com.tencent.iot.explorer.device.trtc.data_template;

public abstract class TXTRTCCallBack {

    /**
     * 获取AI人脸License回调
     *
     * @param callStatus            呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param userid                用户id
     * @param username              用户名
     * @param callType              1video 2audio
     */
    public abstract void onGetCallStatusCallBack(Integer callStatus, String userid, String username, Integer callType);
}
