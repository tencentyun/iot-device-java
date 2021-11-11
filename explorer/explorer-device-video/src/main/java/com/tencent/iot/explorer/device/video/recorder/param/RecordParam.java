package com.tencent.iot.explorer.device.video.recorder.param;

import android.text.TextUtils;

public class RecordParam {

    private String storePath; // 存储路径， null 或者 "" 不保存录像，其他则保存录像

    public boolean isStoreMP4File() {
        return TextUtils.isEmpty(storePath)? false : true;
    }

    public RecordParam(String path) {
        this.storePath = path;
    }

    public String getPath() {
        return storePath;
    }

    public void setPath(String path) {
        this.storePath = path;
    }
}
