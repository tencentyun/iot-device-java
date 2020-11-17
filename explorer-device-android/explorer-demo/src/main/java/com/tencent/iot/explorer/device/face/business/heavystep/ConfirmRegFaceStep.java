package com.tencent.iot.explorer.device.face.business.heavystep;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.AnyThread;
import android.text.TextUtils;

import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.utils.ImageConverter;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.StuffId;
import com.tencent.iot.explorer.device.face.business.lightstep.PreprocessStep;
import com.tencent.youtu.YTFaceTracker.TrackedFace;
import com.tencent.youtu.YTImage;
import com.tencent.youtu.YTUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 1:N 场景手动(UI交互)注册人脸 */
public abstract class ConfirmRegFaceStep extends AbsStep<StuffBox> {

    public static final StuffId<Map<TrackedFace, String>> OUT_CONFIRMED_FACES = new StuffId<>(Collections.EMPTY_MAP);

    private final StuffId<FrameGroup> mInputFrameGroupId = PreprocessStep.OUT_CONVERTED_FRAME_GROUP;
    private StuffId<Collection<TrackedFace>> mInputColorFacesId;
    private InputProvider mInputFacesProvider;

    public ConfirmRegFaceStep(StuffId<Collection<TrackedFace>> inputColorFacesId) {
        if (inputColorFacesId == null) {
            throw new NullPointerException("inputColorFacesId can not be null");
        }
        mInputColorFacesId = inputColorFacesId;
    }

    public ConfirmRegFaceStep(InputProvider inputFacesProvider) {
        if (inputFacesProvider == null) {
            throw new NullPointerException("inputFacesProvider can not be null");
        }
        mInputFacesProvider = inputFacesProvider;
    }
    
      /**
     * 由用户手动决定是否选择此脸用于注册
     * @param faceForConfirm 候选人脸, {@link FaceForConfirm#faceBmp} 可用于 UI 预览, 如果决定注册, 那么请为人脸设置名字 {@link FaceForConfirm#name}
     * @return 确认是否注册此人脸
     */
    @AnyThread
    protected abstract boolean onConfirmFace(FaceForConfirm faceForConfirm);

    public interface InputProvider {

        Collection<TrackedFace> onGetInput(StuffBox stuffBox);
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        // 输入
        FrameGroup inFrameGroup = stuffBox.find(mInputFrameGroupId);
        Collection<TrackedFace> inColorFaces = null;
        if (mInputFacesProvider != null) {
            inColorFaces = mInputFacesProvider.onGetInput(stuffBox);
        } else if (mInputColorFacesId != null) {
            inColorFaces = stuffBox.find(mInputColorFacesId);
        }
        // 输出
        Map<TrackedFace, String> outConfirmedFaces = new HashMap<>();
        stuffBox.store(OUT_CONFIRMED_FACES, outConfirmedFaces);

        for (TrackedFace face : inColorFaces) {
            Bitmap faceBitmap = getFaceBitmap(inFrameGroup.colorFrame, face.faceRect);// 裁出人脸小图
            FaceForConfirm confirmedFace = new FaceForConfirm(face, "张三", faceBitmap);
            if (onConfirmFace(confirmedFace)) {
                if (TextUtils.isEmpty(confirmedFace.name)) {
                    throw new IllegalArgumentException("人脸名称不能为空");
                }
                outConfirmedFaces.put(confirmedFace.face, confirmedFace.name);
            }
        }

        return true;//流水线结束, 原料 stuff.rawFrameGroup 会被回收
    }

    /** 从帧中裁剪出人脸图片 */
    private Bitmap getFaceBitmap(Frame colorFrame, Rect faceRect) {
        YTImage faceImage = YTUtils.cropRGB888(colorFrame.data, colorFrame.width, colorFrame.height, faceRect);
        return ImageConverter.rgbToBitmap(faceImage.data, faceImage.width, faceImage.height);
    }

}
