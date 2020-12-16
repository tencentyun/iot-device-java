package com.tencent.iot.explorer.device.trtc.data_template.model;

public interface TXTRTCDataTemplateConstants {

    /**
     * sys property
     */
    String PROPERTY_SYS_VIDEO_CALL_STATUS = "_sys_video_call_status";
    String PROPERTY_SYS_AUDIO_CALL_STATUS = "_sys_audio_call_status";
    String PROPERTY_SYS_USERID = "_sys_userid";
    String PROPERTY_SYS_CALL_USERLIST = "_sys_call_userlist";


    /**
     * _sys_call_userlist
     */
    String PROPERTY_SYS_CALL_USERLIST_USERID = "UserId";
    String PROPERTY_SYS_CALL_USERLIST_NICKNAME = "NickName";

    /**
     * sys action
     */
    String ACTION_SYS_TRTC_JOIN_ROOM = "_sys_trtc_join_room";
}
