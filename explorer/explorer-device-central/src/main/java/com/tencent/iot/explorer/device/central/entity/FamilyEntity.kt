package com.tencent.iot.explorer.device.central.entity


/**
 * 家庭实体
 */
class FamilyEntity {
    var FamilyId = ""
    var FamilyType = 0
    var FamilyName = ""
    var Address = ""
    var showAddress = false
    var RoomsNum = ""
    var Role = 0   // 1:自己是管理员  0：普通成员
    var CreateTime = 0L
    var UpdateTime = 0L
}