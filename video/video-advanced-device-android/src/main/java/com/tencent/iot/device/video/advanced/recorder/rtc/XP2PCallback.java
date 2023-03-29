package com.tencent.iot.device.video.advanced.recorder.rtc;

import java.util.Map;

public interface XP2PCallback {

    /**
     * sdk内部发生了错误
     * @param code 错误码
     * @param msg 错误消息
     */
    void onError(int code, String msg);

    /**
     * 链接成功与否的事件回调
     * @param result 如果加入成功，回调 result 会是一个正数（result > 0），代表链接所消耗的时间，单位是毫秒（ms），如果链接失败，回调 result 会是一个负数（result < 0），代表失败原因的错误码。|
     */
    void onConnect(long result);

    /**
     * 释放链接的事件回调
     * @param reason 离开房间原因，0：主动调用 exitRoom 退出房间；1：被服务器踢出当前房间；2：当前房间整个被解散。
     */
    void onRelease(int reason);

    /**
     * 如果有用户同意进入通话，那么会收到此回调
     * @param rtc_uid 进入通话的用户
     */
    void onUserEnter(String rtc_uid);

    /**
     * 如果有用户同意离开通话，那么会收到此回调
     * @param rtc_uid 离开通话的用户
     */
    void onUserLeave(String rtc_uid);

    /**
     * 远端用户开启/关闭了摄像头
     * @param rtc_uid 远端用户ID
     * @param isVideoAvailable true:远端用户打开摄像头  false:远端用户关闭摄像头
     */
    void onUserVideoAvailable(String rtc_uid, boolean isVideoAvailable);

    /**
     * 用户说话音量回调
     * @param volumeMap 音量表，根据每个userid可以获取对应的音量大小，音量最小值0，音量最大值100
     */
    void onUserVoiceVolume(Map<String, Integer> volumeMap);

    /**
     * 收到自定义消息的事件回调
     * @param rtc_uid 用户标识
     * @param message 消息数据
     */
    void onRecvCustomCmdMsg(String rtc_uid, String message);

    /**
     * SDK 开始渲染自己本地或远端用户的首帧画面
     * @param rtc_uid 用户标识
     * @param width 画面的宽度
     * @param height 画面的高度
     */
    void onFirstVideoFrame(String rtc_uid, int width, int height);
}
