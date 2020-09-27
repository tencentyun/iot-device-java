package com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep;

import android.graphics.Rect;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 3. 过滤掉不符合要求的人脸: 业务方可以根据人脸的角度，人脸位置过滤掉不符合要求的人脸 */
public class FilterStep extends AbsStep<StuffBox> {

    /** 已通过过滤的人脸(已剔除不合格的人脸) */
    public static final StuffId<Collection<TrackedFace>> OUT_FILTER_OK_FACES = new StuffId<>(Collections.EMPTY_LIST);
    /** 不能通过过滤的人脸(不合格的人脸) */
    public static final StuffId<Map<TrackedFace, String>> OUT_FILTER_FAIL_FACES = new StuffId<>(Collections.EMPTY_MAP);

    private StuffId<Collection<TrackedFace>> mInputFacesStuffId;

    /** @see FilterStep */
    public FilterStep(StuffId<Collection<TrackedFace>> inputFacesStuffId) {
        mInputFacesStuffId = inputFacesStuffId;
    }

    @Override
    public boolean onProcess(StuffBox stuffBox) {
        // 输入
        Collection<TrackedFace> inputFaces = stuffBox.find(mInputFacesStuffId);
        // 输出
        Collection<TrackedFace> outFilterOkFaces = new ArrayList<>();
        Map<TrackedFace, String> outFilterFailFaces = new HashMap<>();
        stuffBox.store(OUT_FILTER_OK_FACES, outFilterOkFaces);
        stuffBox.store(OUT_FILTER_FAIL_FACES, outFilterFailFaces);
        
        if (inputFaces != null) {
            for (TrackedFace face : inputFaces) {
                if (Math.abs(face.pitch) > 30 || Math.abs(face.yaw) > 30 || Math.abs(face.roll) > 30) {
                    String msg = "姿态不符合要求";
                    outFilterFailFaces.put(face, msg);
                    continue;
                }
                Rect faceRect = face.faceRect;
                if (faceRect.width() < 60 || faceRect.height() < 60) {
                    String msg = "人脸太小";
                    outFilterFailFaces.put(face, msg);
                    continue;
                }
                FrameGroup convertedFrameGroup = stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP);
                if (faceRect.left <= 0 || faceRect.top <= 0
                        || faceRect.left + faceRect.width() >= convertedFrameGroup.colorFrame.width
                        || faceRect.top + faceRect.height() >= convertedFrameGroup.colorFrame.height) {
                    String msg = "人脸出框，不完整";
                    outFilterFailFaces.put(face, msg);
                    continue;
                }
                outFilterOkFaces.add(face);
            }
        }
        
        return true;//告诉流水线此任务是否成功, 如果成功则会执行下游任务, 否则中断流水线
    }
}
