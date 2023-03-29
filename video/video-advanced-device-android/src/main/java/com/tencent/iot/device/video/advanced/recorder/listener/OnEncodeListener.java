package com.tencent.iot.device.video.advanced.recorder.listener;


public interface OnEncodeListener {
    void onAudioEncoded(byte[] datas, long pts, long seq);
    void onVideoEncoded(byte[] datas, long pts, long seq);
}
