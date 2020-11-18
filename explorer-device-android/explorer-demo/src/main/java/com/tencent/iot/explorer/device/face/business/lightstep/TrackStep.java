package com.tencent.iot.explorer.device.face.business.lightstep;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.StuffId;
import com.tencent.youtu.YTFaceTracker;
import com.tencent.youtu.YTFaceTracker.Options;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 2.2 检测出全部人脸 */
public class TrackStep extends AbsStep<StuffBox> {

    public static final StuffId<Collection<TrackedFace>> OUT_COLOR_FACE = new StuffId<>(Collections.EMPTY_LIST);
    public static final StuffId<Collection<TrackedFace>> OUT_IR_FACE = new StuffId<>(Collections.EMPTY_LIST);
    public static final StuffId<Map<TrackedFace, TrackedFace>> OUT_MATCHED_COLOR_IR_FACE = new StuffId<>(Collections.EMPTY_MAP);
    public static final StuffId<Map<TrackedFace, String>> OUT_MATCH_FAILED_COLOR_IR_FACE = new StuffId<>(Collections.EMPTY_MAP);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;

    private Options options = new Options();

    @Override
    public boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        FrameGroup inFrameGroup = stuffBox.find(mInputFrameGroupId);
        // 输出
        Collection<TrackedFace> outColorFaces = new ArrayList<>();
        Collection<TrackedFace> outIrFaces = new ArrayList<>();
        Map<TrackedFace, TrackedFace> outMatchedColorIrFaces = new HashMap<>();
        Map<TrackedFace, String> outMatchFailIrFaces = new HashMap<>();
        stuffBox.store(OUT_COLOR_FACE, outColorFaces);
        stuffBox.store(OUT_IR_FACE, outIrFaces);
        stuffBox.store(OUT_MATCHED_COLOR_IR_FACE, outMatchedColorIrFaces);
        stuffBox.store(OUT_MATCH_FAILED_COLOR_IR_FACE, outMatchFailIrFaces);

        boolean isContinuous = inFrameGroup.isContinuous;
        YTFaceTracker tracker = inSdkManager.mYTFaceTracker;

        // 检测人脸框和5个关键点
        Frame colorFrame = inFrameGroup.colorFrame;
        track(colorFrame, isContinuous, tracker, outColorFaces);
        // 检测红外图人脸框和5个关键点
        Frame irFrame = inFrameGroup.irFrame;
        if (irFrame != null) {
            track(irFrame, isContinuous, tracker, outIrFaces);
            outMatchedColorIrFaces.putAll(matchColorIrFaces(outMatchFailIrFaces, outColorFaces, outIrFaces,
                    matrixFromColorToIr(colorFrame, irFrame), colorFrame, irFrame));
        }

        return true;//告诉流水线此任务是否成功, 如果成功则会执行下游任务, 否则中断流水线
    }
    
    private void track(Frame frame, boolean isContinuous, YTFaceTracker tracker, Collection<TrackedFace> out){
        TrackedFace[] trackedFaces;
        if (isContinuous) {
            trackedFaces = tracker.track(frame.data, frame.width, frame.height);
        } else {
            options.minFaceSize = 100;//如果检测不到人脸, 可以尝试调小这个值
            options.maxFaceSize = Math.min(frame.width, frame.height);//最大脸也就图那么大
            options.biggerFaceMode = true;
            trackedFaces = tracker.detect(frame.data, frame.width, frame.height, options);
        }

        if (trackedFaces == null) {
            trackedFaces = new TrackedFace[0];
            Log.w(name(), "人脸检测返回异常, 请检查SDK授权是否成功");
        }
        out.addAll(Arrays.asList(trackedFaces));
    }

    /**
     * 华捷艾米 A200+mini 摄像头模组测试通过 <br/>
     * <br/>
     * 计算彩图变换矩阵, 使彩图变换后, 图像内容能与红外图重合. <br/>
     * 算法: 缩放彩图短边, 使之与红外图短边一致, 然后2图中心点对齐
     */
    private static Matrix matrixFromColorToIr(Frame colorFrame, Frame irFrame) {
        int irFrameShortSide = Math.min(irFrame.width, irFrame.height);
        int colorFrameShortSide = Math.min(colorFrame.width, colorFrame.height);
        int irFrameLongSide = Math.max(irFrame.width, irFrame.height);
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

    /**
     * 匹配彩图和红外图中的人脸框, 当图中有多人的时候, 此步骤是必须的.
     * @param matchFailIrFaces 容器, 存储匹配失败的红外图人脸, {@code Map<irFace, "失败原因"> } 原因可能是偏移太大, 人脸出框
     * @return 匹配成功的人脸框 {@code Map<colorFace, irFace> }
     */
    private static Map<TrackedFace, TrackedFace> matchColorIrFaces(Map<TrackedFace, String> matchFailIrFaces, Collection<TrackedFace> colorFaces, Collection<TrackedFace> irFaces, Matrix matrixFromColorToIr, Frame colorFrame, Frame irFrame) {
        Rect colorFrameRect = new Rect(0, 0, colorFrame.width, colorFrame.height);
        Rect irFrameRect = new Rect(0, 0, irFrame.width, irFrame.height);
        Map<TrackedFace, TrackedFace> pairedColorIrFaces = new HashMap<>(irFaces.size());
        for (TrackedFace colorFace : colorFaces) {
            RectF mappedColorFaceRectF = new RectF();
            matrixFromColorToIr.mapRect(mappedColorFaceRectF, new RectF(colorFace.faceRect));
            Rect mappedColorFaceRect = new Rect();
            mappedColorFaceRectF.round(mappedColorFaceRect);
            for (TrackedFace irFace : irFaces) {
                boolean isIntersects = Rect.intersects(mappedColorFaceRect, irFace.faceRect);
                int offsetX = mappedColorFaceRect.centerX() - irFace.faceRect.centerX();
                int offsetY = mappedColorFaceRect.centerY() - irFace.faceRect.centerY();
                float offsetPercentX = offsetX * 1f / mappedColorFaceRect.width();
                float offsetPercentY = offsetY * 1f / mappedColorFaceRect.height();
                if (isIntersects) {
                    float offsetThreshold = 0.12f;
                    if (Math.abs(offsetPercentX) > offsetThreshold || Math.abs(offsetPercentY) > offsetThreshold) {
                        matchFailIrFaces.put(irFace, "与彩色图偏移过大");
                        continue;
                    }
                    if (!colorFrameRect.contains(colorFace.faceRect)) {
                        matchFailIrFaces.put(irFace, "对应的彩图人脸出框");
                        continue;
                    }
                    if (!irFrameRect.contains(irFace.faceRect)) {
                        matchFailIrFaces.put(irFace, "人脸出框");
                        continue;
                    }
                    pairedColorIrFaces.put(colorFace, irFace);
                }
            }
        }
        return pairedColorIrFaces;
    }
}
