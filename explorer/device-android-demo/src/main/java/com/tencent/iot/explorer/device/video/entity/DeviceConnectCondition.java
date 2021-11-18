package com.tencent.iot.explorer.device.video.entity;

import android.text.TextUtils;

public class DeviceConnectCondition {
    public static String TAG = DeviceConnectCondition.class.getSimpleName();

    private String brokeUrl = "";
    private String productId = "";
    private String devName = "";
    private String devPsk = "";

    public DeviceConnectCondition(String productId, String devName, String devPsk) {
        this.productId = productId;
        this.devName = devName;
        this.devPsk = devPsk;
    }

    public DeviceConnectCondition() { }

    public String getProductId() {
        return productId;
    }

    public String getBrokeUrl() {
        if (TextUtils.isEmpty(brokeUrl)) {
            return null;
        }
        return brokeUrl;
    }

    public String getDevName() {
        return devName;
    }

    public String getDevPsk() {
        return devPsk;
    }

    public void setBrokeUrl(String brokeUrl) {
        this.brokeUrl = brokeUrl;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public void setDevPsk(String devPsk) {
        this.devPsk = devPsk;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
