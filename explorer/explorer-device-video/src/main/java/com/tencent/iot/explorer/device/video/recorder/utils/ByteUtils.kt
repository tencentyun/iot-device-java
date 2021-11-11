package com.tencent.iot.explorer.device.video.recorder.utils

class ByteUtils {

    companion object {
        /**
         * 字节数组转 Hex
         * @param bytes 字节数组
         * @return Hex
         */
        fun bytesToHex(bytes: ByteArray?): String? {
            val sb = StringBuffer()
            if (bytes != null && bytes.size > 0) {
                for (i in bytes.indices) {
                    val hex = byteToHex(bytes[i])
                    sb.append(hex)
                }
            }
            return sb.toString()
        }

        fun byteToHex(b: Byte): String? {
            var hexString = Integer.toHexString(b.toInt() and 0xFF)
            //由于十六进制是由0~9、A~F来表示1~16，所以如果Byte转换成Hex后如果是<16,就会是一个字符（比如A=10），通常是使用两个字符来表示16进制位的,
            //假如一个字符的话，遇到字符串11，这到底是1个字节，还是1和1两个字节，容易混淆，如果是补0，那么1和1补充后就是0101，11就表示纯粹的11
            if (hexString.length < 2) {
                hexString = java.lang.StringBuilder(0.toString()).append(hexString).toString()
            }
            return hexString.toUpperCase()
        }
    }
}