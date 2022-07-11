package com.tencent.iot.explorer.device.android.http.utils

import android.util.Base64
import com.tencent.iot.explorer.device.android.utils.TXLog
import java.nio.charset.Charset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * SDK签名工具
 */
object SignatureUtil {

    private val TAG = "SignatureUtil"

    /**
     * 签名数据
     */
    fun format(params: Map<String, Any>): String {
        val sb = StringBuilder()
        params.toSortedMap().forEach {
            TXLog.d(TAG, "${it.key}=${it.value}")
            sb.append(it.key).append("=").append(it.value).append("&")
        }
        return sb.substring(0, sb.lastIndex)
    }

    fun signature(sign: String, secret: String): String {
        TXLog.d(TAG, "待签名=$sign")
        val secretKey = SecretKeySpec(secret.toByteArray(Charset.forName("utf-8")), "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(secretKey)
        val result = mac.doFinal(sign.toByteArray(Charset.forName("utf-8")))
        return Base64.encodeToString(result, Base64.DEFAULT).replace("\n", "")
    }

}