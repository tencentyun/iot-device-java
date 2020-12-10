package com.tencent.iot.hub.device.java.main.mqtt;


public class MQTTRequest {

    private static final String TAG = MQTTRequest.class.getSimpleName();

    /**
     * 请求类型
     */
    private String requestType = "";

    /**
     * 请求ID
     */
    private int requestId = 0;

    public MQTTRequest() {
    }

    public MQTTRequest(String requestType, int requestId) {
        this.requestType = requestType;
        this.requestId = requestId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "MQTTRequest{" +
                "requestType='" + requestType + '\'' +
                ", requestId=" + requestId +
                '}';
    }
}
