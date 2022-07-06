package com.tencent.iot.explorer.device.common.stateflow;

import com.tencent.iot.explorer.device.common.stateflow.entity.CallExtraInfo;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;

import org.json.JSONObject;

public abstract class OnCall {

    /**
     * 获取TRTC属性呼叫状态
     *
     * @param callStatus            呼叫状态 0 - 空闲或拒绝呼叫  1 - 进行呼叫  2 - 通话中
     * @param userid                用户 ID
     * @param agent                 代理方
     * @param callExtraInfo         通话的附加信息
     * @param callType              邀请类型 1-语音通话，2-视频通话
     */
    public abstract void onGetCallStatusCallBack(Integer callStatus, String userid, String agent, Integer callType, CallExtraInfo callExtraInfo);

    /**
     * 获取trtc进入房间所需参数模型
     *
     * @param room
     */
    public abstract void trtcJoinRoomCallBack(RoomKey room);

    /**
     * 获取用户头像结果
     *
     * @param code  0成功，400请求不是json格式，401无权限，404userid不存在，500内部错误
     * @param errorMsg 0成功，400请求不是json格式，401无权限，404userid不存在，500内部错误
     * @param avatarList userId对应用户头像 json
     */
    public abstract void trtcGetUserAvatarCallBack(Integer code, String errorMsg, JSONObject avatarList);
}
