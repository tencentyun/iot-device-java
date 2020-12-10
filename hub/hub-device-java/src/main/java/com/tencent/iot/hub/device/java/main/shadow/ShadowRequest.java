package com.tencent.iot.hub.device.java.main.shadow;



public class ShadowRequest {

    private int mRequestId;

    public ShadowRequest() {
    }

    public ShadowRequest(int mRequestId) {
        this.mRequestId = mRequestId;
    }

    @Override
    public String toString() {
        return "ShadowRequest{" +
                "mRequestId=" + mRequestId +
                '}';
    }
}
