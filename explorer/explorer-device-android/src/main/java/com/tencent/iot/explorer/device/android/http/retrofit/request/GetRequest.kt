package com.tencent.iot.explorer.device.android.http.retrofit.request


import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * GET请求接口
 */
interface GetRequest {

    @GET
    fun get(@Url url: String): Call<String>

    @GET
    fun get(@Url url: String, @QueryMap params: HashMap<String, Any>): @JvmSuppressWildcards Call<String>

}
