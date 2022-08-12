package com.tencent.iot.explorer.device.video.call.entity;

public class FrameRateEntity {
    public static String TAG = FrameRateEntity.class.getSimpleName();
    private int rate;
    private boolean isSelect = false;

    public FrameRateEntity() {

    }

    public FrameRateEntity(int rate) {
        this.rate = rate;
        this.isSelect = false;
    }

    public FrameRateEntity(int rate, boolean isSelect) {
        this.rate = rate;
        this.isSelect = isSelect;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public boolean getIsSelect() {
        return isSelect;
    }

    public void setIsSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }
}
