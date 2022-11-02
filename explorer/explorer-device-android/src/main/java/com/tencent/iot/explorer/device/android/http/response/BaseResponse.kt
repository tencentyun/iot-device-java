package com.tencent.iot.explorer.device.android.http.response

import com.tencent.iot.explorer.device.android.http.utils.JsonManager


/**
 * 基础响应实体
 */
class BaseResponse {

    var code = -1
    var msg = ""
    var data = Any()

    /**
     * 请求成功
     */
    fun isSuccess(): Boolean {
        return code == 0
    }

    /**
     * 解析对应的实体
     */
    fun <T> parse(clazz: Class<T>): T? {
        return JsonManager.parseJson(data.toString(), clazz)
    }
}