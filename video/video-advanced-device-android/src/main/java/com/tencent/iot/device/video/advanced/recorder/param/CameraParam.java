package com.tencent.iot.device.video.advanced.recorder.param;

import com.tencent.iot.device.video.advanced.recorder.opengles.render.Renderer;

import javax.microedition.khronos.egl.EGLContext;


public class CameraParam {

    private EGLContext eglContext;
    private Renderer renderer; // 渲染 Renderer
    private int renderMode = -1; // 渲染模式
    private int facing = -1; // 摄像头前后摄

    private CameraParam() { }

    public EGLContext getEglContext() {
        return eglContext;
    }

    public void setEglContext(EGLContext eglContext) {
        this.eglContext = eglContext;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public int getRenderMode() {
        return renderMode;
    }

    public void setRenderMode(int renderMode) {
        this.renderMode = renderMode;
    }

    public int getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing;
    }

    public static class Builder {
        private CameraParam cameraParam;

        public Builder() {
            cameraParam = new CameraParam();
        }

        public Builder setEGLContext(EGLContext eglContext) {
            cameraParam.setEglContext(eglContext);
            return this;
        }

        public Builder setRenderer(Renderer renderer) {
            cameraParam.setRenderer(renderer);
            return this;
        }

        public Builder setRenderMode(int renderMode) {
            cameraParam.setRenderMode(renderMode);
            return this;
        }

        public Builder setFacing(int facing) {
            cameraParam.setFacing(facing);
            return this;
        }

        public CameraParam build() {
            return cameraParam;
        }
    }

    public boolean isEmpty() {
        return eglContext == null || renderer == null ||
                renderMode == -1 || facing == -1;
    }
}
