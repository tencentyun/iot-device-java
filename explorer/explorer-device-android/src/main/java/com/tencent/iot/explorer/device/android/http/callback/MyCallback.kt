package com.tencent.iot.explorer.device.android.http.callback

import com.tencent.iot.explorer.device.android.http.response.BaseResponse


/**
 *  响应回调
 */
interface MyCallback {

    fun fail(msg: String?, reqCode: Int)

    fun success(response: BaseResponse, reqCode: Int)

}