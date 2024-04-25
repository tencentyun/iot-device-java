package com.tencent.iot.explorer.device.video.recorder.encoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.iot.gvoice.interfaces.GvoiceJNIBridge;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.listener.OnReadAECProcessedPcmListener;
import com.tencent.iot.explorer.device.video.recorder.param.AudioEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.param.MicParam;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import static com.tencent.iot.explorer.device.android.utils.ConvertUtils.byte2HexOnlyLatest8;
import static com.tencent.iot.explorer.device.video.recorder.consts.LogConst.RTC_TAG;

public class AudioEncoder implements Handler.Callback {

    /**
     * 采样频率对照表
     */
    private static final Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap<>();

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

    private final String TAG = AudioEncoder.class.getSimpleName();
    private static final int MSG_START = 1;
    private static final int MSG_STOP = 2;
    private static final int MSG_REC_PLAY_PCM = 3;
    private static final int MSG_RELEASE = 4;
    private boolean isRecord = false;
    private MediaCodec audioCodec;
    private AudioRecord audioRecord;
    private AcousticEchoCanceler canceler;
    private AutomaticGainControl control;

    private final MicParam micParam;
    private final AudioEncodeParam audioEncodeParam;
    private OnEncodeListener encodeListener;
    private OnReadAECProcessedPcmListener micPcmListener;

    private volatile boolean stopEncode = false;
    private long seq = 0L;
    private long beforSeq = 0L;
    private int bufferSizeInBytes;

    private final HandlerThread readThread;
    private final ReadHandler mReadHandler;
    private volatile boolean recorderState = true; //录制状态

    private boolean enableAEC = false;

    private FileOutputStream fos1;
    private FileOutputStream fos2;
    private FileOutputStream fos3;
    private String speakPcmFilePath = "/storage/emulated/0/speak_pcm_";

    private static final int SAVE_PCM_DATA = 1;
    private LinkedBlockingDeque<Byte> playPcmData = new LinkedBlockingDeque<>();  // 内存队列，用于缓存获取到的播放器音频pcm

    @Override
    public boolean handleMessage(@NotNull Message msg) {
        switch (msg.what) {
            case MSG_START:
                startInternal();
                break;
            case MSG_STOP:
                stopInternal();
                break;
            case MSG_REC_PLAY_PCM:
                recPlayPcmInternal((byte[]) msg.obj);
                break;
            case MSG_RELEASE:
                releaseInternal();
                break;
        }
        return false;
    }

    private class MyHandler extends Handler {

        public MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            try {
                if (msg.what == SAVE_PCM_DATA && fos1 != null && fos2 != null && fos3 != null) {
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    byte[] nearBytesData = (byte[]) jsonObject.get("nearPcmBytes");
                    fos1.write(nearBytesData);
                    fos1.flush();
                    byte[] playerPcmBytes = (byte[]) jsonObject.get("playerPcmBytes");
                    fos2.write(playerPcmBytes);
                    fos2.flush();
                    byte[] aecPcmBytes = (byte[]) jsonObject.get("aecPcmBytes");
                    fos3.write(aecPcmBytes);
                    fos3.flush();
                }

            } catch (IOException e) {
                Log.e(TAG, "*======== IOException: " + e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(TAG, "*======== JSONException: " + e);
                e.printStackTrace();
            }
        }
    }
    private final Handler mHandler = new MyHandler();

    public AudioEncoder(Context context, MicParam micParam, AudioEncodeParam audioEncodeParam) {
        this(context, micParam, audioEncodeParam, false, false);
    }

    public AudioEncoder(Context context, MicParam micParam, AudioEncodeParam audioEncodeParam, boolean enableAEC, boolean enableAGC) {
        this.micParam = micParam;
        this.audioEncodeParam = audioEncodeParam;
        initAudio();
        int audioSessionId = audioRecord.getAudioSessionId();
        this.enableAEC = enableAEC;
        if (enableAEC && audioSessionId != 0) {
            Log.e(TAG, "=====initAEC result: " + initAEC(audioSessionId));
        }
        if (enableAGC && audioSessionId != 0) {
            Log.e(TAG, "=====initAGC result: " + initAGC(audioSessionId));
        }
        readThread = new HandlerThread(TAG);
        readThread.start();
        mReadHandler = new ReadHandler(readThread.getLooper(), this);
        GvoiceJNIBridge.init(context);
    }

    public void setOnEncodeListener(OnEncodeListener listener) {
        this.encodeListener = listener;
    }

    public void setOnReadAECProcessedPcmListener(OnReadAECProcessedPcmListener listener) {
        this.micPcmListener = listener;
    }

