package com.tencent.iot.device.video.advanced.recorder.opengles.render.base;

public interface IRender {

    void onCreate(); // 创建
    void onChange(int width, int height); // 设置尺寸
    void onDraw(int textureId); // 绘制
    void onRelease(); // 释放资源
}
