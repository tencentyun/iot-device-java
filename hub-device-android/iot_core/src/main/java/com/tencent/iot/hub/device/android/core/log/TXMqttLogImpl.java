package com.tencent.iot.hub.device.android.core.log;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.HmacSha1;
import com.tencent.iot.hub.device.android.core.util.TXLog;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TXMqttLogImpl {

    public static final String TAG = TXMqttLogImpl.class.getName();

    /**
     * 定时上传时间：单位ms，30S一次
     */
    private long timeInterval = 30000;

    /**
     * 日志队列，容量10000条日志，剩余容量低于四分之一时触发一次日志上传
     */
    private LinkedBlockingDeque<String> logDeque;
    private static int dequeSize = 10000; //最大容量10000条日志
    private static int dequeThreshold = dequeSize/4; //剩余容量预警阈值

    /**
     * http客户端，用于上传日志到服务器
     */
    private OkHttpClient mOkHttpClient;

    /**http 服务器 URL*/
    private static final String MQTT_LOG_UPLOAD_SERVER_URL =  "http://devicelog.iot.cloud.tencent.com:80/cgi-bin/report-log";

    /**Content Type*/
    private static final MediaType MEDIA_TYPE_LOG = MediaType.parse("text/plain;charset=utf-8");

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

    TXMqttLogImpl(TXMqttConnection mqttConnection) {
        this.mOkHttpClient =  new OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.SECONDS).build();
        this.logDeque = new LinkedBlockingDeque<String>(dequeSize);
        //固定头部格式：[鉴权类型（1字符，C代表证书方式，P代表PSK方式）][预留（3字符，填充#）][产品ID（10字符，不足后面补#）][设备ID（48字符，不足后面补#）]
        this.mFixedHead = String.format("%c###%s%s",
                mqttConnection.mSecretKey == null ? 'C' : 'P',
                String.format("%-10s",mqttConnection.mProductId).replace(" ","#"),
                String.format("%-48s", mqttConnection.mDeviceName).replace(" ","#")
        );
        this.mUploadFlag = false;
        this.mMqttLogCallBack = mqttConnection.mMqttLogCallBack;
        this.mSecretKey = mMqttLogCallBack.setSecretKey();
        new UploaderToServer().start();
    }

    /**
     * 完成日志上传的操作
     */
    class UploaderToServer extends Thread {
        @Override
        public void run() {

            long nowCurrentMillis = System.currentTimeMillis();

            while (true) {
                if (mUploadFlag || (logDeque.size() > dequeSize - dequeThreshold)
                    || (nowCurrentMillis < System.currentTimeMillis()- timeInterval)) {

                    if (logDeque.size() == 0) {
                        nowCurrentMillis = System.currentTimeMillis();
                        mUploadFlag = false;
                        continue;
                    }

                    StringBuffer log = new StringBuffer();

                    //获取所有的log
                    try {
                        while (logDeque.size() > 0)
                            log.append(logDeque.take());
                    } catch (Exception e) {
                        mMqttLogCallBack.printDebug( "Take log from deque failed");
                    }

                    //格式为[签名][固定头部][时间戳（10位）][日志]
                    String payLoad = String.format("%s%s%s", mFixedHead, String.valueOf(System.currentTimeMillis()).substring(0, 10), log.toString());
                    payLoad = HmacSha1.getSignature(payLoad.getBytes(), mSecretKey.getBytes()) + payLoad;

                    Request request = new Request.Builder()
                            .url(MQTT_LOG_UPLOAD_SERVER_URL)
                            .post(RequestBody.create(MEDIA_TYPE_LOG, payLoad))
                            .build();

                    //发送请求
                    try {
                        Response response = mOkHttpClient.newCall(request).execute();
                        if(!response.isSuccessful()) {
                            mMqttLogCallBack.printDebug(String.format("Upload log to %s failed! Response:[%s]",MQTT_LOG_UPLOAD_SERVER_URL,response.body().string()));
                        } else {
                            mMqttLogCallBack.printDebug(String.format("Upload log to %s success!",MQTT_LOG_UPLOAD_SERVER_URL));
                        }
                    } catch (IOException e) {
                        mMqttLogCallBack.saveLogOffline(log.toString()); //存在文本中
                        mMqttLogCallBack.printDebug(String.format("Lost Connection! Call mMqttCallBack.saveLogOffline()"));
                    }

                    nowCurrentMillis = System.currentTimeMillis();
                    mUploadFlag = false;

                    //TXLog.i(TAG, "log upload :%s", log);
                }

                try {
                    Thread.sleep(10); //休眠10ms
                } catch (InterruptedException e) {
                    TXLog.w(TAG, "The thread has been interrupted");
                }
            }
        }
    }

    /**
     * 添加日志到队列中，如果队列空间不足则上传
     * @param log 日志
     * @return 添加成功，返回true，添加失败，返回false
     */
    boolean appendToLogDeque(String log) {
        try {
            logDeque.add(log);
            mMqttLogCallBack.printDebug(String.format("Add log to log Deque! %s",log).replace("\n\f",""));
            return true;
        } catch (Exception e) {
            mMqttLogCallBack.printDebug(String.format("Add log to log Deque failed! %s" ,log).replace("\n\f",""));
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
