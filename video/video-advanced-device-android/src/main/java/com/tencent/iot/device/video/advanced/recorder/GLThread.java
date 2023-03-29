package com.tencent.iot.device.video.advanced.recorder;

import android.view.Surface;

import com.tencent.iot.device.video.advanced.recorder.opengles.egl.EglHelper;
import com.tencent.iot.device.video.advanced.recorder.opengles.view.base.EGLTextureView;
import com.tencent.iot.device.video.advanced.recorder.param.CameraParam;
import com.tencent.iot.device.video.advanced.recorder.param.VideoEncodeParam;

import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLContext;

/**
 * 渲染线程
 */
public class GLThread extends Thread {
    private EglHelper eglHelper;
    private boolean isCreate;
    private boolean isChange;
    private boolean isStart;
    private boolean isExit;
    private volatile Surface surface;
    private CameraParam cameraParam;
    private VideoEncodeParam videoEncodeParam;

    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    private final Object object = new Object();

    public GLThread(CameraParam cameraParam, VideoEncodeParam videoEncodeParam) {
        isCreate = true;
        isChange = true;
        this.cameraParam = cameraParam;
        this.videoEncodeParam = videoEncodeParam;
    }

    private ArrayList<Runnable> mEventQueue = new ArrayList<>();

    @Override
    public void run() {
        if (checkState()) {
            return;
        }
        try {
            guardedRun();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查参数状态
     *
     * @return 是否初始化完成
     */
    private boolean checkState() {
        return surface == null;
    }

    private void guardedRun() throws InterruptedException {
        isExit = false;
        isStart = false;
        eglHelper = new EglHelper();
        eglHelper.init(cameraParam.getEglContext(), surface);
        Runnable event = null;
        while (true) {
            if (isExit) {
                release();
                break;
            }
            if (isStart) {
                if (cameraParam.getRenderMode() == EGLTextureView.RENDERMODE_WHEN_DIRTY) {
                    synchronized (object) {
                        object.wait();
                    }
                } else if (cameraParam.getRenderMode() == EGLTextureView.RENDERMODE_CONTINUOUSLY) {
                    Thread.sleep(1000 / 60);
                } else {
                    throw new IllegalArgumentException("render mode error");
                }
            }

            if (!mEventQueue.isEmpty()) {
                event = mEventQueue.remove(0);
            }

            if (event != null) {
                event.run();
                event = null;
                continue;
            }

            onCreate();
            onChange(videoEncodeParam.getWidth(), videoEncodeParam.getHeight());
            onDrawFrame();
            isStart = true;
        }
    }

    private void onCreate() {
        if (!isCreate || cameraParam.getRenderer() == null) {
            return;
        }
        isCreate = false;
        cameraParam.getRenderer().onCreate();
    }

    private void onChange(int width, int height) {
        if (!isChange || cameraParam.getRenderer() == null) {
            return;
        }
        isChange = false;
        cameraParam.getRenderer().onChange(width, height);
    }

    private void onDrawFrame() {
        if (cameraParam.getRenderer() == null) {
            return;
        }
        cameraParam.getRenderer().onDraw();
        if (!isStart) {
            cameraParam.getRenderer().onDraw();
        }
        eglHelper.swapBuffers();
    }

    void requestRender() {
        if (object != null) {
            synchronized (object) {
                object.notifyAll();
            }
        }
    }

    void onDestroy() {
        isExit = true;
        requestRender();
    }

    void release() {
        if (eglHelper != null) {
            eglHelper.destoryEgl();
            eglHelper = null;
        }
    }

    EGLContext getEGLContext() {
        if (eglHelper != null) {
            return eglHelper.getEglContext();
        }
        return null;
    }

    void queueEvent(Runnable r) {
        if (r == null) {
            return;
        }
        if (object != null) {
            synchronized (object) {
                mEventQueue.add(r);
                object.notifyAll();
            }
        }
    }
}
