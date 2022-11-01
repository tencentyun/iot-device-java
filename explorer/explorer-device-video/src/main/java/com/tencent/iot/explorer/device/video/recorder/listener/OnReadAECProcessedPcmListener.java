package com.tencent.iot.explorer.device.video.recorder.listener;

public interface OnReadAECProcessedPcmListener {
    byte[] onReadAECProcessedPcmListener(byte[] micPcmBytes);
    void audioCodecRelease();
}
