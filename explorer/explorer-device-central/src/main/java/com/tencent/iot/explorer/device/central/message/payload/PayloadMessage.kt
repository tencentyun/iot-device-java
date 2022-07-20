package com.tencent.iot.explorer.device.central.message.payload

class PayloadMessage {

    var action = ""
    var params: Param? = null
    var push = false

    class Param {
        var Time = ""
        var Type = ""
        var SubType = ""
        var Topic = ""
        var Payload = ""
        var Seq = 0L
        var DeviceId = ""
    }

}