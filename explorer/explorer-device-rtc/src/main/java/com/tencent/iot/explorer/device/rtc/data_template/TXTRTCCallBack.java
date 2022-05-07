package com.tencent.iot.explorer.device.rtc.data_template;

import com.tencent.iot.explorer.device.rtc.data_template.model.RoomKey;

public abstract class TXTRTCCallBack {

    /**
     * 获取TRTC属性呼叫状态
     *
     * @param callStatus            呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param userid                用户id
     * @param agent                 代理方
     * @param callType              邀请类型 1-语音通话，2-视频通话
     */
    public abstract void onGetCallStatusCallBack(Integer callStatus, String userid, String agent, Integer callType);

    /**
     * 获取trtc进入房间所需参数模型
     *
     * @param room
     */
    public abstract void trtcJoinRoomCallBack(RoomKey room);
}
