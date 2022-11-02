package com.tencent.iot.explorer.device.android.http

import android.text.TextUtils
import com.tencent.iot.explorer.device.android.BuildConfig
import com.tencent.iot.explorer.device.android.http.callback.MyCallback
import com.tencent.iot.explorer.device.android.http.response.BaseResponse
import com.tencent.iot.explorer.device.android.http.retrofit.Callback
import com.tencent.iot.explorer.device.android.http.retrofit.StringRequest
import com.tencent.iot.explorer.device.android.http.utils.JsonManager
import com.tencent.iot.explorer.device.android.http.utils.RequestCode
import com.tencent.iot.explorer.device.android.http.utils.SignatureUtil
import com.tencent.iot.explorer.device.android.utils.TXLog
import java.util.*
import kotlin.collections.HashMap

/**
 * 接口请求文件
 */
class HttpRequest private constructor() {

    private val TAG = "HttpRequest"

    private object HttpRequestHolder {
        val request = HttpRequest()
    }

    init {
        //初始化请求
        StringRequest.instance.init(OEM_TOKEN_API)
    }

    companion object {
        val instance = HttpRequestHolder.request
        const val OEM_TOKEN_API = "https://iot.cloud.tencent.com/api/exploreropen/tokenapi"  // 可安全在设备端调用。
    }

    /**
     *  重连
     */
    fun reconnect() {
        StringRequest.instance.reconnect()
    }

    /**
     * 登录后接口公共参数
     */
    private fun tokenParams(action: String): HashMap<String, Any> {
        val param = HashMap<String, Any>()
        param["RequestId"] = UUID.randomUUID().toString()
        param["Action"] = action
        param["AccessToken"] = IoTAuth.getToken()
        return param
    }

    /**
     * 登录后请求
     */
    private fun tokenPost(param: HashMap<String, Any>, callback: MyCallback, reqCode: Int) {
        if (TextUtils.isEmpty(IoTAuth.getToken())) {//登录过期或未登录
            callback.fail(ErrorCode.DATA_MSG.ACCESS_TOKEN_ERR, reqCode)
            return
        }
        val host:String = OEM_TOKEN_API
        val api = ""
        val json = JsonManager.toJson(param)

        StringRequest.instance.postJson(host, api, json, object : Callback {
            override fun fail(msg: String?, reqCode: Int) {
                callback.fail(msg, reqCode)
            }

            override fun success(json: String?, reqCode: Int) {
                TXLog.e(TAG, "响应${param["Action"]} ${json ?: ""}")
                JsonManager.parseJson(json, BaseResponse::class.java)?.run {
                    // 检查特殊情况 token 失效
                    if (checkRespTokenValid(this)) {
                        callback.success(this, reqCode)
                    } else {
                        callback.fail(ErrorCode.DATA_MSG.ACCESS_TOKEN_ERR, reqCode)
                    }
                }
            }
        }, reqCode)
    }

    // 处理当使用过期 token 请求时，返回的数据
    private fun checkRespTokenValid(resp: BaseResponse): Boolean {
        if (resp.code == ErrorCode.REQ_ERROR_CODE && resp.data != null) {
            val errMsg = ErrorMessage.parseErrorMessage(resp.data.toString())
            if (errMsg.Code == ErrorCode.DATA_MSG.ACCESS_TOKEN_ERR) {
                return false
            }
        }
        return true
    }

    /*************************************  家庭接口开始  ************************************************/

    /**
     * 请求获取家庭列表
     */
    fun familyList(offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetFamilyList")
        param["Offset"] = offset
        param["Limit"] = 999
        tokenPost(param, callback, RequestCode.family_list)
    }

    /**
     * 新增家庭
     */
    fun createFamily(familyName: String, address: String, callback: MyCallback) {
        val param = tokenParams("AppCreateFamily")
        param["Name"] = familyName
        param["Address"] = address
        tokenPost(param, callback, RequestCode.create_family)
    }

    /**
     * 房间列表
     */
    fun roomList(familyId: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetRoomList")
        param["FamilyId"] = familyId
        param["Offset"] = offset
        param["Limit"] = 999
        tokenPost(param, callback, RequestCode.room_list)
    }

