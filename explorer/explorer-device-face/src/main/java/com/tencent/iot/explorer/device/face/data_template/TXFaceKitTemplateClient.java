package com.tencent.iot.explorer.device.face.data_template;

import android.content.Context;
import android.util.Log;

import com.tencent.cloud.ai.fr.sdksupport.Auth;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.face.resource.TXResourceCallBack;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.youtu.YTCommonInterface;
import com.tencent.youtu.YTDeviceInfo;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;

public class TXFaceKitTemplateClient extends TXMqttConnection {

    static {
        System.loadLibrary("YTCommon");
    }
    //数据模板
    private TXFaceKitTemplate mDataTemplate;
    //属性下行topic
    public String mPropertyDownStreamTopic;

    String METHOD_SERVICE_CALL_SERVICE_REPLY = "call_service_reply";

    private static final String LICENSE_FILE = "licenseKey.dat";

    private Boolean isAuthoried = false;

    private TXAuthCallBack mAuthCallBack;

    private TXResourceCallBack mResourceCallBack;
    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     * @param jsonFileName      数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXFaceKitTemplateClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                                MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                                final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mDataTemplate = new TXFaceKitTemplate(context, this,  productID,  deviceName, jsonFileName, downStreamCallBack);
        this.mPropertyDownStreamTopic = mDataTemplate.mPropertyDownStreamTopic;
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return this.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected);
    }

    /**
     * 是否AI人脸识别 SDK 是否鉴权通过
     */
    public boolean isAuthoried() {
        return this.isAuthoried;
    }

    /**
     * 订阅数据模板相关主题
     * @param topicId 主题ID
     * @param qos QOS等级
     * @return 发送请求成功时返回Status.OK;
     */
    public Status subscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId, final int qos) {
        return  mDataTemplate.subscribeTemplateTopic(topicId, qos);
    }

    /**
     * 订阅Service主题
     */
    public Status subscribeServiceTopic() {
        return mDataTemplate.subscribeServiceTopic();
    }

    /**
     * 取消订阅Service主题
     */
    public Status unSubscribeServiceTopic() {
        return mDataTemplate.unSubscribeServiceTopic();
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeTemplateTopic(TXDataTemplateConstants.TemplateSubTopic topicId) {
        return mDataTemplate.unSubscribeTemplateTopic(topicId);
    }

    /**
     * 属性上报
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    public Status propertyReport(JSONObject property, JSONObject metadata) {
        return mDataTemplate.propertyReport(property, metadata);
    }

    /**
     * 获取状态
     * @param type 类型
     * @param showmeta 是否携带showmeta
     * @return 结果
     */
    public Status propertyGetStatus(String type, boolean showmeta) {
        return mDataTemplate.propertyGetStatus(type, showmeta);
    }

    /**
     * 设备基本信息上报
     * @param params 参数
     * @return 结果
     */
    public Status propertyReportInfo(JSONObject params) {
        return mDataTemplate.propertyReportInfo(params);
    }

    /**
     * 清理控制信息
     * @return 结果
     */
    public Status propertyClearControl() {
        return mDataTemplate.propertyClearControl();
    }

    /**
     * 单个事件上报
     * @param eventId 事件ID
     * @param type 事件类型
     * @param params 参数
     * @return 结果
     */
    public Status eventSinglePost(String eventId, String type, JSONObject params) {
        return  mDataTemplate.eventSinglePost(eventId, type, params);
    }

    /**
     * 系统单个事件上报， 不检查构造是否符合json文件中的定义
     * @param resourceName csv资源文件名
     * @param version 版本
     * @param featureId 特征ID
     * @param result 结果值 1：下载成功，待注册 2：下载失败 3：注册成功 4：注册失败 5：删除成功 6：删除失败
     * @return 返回状态
     */
    public Status eventPost(String resourceName, String version, String featureId, int result) {
        return  mDataTemplate.eventPost(resourceName, version, featureId, result);
    }

    /**
     * 系统单个事件上报，不对本地json进行检验
     * @param eventId 事件ID
     * @param type 事件类型
     * @param params 参数
     * @return 结果
     */
    private Status sysEventSinglePost(String eventId, String type, JSONObject params) {
        return  mDataTemplate.sysEventSinglePost(eventId, type, params);
    }

    /**
     * 检索人脸事件上报  当前断连则通过resource的资源回调通知上层
     * @param feature_id 特征id，对应控制台的人员ID。
     * @param score 检索分数
     * @param sim 检索和特征的相似度
     * @return 结果
     */
    public Status reportSysRetrievalResultEvent(String feature_id, float score, float sim){

        if (!isConnected()) { //设备断连，存储数据
            if (mResourceCallBack != null) {
                Long timestamp = System.currentTimeMillis()/1000; // 图像时间戳
                mResourceCallBack.onOfflineRetrievalResultEventSave(feature_id, score, sim, timestamp.intValue());
            }
            return Status.MQTT_NO_CONN;
        }
        return reportSysRetrievalResultEvent(feature_id, score, sim, 0);
    }

    /**
     * 检索人脸事件上报
     * @param feature_id    特征id，对应控制台的人员ID。
     * @param score         检索分数
     * @param sim           检索和特征的相似度
     * @param timestamp     时间戳
     * @return 结果
     */
    public Status reportSysRetrievalResultEvent(String feature_id, float score, float sim, int timestamp){
        if (mDataTemplate == null) {
            TXLog.d(TAG, "mDataTemplate is null!");
            return Status.ERROR;
        }
        String eventId = "_sys_retrieval_result";
        String type = "info";
        JSONObject params = new JSONObject();
        try {
            params.put("threshold", YTSDKManager.FACE_RETRIEVE_THRESHOLD);//识别阈值
            if (feature_id.length() == 0) {
                params.put("feature_id",feature_id);//特征id
            } else {
                String [] splitStr = feature_id.split("\\.");
                if (splitStr.length <= 0) {
                    TXLog.d(TAG,"wrong feature_id:" + feature_id);
                    return Status.ERROR;
                }
                params.put("feature_id",splitStr[0]);//特征id
            }
            params.put("score",score);//分数
            params.put("sim",sim);//相似度
            if (timestamp == 0) {
                Long timestampOnline = System.currentTimeMillis()/1000;
                params.put("timestamp", timestampOnline.intValue());//图像时间戳
            } else {
                params.put("timestamp", timestamp);//图像时间戳
            }
        } catch (JSONException e) {
            TXLog.d(TAG, "Construct params failed!");
            return Status.ERROR;
        }

        return sysEventSinglePost(eventId, type, params);
    }

    /**
     * 多个事件上报
     * @param events 事件集合
     * @return 结果
     */
    public Status eventsPost(JSONArray events) {
        return mDataTemplate.eventsPost(events);
    }

    /**
     * 初始化授权
     */
    public void initAuth(TXAuthCallBack authCallBack) {
        mAuthCallBack = authCallBack;
        YTDeviceInfo device = getMessage();
        String machineId = device.device_id;
        String machineInfo = device.device_info_encrypted;
        mDataTemplate.initAuth(machineId, machineInfo);
        checkLocalAuthLicense();
    }

    /**
     * 检查本地是否有license文件，有可以提前调用进行鉴权
     */
    private void checkLocalAuthLicense() {
        try {
            //检查本地是否有license文件
            FileInputStream inStream = mContext.openFileInput(LICENSE_FILE);
            byte[] buffer = new byte[1024];
            int hasRead = 0;
            StringBuilder sb = new StringBuilder();
            while ((hasRead = inStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, hasRead));
            }
            inStream.close();
            String response = sb.toString();
            JSONObject jsonResponse = new JSONObject(response);
            String license = null;
            String secret_key = null;
            license = jsonResponse.getString("license");
            secret_key = jsonResponse.getString("secret_key");
            onGetAIFaceLicenseCallBack(0,"ok",license, secret_key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化资源下载功能。
     *
     * @param storagePath 资源下载文件存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param cosServerCaCrtList 资源下载服务器的CA证书链
     * @param callback 事件回调
     */
    public void initResource(String storagePath, String[] cosServerCaCrtList, TXResourceCallBack callback) {
        mDataTemplate.initResource(storagePath, cosServerCaCrtList, callback);
        mResourceCallBack = callback;
    }

    /**
     * 初始化资源下载功能。
     *
     * @param storagePath
     *            资源下载文件存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param callback
     *            事件回调
     */
    public void initResource(String storagePath, TXResourceCallBack callback) {
        mDataTemplate.initResource(storagePath, callback);
        mResourceCallBack = callback;
    }

    /**
     * 上报设备当前资源版本信息到后台服务器。
     *
     * @param resourceList JSONArray
     *            装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentResourceVersion(JSONArray resourceList) {
        return mDataTemplate.reportCurrentResourceVersion(resourceList);
    }

    /**
     * 上报设备资源下载状态到后台服务器。
     *
     * @param state
     *            状态
     * @param resultCode
     *            结果代码。0：表示成功；其它：表示失败；常见错误码：-1: 下载超时;
     *            -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新资源文件失败
     * @param resultMsg
     *            结果描述
     * @param version
     *            版本号
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportResourceState(String resourceName , TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
        return mDataTemplate.reportResourceState(resourceName, state, resultCode, resultMsg, version);
    }

    /**
     * 服务下行消息处理
     * @param message 消息内容
     */
    private void onServiceMessageArrivedCallBack(MqttMessage message){
        TXLog.d(TAG, "service down stream message received " + message);
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            //获取license消息处理
            if (method.equals(METHOD_SERVICE_CALL_SERVICE_REPLY)) {
                Integer code = jsonObj.getInt("code");
                String status = jsonObj.getString("status");
                String license = null;
                String secret_key = null;
                if (code == 0) {//获取AI License成功，解析获取License
                    JSONObject response = jsonObj.getJSONObject("response");
                    license = response.getString("license");
                    secret_key = response.getString("secret_key");

                    if (license.length() != 0 && secret_key.length() != 0) { //获取到了license和key,存到本地
                        // 步骤1:创建一个FileOutputStream对象,MODE_APPEND追加模式
                        FileOutputStream fos = mContext.openFileOutput(LICENSE_FILE,
                                Context.MODE_PRIVATE);
                        // 步骤2：将获取过来的值放入文件
                        fos.write(response.toString().getBytes());
                        // 步骤3：关闭数据流
                        fos.close();
                    }
                }
                onGetAIFaceLicenseCallBack(code,status,license, secret_key);
            } else {

            }
        } catch (Exception e) {
            TXLog.e(TAG, "onServiceMessageArrivedCallBack: invalid message: " + message);
        }
    }

    /**
     * 消息到达回调函数
     * @param topic   消息主题
     * @param message 消息内容
     * @throws Exception 异常
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        super.messageArrived(topic, message);
        mDataTemplate.onMessageArrived(topic, message);
        if (topic.equals(TOPIC_SERVICE_DOWN_PREFIX + mProductId + "/"  + mDeviceName)) {
            onServiceMessageArrivedCallBack(message);
        }
    }
    private void onGetAIFaceLicenseCallBack(Integer code, String status, String license, String secret_key) {
        if (code == 0) {//请求License成功
            Auth.AuthResult authResult = Auth.authLicenseString(license, secret_key);

            String msg = String.format("授权%s, licenceFileName=%s   base64授权", authResult.isSucceeded() ? "成功" : "失败",  authResult.toString());
            if (authResult.isSucceeded()) {
                if (mAuthCallBack != null && !isAuthoried) {
                    mAuthCallBack.onSuccess();
                }
            } else {
                if (mAuthCallBack != null) {
                    mAuthCallBack.onFailure(authResult.getCode(), authResult.toString());
                }
            }
            isAuthoried = authResult.isSucceeded();
            TXLog.d(TAG, "authWithLicence code = " + code + msg);
        } else {
            isAuthoried = false;
            if (mAuthCallBack != null) {
                mAuthCallBack.onFailure(code, status);
            }
        }
    }

    /**
     * 获取设备信息
     * @return YTDeviceInfo
     */
    public YTDeviceInfo getMessage(){
        //设备信息
        YTDeviceInfo deice = new YTDeviceInfo();
        YTCommonInterface.getDeviceInfo(mContext,deice);
        return deice;
    }
}
