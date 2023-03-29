package com.tencent.iot.device.video.advanced.recorder.listener;

public interface OnReadAECProcessedPcmListener {
    byte[] onReadAECProcessedPcmListener(byte[] micPcmBytes);
    void audioCodecRelease();
}
