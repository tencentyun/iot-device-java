package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.device.CA;

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

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class TXResourceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(TXResourceImpl.class);

    private TXMqttConnection mConnection;
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

    // 加载服务器证书
    private static void prepareOTAServerCA() {
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
                    LOG.error("{}", "prepareOTAServerCA error:", e);
                } finally {
                    if (caInput != null) {
                        try {
                            caInput.close();
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }
    }

    /**
     * 构造Resource资源对象
     *
     * @param connection MQTT连接
     * @param storagePath 用于保存固件的路径（调用者须保证目录已存在，并具有写权限）
     * @param cosServerCaCrtList OTA升级包下载服务器的CA证书链
     * @param callback 事件回调
     */
    public TXResourceImpl(TXMqttConnection connection, String storagePath, String[] cosServerCaCrtList, TXResourceCallBack callback) {
        this.mConnection = connection;
        this.mStoragePath = storagePath;
        this.mCallback = callback;

        RESOURCE_DOWN_TOPIC = "$thing/down/service/" + mConnection.mProductId + "/" + mConnection.mDeviceName;
        RESOURCE_UP_TOPIC = "$thing/up/service/" + mConnection.mProductId + "/" + mConnection.mDeviceName;

        if (cosServerCaCrtList != null && cosServerCaCrtList.length > 0) {
            mCosServerCaCrtList = cosServerCaCrtList;
        }

        prepareOTAServerCA();

        subscribeTopic();  // 提前订阅话题
    }

    /**
     * 构造Resource资源对象
     *
     * @param connection MQTT连接
     * @param storagePath 用于保存固件的路径（调用者须保证目录已存在，并具有写权限）
     * @param callback 事件回调
     */
    public TXResourceImpl(TXMqttConnection connection, String storagePath, TXResourceCallBack callback) {
        this(connection, storagePath, null, callback);
    }

    /**
     * 设置资源 TOPIC订阅是否成功的标记
     *
     * @param state
     *            true：表示订阅成功； false: 表示订阅失败
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
     * @param timeout
     *            超时时间(必须大于0); 单位：毫秒
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
                    if (topics[i].startsWith("$thing/down/service/") || topics[i].startsWith("$thing/up/service/")) {
                        mSubscribedState = true;
                    }
                }
            }
        }
    }

    /**
     * 处理从服务器收到的MQTT消息，如果是Resource消息则处理，不是Resource消息则忽略
     *
     * @param topic
     *            来自哪个TOPIC的消息
     * @param message
     *            MQTT消息
     * @return 返回true, 表示此消息已由OTA模块处理；返回false，表示些消息不是OTA消息；
     */
    public boolean processMessage(String topic, MqttMessage message) {
        if (!(topic.startsWith("$thing/down/service/") || topic.startsWith("$thing/up/service/"))) {
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

                System.out.println("mStoragePath=" + mStoragePath);

                if (!mCallback.onLastestFirmwareReady(firmwareURL, md5Sum, version)) {
                    downloadResource(firmwareURL, mStoragePath + "/" + md5Sum, md5Sum, version);
                }
            } else if (method.equalsIgnoreCase("report_version_rsp")) {
                String resultCode = jsonObject.getString("result_code");
                String resultMsg = jsonObject.getString("result_msg");
                JSONArray resourceList = jsonObject.getJSONArray("resource_list");

                if (mCallback != null) {
                    mCallback.onReportFirmwareVersion(Integer.valueOf(resultCode), resourceList, resultMsg);
                }
            }

        } catch (JSONException e) {

        }

        return true;
    }

    /**
     * 上报资源文件版本信息到后台服务器
     *
     * @param resourceList JSONArray
     *            装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentFirmwareVersion(JSONArray resourceList) {
        return reportResourceVersion(RESOURCE_UP_TOPIC, resourceList);
    }

    /**
     * 上报资源文件版本信息到后台服务器。
     *
     * @param resourceList JSONArray
     *            装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
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
    public Status reportUpdateResourceState(String state, String resourceName ,int resultCode, String resultMsg, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, state, resultCode, resultMsg, version);
    }

    public Status reportFailedMessage(String resourceName ,int errorCode, String errorMsg, String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, "fail", errorCode, errorMsg, version);
    }

    public Status reportSuccessMessage(String resourceName ,String version) {
        return reportMessage(RESOURCE_UP_TOPIC, "report_progress", resourceName, "done", 0, "", version);
    }

    public Status reportBurnngMessage(String resourceName ,String version) {
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
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
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
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    private Status reportProgressMessage(String topic, int percent, String version) {
        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", "report_progress");

            JSONObject reportJsonObject = new JSONObject();
            JSONObject progressJsonObject = new JSONObject();

            progressJsonObject.put("state", "downloading");
            progressJsonObject.put("percent", String.valueOf(percent));
            progressJsonObject.put("result_code", "0");
            progressJsonObject.put("result_msg", "");
            reportJsonObject.put("progress", progressJsonObject);
            reportJsonObject.put("version", version);

            jsonObject.put("report", reportJsonObject);

        } catch (JSONException e) {

        }

        message.setQos(0);
        message.setPayload(jsonObject.toString().getBytes());

        Status status = mConnection.publish(topic, message, null);
        return status;
    }

    /**
     * 根据URL创建对应的HTTP或HTTPS连接对象
     *
     * @param resourceURL
     *            资源URL
     * @return HttpURLConnection或HttpsURLConnection对象
     */
    private HttpURLConnection createURLConnection(String resourceURL) throws Exception {

        if (resourceURL.toLowerCase().startsWith("https://")) {
            URL url = new URL(resourceURL);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = { new X509TrustManager() {
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
                        throw new CertificateException("check OTA server x509Certificates is null");
                    }

                    if (x509Certificates.length <= 0) {
                        throw new CertificateException("check OTA server x509Certificates is empty");
                    }

                    int match = 0;
                    for (X509Certificate cert : x509Certificates) {

                        try {
                            cert.checkValidity();

                            for (X509Certificate c : serverCertList) {
                                if (cert.equals(c))
                                    match++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (match > 0 && match == mCosServerCaCrtList.length) {
                        LOG.info("checkServerTrusted OK!!!");
                        return;
                    }

                    throw new CertificateException("check OTA server x509Certificates failed");
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } };
            sslContext.init(null, tm, new java.security.SecureRandom());

            SSLSocketFactory ssf = sslContext.getSocketFactory();
            conn.setSSLSocketFactory(ssf);

            return conn;
        }

        URL url = new URL(resourceURL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        return conn;
    }

    /**
     * 开启线程下载资源文件
     *
     * @param resourceURL
     *            资源文件URL
     * @param outputFile
     *            资源文件要保存的全路径及文件名
     * @param md5Sum
     *            用于下载完成后做校验的MD5
     */
    private void downloadResource(final String resourceURL, final String outputFile, final String md5Sum,
                                  final String version) {

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
                                    mCallback.onDownloadProgress(percent, version);
                                }

                                LOG.debug("download " + downloadBytes + " bytes. percent:" + percent);
                                reportProgressMessage(RESOURCE_UP_TOPIC, percent, version);
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
                                mCallback.onDownloadFailure(-4, version); // 校验失败
//								reportFailedMessage(-4, "MD5不匹配", version);
                            }

                            new File(outputFile).delete(); // delete

                            continue; // try again
                        } else {
                            if (mCallback != null) {
//								reportSuccessMessage(version);
                                mCallback.onDownloadCompleted(outputFile, version);
                            }

                            break; // quit loop
                        }
                    } catch (CertificateException e) {
                        if (mCallback != null) {
                            mCallback.onDownloadFailure(-4, version); // 校验失败
//							reportFailedMessage(-4, "MD5不匹配", version);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Exception e) {

                            }
                        }

                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                } while (tryTimes <= MAX_TRY_TIMES);
                mDownloadThreadRunning = false;
            }
        });

        mDownloadThread.start();
    }

    /**
     * 计算文件的MD5摘要值
     *
     * @param filePath
     *            全路径文件名
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
                }
            }
        }
    }

    /**
     * 转换摘要值为字符串形式
     *
     * @param digestBytes
     *            二进制摘要值
     * @return 以16进制字符表示的摘要字符串
     */
    private static String convertHashToString(byte[] digestBytes) {
        String returnVal = "";

        for (int i = 0; i < digestBytes.length; i++) {
            returnVal += Integer.toString((digestBytes[i] & 0xff) + 0x100, 16).substring(1);
        }

        return returnVal.toLowerCase();
    }
}
