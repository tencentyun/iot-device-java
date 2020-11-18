package com.tencent.iot.explorer.device.face;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.face.business.heavystep.AlignmentStep;
import com.tencent.iot.explorer.device.face.business.heavystep.ExtractFeatureStep;
import com.tencent.iot.explorer.device.face.business.heavystep.FaceForReg;
import com.tencent.iot.explorer.device.face.business.heavystep.FileToFrameStep;
import com.tencent.iot.explorer.device.face.business.heavystep.QualityProStep;
import com.tencent.iot.explorer.device.face.business.heavystep.RegStep;
import com.tencent.iot.explorer.device.face.business.heavystep.RegStep.InputProvider;
import com.tencent.iot.explorer.device.face.business.heavystep.SaveFeaturesToFileStep;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.SyncJobBuilder;
import com.tencent.iot.explorer.device.face.business.lightstep.PreprocessStep;
import com.tencent.iot.explorer.device.face.business.lightstep.TrackStep;
import com.tencent.iot.explorer.device.face.business.thread.AIThreadPool;
import com.tencent.iot.explorer.device.face.widgets.FakeCameraActivityViewController;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RegWithFileActivity extends AppCompatActivity {

    private static final String TAG = RegWithFileActivity.class.getSimpleName();

    private FakeCameraActivityViewController mViewController;

    /** 需要注册的人脸图片文件夹 */
    private static final String FACE_IMG_DIR = "/sdcard/face_for_reg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AIThreadPool.instance().init(this);//重要!!

        // 初始化相机
        // 初始化UI
        mViewController = new FakeCameraActivityViewController(this);
        // 显示UI
        setContentView(mViewController.getRootView());

        new Thread() {
            @Override
            public void run() {
                YTSDKManager sdkMgr = new YTSDKManager(getAssets());//每个线程都必须使用单独的算法SDK实例, 这里为当前线程生成一份实例

                for (File file : getFaceImageFiles()) {
                    //创建流水线运行过程所需的物料箱
                    StuffBox stuffBox = new StuffBox()
                            .store(FileToFrameStep.IN_FILE, file)
                            .setSdkManagerForThread(sdkMgr, Thread.currentThread().getId());
                    //创建任务
                    new AbsJob<>(stuffBox, regPipeline).run(/*直接在当前线程执行任务*/);
                }

                sdkMgr.destroy();//销毁算法SDK实例
            }
        }.start();

    }

    private final List<AbsStep<StuffBox>> regPipeline = new SyncJobBuilder()
            .addStep(new FileToFrameStep(FileToFrameStep.IN_FILE))
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.setPreviewImageColor(stuffBox.find(FileToFrameStep.OUT_BITMAP));//显示帧预览
                    return true;
                }
            })
            .addStep(new PreprocessStep(FileToFrameStep.OUT_FRAME_GROUP))//【必选】图像预处理
            .addStep(new TrackStep())//【必选】人脸检测跟踪, 是后续任何人脸算法的前提
            .addStep(new AbsStep<StuffBox>() {//检测上一个步骤的结果, 决定流水线是否能继续
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.drawLightThreadStuff(stuffBox);//显示人脸检测结果
                    Collection<TrackedFace> faces = stuffBox.find(TrackStep.OUT_COLOR_FACE);
                    int size = faces.size();
                    if (size != 1) {
                        String msg = "图片中" + (size == 0 ? "检测不到" : "多于一张") + "人脸, 无法注册: " + stuffBox.find(FileToFrameStep.IN_FILE).getName();
                        Log.w(TAG, msg);
                        mViewController.appendLogText(msg);
                        return false;
                    }
                    return true;
                }
            })
            .addStep(new AlignmentStep(TrackStep.OUT_COLOR_FACE))//【必选】遮挡检查, 保证注册照质量, 才能保证以后人脸搜索的准确度
            .addStep(new AbsStep<StuffBox>() {//检测上一个步骤的结果, 决定流水线是否能继续
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    for (Entry<TrackedFace, String> entry : stuffBox.find(AlignmentStep.OUT_ALIGNMENT_FAILED_FACES).entrySet()) {
                        String frameName = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;
                        String msg = String.format("%s: Alignment 失败, msg:%s", frameName, entry.getValue());
                        Log.i(TAG, msg);
                        mViewController.appendLogText(msg);
                    }

                    return stuffBox.find(AlignmentStep.OUT_ALIGNMENT_OK_FACES).size() > 0;//符合条件的人脸大于0才继续
                }
            })
            .addStep(new QualityProStep(AlignmentStep.OUT_ALIGNMENT_OK_FACES))//【必选】质量检查, 保证注册照质量, 才能保证以后人脸搜索的准确度
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    for (Entry<TrackedFace, String> entry : stuffBox.find(QualityProStep.OUT_QUALITY_PRO_FAILED).entrySet()) {
                        String frameName = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;
                        String msg = String.format("%s: QualityPro 失败, msg:%s", frameName, entry.getValue());
                        Log.i(TAG, msg);
                        mViewController.appendLogText(msg);
                    }

                    return stuffBox.find(QualityProStep.OUT_QUALITY_PRO_OK).size() > 0;//符合条件的人脸大于0才继续
                }
            })
            .addStep(new ExtractFeatureStep(QualityProStep.OUT_QUALITY_PRO_OK))//【必选】提取人脸特征
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    Map<TrackedFace, float[]> features = stuffBox.find(ExtractFeatureStep.OUT_FACE_FEATURES);
                    if (features.size() > 1) {
                        //如果这里触发了, 说明前面的步骤检查失效, 请检查流水线改动
                        throw new IllegalArgumentException("图中多于一张人脸, 无法注册: " + stuffBox.find(FileToFrameStep.IN_FILE).getName());
                    } else if (features.size() == 0) {
                        String msg = "图片提取人脸特征失败, 无法注册: " + stuffBox.find(FileToFrameStep.IN_FILE).getName();
                        Log.w(TAG, msg);
                        mViewController.appendLogText(msg);
                        return false;
                    }
                    return true;// 此时 size == 1
                }
            })
            .addStep(new RegStep(new InputProvider() {//【必选】人脸特征入库(RAM)
                @Override
                public Collection<FaceForReg> onGetInput(StuffBox stuffBox) {
                    Map<TrackedFace, float[]> features = stuffBox.find(ExtractFeatureStep.OUT_FACE_FEATURES);
                    
                    Collection<FaceForReg> faces = new ArrayList<>(1);
                    for (Entry<TrackedFace, float[]> entry : features.entrySet()) {
                        TrackedFace face = entry.getKey();
                        float[] feature = entry.getValue();
                        String name = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;//输入帧的名字(文件名)作为人名
                        faces.add(new FaceForReg(face, name, feature));
                        mViewController.appendLogText("注册成功: " + name);
                    }
                    return faces;
                }
            }))
            .addStep(new SaveFeaturesToFileStep(RegStep.IN_FACES))//【必选】把人脸特征保存到磁盘, 用于下次程序启动时恢复人脸库
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    mViewController.drawHeavyThreadStuff(stuffBox);//绘制人脸详细信息
                    return true;
                }
            })
            .build();

    private List<File> getFaceImageFiles() {
        List<File> outFiles = Collections.emptyList();

        File dir = new File(FACE_IMG_DIR);
        File[] files = dir.listFiles();
        if (files == null) {
            String msg = "目录里面没找到图片文件: " + dir.getAbsolutePath();
            Log.w(TAG, msg);
            mViewController.appendLogText(msg);
        } else {
            outFiles = new ArrayList<>(Arrays.asList(files));
            Collections.sort(outFiles);
        }
        return outFiles;
    }
    
    
}
