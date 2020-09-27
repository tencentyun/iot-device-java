package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import android.util.Log;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.youtu.YTFaceRetrieval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class RegStep extends AbsStep<StuffBox> {

    /** 入参ID: 需要注册的人脸集合 */
    public static final StuffId<Collection<FaceForReg>> IN_FACES = new StuffId<>(Collections.EMPTY_LIST);
    /** 人脸库中人脸数量 */
    public static final StuffId<Integer> OUT_FACE_LIB_FEATURES_COUNT = new StuffId<>(0);
    
    private  StuffId<Collection<FaceForReg>> mInputId;
    private InputProvider mInputProvider;

    public RegStep(StuffId<Collection<FaceForReg>> inputId) {
        if (inputId == null) {
            throw new NullPointerException("inputId can not be null");
        }
        mInputId = inputId;
    }

    public RegStep(InputProvider inputProvider) {
        if (inputProvider == null) {
            throw new NullPointerException("inputProvider can not be null");
        }
        mInputProvider = inputProvider;
    }

    public interface InputProvider {
        Collection<FaceForReg> onGetInput(StuffBox stuffBox);
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        //输入
        YTSDKManager inSdkManager = stuffBox.getCurrentThreadSdkManager();
        Collection<FaceForReg> faces;
        if (mInputProvider != null) {
            faces = mInputProvider.onGetInput(stuffBox);
            if (faces == null) {
                throw new NullPointerException("RegFaceStep.InputProvider.onGetInput() can not return null");
            }
        } else {
            faces = stuffBox.find(mInputId);
        }

        stuffBox.store(IN_FACES, faces);//给入参赋予约定的ID, 方便流水线其它步骤使用
        
        Collection<FaceForReg> filteredFaces = new ArrayList<>();
        int oneFeatureLength = 0;
        for (FaceForReg f : faces) {
            if (f.name != null
                    && !f.name.isEmpty()
                    && f.feature != null
                    && f.feature.length > 0) {
                filteredFaces.add(f);
                oneFeatureLength = f.feature.length;
            }
        }
        int count = filteredFaces.size();

        String[] allNames = new String[count];
        float[][] allFeatures = new float[count][oneFeatureLength];

        int i = 0;
        for (FaceForReg faceToBeReg : filteredFaces) {
            allNames[i] = faceToBeReg.name;
            allFeatures[i] = faceToBeReg.feature;
            i++;
        }

        YTFaceRetrieval retriever = inSdkManager.mYTFaceRetriever;
        int code = retriever.insertFeatures(allFeatures, allNames);
        if (code == 0) {
            int featureCount = retriever.queryFeatureNum();
            //输出
            stuffBox.store(OUT_FACE_LIB_FEATURES_COUNT, featureCount);
        }
        if (code != 0) {
            Log.w(name(), "insertFeatures() code=" + code);
        }
        return true;
    }

}
