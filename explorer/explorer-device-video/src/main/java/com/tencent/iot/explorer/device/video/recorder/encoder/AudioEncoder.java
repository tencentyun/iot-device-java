package com.tencent.iot.explorer.device.video.recorder.encoder;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.iot.speexdsp.interfaces.SpeexJNIBridge;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.listener.OnReadPlayerPlayPcmListener;
import com.tencent.iot.explorer.device.video.recorder.param.AudioEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.param.MicParam;

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
import java.util.concurrent.LinkedBlockingDeque;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

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

    private Handler writeHandler;
    private String audioCacheFilePath = "";
    private FileOutputStream fos1;
    private FileOutputStream fos2;
    private FileOutputStream fos3;
    private final int MESSAGE_AUDIO_ENCODE_FROM_BYTE = 3000;

    private volatile boolean denoise = false;

    private IjkMediaPlayer player;
    private LinkedBlockingDeque<Byte> playPcmData = new LinkedBlockingDeque<>();  // 内存队列，用于缓存获取到的播放器音频pcm

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam) {
        this(micParam, audioEncodeParam, false, false);
    }

    public AudioEncoder(MicParam micParam, AudioEncodeParam audioEncodeParam, boolean denoise) {
        this(micParam, audioEncodeParam, false, false);
        this.denoise = denoise;
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

    public void setPlayer(IjkMediaPlayer player) {
        this.player = player;
    }

    public void setAudioCacheFilePath(String audioCacheFilePath) {
        this.audioCacheFilePath = audioCacheFilePath;
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
        if (!TextUtils.isEmpty(audioCacheFilePath)) {
            new WriteThread().start();
        }
        new CodecThread().start();
    }

    public void stop() {
        playPcmData.clear();
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

    class WriteThread extends Thread {
        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            super.run();
            Looper.prepare();
            File file1 = new File(audioCacheFilePath+"_file1.pcm");
            File file2 = new File(audioCacheFilePath+"_file2.pcm");
            File file3 = new File(audioCacheFilePath+"_file3.pcm");
            Log.i(TAG, "audio cache pcm file path:" + audioCacheFilePath);
            if (file1.exists()) {
                file1.delete();
            }
            if (file2.exists()) {
                file2.delete();
            }
            if (file3.exists()) {
                file3.delete();
            }
            try {
                file1.createNewFile();
                file2.createNewFile();
                file3.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos1 = new FileOutputStream(file1);
                fos2 = new FileOutputStream(file2);
                fos3 = new FileOutputStream(file3);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "临时缓存文件未找到");
            }
            writeHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == MESSAGE_AUDIO_ENCODE_FROM_BYTE) {
                        JSONObject jsonObject = (JSONObject) msg.obj;
                        if (AudioRecord.ERROR_INVALID_OPERATION != msg.arg1) {
                            if (fos1 != null && fos2 != null && fos3 != null) {
                                try {
                                    byte[] micBytesData = (byte[]) jsonObject.get("micBytesData");
                                    byte[] playerBytesData = (byte[]) jsonObject.get("playerBytesData");
                                    byte[] cancellBytesData = (byte[]) jsonObject.get("cancellBytesData");
                                    fos1.write(micBytesData);
                                    fos1.flush();
                                    fos2.write(playerBytesData);
                                    fos2.flush();
                                    fos3.write(cancellBytesData);
                                    fos3.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            };
            Looper.loop();
        }
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
                        if (denoise && player != null) {
                            byte[] playerBytes = onReadPlayerPlayPcm(audioRecordData.length);
                            byte[] cancell = SpeexJNIBridge.cancellation(audioRecordData, playerBytes);
                            inputBuffer.put(cancell);
                            if (writeHandler != null) {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("micBytesData", audioRecordData);
                                    jsonObject.put("playerBytesData", playerBytes);
                                    jsonObject.put("cancellBytesData", cancell);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Message message = writeHandler.obtainMessage(MESSAGE_AUDIO_ENCODE_FROM_BYTE, jsonObject);
                                message.arg1 = readSize;
                                writeHandler.sendMessage(message);
                            }
                        } else {
                            inputBuffer.put(audioRecordData);
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
                    outputBuffer.position(audioInfo.offset);
                    outputBuffer.limit(audioInfo.offset + audioInfo.size);
                    addADTStoPacket(outputBuffer);
                    audioCodec.releaseOutputBuffer(audioOutputBufferId, false);
                    audioOutputBufferId = audioCodec.dequeueOutputBuffer(audioInfo, 0);
                }
            }

            Looper.loop();
        }
    }

    private byte[] onReadPlayerPlayPcm(int length) {
        if (player != null && player.isPlaying()) {
            byte[] data = new byte[204800];
            int len = player._getPcmData(data);
            if (playPcmData.size() > 8*length) {
                if (len > 6*length) {
                    len = 6*length;
                } else if (len == 0) {

                } else {
                    int temp = playPcmData.size() - (6*length - len);
                    for (int i = 0 ; i < temp ; i++) {
                        playPcmData.remove();
                    }
                }
            } else if (len > 8*length) {
                len = 6*length;
            }
            if (len > 0) {
                byte[] playerBytes = new byte[len];
                System.arraycopy(data, 0, playerBytes, 0, len);
                List<Byte> tmpList = new ArrayList<>();
                for (byte b : playerBytes){
                    tmpList.add(b);
                }
                playPcmData.addAll(tmpList);
            }
            if (playPcmData.size() > length) {
                byte[] res = new byte[length];
                try {
                    for (int i = 0 ; i < length ; i++) {
                        res[i] = playPcmData.take();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return res;
            } else {
                return new byte[length];
            }
        }
        return new byte[length];
    }
}
