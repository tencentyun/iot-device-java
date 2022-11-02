package com.tencent.iot.explorer.device.android.http.retrofit.converter


import com.tencent.iot.explorer.device.android.utils.TXLog
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

import java.io.IOException

/**
 * 自定义RequestBodyConverter
 */
class StringRequestBodyConverter : Converter<String, RequestBody> {

    private val TAG = "StringRequestBodyConverter"

    @Throws(IOException::class)
    override fun convert(s: String): RequestBody {
        TXLog.e(TAG, "请求数据json：$s")
        return RequestBody.create(MEDIA_TYPE, s)
    }

    companion object {

        private val MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8")
    }
}
