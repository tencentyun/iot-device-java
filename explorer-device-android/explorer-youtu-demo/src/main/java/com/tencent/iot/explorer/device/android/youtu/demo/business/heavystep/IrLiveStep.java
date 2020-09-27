package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import android.util.Log;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.cloud.ai.fr.utils.Case;
import com.tencent.youtu.YTFaceLiveIR;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 红外活体判断
 * 由于红外活体使用的是另外一个摄像头，因此业务方需要自行保证 **多目摄像头的帧同步**
 * 保证进行活体检测的 frame 和人脸识别的 frame 的时序要一致
 */
public class IrLiveStep extends AbsStep<StuffBox> {

    public static final StuffId<Collection<TrackedFace>> OUT_IR_LIVE_FAILED = new StuffId<>(Collections.EMPTY_LIST);
    public static final StuffId<Collection<TrackedFace>> OUT_IR_LIVE_OK = new StuffId<>(Collections.EMPTY_LIST);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private StuffId<Collection<TrackedFace>> mInputColorFacesId;
    private final StuffId<Map<TrackedFace, TrackedFace>> mInputColorIrFacesId;

    public IrLiveStep(StuffId<Collection<TrackedFace>> colorFacesId, StuffId<Map<TrackedFace, TrackedFace>> matchColorIrFacesId) {
        mInputColorFacesId = colorFacesId;
        mInputColorIrFacesId = matchColorIrFacesId;
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
        stuffBox.store(OUT_IR_LIVE_FAILED, outIrLiveFail);
        stuffBox.store(OUT_IR_LIVE_OK, outIrLiveOk);

        Frame irFrame = inFrameGroup.irFrame;
        if (irFrame == null) {
            return true;
        }
        Frame colorFrame = inFrameGroup.colorFrame;

        long time = System.currentTimeMillis();

        for (TrackedFace colorFace : inColorFaces) {
            Map<TrackedFace, TrackedFace> inputColorIrFaces = stuffBox.find(mInputColorIrFacesId);
            TrackedFace irFace = inputColorIrFaces.get(colorFace);
            if (irFace == null) {
                outIrLiveFail.add(colorFace);
                Log.d(name(), "没找到匹配的红外人脸");
                continue;
            }
            boolean isLive = inSdkManager.mYTFaceLiveIR.detect(
                    colorFace.xy5Points, colorFrame.data, colorFrame.width, colorFrame.height,
                    irFace.xy5Points, irFrame.data, irFrame.width, irFrame.height);
            if (isLive) {
                outIrLiveOk.add(colorFace);
            } else {
                outIrLiveFail.add(colorFace);
            }
            //案例存到磁盘
            new Case("irLive", isLive, time, -1, stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP), -1, YTFaceLiveIR.getVersion()).save();
        }
        return true;
    }
}