    /**
     * 创建房间
     */
    fun createRoom(familyId: String, roomName: String, callback: MyCallback) {
        val param = tokenParams("AppCreateRoom")
        param["Name"] = roomName
        param["FamilyId"] = familyId
        tokenPost(param, callback, RequestCode.create_room)
    }

    /**
     * 修改家庭
     */
    fun modifyFamily(familyId: String, familyName: String, address: String, callback: MyCallback) {
        val param = tokenParams("AppModifyFamily")
        param["FamilyId"] = familyId
        if (!TextUtils.isEmpty(familyName))
            param["Name"] = familyName
        if (!TextUtils.isEmpty(address))
            param["Address"] = address
        tokenPost(param, callback, RequestCode.modify_family)
    }

    /**
     * 修改房间
     */
    fun modifyRoom(familyId: String, roomId: String, roomName: String, callback: MyCallback) {
        val param = tokenParams("AppModifyRoom")
        param["FamilyId"] = familyId
        param["RoomId"] = roomId
        param["Name"] = roomName
        tokenPost(param, callback, RequestCode.modify_room)
    }

    /**
     * 更换房间
     */
    fun changeRoom(
        familyId: String,
        roomId: String,
        productId: String,
        deviceName: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppModifyFamilyDeviceRoom")
        param["FamilyId"] = familyId
        param["RoomId"] = roomId
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        tokenPost(param, callback, RequestCode.change_room)
    }

    /**
     * 删除家庭
     */
    fun deleteFamily(familyId: String, familyName: String, callback: MyCallback) {
        val param = tokenParams("AppDeleteFamily")
        param["FamilyId"] = familyId
        param["Name"] = familyName
        tokenPost(param, callback, RequestCode.delete_family)
    }

    /**
     * 删除房间
     */
    fun deleteRoom(familyId: String, roomId: String, callback: MyCallback) {
        val param = tokenParams("AppDeleteRoom")
        param["FamilyId"] = familyId
        param["RoomId"] = roomId
        tokenPost(param, callback, RequestCode.delete_room)
    }

    /**
     * 家庭详情
     */
    fun familyInfo(familyId: String, callback: MyCallback) {
        val param = tokenParams("AppDescribeFamily")
        param["FamilyId"] = familyId
        tokenPost(param, callback, RequestCode.family_info)
    }

    /**
     * 家庭成员列表
     */
    fun memberList(familyId: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetFamilyMemberList")
        param["FamilyId"] = familyId
        param["Offset"] = offset
        param["Limit"] = 50
        tokenPost(param, callback, RequestCode.member_list)
    }

    /**
     * 移除家庭成员
     */
    fun deleteFamilyMember(familyId: String,memberId: String, callback: MyCallback) {
        val param = tokenParams("AppDeleteFamilyMember")
        param["FamilyId"] = familyId
        param["MemberID"] = memberId
        tokenPost(param, callback, RequestCode.delete_family_member)
    }

    /**
     * 成员加入家庭
     */
    fun joinFamily(shareToken: String, callback: MyCallback) {
        val param = tokenParams("AppJoinFamily")
        param["ShareToken"] = shareToken
        tokenPost(param, callback, RequestCode.join_family)
    }

    /**
     * 成员自动退出家庭
     */
    fun exitFamily(familyId: String, callback: MyCallback) {
        val param = tokenParams("AppExitFamily")
        param["FamilyId"] = familyId
        tokenPost(param, callback, RequestCode.exit_family)
    }

    /**
     *  发送邀请成员
     */
    fun sendFamilyInvite(familyId: String, userId: String, callback: MyCallback) {
        val param = tokenParams("AppSendShareFamilyInvite")
        param["FamilyId"] = familyId
        param["ToUserID"] = userId
        tokenPost(param, callback, RequestCode.send_family_invite)
    }

    /*************************************  家庭接口结束  ************************************************/


    /*************************************  设备接口开始  ************************************************/

