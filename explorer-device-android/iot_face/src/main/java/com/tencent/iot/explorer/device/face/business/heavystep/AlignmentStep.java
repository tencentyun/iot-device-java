package com.tencent.iot.explorer.device.face.business.heavystep;

import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.StuffId;
import com.tencent.iot.explorer.device.face.business.lightstep.PreprocessStep;
import com.tencent.youtu.YTFaceAlignment.FaceShape;
import com.tencent.youtu.YTFaceAlignment.FaceStatus;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 人脸遮挡, 表情, 瞳间距判断 */
public class AlignmentStep extends AbsStep<StuffBox> {

    public static final StuffId<Map<TrackedFace, FaceShape>> OUT_FACE_SHAPE = new StuffId<>(Collections.EMPTY_MAP);
    public static final StuffId<Map<TrackedFace, FaceStatus>> OUT_FACE_FACE_STATUS = new StuffId<>(Collections.EMPTY_MAP);
    public static final StuffId<Map<TrackedFace, String>> OUT_ALIGNMENT_FAILED_FACES = new StuffId<>(Collections.EMPTY_MAP);
    public static final StuffId<Collection<TrackedFace>> OUT_ALIGNMENT_OK_FACES = new StuffId<>(Collections.EMPTY_LIST);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private final StuffId<Collection<TrackedFace>> mInputFacesId;

    public AlignmentStep(StuffId<Collection<TrackedFace>> inputFacesId) {
        mInputFacesId = inputFacesId;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager ytsdkManager = stuffBox.getCurrentThreadSdkManager();
        
        FrameGroup convertedFrameGroup = stuffBox.find(mInputFrameGroupId);
        Collection<TrackedFace> tracePipelineOkFaces = stuffBox.find(mInputFacesId);
        // 输出
        Map<TrackedFace, FaceShape> outFaceShape = new HashMap<>();
        Map<TrackedFace, FaceStatus> outFaceStatus = new HashMap<>();
        Map<TrackedFace, String> outAlignmentFail = new HashMap<>();
        Collection<TrackedFace> outAlignmentOK = new ArrayList<>();
        stuffBox.store(OUT_FACE_SHAPE, outFaceShape);
        stuffBox.store(OUT_FACE_FACE_STATUS, outFaceStatus);
        stuffBox.store(OUT_ALIGNMENT_FAILED_FACES, outAlignmentFail);
        stuffBox.store(OUT_ALIGNMENT_OK_FACES, outAlignmentOK);

        Frame colorFrame = convertedFrameGroup.colorFrame;

        for (TrackedFace face : tracePipelineOkFaces) {
            FaceShape shape = ytsdkManager.mYTFaceAlignment.align(colorFrame.data, colorFrame.width, colorFrame.height, face.faceRect);
            outFaceShape.put(face, shape);

            FaceStatus status = ytsdkManager.mYTFaceAlignment.getStatus(shape);

            // 遮挡检测
            if (status.leftEyebrowBlock <= 80 ||
                    status.rightEyebrowBlock <= 80 ||
                    status.leftEyeBlock <= 80 ||
                    status.rightEyeBlock <= 80 ||
                    status.noseBlock <= 60 ||
                    status.mouthBlock <= 50 ||
                    status.leftProfileBlock <= 70 ||
                    status.chinBlock <= 70 ||
                    status.rightProfileBlock <= 70) {
                outAlignmentFail.put(face, "人脸遮挡");
                continue;
            }

            // 闭眼、张嘴检测
            if (status.mouthOpen > 1f) {
                outAlignmentFail.put(face, "表情不满足: 张嘴");
                continue;
            }
            if (status.leftEyeOpen < 0.1f || status.rightEyeOpen < 0.1f) {
                outAlignmentFail.put(face, "表情不满足: 闭眼");
                continue;
            }

            // 将瞳间距检测
            if (status.pupilDist < 60/*像素*/) {
                outAlignmentFail.put(face, "瞳间距不满足");
                continue;
            }

            // 通过上面层层检查后的合格人脸
            outAlignmentOK.add(face);
        }
        
        return true;
    }
}
