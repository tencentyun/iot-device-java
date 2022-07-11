package com.tencent.iot.explorer.device.android.http.retrofit.request


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * json请求接口
 */
interface JsonRequest {

    @Headers("Content-type:application/json;charset=UTF-8")
    @POST
    fun postJson(@Url url: String, @Body param: String): Call<String>
}
