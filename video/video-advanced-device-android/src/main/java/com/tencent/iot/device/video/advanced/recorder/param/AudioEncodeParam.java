package com.tencent.iot.device.video.advanced.recorder.param;

import android.media.MediaFormat;

/**
 * 音频编码参数
 */
public class AudioEncodeParam {

    private int bitRate = 96000; // 比特率
    private int maxInputSize = 1024 * 1024; // 最大输入数据大小
    private String mime = MediaFormat.MIMETYPE_AUDIO_AAC; // 编码格式: 默认 AAC

    private AudioEncodeParam() { }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getMaxInputSize() {
        return maxInputSize;
    }

    public void setMaxInputSize(int maxInputSize) {
        this.maxInputSize = maxInputSize;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public static class Builder {
        private AudioEncodeParam audioEncodeParam;

        public Builder() {
            audioEncodeParam = new AudioEncodeParam();
        }

        public Builder setBitRate(int bitRate) {
            audioEncodeParam.setBitRate(bitRate);
            return this;
        }

        public Builder setMaxInputSize(int maxInputSize) {
            audioEncodeParam.setMaxInputSize(maxInputSize);
            return this;
        }

        public Builder setMime(String mime) {
            audioEncodeParam.setMime(mime);
            return this;
        }

        public AudioEncodeParam build() {
            return audioEncodeParam;
        }
    }

}
