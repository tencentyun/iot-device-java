package com.tencent.iot.device.video.advanced.recorder.param;

import android.text.TextUtils;

import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;

public class RecordParam {

    private String storePath; // 存储路径， null 或者 "" 不保存录像，其他则保存录像

    private int recorderType = CallingType.TYPE_AUDIO_CALL; // VideoCalling

    public int getRecorderType() {
        return recorderType;
    }

    public void setRecorderType(int recorderType) {
        this.recorderType = recorderType;
    }

    public boolean isStoreMP4File() {
        return TextUtils.isEmpty(storePath)? false : true;
    }

    public RecordParam(String path) {
        this.storePath = path;
    }

    public RecordParam(int recorderType, String path) {
        this.storePath = path;
        this.recorderType = recorderType;
    }

    public String getPath() {
        return storePath;
    }

    public void setPath(String path) {
        this.storePath = path;
    }
}
