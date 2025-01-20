package com.tencent.iot.explorer.device.android.http.retrofit.adapter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type


class StringCallAdapterFactory private constructor() : CallAdapter.Factory() {

    private val LOG: Logger = LoggerFactory.getLogger(StringCallAdapterFactory::class.java)

    override fun get(
        type: Type?,
        array: Array<out Annotation>?,
        retrofit: Retrofit?
    ): CallAdapter<Any, String>? {
        if (type == String::class.java)
            return StringCallAdapter()
        return null
    }

    inner class StringCallAdapter : CallAdapter<Any, String> {
        override fun adapt(call: Call<Any>): String {
            try {
                return call.execute().body().toString()
            } catch (e: Exception) {
                LOG.error(e.message)
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