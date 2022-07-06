package com.tencent.iot.explorer.device.video.recorder.opengles.render;

public interface Renderer {

    void onCreate();
    void onChange(int width, int height);
    void onDraw();
}
