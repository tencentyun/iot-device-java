package com.tencent.iot.explorer.device.video.recorder.opengles.render.manager;

import android.content.Context;
import com.example.ivdemo.recorder.opengles.render.base.bean.base.BaseRenderBean;
import com.tencent.iot.explorer.device.video.recorder.opengles.render.RenderConstants;


public class RenderManager {
    private static volatile RenderManager instance;

    private final RenderProcess cameraRenderProcess;
    private final RenderProcess videoRenderProcess;
    private final RenderProcess imageRenderProcess;
    private final RenderProcess takePhotoRenderProcess;
    private final RenderProcess recordVideoRenderProcess;

    private RenderManager(Context context) {
        cameraRenderProcess = new RenderProcess(context);
        videoRenderProcess = new RenderProcess(context);
        imageRenderProcess = new RenderProcess(context);
        takePhotoRenderProcess = new RenderProcess(context);
        recordVideoRenderProcess = new RenderProcess(context);
    }

    public static RenderManager getInstance(Context context) {
        if (instance == null) {
            synchronized (RenderManager.class) {
                if (instance == null) {
                    instance = new RenderManager(context);
                }
            }
        }
        return instance;
    }

    public void init(int process) {
        getRenderProcess(process).init();
    }

    public void release(int process) {
        getRenderProcess(process).release();
    }

    public void onCreate(int process) {
        getRenderProcess(process).onCreate();
    }

    public void onChange(int process) {
        getRenderProcess(process).onChange();
    }

    public int onDraw(int process, int textureId, int width, int height) {
        return getRenderProcess(process).onDraw(textureId, width, height);
    }

    public void setFilter(int process, BaseRenderBean bean) {
        getRenderProcess(process).setFilter(bean);
    }

    private RenderProcess getRenderProcess(int process) {
        RenderProcess renderProcess = null;
        switch (process) {
            case RenderConstants.Process.CAMERA:
                renderProcess = cameraRenderProcess;
                break;
            case RenderConstants.Process.VIDEO:
                renderProcess = videoRenderProcess;
                break;
            case RenderConstants.Process.IMAGE:
                renderProcess = imageRenderProcess;
                break;
            case RenderConstants.Process.TAKE_PHOTO:
                renderProcess = takePhotoRenderProcess;
                break;
            case RenderConstants.Process.RECORD_VIDEO:
                renderProcess = recordVideoRenderProcess;
                break;
        }
        return renderProcess;
    }
}
