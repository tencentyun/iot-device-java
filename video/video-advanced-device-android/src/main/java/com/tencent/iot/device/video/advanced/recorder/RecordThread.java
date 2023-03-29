package com.tencent.iot.device.video.advanced.recorder;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.alibaba.fastjson.JSONObject;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.device.video.advanced.recorder.listener.OnEncodeListener;
import com.tencent.iot.device.video.advanced.recorder.param.AudioEncodeParam;
import com.tencent.iot.device.video.advanced.recorder.param.CameraParam;
import com.tencent.iot.device.video.advanced.recorder.param.MicParam;
import com.tencent.iot.device.video.advanced.recorder.param.RecordParam;
import com.tencent.iot.device.video.advanced.recorder.param.RecordThreadParam;
import com.tencent.iot.device.video.advanced.recorder.param.VideoEncodeParam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class RecordThread extends Thread {

    private String TAG = RecordThread.class.getSimpleName();
    // 编解码器，混合器
    private MediaMuxer mediaMuxer;
    private MediaCodec audioCodec;
    private MediaCodec videoCodec;
    private AudioRecord audioRecord;
    private int bufferSizeInBytes;

    // 参数设置
    private RecordParam recordParam;
    private MicParam micParam;
    private AudioEncodeParam audioEncodeParam;
    private CameraParam cameraParam;
    private VideoEncodeParam videoEncodeParam;
    private OnEncodeListener encodeListener;

    private GLThread glThread;
    private volatile Surface surface;
    private boolean isStopRecord = false;   // 主动停止记录
    private boolean isCancelRecord = false;  // 取消记录
    private boolean storeMP4 = false;  // 是否保存混合音频视频的 MP4 文件
    private volatile boolean startStore = false; // 临时保存，true 开始保存
    private String storePath = "";
    private String audioName = "";
    private String videoName = "";
    private volatile FileOutputStream storeAudioDataStream;
    private volatile FileOutputStream storeVideoDataStream;
    private volatile boolean hasIDR = false;
    private File videoDataFile;
    private File audioDataFile;
    // 记录视频裸流的临时文件，调试使用
    private String path = "/mnt/sdcard/videoTest.flv";
    private File videoTmpFile = new File(path);
    private volatile FileOutputStream storeVideoStream;
    // 记录视频裸流的h264临时文件，调试使用
    private String h264path = "/mnt/sdcard/tmpVideo.h264";
    private File h264TmpFile = new File(h264path);
    private volatile FileOutputStream storeH264VideoStream;
    private volatile long seq = 0L;
    private volatile long audioSeq = 0L;

    // 采样频率对照表
    private static Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap<>();

    static {
        samplingFrequencyIndexMap.put(96000, 0);
        samplingFrequencyIndexMap.put(88200, 1);
        samplingFrequencyIndexMap.put(64000, 2);
        samplingFrequencyIndexMap.put(48000, 3);
        samplingFrequencyIndexMap.put(44100, 4);
        samplingFrequencyIndexMap.put(32000, 5);
        samplingFrequencyIndexMap.put(24000, 6);
        samplingFrequencyIndexMap.put(22050, 7);
        samplingFrequencyIndexMap.put(16000, 8);
        samplingFrequencyIndexMap.put(12000, 9);
        samplingFrequencyIndexMap.put(11025, 10);
        samplingFrequencyIndexMap.put(8000, 11);
    }

    // 先设置保存路径再设置该参数才会生效
    protected int startStore(boolean startStore) {
        if (!startStore) { // 结束录像
            this.startStore = startStore;
            try {
                if (storeAudioDataStream != null) {
                    storeAudioDataStream.close();
                }
                if (storeVideoDataStream != null) {
                    storeVideoDataStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                storeAudioDataStream = null;
                storeVideoDataStream = null;
            }
            return ErrorCode.SUCCESS;
        }

        // 开始录像
        if (!storePath.endsWith("/")) {
            storePath = storePath + "/";
        }

        audioDataFile = new File(storePath + audioName);
        videoDataFile = new File(storePath + videoName);
        if (audioDataFile.exists()) {
            audioDataFile.delete();
        }
        if (videoDataFile.exists()) {
            videoDataFile.delete();
        }

        try {
            hasIDR = false;
            audioDataFile.createNewFile();
            videoDataFile.createNewFile();
            storeAudioDataStream = new FileOutputStream(audioDataFile, true);
            storeVideoDataStream = new FileOutputStream(videoDataFile, true);
            this.startStore = startStore;
            return ErrorCode.SUCCESS;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_FILE_NOT_FOUND;
        } catch (IOException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_IO;
        }
    }

    protected boolean isStartStore() {
        return this.startStore;
    }

    protected void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    protected String getAudioName() {
        return this. audioName;
    }

    protected void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    protected String getVideoName() {
        return this.videoName;
    }

    protected void setStorePath(String storePath) {
         this.storePath = storePath;
    }

    protected String getStorePath() {
        return this.storePath;
    }

    // 记录过程回调
    private OnRecordListener onRecordListener;
    public void setOnRecordListener(OnRecordListener onRecordListener) {
        this.onRecordListener = onRecordListener;
    }

    private void onRecordStart() {
        if (onRecordListener != null) onRecordListener.onRecordStart();
    }

    private void onRecordTime(long time) {
        if (onRecordListener != null) onRecordListener.onRecordTime(time);
    }

    private void onRecordComplete(String path) {
        stopGLThread();
        if (onRecordListener != null) {
            onRecordListener.onRecordComplete(path);
        }
    }

    private void onRecordCancel() {
        if (onRecordListener != null) onRecordListener.onRecordCancel();
    }

    private void onRecordError(Exception e) {
        if (onRecordListener != null) onRecordListener.onRecordError(e);
    }

    private void stopGLThread() {
        if (glThread != null) {
            glThread.onDestroy();
            glThread = null;
        }
    }

    public RecordThread(RecordThreadParam recordThreadParam, OnEncodeListener listener) {
        this(recordThreadParam.getRecordParam(), recordThreadParam.getMicParam(),
                recordThreadParam.getAudioEncodeParam(), recordThreadParam.getCameraParam(),
                recordThreadParam.getVideoEncodeParam(), listener);
    }

    private RecordThread(RecordParam recordParam, MicParam micParam, AudioEncodeParam audioEncodeParam,
                         CameraParam cameraParam, VideoEncodeParam videoEncodeParam, OnEncodeListener listener) {
        this.recordParam = recordParam;
        this.micParam = micParam;
        this.audioEncodeParam = audioEncodeParam;
        this.cameraParam = cameraParam;
        this.videoEncodeParam = videoEncodeParam;
        this.storeMP4 = recordParam.isStoreMP4File();
        this.encodeListener = listener;
        Log.d(TAG, "init RecordThread with storeMP4 " + storeMP4);
        glThread = new GLThread(this.cameraParam, this.videoEncodeParam);
        initMuxer();
        initAudio();
        initVideo();
    }

    private void initMuxer() {
        try {
            mediaMuxer = new MediaMuxer(recordParam.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            mediaMuxer = null;
        }
    }

    private void initAudio() {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat());
        audioRecord = new AudioRecord(micParam.getAudioSource(), micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat(), bufferSizeInBytes);
        try {
            audioCodec = MediaCodec.createEncoderByType(audioEncodeParam.getMime());
            MediaFormat format = MediaFormat.createAudioFormat(audioEncodeParam.getMime(), micParam.getSampleRateInHz(), 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioEncodeParam.getBitRate());
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioEncodeParam.getMaxInputSize());
            audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioRecord = null;
            audioCodec = null;
        }
    }

    private void initVideo() {
        try {
            Log.d(TAG, "initVideo videoEncodeParam " + JSONObject.toJSONString(videoEncodeParam));

            videoCodec = MediaCodec.createEncoderByType(videoEncodeParam.getMime());
            MediaFormat format = MediaFormat.createVideoFormat(videoEncodeParam.getMime(), videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);//MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, videoEncodeParam.getFrameRate());
            format.setInteger(MediaFormat.KEY_BIT_RATE, 50000);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoEncodeParam.getiFrameInterval());
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);


            //设置压缩等级  默认是 baseline
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
            }

            videoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = videoCodec.createInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
            videoCodec = null;
            surface = null;
        }
        glThread.setSurface(surface);
    }

    void cancelRecord() {
        isCancelRecord = true;
        stopSaveTmpFile(true);
        try {
            join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (glThread != null) {
            glThread.onDestroy();
            glThread = null;
        }
    }

    void stopRecord() {
        isStopRecord = true;
        stopSaveTmpFile(false);
        try {
            join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (glThread != null) {
            glThread.onDestroy();
            glThread = null;
        }
    }

    void stopSaveTmpFile(boolean clean) {
        try {
            if (storeVideoStream != null) {
                storeVideoStream.close();
            }
            if (storeH264VideoStream != null) {
                storeH264VideoStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (clean) {
            videoTmpFile.deleteOnExit();
            h264TmpFile.deleteOnExit();
        }
    }

    private void release() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioCodec != null) {
            audioCodec.stop();
            audioCodec.release();
            audioCodec = null;
        }

        if (videoCodec != null) {
            videoCodec.stop();
            videoCodec.release();
            videoCodec = null;
        }

        if (mediaMuxer != null && storeMP4) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }

        if (isCancelRecord) {
            onRecordCancel();
            return;
        }

        if (isStopRecord) {
            onRecordComplete(recordParam.getPath());
        }
    }

    @Override
    public void run() {
        super.run();
        record();
    }

    private void sendOriAudioData(ByteBuffer outputBuffer) {
        Log.e(TAG, "sendOriAudioData");
        try {
            byte[] bytes = new byte[outputBuffer.remaining()];
            outputBuffer.get(bytes, 0, bytes.length);
            byte[] dataBytes = new byte[bytes.length + 7];
            System.arraycopy(bytes, 0, dataBytes, 7, bytes.length);
            addADTStoPacket(dataBytes, dataBytes.length);
            if (dataBytes != null /*&& storeVideoStream != null*/) {
                if (startStore && storeAudioDataStream != null) {
                    storeAudioDataStream.write(dataBytes);
                    storeAudioDataStream.flush();
                }

                if (isStopRecord || isCancelRecord) return;
//                storeVideoStream.write(dataBytes);
//                storeVideoStream.flush();
                if (encodeListener != null) {
                    encodeListener.onAudioEncoded(dataBytes, System.currentTimeMillis(), audioSeq);
                } else {
                    Log.e(TAG, "Encode listener is null, please set encode listener.");
                }
                audioSeq++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  // AAC LC
        int chanCfg = 1;  // CPE
        int freqIdx = samplingFrequencyIndexMap.get(micParam.getSampleRateInHz());
        // filled in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private void storeOriVideoData(ByteBuffer outputBuffer, MediaCodec.BufferInfo videoInfo) {
        Log.e(TAG, "storeOriVideoData videoInfo.flags：" + videoInfo.flags);
        if (recordParam != null &&
                recordParam.getRecorderType() != CallingType.TYPE_VIDEO_CALL) {
            return;
        }
        Log.e(TAG, "send video info");
        try {
            byte[] bytes = new byte[outputBuffer.remaining()];
            outputBuffer.get(bytes, 0, bytes.length);
            if (bytes != null /*&& storeVideoStream != null*/) {
                if (isStopRecord || isCancelRecord) return;
                if (videoInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {  // I 帧的处理逻辑
                    ByteBuffer spsb = videoCodec.getOutputFormat().getByteBuffer("csd-0");
                    byte[] sps = new byte[spsb.remaining()];
                    spsb.get(sps, 0, sps.length);
                    ByteBuffer ppsb = videoCodec.getOutputFormat().getByteBuffer("csd-1");
                    byte[] pps = new byte[ppsb.remaining()];
                    ppsb.get(pps, 0, pps.length);

                    byte[] dataBytes = new byte[sps.length + pps.length + bytes.length];
                    System.arraycopy(sps, 0, dataBytes, 0, sps.length);
                    System.arraycopy(pps, 0, dataBytes, sps.length, pps.length);
                    System.arraycopy(bytes, 0, dataBytes, pps.length + sps.length, bytes.length);

                    if (startStore && storeVideoDataStream != null) {
                        storeVideoDataStream.write(dataBytes);
                        storeVideoDataStream.flush();
                        hasIDR = true;
                    }
                    if (storeH264VideoStream != null) {
                        storeH264VideoStream.write(bytes);
                        storeH264VideoStream.flush();
                    }
                    if (encodeListener != null) {
                        encodeListener.onVideoEncoded(dataBytes, System.currentTimeMillis(), audioSeq);
                    } else {
                        Log.e(TAG, "Encode listener is null, please set encode listener.");
                    }
                } else {

                    if (startStore && storeVideoDataStream != null && hasIDR) { // 等待存在 IDR 帧以后，再开始添加 P 帧
                        storeVideoDataStream.write(bytes);
                        storeVideoDataStream.flush();
                    }
                    if (storeH264VideoStream != null) {
                        storeH264VideoStream.write(bytes);
                        storeH264VideoStream.flush();
                    }
                    if (encodeListener != null) {
                        encodeListener.onVideoEncoded(bytes, System.currentTimeMillis(), audioSeq);
                    } else {
                        Log.e(TAG, "Encode listener is null, please set encode listener.");
                    }
                }
                seq++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restartKeepOriData() {
        try {
            if (videoTmpFile.exists()) {
                videoTmpFile.deleteOnExit();
            }
            videoTmpFile.createNewFile();
            storeVideoStream = new FileOutputStream(videoTmpFile, true);
            if (h264TmpFile.exists()) {
                h264TmpFile.delete();
            }
            h264TmpFile.createNewFile();
            storeH264VideoStream = new FileOutputStream(h264TmpFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void record() {
        // 存储当前文件，容器混合器为空，不进行后续流程
        if (storeMP4 && mediaMuxer == null) return;

        if (audioCodec == null || videoCodec == null) {
            onRecordError(new IllegalArgumentException("widget is null"));
            return;
        }
//        restartKeepOriData();

        boolean isStartMuxer = false; // 合成是否开始
        seq = 0L;
        isStopRecord = false;
        long audioPts = 0;
        long videoPts = 0;
        int audioTrackIndex = -1;
        int videoTrackIndex = -1;

        onRecordStart();
        audioRecord.startRecording();
        audioCodec.start();
        videoCodec.start();
        glThread.start();
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
        while (true) {
            if (isStopRecord || isCancelRecord) {
                release();
                break;
            }

            // 将 AudioRecord 获取的 PCM 原始数据送入编码器
            int audioInputBufferId = audioCodec.dequeueInputBuffer(0);
            if (audioInputBufferId >= 0) {
                ByteBuffer inputBuffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    inputBuffer = audioCodec.getInputBuffer(audioInputBufferId);
                } else {
                    inputBuffer = audioCodec.getInputBuffers()[audioInputBufferId];
                }
                int readSize = -1;
                if (inputBuffer != null) readSize = audioRecord.read(inputBuffer, bufferSizeInBytes);
                if (readSize >= 0) audioCodec.queueInputBuffer(audioInputBufferId, 0, readSize, System.nanoTime() / 1000, 0);
            }

            // 获取从 surface 获取数据，写入 Muxer
            int videoOutputBufferId = videoCodec.dequeueOutputBuffer(videoInfo, 0);
            if (videoOutputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (storeMP4) {
                    videoTrackIndex = mediaMuxer.addTrack(videoCodec.getOutputFormat());
                    if (audioTrackIndex != -1 && !isStartMuxer) {
                        isStartMuxer = true;
                        mediaMuxer.start();
                    }
                }

            } else if (videoOutputBufferId >= 0) {
                ByteBuffer outputBuffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = videoCodec.getOutputBuffer(videoOutputBufferId);
                } else {
                    outputBuffer = videoCodec.getOutputBuffers()[videoOutputBufferId];
                }
                if (outputBuffer != null && videoInfo.size != 0) {
                    outputBuffer.position(videoInfo.offset);
                    outputBuffer.limit(videoInfo.offset + videoInfo.size);
                    if (storeMP4 && isStartMuxer) mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, videoInfo);
                    storeOriVideoData(outputBuffer, videoInfo);
                }
                videoCodec.releaseOutputBuffer(videoOutputBufferId, false);
            }

            // 将音频数据，写入 Muxer
            int audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
            if (audioOutputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (storeMP4) {
                    audioTrackIndex = mediaMuxer.addTrack(audioCodec.getOutputFormat());
                    if (videoTrackIndex != -1 && !isStartMuxer) {
                        isStartMuxer = true;
                        mediaMuxer.start();
                    }
                }

            } else if (audioOutputBufferId >= 0) {
                ByteBuffer outputBuffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = audioCodec.getOutputBuffer(audioOutputBufferId);
                } else {
                    outputBuffer = audioCodec.getOutputBuffers()[audioOutputBufferId];
                }
                if (outputBuffer != null && audioInfo.size != 0) {
                    outputBuffer.position(audioInfo.offset);
                    outputBuffer.limit(audioInfo.offset + audioInfo.size);
                    if (storeMP4 && isStartMuxer) mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, audioInfo);
                    sendOriAudioData(outputBuffer);
                }
                audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
            }

            onRecordTime(videoInfo.presentationTimeUs);
        }
    }
}
