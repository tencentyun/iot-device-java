package com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep;

import android.graphics.Rect;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 4.1 筛选出最佳的一个人脸: 根据业务方自己的规则，自行优选出最佳的人脸, 如人脸大小，人脸数量，人脸位置等规则. 这里示范把 "最大脸" 作为 "最佳脸" */
public class PickBestStep extends AbsStep<StuffBox> {

    /** 优选成功的人脸(优质的人脸) */
    public static final StuffId<Collection<TrackedFace>> OUT_PICK_OK_FACES = new StuffId<>(Collections.EMPTY_LIST);
    /** 优选失败的人脸 {@code Map<colorFace, "优选失败的原因">} */
    public static final StuffId<Map<TrackedFace, String>> OUT_PICK_FAILED_FACES = new StuffId<>(Collections.EMPTY_MAP);
    
    private StuffId<Collection<TrackedFace>> mInputFacesStuffId;

    /** @see PickBestStep */
    public PickBestStep(StuffId<Collection<TrackedFace>> inputFacesStuffId) {
        mInputFacesStuffId = inputFacesStuffId;
    }

    @Override
    public boolean onProcess(StuffBox stuffBox) {
        // 输入
        Collection<TrackedFace> inFaces = stuffBox.find(mInputFacesStuffId);
        // 输出
        Collection<TrackedFace> outOkFaces = new ArrayList<>();
        Map<TrackedFace, String> outFailedFaces = new HashMap<>();
        stuffBox.store(OUT_PICK_OK_FACES, outOkFaces);
        stuffBox.store(OUT_PICK_FAILED_FACES, outFailedFaces);

        // 以下代码示范如何找出面积最大的脸, 可以根据实际业务修改成其它筛选逻辑, 例如"找出最居中的脸" 
        outOkFaces.addAll(getBiggestFace(inFaces));// 最大脸. 理论上只有一个元素, 不需要 List, 但仍然使用 List 是为了保持数据结构统一, 方便以后业务调整

        List<TrackedFace> optimizeFailFaces = new ArrayList<>(inFaces); // 最大脸. 理论上只有一个元素, 不需要 List, 但仍然使用 List 是为了保持数据结构统一, 方便以后业务调整
        optimizeFailFaces.removeAll(outOkFaces);//除去最大脸, 剩下就是失败脸

        for (TrackedFace failFace : optimizeFailFaces) {
            outFailedFaces.put(failFace, "不是最大脸");
        }

        return true;//告诉流水线此任务是否成功, 如果成功则会执行下游任务, 否则中断流水线
    }

    /** 选出面积最大的脸 */
    private Collection<TrackedFace> getBiggestFace(Collection<TrackedFace> faces) {
        List<TrackedFace> results = new ArrayList<>();

        if (faces.size() > 0) {
            int maxArea = -1;
            TrackedFace bigger = null;
            for (TrackedFace face : faces) {
                Rect faceRect = face.faceRect;
                int area = faceRect.width() * faceRect.height();
                if (area > maxArea) {
                    maxArea = area;
                    bigger = face;
                }
            }
            results.add(bigger);
        }
        return results;
    }
}
