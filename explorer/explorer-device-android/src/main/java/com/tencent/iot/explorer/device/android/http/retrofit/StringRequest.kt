package com.tencent.iot.explorer.device.android.http.retrofit

import com.tencent.iot.explorer.device.android.http.retrofit.adapter.StringCallAdapterFactory
import com.tencent.iot.explorer.device.android.http.retrofit.converter.StringConverterFactory
import com.tencent.iot.explorer.device.android.http.retrofit.request.GetRequest
import com.tencent.iot.explorer.device.android.http.retrofit.request.JsonRequest
import com.tencent.iot.explorer.device.android.http.retrofit.request.PostRequest
import com.tencent.iot.explorer.device.android.http.retrofit.request.UploadRequest
import com.tencent.iot.explorer.device.android.utils.TXLog
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import java.io.File

/**
 * 请求返回String类型的响应数据
 */
class StringRequest private constructor() {

    private val TAG = "StringRequest"

    private object StringRequestHolder {
        val holder = StringRequest()
    }

    companion object {
        private const val TAG = "StringRequest"
        val instance = StringRequestHolder.holder
    }

    var okHttpClient: OkHttpClient? = null
    private var host = ""
    private lateinit var retrofit: Retrofit

    /**
     * 初始化连接
     */
    fun init(host: String) {
        if (this.host != host) {
            this.host = host
            connect(host)
        }
    }

    /**
     * 重连
     */
    fun reconnect() {
        if (ping("www.baidu.com")) {
            connect(this.host)
        }
    }

    /**
     * 连接
     */
    private fun connect(host: String) {
        val builder = Retrofit.Builder().baseUrl(host+"/")
        if (okHttpClient != null) {
            builder.client(okHttpClient)
        }
        retrofit = builder.addCallAdapterFactory(StringCallAdapterFactory.create())
            .addConverterFactory(StringConverterFactory.create()).build()
    }

    /**
     * 当host发生改变的时候connect，避免重复connect
     */
    private fun connectOnHostChanged(host: String) {
        var currentHost = retrofit.baseUrl().url().toString()
        currentHost = currentHost.substring(0, currentHost.length-1)
        if (currentHost != host) {
            connect(host)
        }
    }

    /**
     * get请求
     */
    fun get(host: String, path: String, callback: com.tencent.iot.explorer.device.android.http.retrofit.Callback, reqCode: Int) {
        connectOnHostChanged(host) // 避免重复connect
        retrofit.create(GetRequest::class.java).get(path).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>?, throwable: Throwable?) {
                call?.cancel()
                callback.fail(throwable?.message, reqCode)
                TXLog.d(TAG, "请求失败：${throwable?.message}")
            }

