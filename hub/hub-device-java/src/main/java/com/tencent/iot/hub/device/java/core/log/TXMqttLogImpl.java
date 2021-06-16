package com.tencent.iot.hub.device.java.core.log;


import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha1;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TXMqttLogImpl {
    private static final Logger LOG = LoggerFactory.getLogger(TXMqttLogImpl.class);

    public static final String TAG = TXMqttLogImpl.class.getName();

    private static final String HMAC_ALGO = "hmacsha256";


    /**
     * 定时上传时间：单位ms，30S一次
     */
    private long timeInterval = 30000;

    /**
     * 日志队列，容量10000条日志，剩余容量低于四分之一时触发一次日志上传
     */
    private LinkedBlockingDeque<String> logDeque;
    private static int dequeSize = 10000; //最大容量10000条日志
    private static int dequeThreshold = dequeSize / 4; //剩余容量预警阈值

    /**
     * http客户端，用于上传日志到服务器
     */
    private OkHttpClient mOkHttpClient;

    /**
     * http 服务器 URL
     */
    private static final String MQTT_LOG_UPLOAD_SERVER_URL = "https://ap-guangzhou.gateway.tencentdevices.com/device/reportlog";

    /**
     * Content Type
     */
    private static final MediaType MEDIA_TYPE_LOG = MediaType.parse("application/json;charset=utf-8");

    /**
     * 固定头部
     * 格式：[鉴权类型（1字符，C代表证书方式，P代表PSK方式）][预留（3字符，填充#）][产品ID（10字符，不足后面补#）][设备ID（48字符，不足后面补#）]
     */
    private String mFixedHead;

    /**
     * 签名密钥,最多保留24位
     */
    private String mSecretKey;

    /**
     * 上传标志，true表示立刻上传
     */
    private boolean mUploadFlag;

    /**
     * 日志上传回调函数，用于离线下的日志存储和上线后的日志上传
     */
    private TXMqttLogCallBack mMqttLogCallBack;

    /**
     * 日志上报URL
     */
    private String mLogUrl;

    TXMqttLogImpl(TXMqttConnection mqttConnection) {
        this.mOkHttpClient = new OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.SECONDS).build();
        this.logDeque = new LinkedBlockingDeque<String>(dequeSize);
        //固定头部格式：[鉴权类型（1字符，C代表证书方式，P代表PSK方式）][预留（3字符，填充#）][产品ID（10字符，不足后面补#）][设备ID（48字符，不足后面补#）]
        this.mFixedHead = String.format("%c###%s%s",
                mqttConnection.mSecretKey == null ? 'C' : 'P',
                String.format("%-10s", mqttConnection.mProductId).replace(" ", "#"),
                String.format("%-48s", mqttConnection.mDeviceName).replace(" ", "#")
        );
        this.mUploadFlag = false;
        this.mMqttLogCallBack = mqttConnection.mMqttLogCallBack;
        this.mSecretKey = mMqttLogCallBack.setSecretKey();
        new UploaderToServer(mqttConnection.mProductId, mqttConnection.mDeviceName).start();
    }

    TXMqttLogImpl(TXMqttConnection mqttConnection, String logUrl) {
        this.mLogUrl = logUrl;
        this.mOkHttpClient = new OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.SECONDS).build();
        this.logDeque = new LinkedBlockingDeque<String>(dequeSize);
        //固定头部格式：[鉴权类型（1字符，C代表证书方式，P代表PSK方式）][预留（3字符，填充#）][产品ID（10字符，不足后面补#）][设备ID（48字符，不足后面补#）]
        this.mFixedHead = String.format("%c###%s%s",
                mqttConnection.mSecretKey == null ? 'C' : 'P',
                String.format("%-10s", mqttConnection.mProductId).replace(" ", "#"),
                String.format("%-48s", mqttConnection.mDeviceName).replace(" ", "#")
        );
        this.mUploadFlag = false;
        this.mMqttLogCallBack = mqttConnection.mMqttLogCallBack;
        this.mSecretKey = mMqttLogCallBack.setSecretKey();
        new UploaderToServer(mqttConnection.mProductId, mqttConnection.mDeviceName).start();
    }

    /**
     * 完成日志上传的操作
     */
    class UploaderToServer extends Thread {

        private final String productId;
        private final String deviceName;

        public UploaderToServer(String productId, String deviceName) {
            this.productId = productId;
            this.deviceName = deviceName;
        }

        @Override
        public void run() {

            long nowCurrentMillis = System.currentTimeMillis();

            while (true) {
                if (mUploadFlag || (logDeque.size() > dequeSize - dequeThreshold)
                        || (nowCurrentMillis < System.currentTimeMillis() - timeInterval)) {

                    if (logDeque.size() == 0) {
                        nowCurrentMillis = System.currentTimeMillis();
                        mUploadFlag = false;
                        continue;
                    }

                    StringBuffer log = new StringBuffer();
                    //获取所有的log
                    try {
                        while (logDeque.size() > 0) {
                            log.append(logDeque.take());
                        }
                    } catch (Exception e) {
                        mMqttLogCallBack.printDebug("Take log from deque failed");
                    }
                    int randNum = (int) (Math.random() * ((1 << 31) - 1));
                    int timestamp = (int) (System.currentTimeMillis() / 1000);

                    final JSONObject obj = new JSONObject();
                    final JSONArray array = new JSONArray();
                    array.put(log.toString());
                    obj.put("ProductId", productId);
                    obj.put("DeviceName", deviceName);
                    obj.put("Message", array);

                    String payload = obj.toString();
                    String hashedPayload = "";
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] encodedhash = digest.digest(payload.getBytes(Charset.forName("UTF-8")));
                        hashedPayload = HmacSha256.bytesToHexString(encodedhash);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    @SuppressWarnings("DefaultLocale")
                    String signSourceStr = String.format("%s\n%s\n%s\n%s\n%s\n%d\n%d\n%s",
                            "POST",
                            "ap-guangzhou.gateway.tencentdevices.com",
                            "/device/reportlog",
                            "",
                            HMAC_ALGO,
                            timestamp,
                            randNum,
                            hashedPayload
                    );

                    String hmacSign = "";
                    try {
                        SecretKeySpec signKey = new SecretKeySpec(mSecretKey.getBytes(), HMAC_ALGO);
                        Mac mac = Mac.getInstance(HMAC_ALGO);
                        mac.init(signKey);
                        byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
                        hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        e.printStackTrace();
                    }

                    String url = MQTT_LOG_UPLOAD_SERVER_URL;
                    if (mLogUrl != null && mLogUrl.length() > 0) {
                        url = mLogUrl;
                    }

                    Request request = new Request.Builder()
                            .addHeader("X-TC-Algorithm", HMAC_ALGO)
                            .addHeader("X-TC-Timestamp", String.valueOf(timestamp))
                            .addHeader("X-TC-Nonce", String.valueOf(randNum))
                            .addHeader("X-TC-Signature", hmacSign)
                            .url(url)
                            .post(RequestBody.create(MEDIA_TYPE_LOG, payload))
                            .build();

                    //发送请求
                    try {
                        Response response = mOkHttpClient.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            mMqttLogCallBack.printDebug(String.format("Upload log to %s failed! Response:[%s]", url, response.body().string()));
                        } else {
                            ResponseBody responseBody = response.body();
                            if (responseBody == null) {
                                LOG.error("Response body is null.");
                                return;
                            }
                            String respStr = responseBody.string();
                            JSONObject jsonObj = new JSONObject(respStr);
                            JSONObject resp = jsonObj.getJSONObject("Response");
                            if (resp != null && !resp.has("Error")) {
                                mMqttLogCallBack.printDebug(String.format("Upload log to %s success!", url));
                            } else {
                                mMqttLogCallBack.printDebug(String.format("Upload log to %s failed! Response:[%s]", url, respStr));
                            }
                        }
                    } catch (IOException e) {
                        mMqttLogCallBack.saveLogOffline(log.toString()); //存在文本中
                        mMqttLogCallBack.printDebug(String.format("Lost Connection! Call mMqttCallBack.saveLogOffline()"));
                    }

                    nowCurrentMillis = System.currentTimeMillis();
                    mUploadFlag = false;
                }

                try {
                    Thread.sleep(10); //休眠10ms
                } catch (InterruptedException e) {
                    LOG.warn("The thread has been interrupted");
                }
            }
        }
    }

    /**
     * 添加日志到队列中，如果队列空间不足则上传
     *
     * @param log 日志
     * @return 添加成功，返回true，添加失败，返回false
     */
    boolean appendToLogDeque(String log) {
        try {
            logDeque.add(log);
            mMqttLogCallBack.printDebug(String.format("Add log to log Deque! %s", log).replace("\n\f", ""));
            return true;
        } catch (Exception e) {
            mMqttLogCallBack.printDebug(String.format("Add log to log Deque failed! %s", log).replace("\n\f", ""));
            return false;
        }
    }

    /**
     * 触发一次日志上传
     */
    void uploadMqttLog() {
        mUploadFlag = true;
    }

    /**
     * 上传离线日志
     */
    void uploadOfflineLog() {
        String offlineLog = mMqttLogCallBack.readOfflineLog();
        if (offlineLog != null) {
            appendToLogDeque(offlineLog);
            mMqttLogCallBack.delOfflineLog();
        }
        mUploadFlag = true;
    }

    /**
     * 清空队列
     */
    void resetLogDeque() {
        logDeque.clear();
    }

}
