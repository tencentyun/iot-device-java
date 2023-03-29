package com.tencent.iot.device.video.advanced.recorder;

public interface OnRecordListener {

    void onRecordStart(); // 开始录制回调
    void onRecordTime(long time); // 录制时长回调
    void onRecordComplete(String path); // 录制完成回调
    void onRecordCancel(); // 录制取消回调
    void onRecordError(Exception e); // 录制错误回调
}
