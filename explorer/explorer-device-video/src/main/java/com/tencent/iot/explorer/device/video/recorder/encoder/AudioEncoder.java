package com.tencent.iot.explorer.device.video.recorder.encoder;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;

import com.iot.speexdsp.interfaces.SpeexJNIBridge;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.listener.OnReadPlayerPlayPcmListener;
import com.tencent.iot.explorer.device.video.recorder.param.AudioEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.param.MicParam;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class AudioEncoder {

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
    private MediaCodec audioCodec;
    private AudioRecord audioRecord;
    private AcousticEchoCanceler canceler;
    private AutomaticGainControl control;

    private final MicParam micParam;
    private final AudioEncodeParam audioEncodeParam;
    private OnEncodeListener encodeListener;

    private volatile boolean stopEncode = false;
    private long seq = 0L;
    private int bufferSizeInBytes;

    private volatile boolean denoise = false;
    private OnReadPlayerPlayPcmListener mPlayPcmListener;

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam) {
        this(micParam, audioEncodeParam, false, false);
    }

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean denoise, OnReadPlayerPlayPcmListener playPcmListener) {
        this(micParam, audioEncodeParam, false, false);
        this.denoise = denoise;
        this.mPlayPcmListener = playPcmListener;
        if (denoise) {
            SpeexJNIBridge.init(bufferSizeInBytes, micParam.getSampleRateInHz());
        }
    }

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean enableAEC, boolean enableAGC) {
        this.micParam = micParam;
        this.audioEncodeParam = audioEncodeParam;
        initAudio();
        int audioSessionId = audioRecord.getAudioSessionId();
        if (enableAEC && audioSessionId != 0) {
            Log.e(TAG, "=====initAEC result: " + initAEC(audioSessionId));
        }
        if (enableAGC && audioSessionId != 0) {
            Log.e(TAG, "=====initAGC result: " + initAGC(audioSessionId));
        }
    }

    public void setOnEncodeListener(OnEncodeListener listener) {
        this.encodeListener = listener;
    }

    private void initAudio() {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(micParam.getSampleRateInHz(), micParam.getChannelConfig(), micParam.getAudioFormat());
        Log.e(TAG, "=====bufferSizeInBytes: " + bufferSizeInBytes);
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

    public void start() {
        new Thread(this::record).start();
    }

    public void stop() {
        stopEncode = true;
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
        canceler.setEnabled(true);
        return canceler.getEnabled();
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
        control.setEnabled(true);
        return control.getEnabled();
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
        if (denoise) {
            SpeexJNIBridge.destory();
        }
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

    private void record() {
        if (audioCodec == null) {
            return;
        }
        stopEncode = false;
        audioRecord.startRecording();
        audioCodec.start();
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
                byte[] audioRecordData = new byte[bufferSizeInBytes];
                if (inputBuffer != null) {
                    readSize = audioRecord.read(audioRecordData, 0, bufferSizeInBytes);
                }
                if (readSize >= 0) {
                    inputBuffer.clear();
                    if (denoise && mPlayPcmListener != null) {
                        byte[] playerBytes = mPlayPcmListener.onReadPlayerPlayPcm(audioRecordData.length);
                        byte[] cancell = SpeexJNIBridge.cancellation(audioRecordData, playerBytes);
                        inputBuffer.put(cancell);
                    } else {
                        inputBuffer.put(audioRecordData);
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
                outputBuffer.position(audioInfo.offset);
                outputBuffer.limit(audioInfo.offset + audioInfo.size);
                addADTStoPacket(outputBuffer);
                audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
                audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
            }
        }
    }
}
