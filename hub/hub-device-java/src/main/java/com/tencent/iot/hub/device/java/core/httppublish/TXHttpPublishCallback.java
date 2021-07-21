package com.tencent.iot.hub.device.java.core.httppublish;

public interface TXHttpPublishCallback {
    void onFailedPublish(Throwable throwable);

    void onFailedPublish(Throwable throwable, String message);

    void onSuccessPublishGetRequestId(String requestId);
}
