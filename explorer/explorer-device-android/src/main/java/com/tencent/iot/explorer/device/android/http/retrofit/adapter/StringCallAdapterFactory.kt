package com.tencent.iot.explorer.device.android.http.retrofit.adapter

import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type
import retrofit2.Call
import java.lang.Exception


class StringCallAdapterFactory private constructor() : CallAdapter.Factory() {
    override fun get(
        type: Type?,
        array: Array<out Annotation>?,
        retrofit: Retrofit?
    ): CallAdapter<*>? {
        if (type == String::class.java)
            return StringCallAdapter()
        return null
    }

    inner class StringCallAdapter : CallAdapter<String> {
        override fun <R : Any?> adapt(call: Call<R>?): String {
            try {
                return call?.execute()?.body().toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        override fun responseType(): Type {
            return String::class.java
        }
    }

    companion object {
        fun create(): StringCallAdapterFactory {
            return StringCallAdapterFactory()
        }
    }
}