package com.tencent.iot.explorer.device.face.data_template;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.tencent.cloud.ai.fr.business.heavystep.AlignmentStep;
import com.tencent.cloud.ai.fr.business.heavystep.ExtractFeatureStep;
import com.tencent.cloud.ai.fr.business.heavystep.FaceForReg;
import com.tencent.cloud.ai.fr.business.heavystep.FileToFrameStep;
import com.tencent.cloud.ai.fr.business.heavystep.GlassesDetectorStep;
import com.tencent.cloud.ai.fr.business.heavystep.QualityProStep;
import com.tencent.cloud.ai.fr.business.heavystep.RegStep;
import com.tencent.cloud.ai.fr.business.heavystep.SaveFeaturesToFileStep;
import com.tencent.cloud.ai.fr.business.job.StuffBox;
import com.tencent.cloud.ai.fr.business.job.SyncJobBuilder;
import com.tencent.cloud.ai.fr.business.lightstep.PickBestStep;
import com.tencent.cloud.ai.fr.business.lightstep.PreprocessStep;
import com.tencent.cloud.ai.fr.business.lightstep.TrackStep;
import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.face.consts.Common;
import com.tencent.iot.explorer.device.face.resource.TXResourceCallBack;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.youtu.YTFaceRetrieval;
import com.tencent.youtu.YTFaceTracker;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.cloud.ai.fr.sdksupport.YTSDKManager.FACE_FEAT_LENGTH;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.SERVICE_DOWN_STREAM_TOPIC;

public class FaceKitSample {
    private static final String TAG = "FaceKitSample";
    // Default Value, should be changed in testing
    private String mBrokerURL = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
    private String mProductID = "PRODUCT_ID";
    private String mDevName = "DEVICE_NAME";
    private String mDevPSK = "DEVICE_SECRET";
    private String mDevCertName = "DEVICE_CERT_NAME ";
    private String mDevKeyName  = "DEVICE_KEY_NAME ";
    private String mJsonFileName = "JSON_FILE_NAME";
    private Context mContext;

    private String mDeviceType = "DEVICE_TYPE";

    private static final String OFFLINE_RETRIEVAL_RESULT_FILE = "offlineRetrievalResult.dat";

    private TXMqttActionCallBack mMqttActionCallBack;
    /**
     * MQTT连接实例
     */
    private TXFaceKitTemplateClient mMqttConnection;
    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    TXDataTemplateDownStreamCallBack mDownStreamCallBack = null;

    private static FaceKitSample sInstance;

    public static synchronized FaceKitSample getInstance() {
        if (sInstance == null) {
            sInstance = new FaceKitSample();
        }
        return sInstance;
    }

    public void init(Context context, String brokerURL, String productId, String devName, String devPSK, TXMqttActionCallBack mqttActionCallBack,
                         final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        mContext = context;
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mDownStreamCallBack = downStreamCallBack;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXFaceKitTemplateClient( mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mMqttActionCallBack,
                mJsonFileName, mDownStreamCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            TXLog.i(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-face sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return mMqttConnection.isConnected();
    }

    /**
     * 是否AI人脸识别 SDK 是否鉴权通过
     */
    public boolean isAuthoried() {
        return mMqttConnection.isAuthoried();
    }

    /**
     * 断开MQTT连接
     */
    public void disconnect() {
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mMqttConnection.disConnect(mqttRequest);
    }

    /**
     * 订阅主题
     *
     */
    public void subscribeTopic() {
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe service down stream topic failed!");
        }
    }

    /**
     * 取消订阅主题
     *
     */
    public void unSubscribeTopic() {
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
        }
    }

    /**
     * 订阅Service主题
     */
    public Status subscribeServiceTopic() {
        return mMqttConnection.subscribeServiceTopic();
    }

    /**
     * 取消订阅Service主题
     */
    public Status unSubscribeServiceTopic() {
        return mMqttConnection.unSubscribeServiceTopic();
    }

    public Status propertyReport(JSONObject property, JSONObject metadata) {
        return mMqttConnection.propertyReport(property, metadata);
    }

    public Status propertyGetStatus(String type, boolean showmeta) {
        return mMqttConnection.propertyGetStatus(type, showmeta);
    }

    public Status propertyReportInfo(JSONObject params) {
        return mMqttConnection.propertyReportInfo(params);
    }

    public Status propertyClearControl() {
        return mMqttConnection.propertyClearControl();
    }

    public Status eventsPost(JSONArray events) {
        return mMqttConnection.eventsPost(events);
    }

    public Status eventSinglePost(String eventId, String type, JSONObject params){
        return  mMqttConnection.eventSinglePost(eventId, type, params);
    }

    public Status eventPost(String resourceName, String version, String featureId, int result) {
        return  mMqttConnection.eventPost(resourceName, version, featureId, result);
    }