    private void initAudio() {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat());
        Log.e(TAG, "=====bufferSizeInBytes: " + bufferSizeInBytes);
        audioRecord = new AudioRecord(micParam.getAudioSource(), micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat(), bufferSizeInBytes);
        try {
            audioCodec = MediaCodec.createEncoderByType(audioEncodeParam.getMime());
            Log.i(RTC_TAG, "audioCodec MediaCodec createEncoderByType");
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

    public void start() {
        Log.i(TAG, "start");
        mReadHandler.obtainMessage(MSG_START).sendToTarget();
    }

    private void startInternal() {
        if (isRecord) {
            fos1 = createFiles("near");
            fos2 = createFiles("far");
            fos3 = createFiles("aec");
        }
        if (!playPcmData.isEmpty()) {
            playPcmData.clear();
        }
        new CodecThread().start();
    }

    private FileOutputStream createFiles(String format) {

        if (!TextUtils.isEmpty(speakPcmFilePath)) {
            File file1 = new File(speakPcmFilePath+format+".pcm");
            Log.i(TAG, "speak cache pcm file path:" + speakPcmFilePath);
            if (file1.exists()) {
                file1.delete();
            }
            try {
                file1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            try {
                FileOutputStream fos = new FileOutputStream(file1);
                return fos;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "临时缓存文件未找到");
                return null;
            }
        }
        return null;
    }

    public void stop() {
        Log.i(TAG, "stop");
        mReadHandler.obtainMessage(MSG_STOP).sendToTarget();
    }

    private void stopInternal() {
        stopEncode = true;
    }

    public void setPlayerPcmData(byte[] pcmData) {
        mReadHandler.obtainMessage(MSG_REC_PLAY_PCM, pcmData).sendToTarget();
    }

    private void recPlayPcmInternal(byte [] pcmData) {
        if (pcmData != null && pcmData.length > 0 && recorderState) {
            List<Byte> tmpList = new ArrayList<>();
            for (byte b : pcmData) {
                tmpList.add(b);
            }
            playPcmData.addAll(tmpList);
        }
    }

    public boolean isDevicesSupportAEC() {
        return AcousticEchoCanceler.isAvailable();
    }

    private boolean initAEC(int audioSession) {
        boolean isDevicesSupportAEC = isDevicesSupportAEC();
        Log.e(TAG, "isDevicesSupportAEC: "+isDevicesSupportAEC);
        if (!isDevicesSupportAEC) {
            return false;
        }
        if (canceler != null) {
            return false;
        }
        canceler = AcousticEchoCanceler.create(audioSession);
        if (canceler != null) {
            canceler.setEnabled(true);
            return canceler.getEnabled();
        } else {
            return false;
        }
    }

    public boolean isDevicesSupportAGC() {
        return AutomaticGainControl.isAvailable();
    }

    private boolean initAGC(int audioSession) {
        boolean isDevicesSupportAGC = isDevicesSupportAGC();
        Log.e(TAG, "isDevicesSupportAGC: "+isDevicesSupportAGC);
        if (!isDevicesSupportAGC) {
            return false;
        }
        if (control != null) {
            return false;
        }
        control = AutomaticGainControl.create(audioSession);
        if (control != null) {
            control.setEnabled(true);
            return control.getEnabled();
        } else {
            return false;
        }
    }

    private void release() {
        Log.i(TAG, "release");
        mReadHandler.obtainMessage(MSG_RELEASE).sendToTarget();
    }

    private void releaseInternal() {
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

        if (canceler != null) {
            canceler.setEnabled(false);
            canceler.release();
            canceler = null;
        }

        if (control != null) {
            control.setEnabled(false);
            control.release();
            control = null;
        }
    }

    public void recordPcmFile(boolean isRecord) {
        this.isRecord = isRecord;
    }

    private void addADTStoPacket(ByteBuffer outputBuffer) {
        byte[] bytes = new byte[outputBuffer.remaining()];
        outputBuffer.get(bytes, 0, bytes.length);
        byte[] dataBytes = new byte[bytes.length + 7];
        System.arraycopy(bytes, 0, dataBytes, 7, bytes.length);
        addADTStoPacket(dataBytes, dataBytes.length);
        if (stopEncode) {
            return;
        }
        if (encodeListener != null) {
            Log.i(RTC_TAG, "on audio encoded byte: "+byte2HexOnlyLatest8(dataBytes) + "; seq: " + seq);
            encodeListener.onAudioEncoded(dataBytes, System.currentTimeMillis(), seq);
            seq++;
        } else {
            Log.e(TAG, "Encode listener is null, please set encode listener.");
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        // AAC LC
        int profile = 2;
        // CPE
        int chanCfg = 1;
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

    class CodecThread extends Thread {
        @Override
        public void run() {
            super.run();
            Looper.prepare();
            if (audioCodec == null) {
                return;
            }
            stopEncode = false;
            audioRecord.startRecording();
            Log.i(RTC_TAG, "audioRecord startRecording");
            audioCodec.start();
            Log.i(RTC_TAG, String.format("audioCodec start with MediaFormat AudioSource: %d, SampleRateInHz: %d, IsChannelMono: %b, bitDepth: %d, encodeMime: %s, bitRate: %d, CodecProfileLevel: %d, KEY_MAX_INPUT_SIZE:%d",
                    micParam.getAudioSource(), micParam.getSampleRateInHz(), micParam.getChannelConfig()==AudioFormat.CHANNEL_IN_MONO, micParam.getAudioFormat(),
                    audioEncodeParam.getMime(), audioEncodeParam.getBitRate(), MediaCodecInfo.CodecProfileLevel.AACObjectLC, audioEncodeParam.getMaxInputSize()));
            MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
            while (true) {
                if (stopEncode) {
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
                    int size = enableAEC ? 640 : bufferSizeInBytes;
                    byte[] audioRecordData = new byte[size];
                    if (inputBuffer != null && micPcmListener == null) {
                        readSize = audioRecord.read(audioRecordData, 0, size);
                    } else if (inputBuffer == null && micPcmListener != null) {
                        readSize = size;
                    }
                    if (readSize >= 0) {
                        inputBuffer.clear();
                        if (enableAEC){
                            byte [] playerPcmBytes = onReadPlayerPlayPcm(size);
                            if (micPcmListener != null) {
                                audioRecordData = micPcmListener.onReadAECProcessedPcmListener(size);
                            }

                            byte[] aecPcmBytes = GvoiceJNIBridge.cancellation(audioRecordData, playerPcmBytes);
                            if (isRecord) {
                                writePcmBytesToFile(audioRecordData, playerPcmBytes, aecPcmBytes);
                            }
                            beforSeq++;
                            inputBuffer.put(aecPcmBytes);
                        } else {
                            inputBuffer.put(audioRecordData);
                            Log.i(RTC_TAG, String.format("audioRecord read capture origina l frame data:%s", byte2HexOnlyLatest8(audioRecordData)));
                            Log.i("audioRecordTest", "---without cancel");
                        }
                        audioCodec.queueInputBuffer(audioInputBufferId, 0, readSize, System.nanoTime() / 1000, 0);
                    }
                }

                int audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
                while (audioOutputBufferId >= 0) {
                    ByteBuffer outputBuffer = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = audioCodec.getOutputBuffer(audioOutputBufferId);
                    } else {
                        outputBuffer = audioCodec.getOutputBuffers()[audioOutputBufferId];
                    }
                    if (audioInfo.size > 2) {
                        outputBuffer.position(audioInfo.offset);
                        outputBuffer.limit(audioInfo.offset + audioInfo.size);
                        Log.i(RTC_TAG, String.format("audioCodec getOutputBuffer audioInfo.offset :%d + audioInfo.size :%d", audioInfo.offset, audioInfo.size));
                        addADTStoPacket(outputBuffer);
                    }
                    audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
                    audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
                }
            }

            Looper.loop();
        }
    }

    private byte[] onReadPlayerPlayPcm(int length) {
        if (playPcmData.size() > length) {
            byte[] res = new byte[length];
            try {
                for (int i = 0 ; i < length ; i++) {
                    res[i] = playPcmData.take();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "onReadPlayerPlayPcm  playPcmData.length： " + playPcmData.size());
            if (playPcmData.size()>20000) {
                playPcmData.clear();
            }
            return res;
        } else {
            return new byte[length];
        }
    }


    private void writePcmBytesToFile(byte[] nearPcmBytes, byte[] playerPcmBytes, byte[] aecPcmBytes) {
        if (mHandler != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("nearPcmBytes", nearPcmBytes);
                jsonObject.put("playerPcmBytes", playerPcmBytes);
                jsonObject.put("aecPcmBytes", aecPcmBytes);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Message message = mHandler.obtainMessage(SAVE_PCM_DATA, jsonObject);
            mHandler.sendMessage(message);
        }
    }

    public static class ReadHandler extends Handler {

        public ReadHandler(Looper looper, Callback callback) {
            super(looper, callback);
        }

        public void runAndWaitDone(final Runnable runnable) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            post(() -> {
                runnable.run();
                countDownLatch.countDown();
            });

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
