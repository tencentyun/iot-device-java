package com.tencent.iot.explorer.device.central.entity



/**
 *  家庭响应实体
 */
class FamilyListResponse {

    var RequestId = ""
    var FamilyList = arrayListOf<FamilyEntity>()
    var Total = 0

}