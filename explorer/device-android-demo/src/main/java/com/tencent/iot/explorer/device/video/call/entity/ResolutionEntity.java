package com.tencent.iot.explorer.device.video.call.entity;

public class ResolutionEntity {
    public static String TAG = ResolutionEntity.class.getSimpleName();
    private int width;
    private int height;
    private boolean isSelect = false;

    public ResolutionEntity(){

    }

    public ResolutionEntity(int width, int height) {
        this.width = width;
        this.height = height;
        this.isSelect = false;
    }

    public ResolutionEntity(int width, int height, boolean isSelect) {
        this.width = width;
        this.height = height;
        this.isSelect = isSelect;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean getIsSelect() {
        return isSelect;
    }

    public void setIsSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }
}
