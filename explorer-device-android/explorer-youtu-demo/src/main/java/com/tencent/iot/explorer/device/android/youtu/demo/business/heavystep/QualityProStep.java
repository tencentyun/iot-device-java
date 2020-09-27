package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.youtu.YTFaceQualityPro;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 人脸质量判断: 正面, 遮挡, 模糊, 光照 */
public class QualityProStep extends AbsStep<StuffBox> {

    /**
     * 脸部正面角度评分阈值
     * <p/>
     * float[] scores = {@link YTFaceQualityPro#evaluate(float[], byte[], int, int)}; <br/>
     * 其中 scores[0] 为正面角度评分, 分数越高表示人脸越正面。<br/>
     */
    final float FACE_QUALITY_SCORE_FACING_THRESHOLD = 0.7783f;

    /**
     * 脸部可见度评分阈值
     * <p/>
     * float[] scores = {@link YTFaceQualityPro#evaluate(float[], byte[], int, int)}; <br/>
     * 其中 scores[1] 为脸部可见度评分, 分数越高表示越没有遮挡。 <br/>
     */
    final float FACE_QUALITY_SCORE_VISIBILITY_THRESHOLD = 0.7001f;

    /**
     * 清晰度评分阈值
     * <p/>
     * float[] scores = {@link YTFaceQualityPro#evaluate(float[], byte[], int, int)}; <br/>
     * 其中 scores[2] 为清晰度评分, 分数越高表示人脸越清晰。<br/>
     */
    final float FACE_QUALITY_SCORE_SHARP_THRESHOLD = 0.4575f;

    /**
     * 针对 <b>亮光</b> 场景的亮度评分阈值
     * <p/>
     * float[] scores = {@link YTFaceQualityPro#evaluate(float[], byte[], int, int)}; <br/>
     * 其中 scores[3] 为亮度评分, 分数越高表示越明亮<br/>
     */
    final float FACE_QUALITY_SCORE_BRIGHT_ENV_THRESHOLD = 0.75f;

    /**
     * 针对 <b>暗光</b> 场景的亮度评分阈值
     * <p/>
     * float[] scores = {@link YTFaceQualityPro#evaluate(float[], byte[], int, int)}; <br/>
     * 其中 scores[3] 为亮度评分, 分数越高表示越明亮<br/>
     */
    final float FACE_QUALITY_SCORE_DARK_ENV_THRESHOLD = 0.3627f;

    /**
     * float[] scores = {@link YTFaceQualityPro#evaluate(float[], byte[], int, int)}; <br/>
     * 其中 scores[3] 为亮度评分<br/>
     * 这个变量用于指定采用暗光还是亮光的阈值.
     */
    final boolean isBrightEnvironmentMode = false;

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private final StuffId<Collection<TrackedFace>> mInputFacesId;

    public static final StuffId<Map<TrackedFace, String>> OUT_QUALITY_PRO_FAILED = new StuffId<>(Collections.EMPTY_MAP);
    public static final StuffId<Collection<TrackedFace>> OUT_QUALITY_PRO_OK = new StuffId<>(Collections.EMPTY_LIST);

    public QualityProStep(StuffId<Collection<TrackedFace>> inputFacesId) {
        mInputFacesId = inputFacesId;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        FrameGroup inFrameGroup = stuffBox.find(mInputFrameGroupId);
        Collection<TrackedFace> inFaces = stuffBox.find(mInputFacesId);
        // 输出
        Map<TrackedFace, String> outQualityProFail = new HashMap<>();
        Collection<TrackedFace> outQualityProOk = new ArrayList<>();
        stuffBox.store(OUT_QUALITY_PRO_FAILED, outQualityProFail);
        stuffBox.store(OUT_QUALITY_PRO_OK, outQualityProOk);

        Frame colorFrame = inFrameGroup.colorFrame;

        for (TrackedFace face : inFaces) {
            float[] qualities = inSdkManager.mYTFaceQualityPro.evaluate(face.xy5Points, colorFrame.data, colorFrame.width, colorFrame.height);
            if (qualities == null || qualities.length == 0) {
                outQualityProFail.put(face, "FaceQualityPro 无结果");
                continue;
            }

            float facingScore = qualities[0];      // 角度：分数越低，角度越大。
            float visibilityScore = qualities[1];  // 遮挡：分数越低，遮挡程度越严重。
            float sharpScore = qualities[2];       // 模糊：分数越低，模糊程度越严重。
            float brightnessScore = qualities[3];  // 光线：分数越低，光线越暗，分数越高，光线越亮。

            // 注：由于不同维度的退化会存在相互影响，推荐使用优先级顺序为光线 -> 模糊 -> 角度 -> 遮挡。
            // (如遮挡分数需保证光线，模糊，角度都在正常分数范围内才能准确反映遮挡程度)
            if ((isBrightEnvironmentMode && brightnessScore < FACE_QUALITY_SCORE_BRIGHT_ENV_THRESHOLD)
                    || (!isBrightEnvironmentMode && brightnessScore < FACE_QUALITY_SCORE_DARK_ENV_THRESHOLD)) {
                outQualityProFail.put(face, "太暗");
                continue;
            } else if (sharpScore < FACE_QUALITY_SCORE_SHARP_THRESHOLD) {
                outQualityProFail.put(face, "模糊");
                continue;
            } else if (facingScore < FACE_QUALITY_SCORE_FACING_THRESHOLD) {
                outQualityProFail.put(face, "不够正面");
                continue;
            } else if (visibilityScore < FACE_QUALITY_SCORE_VISIBILITY_THRESHOLD) {
                outQualityProFail.put(face, "遮挡");
                continue;
            }

            outQualityProOk.add(face);
        }
        return true;
    }
}
