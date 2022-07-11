package com.tencent.iot.explorer.device.android.http.utils

object RequestCode {

    /*************用户接口开始**************/
    const val phone_login = 1000
    const val email_login = 1001
    const val wechat_login = 1002
    const val send_mobile_code = 1003
    const val send_email_code = 1004
    const val check_mobile_code = 1005
    const val check_email_code = 1006
    const val phone_register = 1007
    const val email_register = 1008
    const val update_user_info = 1009
    const val email_reset_pwd = 1010
    const val phone_reset_pwd = 1011
    const val bind_xg = 1012
    const val unbind_xg = 1013
    const val feedback = 1014
    const val user_info = 1015
    const val message_list = 1016
    const val delete_message = 1017
    const val reset_password = 1018
    const val logout = 1019
    const val user_setting = 1020
    const val update_user_setting = 1021
    const val modify_nick = 1022
    const val find_phone_user = 1023
    const val find_email_user = 1024
    const val phone_verifycode_login = 1025
    const val email_verifycode_login = 1026
    const val bind_wx = 1027
    const val cancel_account = 1028
    const val set_unit_of_temperature = 1029
    const val get_global_config = 1030
    const val set_region = 1031
    const val get_last_version = 1032
    const val get_region_list = 1033
    const val get_opensource_license = 1034

    /*************用户接口结束**************/

    /*************家庭接口开始**************/
    const val family_list = 2000
    const val create_family = 2001
    const val room_list = 2002
    const val create_room = 2003
    const val modify_family = 2004
    const val modify_room = 2005
    const val delete_family = 2006
    const val delete_room = 2007
    const val family_info = 2008
    const val invite_family_member = 2009
    const val delete_family_member = 2010
    const val join_family = 2011
    const val exit_family = 2012
    const val member_list = 2013
    const val change_room = 2014
    const val send_family_invite = 2015

    /*************家庭接口结束**************/

    /*************设备接口开始**************/
    const val device_list = 3000
    const val device_online_status = 3001
    const val scan_bind_device = 3002
    const val modify_device_alias_name = 3003
    const val wifi_bind_device = 3004
    const val delete_device = 3005
    //设备状态
    const val device_data = 3006
    //设备详情
    const val get_device_info = 3007
    const val control_device = 3008
    const val control_panel = 3009
    const val device_product = 3010
    const val send_share_invite = 3011
    const val get_bind_device_token = 3012
    const val check_device_bind_token_state = 3013

    const val sig_bind_device = 3014
    const val trtc_call_device = 3015
    const val device_product_info = 3016
    const val gateway_sub_device_list = 3017
    const val bind_gateway_sub_device = 3018

    /*************设备接口结束**************/

    /*************云端定时接口开始**************/
    const val time_list = 4000
    const val create_timer = 4001
    const val modify_timer = 4002
    const val modify_timer_status = 4003  //打开或关闭
    const val delete_timer = 4004

    /*************云端定时接口结束**************/

    //上传图片
    const val app_cos_auth = 5001

    /*************设备分享接口开始**************/
    const val share_device_list = 6000
    const val share_user_list = 6001
    const val delete_share_device = 6002
    const val delete_share_user = 6003
    const val share_ticket = 6004
    const val ticket_to_token = 6005
    const val get_share_token = 6006
    const val get_share_device_info = 6007
    const val bind_share_device = 6008
    const val token_ticket = 6005

    /*************设备分享接口结束**************/

    /*************设备推荐接口开始**************/
    const val get_parent_category_list = 7000
    const val get_recommend_device_list = 7001
    const val get_products_config = 7002
    const val describe_product_config = 7003
    /*************设备推荐接口结束**************/

    /*************场景联动接口开始**************/
    const val create_manual_task = 8001
    const val query_all_manual_task = 8002
    const val create_automic_task = 8003
    const val query_all_automic_task = 8004
    const val run_manual_task = 8005
    const val del_manual_task = 8006
    const val del_automic_task = 8007
    const val update_manual_task = 8008
    const val get_automic_task_detail = 8009
    const val update_automic_task_status = 8010
    const val update_automic_task = 8011
    const val get_run_task_log = 8012
    const val get_tasl_pic_list = 8013

    /*************场景联动接口结束**************/


}