package com.tencent.iot.explorer.device.android.http.retrofit;

/**
 * 请求响应回调
 */
public interface Callback {

    void success(String json, int requestCode);

    void fail(String errorInfo, int requestCode);

}
