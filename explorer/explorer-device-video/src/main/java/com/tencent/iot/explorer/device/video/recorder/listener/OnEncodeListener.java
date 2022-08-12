package com.tencent.iot.explorer.device.video.recorder.listener;


public interface OnEncodeListener {
    void onAudioEncoded(byte[] datas, long pts, long seq);
    void onVideoEncoded(byte[] datas, long pts, long seq);
}
