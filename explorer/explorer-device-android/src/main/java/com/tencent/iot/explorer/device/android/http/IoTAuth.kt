package com.tencent.iot.explorer.device.android.http

import android.text.TextUtils

/**
 * SDK授权认证管理
 */
object IoTAuth {
    private var accessToken = ""

    fun init(token: String) {
        if (TextUtils.isEmpty(token)) {
            throw Exception("Access token can not be empty")
        }
        this.accessToken = token
    }

    fun getToken(): String {
        return accessToken
    }
}