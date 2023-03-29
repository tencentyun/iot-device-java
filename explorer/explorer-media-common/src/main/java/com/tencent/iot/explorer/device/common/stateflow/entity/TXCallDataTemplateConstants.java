package com.tencent.iot.explorer.device.common.stateflow.entity;

public interface TXCallDataTemplateConstants {

    /**
     * sys property
     */
    String PROPERTY_SYS_VIDEO_CALL_STATUS = "_sys_video_call_status";
    String PROPERTY_SYS_AUDIO_CALL_STATUS = "_sys_audio_call_status";
    String PROPERTY_SYS_CALLED_USERID = "_sys_called_id";
    String PROPERTY_SYS_CALLER_USERID = "_sys_caller_id";
    String PROPERTY_SYS_CALL_USERLIST = "_sys_call_userlist";
    String PROPERTY_SYS_AGENT = "_sys_user_agent";
    String PROPERTY_SYS_EXTRA_INFO = "_sys_extra_info";
    String PROPERTY_REJECT_USERID = "rejectUserId";


    /**
     * _sys_call_userlist
     */
    String PROPERTY_SYS_CALL_USERLIST_USERID = "UserId";
    String PROPERTY_SYS_CALL_USERLIST_NICKNAME = "NickName";

    /**
     * sys action
     */
    String ACTION_SYS_TRTC_JOIN_ROOM = "_sys_trtc_join_room";
    String ACTION_TRTC_JOIN_ROOM = "trtc_join_room";
}
