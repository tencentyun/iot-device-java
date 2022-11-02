package com.tencent.iot.explorer.device.android.http.retrofit.request


import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * POST请求接口
 */
interface PostRequest {

    @FormUrlEncoded
    @POST
    fun post(@Url url: String, @FieldMap params: HashMap<String, Any>): @JvmSuppressWildcards Call<String>

}
