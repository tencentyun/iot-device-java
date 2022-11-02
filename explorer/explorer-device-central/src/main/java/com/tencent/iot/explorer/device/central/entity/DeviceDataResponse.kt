package com.tencent.iot.explorer.device.central.entity

import com.alibaba.fastjson.JSON

/**
 * 设备当前数据
 */
class DeviceDataResponse {

    var Data = ""
    var RequestId = ""

    fun parseList(): List<DeviceDataEntity> {
        val list = ArrayList<DeviceDataEntity>()
        val obj = JSON.parseObject(Data)
        obj?.keys?.forEach {
            val entity = DeviceDataEntity()
            entity.id = it
            entity.value = obj.getJSONObject(it).getString("Value")
            entity.lastUpdate = obj.getJSONObject(it).getLong("LastUpdate")
            list.add(entity)
        }
        return list
    }

}