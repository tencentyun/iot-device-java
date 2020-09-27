package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 人脸提特征 */
public class ExtractFeatureStep extends AbsStep<StuffBox> {

    public static final StuffId<Map<TrackedFace, float[]>> OUT_FACE_FEATURES = new StuffId<>(Collections.EMPTY_MAP);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private final StuffId<Collection<TrackedFace>> mInputFacesId;

    public ExtractFeatureStep(StuffId<Collection<TrackedFace>> inputFacesId) {
        mInputFacesId = inputFacesId;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        FrameGroup inFrameGroup = stuffBox.find(mInputFrameGroupId);
        Collection<TrackedFace> inColorFaces = stuffBox.find(mInputFacesId);
        // 输出
        Map<TrackedFace, float[]> outFeatureMap = new HashMap<>();
        stuffBox.store(OUT_FACE_FEATURES, outFeatureMap);

        Frame colorFrame = inFrameGroup.colorFrame;
        for (TrackedFace face : inColorFaces) {
            float[] feature = new float[YTSDKManager.FACE_FEAT_LENGTH];
            int ret = inSdkManager.mYTFaceFeature.extract(face.xy5Points, colorFrame.data, colorFrame.width, colorFrame.height, feature);
            if (ret != 0) {
                // outExtractFail.put(face, "YTFaceFeature.extract() return code = " + ret);
                continue;
            }
            outFeatureMap.put(face, feature);
        }
        return true;
    }
}
