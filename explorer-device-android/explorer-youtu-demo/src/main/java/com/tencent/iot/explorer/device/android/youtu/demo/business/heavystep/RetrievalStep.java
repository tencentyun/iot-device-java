package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import android.util.Log;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.youtu.YTFaceRetrieval.RetrievedItem;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 人脸检索 */
public class RetrievalStep extends AbsStep<StuffBox> {

    public static final StuffId<Map<TrackedFace, RetrievedItem[]>> OUT_RETRIEVE_RESULTS = new StuffId<>(Collections.EMPTY_MAP);

    private StuffId<Map<TrackedFace, float[]>> mInFaceFeatureIds;

    public RetrievalStep(StuffId<Map<TrackedFace, float[]>> inFaceFeatureIds) {
        mInFaceFeatureIds = inFaceFeatureIds;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        Map<TrackedFace, float[]> extractOkResults = stuffBox.find(mInFaceFeatureIds);
        // 输出
        Map<TrackedFace, RetrievedItem[]> outRetrieveResult = new HashMap<>();
        stuffBox.store(OUT_RETRIEVE_RESULTS, outRetrieveResult);

        for (TrackedFace face : extractOkResults.keySet()) {
            float[] feat = extractOkResults.get(face);
            int topN = 1;//返回最相似的 N 个结果
            RetrievedItem[] retrievedItems = inSdkManager.mYTFaceRetriever.retrieve(feat, topN, YTSDKManager.FACE_RETRIEVE_THRESHOLD);
            if (retrievedItems == null) {
                Log.w(name(), "人脸检索返回异常, 请检查SDK授权是否成功");
            } else if (retrievedItems.length > 0) {
                outRetrieveResult.put(face, retrievedItems);
            }
        }

        return true;
    }
}
