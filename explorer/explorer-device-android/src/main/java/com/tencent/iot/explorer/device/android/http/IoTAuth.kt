package com.tencent.iot.explorer.device.android.http


/**
 * SDK授权认证管理
 */
object IoTAuth {
    private var accessToken = ""
    private var expiredTime = 0L //秒

    fun init(token: String, expiredTime: Long) {
        this.accessToken = token
        this.expiredTime = expiredTime
    }

    fun getToken(): String {
        if (System.currentTimeMillis() / 1000 >= expiredTime) {
            return ""
        }
        return accessToken
    }

    fun getExpiredTime(): Long {
        return expiredTime
    }
}