    /**
     * 请求获取设备列表
     */
    fun deviceList(familyId: String, roomId: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetFamilyDeviceList")
        param["FamilyId"] = familyId
        param["RoomId"] = roomId
        param["Offset"] = offset
        param["Limit"] = 999
        tokenPost(param, callback, RequestCode.device_list)
    }


    /**
     * 获取设备在线状态
     */
    fun deviceOnlineStatus(deviceIds: ArrayList<String>, callback: MyCallback) {
        val param = tokenParams("AppGetDeviceStatuses")
        param["DeviceIds"] = deviceIds
        tokenPost(param, callback, RequestCode.device_online_status)
    }

    /**
     * 修改设备别名
     */
    fun modifyDeviceAliasName(
        productId: String,
        deviceName: String,
        aliasName: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppUpdateDeviceInFamily")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["AliasName"] = aliasName
        tokenPost(param, callback, RequestCode.modify_device_alias_name)
    }

    /**
     * 扫码绑定设备
     */
    fun scanBindDevice(familyId: String, roomId: String, signature: String, callback: MyCallback) {
        val param = tokenParams("AppSecureAddDeviceInFamily")
        param["FamilyId"] = familyId
        param["RoomId"] = roomId
        param["DeviceSignature"] = signature
        tokenPost(param, callback, RequestCode.scan_bind_device)
    }

    /**
     * 删除设备
     */
    fun deleteDevice(
        familyId: String,
        productId: String,
        deviceName: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppDeleteDeviceInFamily")
        param["FamilyId"] = familyId
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        tokenPost(param, callback, RequestCode.delete_device)
    }

    /**
     * 设备当前状态(如亮度、开关状态等)
     */
    fun deviceData(productId: String, deviceName: String, callback: MyCallback) {
        val param = tokenParams("AppGetDeviceData")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        tokenPost(param, callback, RequestCode.device_data)
    }

    /**
     * 获取设备详情
     */
    fun getDeviceInfo(productId: String, deviceName: String, familyId: String, callback: MyCallback) {
        val param = tokenParams("AppGetDeviceInFamily")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["FamilyId"] = familyId
        tokenPost(param, callback, RequestCode.get_device_info)
    }

    /**
     * 获取已绑定到家庭下的指定网关的子设备列表
     */
    fun getFamilySubDeviceList(gatewayProductId: String, gatewayDeviceName: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetFamilySubDeviceList")
        param["ProductId"] = gatewayProductId
        param["DeviceName"] = gatewayDeviceName
        param["Offset"] = offset
        param["Limit"] = 50
        tokenPost(param, callback, RequestCode.get_family_subdevice_list)
    }

    /**
     * 控制设备
     */
    fun controlDevice(productId: String, deviceName: String, data: String, callback: MyCallback) {
        val param = tokenParams("AppControlDeviceData")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["Data"] = data
        tokenPost(param, callback, RequestCode.control_device)
    }

    /**
     * 当前产品控制面板风格主题
     */
    fun controlPanel(productIds: ArrayList<String>, callback: MyCallback) {
        val param = tokenParams("AppGetProductsConfig")
        param["ProductIds"] = productIds
        tokenPost(param, callback, RequestCode.control_panel)
    }

    /**
     * 当前设备对应的产品信息
     */
    fun deviceProducts(productIds: ArrayList<String>, callback: MyCallback) {
        val param = tokenParams("AppGetProducts")
        param["ProductIds"] = productIds
        tokenPost(param, callback, RequestCode.device_product)
    }

    /**
     * 发送设备分享
     */
    fun sendShareInvite(
        productId: String, deviceName: String, userId: String, callback: MyCallback
    ) {
        val param = tokenParams("AppSendShareDeviceInvite")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["ToUserID"] = userId
        tokenPost(param, callback, RequestCode.send_share_invite)
    }

    /**
     * 手机请求加入房间
     */
    fun trtcCallDevice(deviceId: String, callback: MyCallback) {
        val param = tokenParams("App::IotRTC::CallDevice")
        param["DeviceId"] = deviceId
        tokenPost(param, callback, RequestCode.trtc_call_device)
    }

