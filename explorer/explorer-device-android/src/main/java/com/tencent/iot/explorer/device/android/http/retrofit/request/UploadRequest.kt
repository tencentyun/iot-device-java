package com.tencent.iot.explorer.device.android.http.retrofit.request


import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

/**
 * 上传图片接口
 */
interface UploadRequest {

    @Multipart
    @POST
    fun uploadImage(@Url url: String, @Part body: MultipartBody.Part): Call<String>

    @Multipart
    @POST
    fun uploadImages(@Url url: String, @PartMap files: HashMap<String, RequestBody>): Call<String>

}