    public Status reportSysRetrievalResultEvent(String feature_id, float score, float sim){
        return  mMqttConnection.reportSysRetrievalResultEvent(feature_id, score, sim);
    }

    public Status reportSysRetrievalResultEvent(String feature_id, float score, float sim, int timestamp){
        return  mMqttConnection.reportSysRetrievalResultEvent(feature_id, score, sim, timestamp);
    }

    /**
     * 上报离线时缓存的数据。
     */
    public void reportOfflineSysRetrievalResultData() {
        try {
            //检查本地是否有license文件
            FileInputStream inStream = mContext.openFileInput(OFFLINE_RETRIEVAL_RESULT_FILE);
            byte[] buffer = new byte[1024];
            int hasRead = 0;
            StringBuilder sb = new StringBuilder();
            while ((hasRead = inStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, hasRead));
            }
            inStream.close();
            String result = sb.toString();
            if (result.contains(";")) {//用;分割每条Retrieval数据
                String [] splitStr = result.split(";");
                for (int i = 0; i < splitStr.length; i++) {
                    String retrieval = splitStr[i];
                    if (retrieval.length() != 0) {
                        JSONObject params = new JSONObject(retrieval);
                        String feature_id = params.getString("feature_id");//特征id
                        float score = (float) params.getDouble("score");//特征id
                        float sim = (float) params.getDouble("sim");//相似度
                        int timestamp = params.getInt("timestamp");//图像时间戳
                        reportSysRetrievalResultEvent(feature_id, score, sim, timestamp);
                    }
                }
                //上报过数据后清除本地存储
                // 步骤1:创建一个FileOutputStream对象,MODE_APPEND追加模式
                FileOutputStream fos = mContext.openFileOutput(OFFLINE_RETRIEVAL_RESULT_FILE,
                        Context.MODE_PRIVATE);
                // 步骤2：将获取过来的值放入文件
                fos.write("".getBytes());
                // 步骤3：关闭数据流
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 存储离线时缓存的数据。
     */
    private void saveOfflineSysRetrievalResultData(JSONObject params) {
        try {
            FileInputStream inStream = mContext.openFileInput(OFFLINE_RETRIEVAL_RESULT_FILE);
            byte[] buffer = new byte[1024];
            int hasRead = 0;
            StringBuilder sb = new StringBuilder();
            while ((hasRead = inStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, hasRead));
            }
            inStream.close();
            String result = sb.toString();
            // 步骤1:创建一个FileOutputStream对象,MODE_APPEND追加模式
            FileOutputStream fos = mContext.openFileOutput(OFFLINE_RETRIEVAL_RESULT_FILE,
                    Context.MODE_PRIVATE);
            // 步骤2：将获取过来的值放入文件 拼接;号来区分每条数据
            String string = result + params.toString() + ";";
            fos.write(string.getBytes());
            // 步骤3：关闭数据流
            fos.close();
        } catch (FileNotFoundException e) {//第一次文件不存在，直接存
            try {
                // 步骤1:创建一个FileOutputStream对象,MODE_APPEND追加模式
                FileOutputStream fos = mContext.openFileOutput(OFFLINE_RETRIEVAL_RESULT_FILE,
                        Context.MODE_PRIVATE);
                // 步骤2：将获取过来的值放入文件 拼接;号来区分每条数据
                String string = params.toString() + ";";
                fos.write(string.getBytes());
                // 步骤3：关闭数据流
                fos.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化授权
     */
    public void initAuth(TXAuthCallBack authCallBack) {
        mMqttConnection.initAuth(authCallBack);
    }

    public void checkFirmware() {

        mMqttConnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                TXLog.e(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                return false;
            }

            @Override
            public void onDownloadProgress(int percent, String version) {
                TXLog.e(TAG, "onDownloadProgress:" + percent);
            }

            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                TXLog.e(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

                mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                TXLog.e(TAG, "onDownloadFailure:" + errCode);

                mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mMqttConnection.reportCurrentFirmwareVersion("0.0.1");
    }

    public void checkResource(String storagePath) {

        mMqttConnection.initResource(storagePath, new TXResourceCallBack() {

            @Override
            public void onReportResourceVersion(int resultCode, JSONArray resourceList, String resultMsg) {
                TXLog.e(TAG, "onReportResourceVersion:" + resultCode + ", resourceList:" + resourceList + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestResourceReady(String url, String md5, String version) {
                return false;
            }

            @Override
            public void onDownloadProgress(String resourceName, int percent, String version) {
                TXLog.e(TAG, "onDownloadProgress:" + percent);
            }

            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                TXLog.e(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);
            }

            @Override
            public void onFaceDownloadCompleted(String resourceName, String outputFile, String version) {
                TXLog.e(TAG, "onFaceDownloadCompleted:" + outputFile + ", version:" + version);
                regWithFile(outputFile, resourceName, version);
            }

            @Override
            public void onDownloadFailure(String resourceName, int errCode, String version) {
                TXLog.e(TAG, "onDownloadFailure:" + errCode);
            }

            @Override
            public void onFeatureDelete(String featureId, String resourceName) {
                TXLog.e(TAG, "onFeatureDelete:" + resourceName);

                String[] featureIds = new String[1];
                featureIds[0] = resourceName;
                float[] cvtTable = YTFaceRetrieval.loadConvertTable(mContext.getAssets(), "models/face-feature-v705/cvt_table_1vN_705.txt");
                YTFaceRetrieval mYTFaceRetriever = new YTFaceRetrieval(cvtTable, FACE_FEAT_LENGTH);
                int code = mYTFaceRetriever.deleteFeatures(featureIds);
                if (code != 0) {
                    Log.w(TAG, resourceName + "deleteFeatures() code=" + code);
                }
            }

            @Override
            public void onFaceLibDelete(String version, String resourceName) {
                TXLog.e(TAG, "onFaceLibDelete: version: " + version + "resourceName:" + resourceName);
            }

            @Override
            public void onOfflineRetrievalResultEventSave(String feature_id, float score, float sim, int timestamp) {
                JSONObject params = new JSONObject();
                try {
                    params.put("feature_id", feature_id);//特征id
                    params.put("score", score);//分数
                    params.put("sim", sim);//相似度
                    params.put("timestamp", timestamp);//图像时间戳
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                saveOfflineSysRetrievalResultData(params);
            }
        });

        JSONArray resourceList = new JSONArray();
        mMqttConnection.reportCurrentResourceVersion(resourceList);
    }

    private HashMap<String, String> staffMap = new HashMap<>();

    private void regWithFile(String filePath, String resourceName, String version) {
        String staffId = new File(filePath).getName();
        staffMap.put(staffId, resourceName + Common.SPLITER + version);
        File file = new File(filePath);
        new Thread() {
            @Override
            public void run() {
                this.setName(String.format("tencent-%s-regWithFile",FaceKitSample.class.getSimpleName()).toLowerCase());

                YTSDKManager sdkMgr = new YTSDKManager(mContext.getAssets());//每个线程都必须使用单独的算法SDK实例, 这里为当前线程生成一份实例
                //创建流水线运行过程所需的物料箱
                StuffBox stuffBox = new StuffBox()
                        .store(FileToFrameStep.IN_FILE, file)
                        .setSdkManagerForThread(sdkMgr, Thread.currentThread().getId());
                //创建任务
                new AbsJob<>(stuffBox, regPipeline).run(/*直接在当前线程执行任务*/);
                sdkMgr.destroy();//销毁算法SDK实例
            }
        }.start();
    }

    private final List<AbsStep<StuffBox>> regPipeline = new SyncJobBuilder()
            .addStep(new FileToFrameStep(FileToFrameStep.IN_FILE))
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    return true;
                }
            })
            .addStep(new PreprocessStep(FileToFrameStep.OUT_FRAME_GROUP))//【必选】图像预处理
            .addStep(new TrackStep())//【必选】人脸检测跟踪, 是后续任何人脸算法的前提
            .addStep(new PickBestStep(TrackStep.OUT_COLOR_FACE))
            .addStep(new AbsStep<StuffBox>() {//检测上一个步骤的结果, 决定流水线是否能继续
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    Collection<YTFaceTracker.TrackedFace> faces = stuffBox.find(TrackStep.OUT_COLOR_FACE);
                    int size = faces.size();
                    if (size != 1) {
                        String name = stuffBox.find(FileToFrameStep.IN_FILE).getName();
                        String msg = "图片中" + (size == 0 ? "检测不到" : "多于一张") + "人脸, 无法注册: " + name;
                        TXLog.e(TAG, msg);
                        //注册失败状态的上报
                        eventPost(name, Common.RESULT_REGISTER_FAIL);
                        return false;
                    }
                    return true;
                }
            })
            .addStep(new GlassesDetectorStep(PickBestStep.OUT_PICK_OK_FACES)) //眼镜检测 支持普通眼镜 墨镜
            .addStep(new AlignmentStep(GlassesDetectorStep.OUT_GLASS_OK_NEXT))//【必选】遮挡检查, 保证注册照质量, 才能保证以后人脸搜索的准确度
            .addStep(new AbsStep<StuffBox>() {//检测上一个步骤的结果, 决定流水线是否能继续
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    for (Map.Entry<YTFaceTracker.TrackedFace, String> entry : stuffBox.find(AlignmentStep.OUT_ALIGNMENT_FAILED_FACES).entrySet()) {
                        String frameName = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;
                        String msg = String.format("%s: Alignment 失败, msg:%s", frameName, entry.getValue());
                        TXLog.e(TAG, msg);
                    }
                    boolean correct = stuffBox.find(AlignmentStep.OUT_ALIGNMENT_OK_FACES).size() > 0;
                    if (!correct) {
                        //注册失败状态的上报
                        String name = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;
                        eventPost(name, Common.RESULT_REGISTER_FAIL);
                    }
                    return correct;//符合条件的人脸大于0才继续
                }
            })
            .addStep(new QualityProStep(AlignmentStep.OUT_ALIGNMENT_OK_FACES))//【必选】质量检查, 保证注册照质量, 才能保证以后人脸搜索的准确度
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    for (Map.Entry<YTFaceTracker.TrackedFace, String> entry : stuffBox.find(QualityProStep.OUT_QUALITY_PRO_FAILED).entrySet()) {
                        String frameName = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;
                        String msg = String.format("%s: QualityPro 失败, msg:%s", frameName, entry.getValue());
                        TXLog.e(TAG, msg);
                    }

                    boolean correct = stuffBox.find(QualityProStep.OUT_QUALITY_PRO_OK).size() > 0;
                    if (!correct) {
                        //注册失败状态的上报
                        String name = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;
                        eventPost(name, Common.RESULT_REGISTER_FAIL);
                    }
                    return correct;//符合条件的人脸大于0才继续
                }
            })
            .addStep(new ExtractFeatureStep(QualityProStep.OUT_QUALITY_PRO_OK))//【必选】提取人脸特征
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    Map<YTFaceTracker.TrackedFace, float[]> features = stuffBox.find(ExtractFeatureStep.OUT_FACE_FEATURES);
                    String name = stuffBox.find(FileToFrameStep.IN_FILE).getName();
                    if (features.size() > 1) {
                        //如果这里触发了, 说明前面的步骤检查失效, 请检查流水线改动
                        TXLog.e(TAG, "注册失败, 图中多于一张人脸, 无法注册: " + name);
                        //注册失败状态的上报
                        eventPost(name, Common.RESULT_REGISTER_FAIL);
                        throw new IllegalArgumentException("图中多于一张人脸, 无法注册: " + name);
                    } else if (features.size() == 0) {
                        String msg = "图片提取人脸特征失败, 无法注册: " + name;
                        TXLog.e(TAG, "注册失败: " + msg);
                        //注册失败状态的上报
                        eventPost(name, Common.RESULT_REGISTER_FAIL);
                        return false;
                    }
                    return true;// 此时 size == 1
                }
            })
            .addStep(new RegStep(new RegStep.InputProvider() {//【必选】人脸特征入库(RAM)
                @Override
                public Collection<FaceForReg> onGetInput(StuffBox stuffBox) {
                    Map<YTFaceTracker.TrackedFace, float[]> features = stuffBox.find(ExtractFeatureStep.OUT_FACE_FEATURES);

                    Collection<FaceForReg> faces = new ArrayList<>(1);
                    for (Map.Entry<YTFaceTracker.TrackedFace, float[]> entry : features.entrySet()) {
                        YTFaceTracker.TrackedFace face = entry.getKey();
                        float[] feature = entry.getValue();
                        String name = stuffBox.find(PreprocessStep.IN_RAW_FRAME_GROUP).name;//输入帧的名字(文件名)作为人名
                        faces.add(new FaceForReg(face, name, feature));
                        TXLog.d(TAG, "注册成功");
                        //注册成功状态的上报
                        eventPost(name, Common.RESULT_REGISTER_SUCCESS);
                    }
                    return faces;
                }
            }))
            .addStep(new SaveFeaturesToFileStep(RegStep.IN_FACES))//【必选】把人脸特征保存到磁盘, 用于下次程序启动时恢复人脸库
            .addStep(new AbsStep<StuffBox>() {
                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    return true;
                }
            })
            .build();

    private String getFileNameWithEx(String name) {
        int dot = name.lastIndexOf('.');
        if ((dot > -1) && (dot < (name.length()))) {
            return name.substring(0, dot);
        } else {
            return name;
        }
    }

    private void eventPost(String name, int status) {
        String info = staffMap.get(name); //${resourceName}__${version}

        if (info != null) {
            String[] items = info.split(Common.SPLITER);
            if (items != null && items.length == 2) {
                String resourceName = items[0];
                String version = items[1];
                eventPost(resourceName, version, getFileNameWithEx(name), status);
            } else {
                TXLog.e(TAG, String.format("Staff Info [%s] 格式错误.", info));
            }
        } else {
            TXLog.e(TAG, String.format("StaffMap不存在 [%] 元素.", name));
        }
    }
}