    /****************************************   设备接口结束  ************************************************/

    /******************************************   云端定时接口开始  *************************************************************/

    /**
     * 云端定时列表
     */
    fun timeList(productId: String, deviceName: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetTimerList")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["Offset"] = offset
        param["Limit"] = 20
        tokenPost(param, callback, RequestCode.time_list)
    }

    /**
     *  创建定时任务
     */
    fun createTimer(
        productId: String,
        deviceName: String,
        timerName: String,
        days: String,
        timePoint: String,
        repeat: Int,
        data: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppCreateTimer")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["TimerName"] = timerName
        param["Days"] = days
        param["TimePoint"] = timePoint
        param["Repeat"] = repeat
        param["Data"] = data
        tokenPost(param, callback, RequestCode.create_timer)
    }

    /**
     *  修改定时任务
     *  @param days 定时器开启时间，每一位——0:关闭,1:开启, 从左至右依次表示: 周日 周一 周二 周三 周四 周五 周六 1000000
     *  @param repeat 是否循环，0表示不需要，1表示需要
     */
    fun modifyTimer(
        productId: String,
        deviceName: String,
        timerName: String,
        timerId: String,
        days: String,
        timePoint: String,
        repeat: Int,
        data: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppModifyTimer")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["TimerId"] = timerId
        param["TimerName"] = timerName
        param["Days"] = days
        param["TimePoint"] = timePoint
        param["Repeat"] = repeat
        param["Data"] = data
        tokenPost(param, callback, RequestCode.modify_timer)
    }

    /**
     *  修改定时任务状态，打开或者关闭
     *  @param status 0 关闭，1 开启
     */
    fun modifyTimerStatus(
        productId: String,
        deviceName: String,
        timerId: String,
        status: Int,
        callback: MyCallback
    ) {
        val param = tokenParams("AppModifyTimerStatus")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["TimerId"] = timerId
        param["Status"] = status
        tokenPost(param, callback, RequestCode.modify_timer_status)
    }

    /**
     *  删除定时
     */
    fun deleteTimer(productId: String, deviceName: String, timerId: String, callback: MyCallback) {
        val param = tokenParams("AppDeleteTimer")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["TimerId"] = timerId
        tokenPost(param, callback, RequestCode.delete_timer)
    }

    /****************************************   云端定时接口结束  ************************************************/


    /****************************************   设备分享接口开始  ************************************************/

    /**
     * 设备分享的设备列表(返回的是设备列表)
     */
    fun shareDeviceList(offset: Int, callback: MyCallback) {
        val param = tokenParams("AppListUserShareDevices")
        param["Offset"] = offset
        param["Limit"] = 999
        tokenPost(param, callback, RequestCode.share_device_list)
    }

    /**
     * 设备分享的用户列表(返回的是用户列表)
     */
    fun shareUserList(productId: String, deviceName: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppListShareDeviceUsers")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["Offset"] = offset
        param["Limit"] = 100
        tokenPost(param, callback, RequestCode.share_user_list)
    }

    /**
     * 删除分享列表的某个设备(删除某个已经分享的设备)
     */
    fun deleteShareDevice(
        productId: String,
        deviceName: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppRemoveUserShareDevice")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        tokenPost(param, callback, RequestCode.delete_share_device)
    }

    /**
     * 删除一个设备的分享用户列表中的某个用户(删除某个用户的分享权限)
     */
    fun deleteShareUser(
        productId: String,
        deviceName: String,
        userId: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppRemoveShareDeviceUser")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["RemoveUserID"] = userId
        tokenPost(param, callback, RequestCode.delete_share_user)
    }

    /**
     * 获取分享票据
     */
    fun getShareTicket(callback: MyCallback) {
        val param = tokenParams("AppGetTokenTicket")
        tokenPost(param, callback, RequestCode.share_ticket)
    }

    /**
     * 获取设备分享Token
     */
    fun getShareToken(
        familyId: String,
        productId: String,
        deviceName: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppCreateShareDeviceToken")
        param["FamilyId"] = familyId
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        tokenPost(param, callback, RequestCode.get_share_token)
    }

