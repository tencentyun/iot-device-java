package com.tencent.iot.explorer.device.android.youtu.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.cameraview.CameraView;
import com.tencent.iot.explorer.device.android.youtu.demo.business.thread.AIThreadPool;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.AsyncJobBuilder;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.AsyncJobBuilder.PipelineBuilder;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox.OnRecycleListener;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.AlignmentStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.ConfirmRegFaceStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.ExtractFeatureStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.FaceForConfirm;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.FaceForReg;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.QualityProStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.SaveFeaturesToFileStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep.SaveFeaturesToFileStep.InputProvider;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.FilterStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PickBestStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.PreprocessStep;
import com.tencent.iot.explorer.device.android.youtu.demo.business.lightstep.TrackStep;
import com.tencent.iot.explorer.device.android.youtu.demo.camera.AndroidCameraManager;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.Frame.Format;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.camera.ICameraManager;
import com.tencent.cloud.ai.fr.camera.ICameraManager.OnFrameArriveListener;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.iot.explorer.device.android.youtu.demo.widgets.AbsActivityViewController;
import com.tencent.iot.explorer.device.android.youtu.demo.widgets.ConfirmRegFaceViewController;
import com.tencent.iot.explorer.device.android.youtu.demo.widgets.ConfirmRegFaceViewController.ConfirmResult;
import com.tencent.iot.explorer.device.android.youtu.demo.widgets.FaceDrawView;
import com.tencent.iot.explorer.device.android.youtu.demo.widgets.FaceDrawView.DrawableFace;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class RegWithAndroidCameraActivity extends AppCompatActivity {

    private ICameraManager mCameraManager;
    private AbsActivityViewController mViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AIThreadPool.instance().init(this);//重要!!
        
        // 初始化UI
        mViewController = new ViewController(this);
        // 设置UI按钮
        mViewController.addButton("切换相机", new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AndroidCameraManager) mCameraManager).switchCamera();//切换相机
            }
        });
        
        // 显示UI
        setContentView(mViewController.getRootView());
        
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

                new AsyncJobBuilder(stuffBox, mRegFromCameraPipelineBuilder).synthesize(/*合成流水线任务*/).launch(/*执行任务*/);
            }
        });
    }

    /**
     * 相机注册人脸流水线
     */
    private PipelineBuilder mRegFromCameraPipelineBuilder = new PipelineBuilder()
            .onThread(AIThreadPool.instance().getHeavyThread())
            .addStep(new PreprocessStep(PreprocessStep.IN_RAW_FRAME_GROUP))//【必选】图像预处理
            .addStep(new TrackStep())//【必选】人脸检测跟踪, 是后续任何人脸算法的前提
            .addStep(new FilterStep(TrackStep.OUT_COLOR_FACE))//【可选】过滤掉不合格的人脸, 推荐使用, 保证入库质量
            .addStep(new PickBestStep(FilterStep.OUT_FILTER_OK_FACES))//【必选】选出最佳的1个人脸, 因为后续步骤设计不支持一次注册多个人脸 
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.drawLightThreadStuff(stuffBox);//UI绘制人脸追踪框
                    return true;
                }
            })
            .submit()
            .onThread(AIThreadPool.instance().getHeavyThread())
            .addStep(new AlignmentStep(PickBestStep.OUT_PICK_OK_FACES))//【可选】人脸遮挡, 表情, 瞳间距判断, 推荐使用, 保证入库质量
            .addStep(new QualityProStep(AlignmentStep.OUT_ALIGNMENT_OK_FACES))//【可选】人脸质量判断: 正面, 遮挡, 模糊, 光照, 推荐使用, 保证入库质量
            .addStep(new ExtractFeatureStep(QualityProStep.OUT_QUALITY_PRO_OK))//【必选】提取人脸特征
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.drawHeavyThreadStuff(stuffBox);//UI绘制人脸详细信息
                    return true;
                }
            })
            .addStep(
                    new ConfirmRegFaceStep(new ConfirmRegFaceStep.InputProvider() {//【可选】UI 询问用户是否确认注册当前人脸
                        @Override
                        public Collection<TrackedFace> onGetInput(StuffBox stuffBox) {//筛选出已成功提取特征的人脸
                            Map<TrackedFace, float[]> faceMap = stuffBox.find(ExtractFeatureStep.OUT_FACE_FEATURES);
                            return faceMap.keySet();
                        }
                    }) {
                        @Override
                        protected boolean onConfirmFace(FaceForConfirm faceForConfirm) {
                            //等待用户操作 UI 
                            ConfirmResult result = new ConfirmRegFaceViewController().waitConfirm(RegWithAndroidCameraActivity.this, faceForConfirm.faceBmp, faceForConfirm.name);
                            if (result.code == ConfirmResult.RESULT_OK) {
                                faceForConfirm.name = result.name;
                                return true;// true: 确认注册此人脸
                            }
                            if (result.code == ConfirmResult.RESULT_CANCEL) {
                                RegWithAndroidCameraActivity.this.finish();
                            }
                            return false;
                        }
                    })
            .addStep(new SaveFeaturesToFileStep(new InputProvider() {//【必选】把人脸特征保存到磁盘, 用于下次程序启动时恢复人脸库
                @Override
                public Collection<FaceForReg> onGetInput(StuffBox stuffBox) {
                    Map<TrackedFace, float[]> faceAndFeatures = stuffBox.find(ExtractFeatureStep.OUT_FACE_FEATURES);
                    Map<TrackedFace, String> faceAndNames = stuffBox.find(ConfirmRegFaceStep.OUT_CONFIRMED_FACES);
                    Collection<FaceForReg> out = new ArrayList<>();
                    for (Entry<TrackedFace, String> faceAndName : faceAndNames.entrySet()) {
                        TrackedFace face = faceAndName.getKey();
                        String name = faceAndName.getValue();
                        float[] feature = faceAndFeatures.get(face);
                        out.add(new FaceForReg(face, name, feature));
                    }
                    return out;
                }
            }))
            .submit();

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
