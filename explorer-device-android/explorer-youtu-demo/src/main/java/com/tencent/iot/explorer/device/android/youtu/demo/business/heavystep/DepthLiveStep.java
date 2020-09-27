package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.cloud.ai.fr.utils.Case;
import com.tencent.youtu.YTFaceLive3D;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * 深度(3D)活体判断
 * 由于深度活体使用的是另外一个摄像头，因此业务方需要自行保证 **多目摄像头的帧同步**
 * 保证进行活体检测的 frame 和人脸识别的 frame 的时序要一致
 */
public class DepthLiveStep extends AbsStep<StuffBox> {

    public static final StuffId<Collection<TrackedFace>> OUT_DEPTH_LIVE_FAILED = new StuffId<>(Collections.EMPTY_LIST);
    public static final StuffId<Collection<TrackedFace>> OUT_DEPTH_LIVE_OK = new StuffId<>(Collections.EMPTY_LIST);
    public static final StuffId<Collection<TrackedFace>> OUT_MAPPED_DEPTH_FACES = new StuffId<>(Collections.EMPTY_LIST);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private final StuffId<Collection<TrackedFace>> mInputColorFacesId;

    public DepthLiveStep(StuffId<Collection<TrackedFace>> inputColorFacesId) {
        mInputColorFacesId = inputColorFacesId;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        FrameGroup inFrameGroup = stuffBox.find(mInputFrameGroupId);
        Collection<TrackedFace> inColorFaces = stuffBox.find(mInputColorFacesId);
        // 输出
        Collection<TrackedFace> outIrLiveOk = new ArrayList<>();
        Collection<TrackedFace> outIrLiveFail = new ArrayList<>();
        Collection<TrackedFace> outMappedFaces = new ArrayList<>();
        stuffBox.store(OUT_DEPTH_LIVE_FAILED, outIrLiveFail);
        stuffBox.store(OUT_DEPTH_LIVE_OK, outIrLiveOk);
        stuffBox.store(OUT_MAPPED_DEPTH_FACES, outMappedFaces);

        Frame depthFrame = inFrameGroup.depthFrame;
        Frame colorFrame = inFrameGroup.colorFrame;

        if (depthFrame == null) {
            return true;
        }

        long time = System.currentTimeMillis();

        Matrix m = matrixFromColorToDepth(colorFrame, depthFrame);

        Rect depthFrameRect = new Rect(0, 0, depthFrame.width, depthFrame.height);

        for (TrackedFace face : inColorFaces) {
            TrackedFace mappedFace = mapTrackedFace(face, m);
            outMappedFaces.add(mappedFace);

            boolean isLive = false;
            // 避免人脸出框的 crash 
            // cv::error(): OpenCV Error: Assertion failed (s >= 0) in void cv::setSize(cv::Mat&, int, const int*, const size_t*, bool), file /home/nihui/osd/opencv-2.4.13/modules/core/src/matrix.cpp, line 116
            // TODO 应当在 jni 规避此问题
            if (depthFrameRect.contains(mappedFace.faceRect)) {
                isLive = inSdkManager.mYTFaceLive3D.detect(mappedFace.faceRect, depthFrame.data, depthFrame.width, depthFrame.height);
            } else {
                Log.d(name(), " mappedFaceRect out of depthFrameRect, depth live assumed false");
            }

            if (isLive) {
                outIrLiveOk.add(face);
            } else {
                outIrLiveFail.add(face);
            }
            //案例存到磁盘
            new Case("depthLive", isLive, time, -1, stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP), -1, YTFaceLive3D.getVersion()).save();
        }
        // onDrawDepthFaces(outMappedFaces, depthFrame.width, depthFrame.height);
        return true;
    }

    /**
     * 华捷艾米 A200+mini 摄像头模组测试通过 <br/>
     * <br/>
     * 计算彩图变换矩阵, 使彩图变换后, 图像内容能与深度图重合. <br/>
     * 算法: 缩放彩图, 使之短边与深度图短边等长, 然后平移彩图使2图中心点对齐
     */
    private static Matrix matrixFromColorToDepth(Frame colorFrame, Frame depthFrame) {
        int irFrameShortSide = Math.min(depthFrame.width, depthFrame.height);
        int colorFrameShortSide = Math.min(colorFrame.width, colorFrame.height);
        int irFrameLongSide = Math.max(depthFrame.width, depthFrame.height);
        int colorFrameLongSide = Math.max(colorFrame.width, colorFrame.height);
        float scale = irFrameShortSide * 1f / colorFrameShortSide;
        int offset = (int) (((colorFrameLongSide * scale) - irFrameLongSide) / 2);// 两图长边差/2
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        if (colorFrame.width < colorFrame.height) {
            m.postTranslate(0, -offset);//竖版
        } else {
            m.postTranslate(-offset, 0);//横版
        }
        return m;
    }

    private static TrackedFace mapTrackedFace(TrackedFace src, Matrix matrix) {
        RectF mappedRectF = new RectF(src.faceRect);
        matrix.mapRect(mappedRectF);
        Rect mappedRect = new Rect();
        mappedRectF.round(mappedRect);

        float[] mappedFloats = new float[src.xy5Points.length];
        matrix.mapPoints(mappedFloats, src.xy5Points);

        TrackedFace face = new TrackedFace();
        face.xy5Points = mappedFloats;
        face.faceRect = mappedRect;
        face.consecutive = src.consecutive;
        face.frameId = src.frameId;
        face.traceId = src.traceId;
        face.pitch = src.pitch;
        face.yaw = src.yaw;
        face.roll = src.roll;
        return face;
    }
}
