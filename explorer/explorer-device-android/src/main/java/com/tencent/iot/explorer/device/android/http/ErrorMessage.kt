package com.tencent.iot.explorer.device.android.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.tencent.iot.explorer.device.android.http.utils.JsonManager

class ErrorMessage {

    var Code = ""

    var Message = ""

    companion object {
        //app数据
        fun parseErrorMessage(data: String): ErrorMessage {

            val jsonObject = JSON.parse(data) as JSONObject
            val errorMessage: ErrorMessage
            errorMessage = if (jsonObject.containsKey("Error")) {
                JsonManager.parseJson(jsonObject.getString("Error"), ErrorMessage::class.java)
            } else {
                JsonManager.parseJson(data, ErrorMessage::class.java)
            }
            return errorMessage
        }
    }

}