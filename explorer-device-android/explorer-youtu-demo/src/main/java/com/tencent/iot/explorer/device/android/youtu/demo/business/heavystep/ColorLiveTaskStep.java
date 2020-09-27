package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.cloud.ai.fr.utils.Case;
import com.tencent.youtu.YTFaceLiveColor;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 彩图活体判断 */
public class ColorLiveTaskStep extends AbsStep<StuffBox> {

    public static final StuffId<Collection<TrackedFace>> OUT_COLOR_LIVE_OK = new StuffId<>(Collections.EMPTY_LIST);
    public static final StuffId<Map<TrackedFace, Float>> OUT_COLOR_LIVE_FAILED = new StuffId<>(Collections.EMPTY_MAP);
    public static final StuffId<Map<TrackedFace, Float>> OUT_COLOR_LIVE_OK_SCORE = new StuffId<>(Collections.EMPTY_MAP);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private final StuffId<Collection<TrackedFace>> mInputFacesId;

    public ColorLiveTaskStep(StuffId<Collection<TrackedFace>> inputFacesId) {
        mInputFacesId = inputFacesId;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        FrameGroup inFrameGroup = stuffBox.find(mInputFrameGroupId);
        Collection<TrackedFace> inFaces = stuffBox.find(mInputFacesId);
        // 输出
        Collection<TrackedFace> outColorLiveOk = new ArrayList<>();
        Map<TrackedFace, Float> outColorLiveOkScore = new HashMap<>();
        Map<TrackedFace, Float> outColorLiveFailed = new HashMap<>();
        stuffBox.store(OUT_COLOR_LIVE_OK, outColorLiveOk);
        stuffBox.store(OUT_COLOR_LIVE_FAILED, outColorLiveFailed);
        stuffBox.store(OUT_COLOR_LIVE_OK_SCORE, outColorLiveOkScore);

        Frame colorFrame = inFrameGroup.colorFrame;
        YTFaceLiveColor faceLiveColor = inSdkManager.mYTFaceLiveColor;
        long time = System.currentTimeMillis();
        for (TrackedFace face : inFaces) {
            boolean live = faceLiveColor.detect(face.xy5Points, colorFrame.data, colorFrame.width, colorFrame.height);
            float liveColorScore = faceLiveColor.getScore();

            if (live) {
                //放到成功结果集
                outColorLiveOk.add(face);
                outColorLiveOkScore.put(face, liveColorScore);
            } else {
                outColorLiveFailed.put(face, liveColorScore);
            }
            //案例存到磁盘
            new Case("colorLive", live, time, liveColorScore, inFrameGroup, -1, YTFaceLiveColor.getVersion()).save();
        }
        return true;
    }
}
