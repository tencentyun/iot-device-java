package com.tencent.iot.explorer.device.video.recorder

import android.util.Log
import com.tencent.iot.explorer.device.video.recorder.utils.ByteUtils
import tv.danmaku.ijk.media.player.misc.IAndroidIO
import java.util.concurrent.LinkedBlockingQueue

class ReadByteIO private constructor(): IAndroidIO {

    companion object {
        private var instance: ReadByteIO? = null
        var URL_SUFFIX = "recv_data_online"

        @Synchronized
        fun getInstance(): ReadByteIO {
            instance?.let {
                return it
            }
            instance = ReadByteIO()
            return instance!!
        }
    }

    private var TAG = ReadByteIO::class.java.simpleName
    private var flvData = LinkedBlockingQueue<Byte>()  // 内存队列，用于缓存获取到的裸流数据

    private fun takeFirstWithLen(len : Int): ByteArray {  // 取 byte 数据用于界面渲染
        var byteList = ByteArray(len)
        for (i in 0 until len) {
            byteList[i] = flvData.take()
        }
        Log.e(TAG, "takeFirstWithLen " + ByteUtils.bytesToHex(byteList))
        return byteList
    }

    @Synchronized
    fun addLast(bytes: ByteArray): Boolean {
        var tmpList:List<Byte> = bytes.toList()
        Log.e(TAG, "tmpList size " + tmpList.size)
        return flvData.addAll(tmpList)
    }

    override fun open(url: String?): Int {
        if (url == URL_SUFFIX) {
            Log.d(TAG, "recv stream opened")
            return 1
        }
        Log.d(TAG, "recv stream open failed")
        return -1
    }

    override fun read(buffer: ByteArray?, size: Int): Int {
        var tmpBytes = takeFirstWithLen(size) // 阻塞式读取
        System.arraycopy(tmpBytes, 0, buffer, 0, size)
        return size
    }

    override fun seek(offset: Long, whence: Int): Long {
        return 0
    }

    override fun close(): Int {
        Log.d(TAG, "flvData cleared")
        flvData.clear()
        return 0
    }
}