    /**
     * 获取设备分享 Token 信息
     */
    fun getShareDeviceInfo(deviceToken: String, callback: MyCallback) {
        val param = tokenParams("AppDescribeShareDeviceToken")
        param["ShareDeviceToken"] = deviceToken
        tokenPost(param, callback, RequestCode.get_share_device_info)
    }

    /**
     * 绑定分享设备(绑定操作)
     */
    fun bindShareDevice(
        productId: String,
        deviceName: String,
        deviceToken: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppBindUserShareDevice")
        param["ProductId"] = productId
        param["DeviceName"] = deviceName
        param["ShareDeviceToken"] = deviceToken
        tokenPost(param, callback, RequestCode.bind_share_device)
    }

    /****************************************   设备分享接口结束   *******************************************************/

    /****************************************   设备推荐接口开始   *******************************************************/

    /**
     * 获取产品推荐父类别列表
     */
    fun getParentCategoryList(callback: MyCallback) {
        val param = tokenParams("AppGetParentCategoryList")
        tokenPost(param, callback, RequestCode.get_parent_category_list)
    }

    /**
     * 推荐产品子类别列表
     */
    fun getRecommList(
        categoryKey: String,
        callback: MyCallback
    ) {
        val param = tokenParams("AppGetRecommList")
        param["CategoryKey"] = categoryKey
        tokenPost(param, callback, RequestCode.get_recommend_device_list)
    }

    fun getProductsConfig(productIds: List<String>, callback: MyCallback) {
        val param = tokenParams("AppGetProductsConfig")
        param["ProductIds"] = productIds
        tokenPost(param, callback, RequestCode.get_products_config)
    }
    /****************************************   设备推荐接口结束   *******************************************************/


    /****************************************   场景联动接口开始   *******************************************************/
    fun queryAutomicTask(familyId: String, callback: MyCallback) {
        val param = tokenParams("AppGetAutomationList")
        param["FamilyId"] = familyId
        tokenPost(param, callback, RequestCode.query_all_automic_task)
    }

    fun queryManualTask(familyId: String, offset: Int, callback: MyCallback) {
        val param = tokenParams("AppGetSceneList")
        param["FamilyId"] = familyId
        param["Offset"] = offset
        param["Limit"] = 999
        tokenPost(param, callback, RequestCode.query_all_manual_task)
    }

    fun runManualTask(sceneId: String, callback: MyCallback) {
        val param = tokenParams("AppRunScene")
        param["SceneId"] = sceneId
        tokenPost(param, callback, RequestCode.run_manual_task)
    }

    fun delManualTask(sceneId: String, callback: MyCallback) {
        val param = tokenParams("AppDeleteScene")
        param["SceneId"] = sceneId
        tokenPost(param, callback, RequestCode.del_manual_task)
    }

    fun delAutomicTask(automationId: String, callback: MyCallback) {
        val param = tokenParams("AppDeleteAutomation")
        param["AutomationId"] = automationId
        tokenPost(param, callback, RequestCode.del_automic_task)
    }

    fun getAutomicTaskDetail(automationId: String, callback: MyCallback) {
        val param = tokenParams("AppDescribeAutomation")
        param["AutomationId"] = automationId
        tokenPost(param, callback, RequestCode.get_automic_task_detail)
    }

    // status 0:开启 1:关闭
    fun updateAutomicTaskStatus(automationId: String, status: Int, callback: MyCallback) {
        val param = tokenParams("AppModifyAutomationStatus")
        param["AutomationId"] = automationId
        param["Status"] = status
        tokenPost(param, callback, RequestCode.update_automic_task_status)
    }

    fun getTaskRunLog(msgId: String, familyId: String, callback: MyCallback) {
        val param = tokenParams("AppGetSceneAndAutomationLogs")
        param["Limit"] = 20
        param["FamilyId"] = familyId
        if (!TextUtils.isEmpty(msgId)) {
            param["MsgId"] = msgId
        }
        tokenPost(param, callback, RequestCode.get_run_task_log)
    }
    /****************************************   场景联动接口结束   *******************************************************/

}