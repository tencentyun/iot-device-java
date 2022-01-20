package com.tencent.iot.explorer.device.video.recorder

import android.util.Log
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType
import com.tencent.iot.explorer.device.video.recorder.utils.ByteUtils
import kotlinx.coroutines.*
import tv.danmaku.ijk.media.player.misc.IAndroidIO
import java.util.concurrent.LinkedBlockingQueue

class ReadByteIO private constructor(): CoroutineScope by MainScope(), IAndroidIO {

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
    @Volatile
    private var chaseFrameThreadStarted = false
    @Volatile
    var chaseFrame = false  // 默认不开启追帧功能
    var chaseFrameRate = 1000L // 默认的追帧扫描频率
    var chaseFrameThreshold = 6000L // 默认的触发追帧的阈值
    var playType = CallingType.TYPE_VIDEO_CALL

    // 从队列头部取数据
    private fun takeFirstWithLen(len : Int): ByteArray {
        var byteList = ByteArray(len)
        for (i in 0 until len) {
            byteList[i] = flvData.take()
        }
        Log.e(TAG, "takeFirstWithLen " + ByteUtils.bytesToHex(byteList))
        return byteList
    }

    // 队列尾部增加新的数据
    @Synchronized
    fun addLast(bytes: ByteArray): Boolean {
        var tmpList:List<Byte> = bytes.toList()
        return flvData.addAll(tmpList)
    }

    // 追帧线程
    private fun startChaseFrameThread() {
        if (!chaseFrame) return  // 直接跳出开启追帧的逻辑

        if (!chaseFrameThreadStarted) {
            Log.d(TAG, "chase frame thread started")
            launch {
                while (this@ReadByteIO != null) {
                    delay(chaseFrameRate) // 每两秒检查一次
                    if (flvData.size > chaseFrameThreshold) { // 发现缓存的数据大于阈值，触发一次追帧动作
                        flvData.clear()
                        Log.d(TAG, "${this@ReadByteIO} chase frame successed")
                    }
                }
                flvData.clear() // 当前的扫描线程不在运行的时候，清空缓存数据
            }
            chaseFrameThreadStarted = true
        }
    }

    override fun open(url: String?): Int {
        if (url == URL_SUFFIX) {
            Log.d(TAG, "recv stream opened")
            return 1
        }
        Log.e(TAG, "recv stream open failed")
        return -1
    }

    override fun read(buffer: ByteArray?, size: Int): Int {
        var readLen = size
        if (playType == CallingType.TYPE_AUDIO_CALL) { // 音频
            readLen = 256
        } else if (playType == CallingType.TYPE_VIDEO_CALL) { // 视频，优化出图时间，可以在此继续缩小缓存 buffer
            readLen = 256  //512
        } else {  // 未知类型
            readLen = 256
        }
        var tmpBytes = takeFirstWithLen(readLen) // 阻塞式读取
        System.arraycopy(tmpBytes, 0, buffer, 0, readLen)
        startChaseFrameThread() // 只有在取到第一段数据以后，才会开启追帧功能，避免漏掉 flv 的文件头
        return readLen
    }

    override fun seek(offset: Long, whence: Int): Long {
        return 0
    }

    override fun close(): Int {
        cancel()
        return 0
    }

    fun reset() {
        flvData.clear()
        instance = null
    }
}