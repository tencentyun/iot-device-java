package com.tencent.iot.explorer.device.face.resource;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.face.consts.Common;
import com.tencent.iot.explorer.device.face.data_template.TXFaceKitTemplate;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.device.CA;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;


public class TXResourceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(TXResourceImpl.class);

    private TXMqttConnection mConnection;
    private TXFaceKitTemplate mDataTemplate;
    private TXResourceCallBack mCallback;

    private final String RESOURCE_DOWN_TOPIC;
    private final String RESOURCE_UP_TOPIC;

    private final String mStoragePath;

    private static boolean mDownloadThreadRunning = false;
    private static Thread mDownloadThread = null;

    private boolean mSubscribedState = false;

    private final int DEFAULT_CONNECT_TIMEOUT = 10000; // 毫秒
    private final int DEFAULT_READ_TIMEOUT = 10000; // 毫秒
    private final int MAX_TRY_TIMES = 3;

    private static List<X509Certificate> serverCertList = null;
    private static String[] mCosServerCaCrtList = CA.cosServerCaCrtList;

    /**
     * AI FACE SDK 存储本地feature的文件夹
     */
    private static final String FACE_FEATURE_LIBRAR = "/sdcard/FaceLibrary";

    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 10, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(50000));

    // 加载服务器证书
    private static void prepareResourceServerCA() {
        if (serverCertList == null) {
            serverCertList = new ArrayList<>();
            for (String certStr : mCosServerCaCrtList) {
                ByteArrayInputStream caInput = null;
                try {
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    caInput = new ByteArrayInputStream(certStr.getBytes());

                    X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(caInput);
                    if (certificate != null) {
                        // TXLog.i(TAG, "add certificate:" + certificate);
                        serverCertList.add(certificate);
                    }
                } catch (Exception e) {
                    LOG.error("{}", "prepareResourceServerCA error:", e);
                } finally {
                    if (caInput != null) {
                        try {
                            caInput.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 构造Resource资源对象
     *
     * @param dataTemplate 数据模板
     * @param connection MQTT连接
     * @param storagePath 用于保存固件的路径（调用者须保证目录已存在，并具有写权限）
     * @param cosServerCaCrtList Resource升级包下载服务器的CA证书链
     * @param callback 事件回调
     */
    public TXResourceImpl(TXFaceKitTemplate dataTemplate, TXMqttConnection connection, String storagePath, String[] cosServerCaCrtList, TXResourceCallBack callback) {
        this.mDataTemplate = dataTemplate;
        this.mConnection = connection;
        this.mStoragePath = storagePath;
        this.mCallback = callback;

        RESOURCE_DOWN_TOPIC = TOPIC_SERVICE_DOWN_PREFIX + mConnection.mProductId + "/" + mConnection.mDeviceName;
        RESOURCE_UP_TOPIC = TOPIC_SERVICE_UP_PREFIX + mConnection.mProductId + "/" + mConnection.mDeviceName;

        if (cosServerCaCrtList != null && cosServerCaCrtList.length > 0) {
            mCosServerCaCrtList = cosServerCaCrtList;
        }

        prepareResourceServerCA();

        subscribeTopic();  // 提前订阅话题
    }

    /**
     * 构造Resource资源对象
     *
     * @param dataTemplate 数据模板
     * @param connection MQTT连接
     * @param storagePath 用于保存固件的路径（调用者须保证目录已存在，并具有写权限）
     * @param callback 事件回调
     */
    public TXResourceImpl(TXFaceKitTemplate dataTemplate, TXMqttConnection connection, String storagePath, TXResourceCallBack callback) {
        this(dataTemplate, connection, storagePath, null, callback);
    }

    /**
     * 设置资源 TOPIC订阅是否成功的标记
     *
     * @param state true：表示订阅成功； false: 表示订阅失败
     */
    public void setSubscribedState(boolean state) {
        this.mSubscribedState = state;
    }

    public Status subscribeTopic() {
        return mConnection.subscribe(RESOURCE_DOWN_TOPIC, TXMqttConstants.QOS1, null);
    }

    /**
     * 订阅用于资源下载的TOPIC
     *
     * @param timeout 超时时间(必须大于0); 单位：毫秒
     * @return Status.OK：表示订阅成功时; 其它返回值表示订阅失败；
     */
    private Status subscribeTopic(int timeout) {
        Status tag = mConnection.subscribe(RESOURCE_DOWN_TOPIC, TXMqttConstants.QOS1, null);
        System.out.println("tag " + tag);

        if (mSubscribedState) {
            return Status.OK;
        }

        return Status.ERROR_TOPIC_UNSUBSCRIBED;
    }

    /**
     * 处理从服务器收到的MQTT应答消息，如果资源下载订阅成功的应答消息则处理，不是资源下载消息则忽略
     *
     * @param status
     * @param token
     * @param userContext
     * @param msg
     */
    public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
        System.out.println("onSubscribeCompleted status " + status);
        if (status == Status.OK) {
            String[] topics = token.getTopics();
            if (topics != null) {
                for (int i = 0; i < topics.length; i++) {
                    System.out.println("onSubscribeCompleted topic " + topics[i]);
                    if (topics[i].startsWith(TOPIC_SERVICE_DOWN_PREFIX) || topics[i].startsWith(TOPIC_SERVICE_UP_PREFIX)) {
                        mSubscribedState = true;
                    }
                }
            }
        }
    }

    /**
     * 处理从服务器收到的MQTT消息，如果是Resource消息则处理，不是Resource消息则忽略
     *
     * @param topic 来自哪个TOPIC的消息
     * @param message MQTT消息
     * @return 返回true, 表示此消息已由Resource模块处理；返回false，表示些消息不是Resource消息；
     */
    public boolean processMessage(String topic, MqttMessage message) {
        if (!(topic.startsWith(TOPIC_SERVICE_DOWN_PREFIX) || topic.startsWith(TOPIC_SERVICE_UP_PREFIX))) {
            return false;
        }

        try {
            byte[] payload = message.getPayload();
            JSONObject jsonObject = new JSONObject(new String(payload));

            String method = jsonObject.getString("method");
            if (method.equalsIgnoreCase("update_resource")) {
                String firmwareURL = jsonObject.getString("url");
                String md5Sum = jsonObject.getString("md5sum");
                String version = jsonObject.getString("version");
                String resourceName = jsonObject.getString("resource_name");
                String resourceType = jsonObject.getString("resource_type");
                System.out.println("mStoragePath=" + mStoragePath);
                if (!mCallback.onLastestResourceReady(firmwareURL, md5Sum, version)) {
                    downloadResource(firmwareURL, mStoragePath + "/" + resourceName, resourceName, md5Sum, version, resourceType);
                }
            } else if (method.equalsIgnoreCase("report_version_rsp")) {
                String resultCode = jsonObject.getString("result_code");
                String resultMsg = jsonObject.getString("result_msg");
                JSONArray resourceList = jsonObject.getJSONArray("resource_list");

                if (mCallback != null) {
                    mCallback.onReportResourceVersion(Integer.valueOf(resultCode), resourceList, resultMsg);
                }
            } else if (method.equalsIgnoreCase("del_resource")) { //人员库都被删除了。
                deleteDirectory(mStoragePath);
                deleteDirectory(FACE_FEATURE_LIBRAR);

                String version = jsonObject.getString("version");
                String resourceName = jsonObject.getString("resource_name");
                reportDeleteSuccessMessage(resourceName, version);

                if (mCallback != null) {
                    mCallback.onFaceLibDelete(version, resourceName);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 上报资源文件版本信息到后台服务器
     *
     * @param resourceList JSONArray 装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentFirmwareVersion(JSONArray resourceList) {
        return reportResourceVersion(RESOURCE_UP_TOPIC, resourceList);
    }

    /**
     * 上报资源文件版本信息到后台服务器。
     *
     * @param resourceList JSONArray 装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportResourceVersion(String topic, JSONArray resourceList) {
        if (!mSubscribedState) {
            subscribeTopic(10000);
        }

        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", "report_version");

            JSONObject obj = new JSONObject();
            obj.put("resource_list", resourceList);

            jsonObject.put("report", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        message.setPayload(jsonObject.toString().getBytes());

        Status status = mConnection.publish(topic, message, null);
        System.out.println("reportResourceVersion status " + status);

        return status;
    }

    /**
     * 上报设备升级状态到后台服务器。
     *
     * @param state
     * @param resultCode
     * @param resultMsg
     * @param version
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportUpdateResourceState(String state, String resourceName, int resultCode, String resultMsg, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, state, resultCode, resultMsg, version);
    }

    public Status reportFailedMessage(String resourceName, int errorCode, String errorMsg, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, "done", errorCode, errorMsg, version);
    }

    public Status reportSuccessMessage(String resourceName, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, "done", 0, "success", version);
    }

    public Status reportDeleteSuccessMessage(String resourceName, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "del_result", resourceName, "done", 0, "success", version);
    }

    public Status reportBurnngMessage(String resourceName, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, "burning", 0, "", version);
    }

    /**
     * 上报资源文件下载状态到后台服务器。
     *
     * @param method
     * @param state
     * @param resultCode
     * @param resultMsg
     * @param version
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败;
     */
    private Status reportMessage(String topic, String method, String resourceName, String state, int resultCode, String resultMsg, String version) {
        MqttMessage message = new MqttMessage();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", method);

            JSONObject reportJsonObject = new JSONObject();
            JSONObject progressJsonObject = new JSONObject();

            progressJsonObject.put("resource_name", resourceName);
            progressJsonObject.put("state", state);
            progressJsonObject.put("result_code", String.valueOf(resultCode));
            progressJsonObject.put("result_msg", resultMsg);

            reportJsonObject.put("progress", progressJsonObject);
            reportJsonObject.put("version", version);

            jsonObject.put("report", reportJsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        message.setQos(0);
        message.setPayload(jsonObject.toString().getBytes());

        Status status = mConnection.publish(topic, message, null);
        return status;
    }

    /**
     * 上报下载进度消息到后台服务器。
     *
     * @param percent
     * @param version
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败;
     */
    private Status reportProgressMessage(String topic, String resourceName, int percent, String version) {
        MqttMessage message = new MqttMessage();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", "report_progress");
            JSONObject reportJsonObject = new JSONObject();
            JSONObject progressJsonObject = new JSONObject();

            progressJsonObject.put("resource_name", resourceName);
            progressJsonObject.put("state", "downloading");
            progressJsonObject.put("percent", String.valueOf(percent));
            progressJsonObject.put("result_code", "0");
            progressJsonObject.put("result_msg", "");

            reportJsonObject.put("progress", progressJsonObject);
            reportJsonObject.put("version", version);

            jsonObject.put("report", reportJsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        message.setQos(0);
        message.setPayload(jsonObject.toString().getBytes());

        Status status = mConnection.publish(topic, message, null);
        return status;
    }

    /**
     * 根据URL创建对应的HTTP或HTTPS连接对象
     *
     * @param resourceURL 资源URL
     * @return HttpURLConnection或HttpsURLConnection对象
     */
    private HttpURLConnection createURLConnection(String resourceURL) throws Exception {
        if (resourceURL.toLowerCase().startsWith("https://")) {
            URL url = new URL(resourceURL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = {new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                    // Do nothing. We only want to check server side
                    // certificate.
                    LOG.warn("checkClientTrusted");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                    if (x509Certificates == null) {
                        throw new CertificateException("check Resource server x509Certificates is null");
                    }

                    if (x509Certificates.length <= 0) {
                        throw new CertificateException("check Resource server x509Certificates is empty");
                    }

                    int match = 0;
                    for (X509Certificate cert : x509Certificates) {
                        try {
                            cert.checkValidity();
                            for (X509Certificate c : serverCertList) {
                                if (cert.equals(c)) {
                                    match++;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (match > 0 && match == mCosServerCaCrtList.length) {
                        LOG.info("checkServerTrusted OK!!!");
                        return;
                    }
                    throw new CertificateException("check Resource server x509Certificates failed");
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            sslContext.init(null, tm, new java.security.SecureRandom());
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            conn.setSSLSocketFactory(ssf);
            return conn;
        }
        URL url = new URL(resourceURL);

        return (HttpURLConnection) url.openConnection();
    }

    private JSONArray generalReportVersionData(String resourceName, String version, String resourceType) {
        JSONArray array = new JSONArray();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("resource_name", resourceName);
            jsonObject.put("version", version);
            jsonObject.put("resource_type", resourceType);
            array.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array;
    }

    /**
     * 开启线程下载资源文件
     *
     * @param resourceURL 资源文件URL
     * @param outputFile 资源文件要保存的全路径及文件名
     * @param md5Sum 用于下载完成后做校验的MD5
     */
    private void downloadResource(final String resourceURL, final String outputFile, final String resourceName, final String md5Sum,
                                  final String version, final String resourceType) {
        if (mDownloadThreadRunning) {
            return;
        }
        mDownloadThreadRunning = true;
        mDownloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int tryTimes = 0;
                do {
                    RandomAccessFile fos = null;
                    InputStream stream = null;
                    try {
                        tryTimes++;
                        File file = new File(mStoragePath);
                        if (!file.exists()) {//不存在创建文件夹
                            file.mkdirs();
                        }

                        fos = new RandomAccessFile(outputFile, "rw");
                        LOG.debug("fileLength " + fos.length() + " bytes");

                        long downloadBytes = 0;
                        int lastPercent = 0;

                        if (downloadBytes > 0) {
                            fos.seek(downloadBytes);
                        }

                        LOG.debug("connect: " + resourceURL);
                        HttpURLConnection conn = createURLConnection(resourceURL);
                        conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
                        conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
                        conn.setRequestProperty("Range", "bytes=" + downloadBytes + "-");
                        conn.connect();

                        int totalLength = conn.getContentLength();
                        LOG.debug("totalLength " + totalLength + " bytes");

                        stream = conn.getInputStream();
                        byte[] buffer = new byte[1024 * 1024];
                        while (downloadBytes < totalLength) {
                            int len = stream.read(buffer);
                            if (len < 0) {
                                break;
                            }
                            downloadBytes += len;
                            fos.write(buffer, 0, len);

                            int percent = (int) (((float) downloadBytes / (float) totalLength) * 100);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                if (mCallback != null) {
                                    mCallback.onDownloadProgress(resourceName, percent, version);
                                }
                                LOG.debug("download " + downloadBytes + " bytes. percent:" + percent);
                                reportProgressMessage(RESOURCE_UP_TOPIC, resourceName, percent, version);
                            }
                        }

                        if (fos != null) {
                            fos.close();
                        }
                        if (stream != null) {
                            stream.close();
                        }

                        String calcMD5 = fileToMD5(outputFile);
                        if (!calcMD5.equalsIgnoreCase(md5Sum)) {
                            LOG.error("{}", "md5 checksum not match!!!" + " calculated md5:" + calcMD5);
                            if (mCallback != null) {
                                reportFailedMessage(resourceName, -4, "MD5不匹配", version);
                                mCallback.onDownloadFailure(resourceName, -4, version); // 校验失败
                            }
                            new File(outputFile).delete(); // delete
                            continue; // try again
                        } else {
                            if (mCallback != null) {
                                reportBurnngMessage(resourceName, version);
                                reportSuccessMessage(resourceName, version);
                                mCallback.onDownloadCompleted(outputFile, version);
                                downloadCsvResource(outputFile, version, resourceName, resourceType);
                            }

                            break; // quit loop
                        }
                    } catch (CertificateException e) {
                        if (mCallback != null) {
                            reportFailedMessage(resourceName, -4, "MD5不匹配", version);
                            mCallback.onDownloadFailure(resourceName, -4, version); // 校验失败
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } while (tryTimes <= MAX_TRY_TIMES);
                mDownloadThreadRunning = false;
            }
        });
        mDownloadThread.setName("tencent-resource-imp-download-thread");
        mDownloadThread.start();
    }

    /**
     * 开启线程下载csv文件中人脸资源资源文件
     *
     * @param csvOutputFile 人脸csv资源文件要的路径
     */
    private void downloadCsvResource(final String csvOutputFile, final String version, final String csvResourceName, final String csvResourceType) throws JSONException {
        ArrayList<JSONObject> readerArr = readCsv(csvOutputFile);
        LOG.debug("readerArr.size :" + readerArr.size());
        for (int i = 0; i < readerArr.size(); i++) {
            JSONObject line = readerArr.get(i);

            String staffId = line.getString("staffId");
            String status = line.getString("status");
            String headerUrl = line.getString("headerUrl");
            String headerSize = line.getString("headerSize");
            String headerMd5 = line.getString("headerMd5");

            String[] headerUrlSplitStr = headerUrl.split("/");
            if (headerUrlSplitStr.length <= 0) {
                LOG.debug("download headerUrl" + headerUrl);
                continue;
            }
            String lastPartStr = headerUrlSplitStr[headerUrlSplitStr.length - 1];
            String[] lastPartSplitStr = lastPartStr.split("\\.");
            if (lastPartSplitStr.length <= 0) {
                LOG.debug("download headerUrl" + headerUrl);
                continue;
            }
            String formatStr = lastPartSplitStr[lastPartSplitStr.length - 1];

            String resourcePath1 = mStoragePath + "/" + staffId + "." + formatStr;
            File resourceFile1 = new File(resourcePath1);
            if (resourceFile1.exists() && resourceFile1.length() == Long.parseLong(headerSize)
                    && status.equals(Common.STATUS_UPDATE)) { //存在创建文件，并且下载完整，并且是新增或更新，就跳过下载下一张
                LOG.debug("resourceFile exists");
                continue;
            }

            int finalI = i;
            Runnable runnable = () -> {
                RandomAccessFile fos = null;
                InputStream stream = null;
                try {
                    if (status.equals(Common.STATUS_DELETE)) { //1为删除，0为新增或更新
                        //删掉本地存储的.feature
                        String featurePath = FACE_FEATURE_LIBRAR + "/" + staffId + "." + formatStr + ".feature";
                        File featureFile = new File(featurePath);
                        if (featureFile.exists()) {//存在创建文件,需要删除
                            featureFile.delete();
                        }
                        //删掉本地存储的 图片
                        String resourcePath = mStoragePath + "/" + staffId + "." + formatStr;
                        File resourceFile = new File(resourcePath);
                        if (resourceFile.exists()) {//存在创建文件,需要删除
                            if (resourceFile.delete()) {
                                // 上报'删除成功'
                                mDataTemplate.faceStatusPost(csvResourceName, version, staffId, Common.RESULT_DELETE_SUCCESS);
                            } else {
                                // 上报'删除失败'
                                mDataTemplate.faceStatusPost(csvResourceName, version, staffId, Common.RESULT_DELETE_FAIL);
                            }
                        }
                        if (mCallback != null) {
                            mCallback.onFeatureDelete(staffId, staffId + "." + formatStr);
                        }
                        if (finalI == (readerArr.size() - 1)) { //删除后上报一下版本
                            reportCurrentFirmwareVersion(generalReportVersionData(csvResourceName, version, csvResourceType));
                        }

                        deleteFile(csvOutputFile); //删除临时csv文件

                        return;
                    }

                    fos = new RandomAccessFile(mStoragePath + "/" + staffId + "." + formatStr, "rw");
                    LOG.debug("fileLength " + fos.length() + " bytes");

                    long downloadBytes = 0;
                    int lastPercent = 0;

                    if (downloadBytes > 0) {
                        fos.seek(downloadBytes);
                    }

                    LOG.debug("connect: " + headerUrl);
                    HttpURLConnection conn = createURLConnection(headerUrl);

                    conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
                    conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
                    conn.setRequestProperty("Range", "bytes=" + downloadBytes + "-");

                    conn.connect();

                    int totalLength = conn.getContentLength();
                    LOG.debug(staffId + "totalLength " + totalLength + " bytes");

                    stream = conn.getInputStream();
                    byte buffer[] = new byte[1024 * 1024];
                    while (downloadBytes < totalLength) {
                        int len = stream.read(buffer);
                        if (len < 0) {
                            break;
                        }
                        downloadBytes += len;
                        fos.write(buffer, 0, len);
                        int percent = (int) (((float) downloadBytes / (float) totalLength) * 100);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            if (mCallback != null) {
                                mCallback.onDownloadProgress(staffId + "." + formatStr, percent, version);
                            }
                            LOG.debug("download " + downloadBytes + " bytes. percent:" + percent);
                            reportProgressMessage(RESOURCE_UP_TOPIC, staffId, percent, version);
                        }
                    }

                    deleteFile(csvOutputFile);

                    if (fos != null) {
                        fos.close();
                    }
                    if (stream != null) {
                        stream.close();
                    }

                    String staffPicPath = mStoragePath + "/" + staffId + "." + formatStr;
                    String calcMD5 = fileToMD5(staffPicPath);
                    if (!calcMD5.equalsIgnoreCase(headerMd5)) {
                        LOG.error("{}", "md5 checksum not match!!!" + " calculated md5:" + calcMD5);
                        // 上报'下载失败'
                        mDataTemplate.faceStatusPost(csvResourceName, version, staffId, Common.RESULT_DOWNLOAD_FAIL);
                        if (mCallback != null) {
                            reportFailedMessage(staffId, -4, "MD5不匹配", version);
                            mCallback.onDownloadFailure(staffId + "." + formatStr, -4, version); // 校验失败
                        }
                        new File(mStoragePath + "/" + staffId).delete(); // delete
                    } else {
                        // 上报'下载成功，待注册'
                        mDataTemplate.faceStatusPost(csvResourceName, version, staffId, Common.RESULT_DOWNLOAD_SUCCESS);
                        if (finalI == (readerArr.size() - 1)) {
                            reportCurrentFirmwareVersion(generalReportVersionData(csvResourceName, version, csvResourceType));
                        }
                        if (mCallback != null) {
                            reportBurnngMessage(staffId, version);
                            reportSuccessMessage(staffId, version);
                            mCallback.onFaceDownloadCompleted(csvResourceName, staffPicPath, version);
                        }
                    }
                } catch (CertificateException e) {
                    if (mCallback != null) {
                        reportFailedMessage(staffId, -4, "MD5不匹配", version);
                        mCallback.onDownloadFailure(staffId + "." + formatStr, -4, version); // 校验失败
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mCallback != null && e.getMessage() != null) {
                        reportFailedMessage(staffId, -5, e.getMessage(), version);
                        mCallback.onDownloadFailure(staffId + "." + formatStr, -5, version); // 下载资源失败
                    }
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            threadPoolExecutor.execute(runnable);
        }
    }

    /**
     * 计算文件的MD5摘要值
     *
     * @param filePath 全路径文件名
     * @return 以16进制字符表示的摘要字符串
     */
    private static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");

            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            }

            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 转换摘要值为字符串形式
     *
     * @param digestBytes 二进制摘要值
     * @return 以16进制字符表示的摘要字符串
     */
    private static String convertHashToString(byte[] digestBytes) {
        StringBuilder returnVal = new StringBuilder();
        for (byte digestByte : digestBytes) {
            returnVal.append(Integer.toString((digestByte & 0xff) + 0x100, 16).substring(1));
        }
        return returnVal.toString().toLowerCase();
    }

    //读取CSV文件
    private ArrayList<JSONObject> readCsv(String path) {
        ArrayList<JSONObject> readerArr = new ArrayList<>();
        File file = new File(path);
        FileInputStream fileInputStream;
        Scanner in;
        try {
            fileInputStream = new FileInputStream(file);
            in = new Scanner(fileInputStream, "UTF-8");
            while (in.hasNextLine()) {
                String[] lines = in.nextLine().split(",");
                LOG.debug("readCsv lines " + lines);
                if (lines.length == 5) { //人脸csv数据格式共5列 A002(人员ID),1(状态标记：0 新增或更新，1 删除),http://www.example.com/2.jpg(头像地址URL),65123(头像大小),d8e8fca2dc0f896fd7cb4cb0031ba248(头像的md5)
                    JSONObject line = new JSONObject();
                    line.put("staffId", String.valueOf(lines[0]));
                    line.put("status", String.valueOf(lines[1]));
                    line.put("headerUrl", String.valueOf(lines[2]));
                    line.put("headerSize", String.valueOf(lines[3]));
                    line.put("headerMd5", String.valueOf(lines[4]));
                    readerArr.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return readerArr;
    }

    /**
     * 删除单个文件
     *
     * @param filePath 被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除文件夹以及目录下的文件
     *
     * @param filePath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public boolean deleteDirectory(String filePath) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
            }
            if (!flag) break;
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

}
