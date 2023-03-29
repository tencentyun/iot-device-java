package com.tencent.iot.device.video.advanced.recorder.opengles.render;

public interface Renderer {

    void onCreate();
    void onChange(int width, int height);
    void onDraw();
}
