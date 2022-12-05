package com.tencent.iot.explorer.device.central.entity

import android.text.TextUtils
import com.alibaba.fastjson.JSON
import java.util.concurrent.CopyOnWriteArrayList

open class DeviceEntity {

    var CategoryId = 0
    var DeviceId = ""
    var ProductId = ""
    var DeviceName = ""
    var AliasName = ""
    var UserID = ""
    var FamilyId = ""
    var RoomId = ""
    var IconUrl = ""
    var DeviceType = ""
    var CreateTime = 0L
    var UpdateTIme = 0L
    var FromUserID = ""
    var deviceDataList = CopyOnWriteArrayList<DeviceDataEntity>()

    //在线状态
    var online = 0
    //共享设备
    var shareDevice = false

    fun getAlias(): String {
        return if (TextUtils.isEmpty(AliasName)) {
            DeviceName
        } else {
            AliasName
        }
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }

}