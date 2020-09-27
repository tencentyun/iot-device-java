package com.tencent.cloud.ai.fr.camera;

import android.support.annotation.AnyThread;
import android.view.View;

public interface ICameraManager {

    void resumeCamera();
    void pauseCamera();
    void destroyCamera();

    /** 获取相机 preview */
    View getPreview();

    /** 设置帧回调监听, 以获取帧 */
    void setOnFrameArriveListener(OnFrameArriveListener l);

    /** 相机帧到达监听器 */
    interface OnFrameArriveListener {

        /** 相机帧到达时, 此方法被回调 */
        void onFrameArrive(byte[] yuvNv21, int width, int height, int exifOrientation);
    }

    /**
     * SDK用完的帧在这里返回, 如果你的相机会复用帧, 在这里进行回收复用. 如果不复用, 则此方法空实现. 回收相机帧可减少内存抖动, 提升性能.
     * 当相机帧使用完毕, 或者被废弃时, 此方法会被回调, 可能在主线程回调, 也可能在后台线程回调.
     * @param frameDataToBeRecycled 待回收的帧数据
     */
    @AnyThread
    void onRecycleFrame(byte[] frameDataToBeRecycled);
}
