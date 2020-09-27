package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.FloatsFileHelper;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * 从文件恢复人脸库的前置步骤
 */
public abstract class PrepareFaceLibraryFromFileStep extends AbsStep<StuffBox> {

    public static final StuffId<Collection<FaceForReg>> OUT_FACES_FOR_REG = new StuffId<>(Collections.EMPTY_LIST);

    protected abstract File[] onGetFaceFeatureFiles();

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        //输入
        File[] featureFiles = onGetFaceFeatureFiles();
        //输出
        Collection<FaceForReg> outFaceForRegs = new ArrayList<>();
        stuffBox.store(OUT_FACES_FOR_REG, outFaceForRegs);

        for (File featFile : featureFiles) {
            float[] feat = FloatsFileHelper.readFloatsFromFile(featFile.getAbsolutePath(), YTSDKManager.FACE_FEAT_LENGTH);
            FaceForReg faceForReg = new FaceForReg(null, featFile.getName(), feat);
            outFaceForRegs.add(faceForReg);
        }

        return true;
    }
}