            override fun onResponse(call: Call<String>?, response: retrofit2.Response<String>) {
                if (response.isSuccessful) {
                    TXLog.d(TAG, "请求成功：${response.body()}")
                    callback.success(response.body(), reqCode)
                } else {
                    TXLog.d(TAG, "请求成功：${response.errorBody()}")
                    callback.success(response.errorBody().string(), reqCode)
                }
            }
        })
    }

    /**
     * get请求
     */
    fun get(
        host: String,
        path: String,
        params: HashMap<String, Any>,
        callback: com.tencent.iot.explorer.device.android.http.retrofit.Callback,
        reqCode: Int
    ) {
        connectOnHostChanged(host) // 避免重复connect
        retrofit.create(GetRequest::class.java).get(path, params)
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, throwable: Throwable?) {
                    call?.cancel()
                    callback.fail(throwable?.message, reqCode)
                    TXLog.d(TAG, "请求失败：${throwable?.message}")
                }

                override fun onResponse(call: Call<String>?, response: retrofit2.Response<String>) {
                    if (response.isSuccessful) {
                        TXLog.d(TAG, "请求成功：${response.body()}")
                        callback.success(response.body(), reqCode)
                    } else {
                        TXLog.d(TAG, "请求成功：${response.errorBody()}")
                        callback.success(response.errorBody().string(), reqCode)
                    }
                }

            })
    }

    /**
     * post请求
     */
    fun post(
        host: String,
        path: String,
        params: HashMap<String, Any>,
        callback: com.tencent.iot.explorer.device.android.http.retrofit.Callback,
        reqCode: Int
    ) {
        connectOnHostChanged(host) // 避免重复connect
        retrofit.create(PostRequest::class.java).post(path, params)
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, throwable: Throwable?) {
                    call?.cancel()
                    callback.fail(throwable?.message, reqCode)
                    TXLog.d(TAG, "请求失败：${throwable?.message}")
                }

                override fun onResponse(call: Call<String>?, response: retrofit2.Response<String>) {
                    if (response.isSuccessful) {
                        TXLog.d(TAG, "请求成功：${response.body()}")
                        callback.success(response.body(), reqCode)
                    } else {
                        TXLog.d(TAG, "请求成功：${response.errorBody()}")
                        callback.success(response.errorBody().string(), reqCode)
                    }
                }

            })
    }

    /**
     * post请求
     */
    fun postJson(
        host: String,
        path: String,
        json: String,
        callback: com.tencent.iot.explorer.device.android.http.retrofit.Callback,
        reqCode: Int
    ) {
        connectOnHostChanged(host) // 避免重复connect
        retrofit.create(JsonRequest::class.java).postJson(path, json)
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, throwable: Throwable?) {
                    call?.cancel()
                    callback.fail(throwable?.message, reqCode)
                    TXLog.d(TAG, "请求失败：${throwable?.message}")
                }

                override fun onResponse(call: Call<String>?, response: retrofit2.Response<String>) {
                    if (response.isSuccessful) {
                        TXLog.d(TAG, "请求成功：${response.body()}")
                        callback.success(response.body(), reqCode)
                    } else {
                        TXLog.d(TAG, "请求成功：${response.errorBody()}")
                        callback.success(response.errorBody().string(), reqCode)
                    }
                }

            })
    }

    /**
     * 上传图片
     */
    fun uploadImages(
        host: String,
        url: String,
        files: List<File>,
        callback: com.tencent.iot.explorer.device.android.http.retrofit.Callback,
        reqCode: Int
    ) {
        val builder = Retrofit.Builder().baseUrl(host)
        if (okHttpClient != null) {
            builder.client(okHttpClient)
        }

        val params = HashMap<String, RequestBody>()
        files.forEachIndexed { i, file ->
            //            val requestBody = RequestBody.create(MediaType.parse("image/png"), file)
            val requestBody = RequestBody.create(MediaType.parse("application/otcet-stream"), file)
//            val requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
//            params["file" + i + "\";filename=\"" + file.name] = requestBody
            params["file"] = requestBody
//            params["file$i\";filename=\"${file.name}"] = requestBody

        }
        val retrofit = builder.addCallAdapterFactory(StringCallAdapterFactory.create())
            .addConverterFactory(StringConverterFactory.create()).build()
        retrofit.create(UploadRequest::class.java).uploadImages(url, params)
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, throwable: Throwable?) {
                    call?.cancel()
                    callback.fail(throwable?.message, reqCode)
                    TXLog.d(TAG, "请求失败：${throwable?.message}")
                }

                override fun onResponse(call: Call<String>?, response: retrofit2.Response<String>) {
                    if (response.isSuccessful) {
                        TXLog.d(TAG, "请求成功：${response.body()}")
                        callback.success(response.body(), reqCode)
                    } else {
                        TXLog.d(TAG, "请求成功：${response.errorBody()}")
                        callback.success(response.errorBody().string(), reqCode)
                    }
                }

            })
    }

    /**
     * 上传图片
     */
    fun uploadImage(
        host: String,
        url: String,
        file: File,
        callback: com.tencent.iot.explorer.device.android.http.retrofit.Callback,
        reqCode: Int
    ) {
        val builder = Retrofit.Builder().baseUrl(host)
        if (okHttpClient != null) {
            builder.client(okHttpClient)
        }

        val params = HashMap<String, RequestBody>()
        val requestBody = RequestBody.create(MediaType.parse("application/otcet-stream"), file)
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        val retrofit = builder.addCallAdapterFactory(StringCallAdapterFactory.create())
            .addConverterFactory(StringConverterFactory.create()).build()
        retrofit.create(UploadRequest::class.java).uploadImage(url, part)
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, throwable: Throwable?) {
                    call?.cancel()
                    callback.fail(throwable?.message, reqCode)
                    TXLog.d(TAG, "请求失败：${throwable?.message}")
                }

                override fun onResponse(call: Call<String>?, response: retrofit2.Response<String>) {
                    if (response.isSuccessful) {
                        TXLog.d(TAG, "请求成功：${response.body()}")
                        callback.success(response.body(), reqCode)
                    } else {
                        TXLog.d(TAG, "请求成功：${response.errorBody()}")
                        callback.success(response.errorBody().string(), reqCode)
                    }
                }

            })
    }

    /**
     * ping外网
     */
    private fun ping(host: String): Boolean {
        var isSuccess: Boolean
        var process: Process? = null
        try {
            process = Runtime.getRuntime()
                .exec("/system/bin/ping -c 1 $host")
            isSuccess = (process.waitFor() == 0)
        } catch (e: Exception) {
            isSuccess = false
            e.printStackTrace()
            process?.destroy()
        } finally {
            process?.destroy()
        }
        return isSuccess
    }

}