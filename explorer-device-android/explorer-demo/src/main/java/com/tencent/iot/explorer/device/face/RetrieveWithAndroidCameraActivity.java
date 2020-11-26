package com.tencent.iot.explorer.device.face;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.tencent.cloud.ai.fr.business.heavystep.ExtractFeatureStep;
import com.tencent.cloud.ai.fr.business.heavystep.PrepareFaceLibraryFromFileStep;
import com.tencent.cloud.ai.fr.business.heavystep.RegStep;
import com.tencent.cloud.ai.fr.business.heavystep.RetrievalStep;
import com.tencent.cloud.ai.fr.business.heavystep.SaveFeaturesToFileStep;
import com.tencent.cloud.ai.fr.business.job.AsyncJobBuilder;
import com.tencent.cloud.ai.fr.business.job.AsyncJobBuilder.PipelineBuilder;
import com.tencent.cloud.ai.fr.business.job.StuffBox;
import com.tencent.cloud.ai.fr.business.job.StuffBox.OnRecycleListener;
import com.tencent.cloud.ai.fr.business.lightstep.PickBestStep;
import com.tencent.cloud.ai.fr.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.business.lightstep.TrackStep;
import com.tencent.cloud.ai.fr.business.thread.AIThreadPool;
import com.tencent.cloud.ai.fr.camera.AndroidCameraManager;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.Frame.Format;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.camera.ICameraManager;
import com.tencent.cloud.ai.fr.camera.ICameraManager.OnFrameArriveListener;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.cloud.ai.fr.widgets.AbsActivityViewController;
import com.tencent.cloud.ai.fr.widgets.FaceDrawView;
import com.tencent.cloud.ai.fr.widgets.FaceDrawView.DrawableFace;
import com.tencent.iot.explorer.device.face.data_template.FaceKitSample;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.youtu.YTFaceRetrieval;
import com.tencent.youtu.YTFaceTracker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RetrieveWithAndroidCameraActivity extends AppCompatActivity {

    private static final String TAG = com.tencent.cloud.ai.fr.RetrieveWithAndroidCameraActivity.class.getSimpleName();

    private ICameraManager mCameraManager;
    private AbsActivityViewController mViewController;

    private FaceKitSample mFaceKitSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AIThreadPool.instance().init(this);//重要!!

        // 恢复人脸库
        new AsyncJobBuilder(new StuffBox(), mRecoverFaceLibraryPipelineBuilder).synthesize(/*合成流水线任务*/).launch(/*执行任务*/);

        // 初始化相机
        mCameraManager = new AndroidCameraManager(this);
        // 监听相机帧回调
        mCameraManager.setOnFrameArriveListener(new OnFrameArriveListener() {//从相机获取帧
            @Override
            public void onFrameArrive(byte[] data, int width, int height, int exifOrientation) {
                //彩图
                Frame colorFrame = new Frame(Format.YUV_NV21, data, width, height, exifOrientation);
                //红外图
                Frame irFrame = null;
                //深度图
                Frame depthFrame = null;

                StuffBox stuffBox = new StuffBox()//创建流水线运行过程所需的物料箱
                        .store(PreprocessStep.IN_RAW_FRAME_GROUP, new FrameGroup(colorFrame, irFrame, depthFrame, true))
                        .setOnRecycleListener(new OnRecycleListener() {
                            @Override
                            public void onRecycle(StuffBox stuffBox) {
                                FrameGroup rawFrameGroup = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP);
                                mCameraManager.onRecycleFrame(rawFrameGroup.colorFrame.data);
                                if (rawFrameGroup.irFrame != null) {/*TODO 回收 stuff.irFrame.data*/}
                                if (rawFrameGroup.depthFrame != null) {/*TODO 回收 stuff.depthFrame.data*/}
                            }
                        });//添加回收监听器

                new AsyncJobBuilder(stuffBox, mRetrievePipelineBuilder).synthesize(/*合成流水线任务*/).launch(/*执行任务*/);
            }
        });

        // 初始化UI
        mViewController = new ViewController(this);
        // 显示UI
        setContentView(mViewController.getRootView());
        // 设置UI按钮
        mViewController.addButton("切换相机", new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AndroidCameraManager) mCameraManager).switchCamera();//切换相机
            }
        });

    }

    /**
     * 恢复人脸库流水线. (运行时人脸库是在内存里的, 每次进程启动时需要恢复内存)
     */
    private final PipelineBuilder mRecoverFaceLibraryPipelineBuilder = new PipelineBuilder()
            .onThread(AIThreadPool.instance().getHeavyThread())
            .addStep(new PrepareFaceLibraryFromFileStep() {
                @Override
                protected File[] onGetFaceFeatureFiles() {//获得人脸特征文件
                    return new File(SaveFeaturesToFileStep.FACE_LIB_PATH).listFiles();
                }
            })
            .addStep(new RegStep(PrepareFaceLibraryFromFileStep.OUT_FACES_FOR_REG) {//注册入库, 达到恢复人脸库的效果
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    boolean shouldContinue = super.onProcess(stuffBox);
                    int size = stuffBox.find(PrepareFaceLibraryFromFileStep.OUT_FACES_FOR_REG).size();
                    final String msg = "恢复人脸库: " + size + "个人脸";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RetrieveWithAndroidCameraActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                    return shouldContinue;
                }
            })
            .submit();

    /**
     * 人脸搜索流水线
     */
    private final PipelineBuilder mRetrievePipelineBuilder = new PipelineBuilder()
            .onThread(AIThreadPool.instance().getLightThread())
            .addStep(new PreprocessStep(PreprocessStep.IN_RAW_FRAME_GROUP))
            .addStep(new TrackStep())
            .addStep(new PickBestStep(TrackStep.OUT_COLOR_FACE))
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.drawLightThreadStuff(stuffBox);//UI绘制
                    return true;
                }
            })
            .submit()
            .onThread(AIThreadPool.instance().getHeavyThread())
            .addStep(new ExtractFeatureStep(TrackStep.OUT_COLOR_FACE))//提取人脸特征, TrackStep.OUT_COLOR_FACE 提取全部人脸特征, PickBestStep.OUT_PICK_OK_FACES: 提取最佳脸特征
            .addStep(new RetrievalStep(ExtractFeatureStep.OUT_FACE_FEATURES))//搜索人脸特征
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.drawHeavyThreadStuff(stuffBox);//UI绘制

                    // 数据 处理
                    Collection<YTFaceTracker.TrackedFace> colorFaces = stuffBox.find(TrackStep.OUT_COLOR_FACE);
                    final List<AbsActivityViewController.FaceResult> faceResults = new ArrayList<>(colorFaces.size());
                    // 每个人脸详细结果
                    for (YTFaceTracker.TrackedFace face : colorFaces) {
                        AbsActivityViewController.FaceResult faceResult = new AbsActivityViewController.FaceResult();
                        faceResults.add(faceResult);

                        if (stuffBox.find(RetrievalStep.OUT_RETRIEVE_RESULTS).containsKey(face)) { //搜索到人脸库中结果。
                            for (YTFaceRetrieval.RetrievedItem i : stuffBox.find(RetrievalStep.OUT_RETRIEVE_RESULTS).get(face)) {
                                Log.i(TAG, String.format("%s, sco=%.1f,sim=%.3f", i.featureId, i.score, i.sim));
                                eventSinglePost(i.featureId, i.score, i.sim);
                            }
                        } else { //没有搜索到人脸库中结果，feature_id传空值，然后其他的都是0。
                            eventSinglePost(null, 0, 0);
                        }
                    }

                    return true;
                }
            })
            .submit();


    private void eventSinglePost(String feature_id, float score, float sim) {
        if (mFaceKitSample == null) {
            Log.d(TAG, "mFaceKitSample is null!");
            return;
        }
        String eventId = "retrieval_result";
        String type = "info";
        JSONObject params = new JSONObject();
        try {
            params.put("threshold", YTSDKManager.FACE_RETRIEVE_THRESHOLD);//识别阈值
            params.put("feature_id",feature_id);//特征id
            params.put("score",score);//分数
            params.put("sim",sim);//相似度
            Long timestamp = System.currentTimeMillis()/1000;
            params.put("timestamp", timestamp.intValue());//图像时间戳
        } catch (JSONException e) {
            Log.d(TAG, "Construct params failed!");
        }
        if(Status.OK != mFaceKitSample.eventSinglePost(eventId, type, params)){
            Log.d(TAG, "single event post failed!");
        }
    }

    private class ViewController extends AbsActivityViewController {

        private FaceDrawView mTracedFaceDrawView;

        ViewController(Context context) {
            super(context);
            mTracedFaceDrawView = new FaceDrawView(context, true);
        }

        @Override
        protected View onCreateCameraPreview() {
            CameraView preview = (CameraView) mCameraManager.getPreview();
            preview.addOverlay(mTracedFaceDrawView);
            return preview;
        }

        @Override
        protected void onDrawColorFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight) {
            mTracedFaceDrawView.onDrawTracedFaces(drawableFaces, frameWidth, frameHeight);
        }

        @Override
        protected void onDrawIrFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight) {
           /* Android has no IR Camera API */
        }

        @Override
        protected void onDrawDepthFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight) {
            /* Android has no Depth Camera API */
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraManager != null) {
            mCameraManager.resumeCamera();
        }
    }

    @Override
    protected void onPause() {
        if (mCameraManager != null) {
            mCameraManager.pauseCamera();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mCameraManager != null) {
            mCameraManager.destroyCamera();
        }
        super.onDestroy();
    }
}
