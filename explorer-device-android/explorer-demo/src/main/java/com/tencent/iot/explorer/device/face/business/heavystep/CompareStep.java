package com.tencent.iot.explorer.device.face.business.heavystep;

import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.StuffId;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** 人脸比对 */
public class CompareStep extends AbsStep<StuffBox> {

    public static final StuffId<float[]> IN_GIVEN_FEATURE = new StuffId<>(new float[0]);
    public static final StuffId<Map<TrackedFace, Float>> OUT_COMPARE_SCORE = new StuffId<>(Collections.EMPTY_MAP);

    private final StuffId<float[]> mGivenFeatureId;
    private final StuffId<Map<TrackedFace, float[]>> mFeaturesToBeCompare;

    public CompareStep(StuffId<float[]> givenFeatureId, StuffId<Map<TrackedFace, float[]>> featuresToBeCompare) {
        mGivenFeatureId = givenFeatureId;
        mFeaturesToBeCompare = featuresToBeCompare;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        //输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        float[] givenFeature = stuffBox.find(mGivenFeatureId);
        Map<TrackedFace, float[]> inFeatures = stuffBox.find(mFeaturesToBeCompare);
        //输出
        Map<TrackedFace, Float> outCompareScore = new HashMap<>();
        stuffBox.store(OUT_COMPARE_SCORE, outCompareScore);

        for (Entry<TrackedFace, float[]> entry : inFeatures.entrySet()) {
            float score = inSdkManager.mYTFaceRetriever.compare(givenFeature, entry.getValue(), true);
            outCompareScore.put(entry.getKey(), score);
        }
        return true;
    }
}
