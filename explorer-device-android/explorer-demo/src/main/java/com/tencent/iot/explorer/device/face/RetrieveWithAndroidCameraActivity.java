package com.tencent.iot.explorer.device.face;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.Frame.Format;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.camera.ICameraManager;
import com.tencent.cloud.ai.fr.camera.ICameraManager.OnFrameArriveListener;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.face.business.heavystep.ExtractFeatureStep;
import com.tencent.iot.explorer.device.face.business.heavystep.PrepareFaceLibraryFromFileStep;
import com.tencent.iot.explorer.device.face.business.heavystep.RegStep;
import com.tencent.iot.explorer.device.face.business.heavystep.RetrievalStep;
import com.tencent.iot.explorer.device.face.business.heavystep.SaveFeaturesToFileStep;
import com.tencent.iot.explorer.device.face.business.job.AsyncJobBuilder;
import com.tencent.iot.explorer.device.face.business.job.AsyncJobBuilder.PipelineBuilder;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.StuffBox.OnRecycleListener;
import com.tencent.iot.explorer.device.face.business.lightstep.PickBestStep;
import com.tencent.iot.explorer.device.face.business.lightstep.PreprocessStep;
import com.tencent.iot.explorer.device.face.business.lightstep.TrackStep;
import com.tencent.iot.explorer.device.face.business.thread.AIThreadPool;
import com.tencent.iot.explorer.device.face.camera.AndroidCameraManager;
import com.tencent.iot.explorer.device.face.data_template.AccessControlTemplate;
import com.tencent.iot.explorer.device.face.widgets.AbsActivityViewController;
import com.tencent.iot.explorer.device.face.widgets.FaceDrawView;
import com.tencent.iot.explorer.device.face.widgets.FaceDrawView.DrawableFace;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.youtu.YTFaceRetrieval;
import com.tencent.youtu.YTFaceTracker;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class RetrieveWithAndroidCameraActivity extends AppCompatActivity {

    private static final String TAG = RetrieveWithAndroidCameraActivity.class.getSimpleName();

    private ICameraManager mCameraManager;
    private AbsActivityViewController mViewController;

    private AccessControlTemplate mAccessControlTemplateSample;
    // Default testing parameters
    private String mBrokerURL = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
    private String mProductID = "product_id";
    private String mDevName = "device_name";
    private String mDevPSK  = "device_psk"; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String

    private final static String mJsonFileName = "facekit.json";

    private EditText mItemText;

    private final static String BROKER_URL = "broker_url";
    private final static String PRODUCT_ID = "product_id";
    private final static String DEVICE_NAME = "dev_name";
    private final static String DEVICE_PSK = "dev_psk";
    private final static String DEVICE_CERT = "dev_cert";
    private final static String DEVICE_PRIV  = "dev_priv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connect();

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

    private void connect() {
        mAccessControlTemplateSample = new AccessControlTemplate(getApplicationContext(), mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack());
        mAccessControlTemplateSample.connect();
    }

    private void eventSinglePost(String feature_id, float score, float sim) {
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
        if(Status.OK != mAccessControlTemplateSample.eventSinglePost(eventId, type, params)){
            Log.d(TAG, "single event post failed!");
        }
    }

    /**
     * 实现下行消息处理的回调接口
     */
    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {

        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            Log.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //可根据自己需求进行处理状态和控制信息的获取结果
            Log.d(TAG, "event down stream message received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            Log.d(TAG, "control down stream message received : " + msg);
            //do something

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message when error occurred or other info message");
                return result;
            } catch (JSONException e) {
                Log.d(TAG, "Construct params failed!");
//                mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                return null;
            }
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            TXLog.d(TAG, "action [%s] received, input:" + params, actionId);
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    Iterator<String> it = params.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        TXLog.d(TAG,"Input parameter[%s]:" + params.get(key), key);
                    }
                    //construct result
                    JSONObject result = new JSONObject();
                    result.put("code",0);
                    result.put("status", "some message when error occurred or other info message");

                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);

                    result.put("response", response);
                    return result;
                } catch (JSONException e) {
                    Log.d(TAG, "Construct params failed!");
//                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return null;
                }
            } else if (actionId.equals("YOUR ACTION")) {
                //do your action
            }
            return null;
        }
    }

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            Log.d(TAG, logInfo);
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            Log.d(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            Log.d(TAG, logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            Log.d(TAG, logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                Log.d(TAG, logInfo);
            } else {
                Log.d(TAG, logInfo);
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            Log.d(TAG, logInfo);
        }

        @Override
        public void onMessageReceived(String topic, MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            Log.d(TAG, logInfo);
        }
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
                    Frame colorFrame = stuffBox.find(PreprocessStep.OUT_CONVERTED_FRAME_GROUP).colorFrame;
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
                        } else { //没有搜索到人脸库中结果。

                        }
                    }

                    return true;
                }
            })
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
