package com.tencent.iot.hub.device.java.core.ota;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Device OTA sample
 */
public class OTASampleTest {

    private static final Logger LOG = LoggerFactory.getLogger(OTASampleTest.class);

    private static final String TAG = OTASampleTest.class.getSimpleName();

    private static String path2Store = System.getProperty("user.dir");

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546

    private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";

    private static String mProductID = BuildConfig.TESTOTASAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTOTASAMPLE_DEVICE_NAME;
    private static String mDevPSK = BuildConfig.TESTOTASAMPLE_DEVICE_PSK;
    private static String mCertFilePath = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mPrivKeyFilePath = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String

    private static TXMqttConnection mqttconnection;
    private static MqttConnectOptions options;

    static {
        Loggor.setLogger(LOG);
    }

    /**日志保存的路径*/
    private final static String mLogPath = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";

    private static void connect() {
        try {
            Thread.sleep(2000);
            String workDir = System.getProperty("user.dir") + "/";

            // init connection
            options = new MqttConnectOptions();
            options.setConnectionTimeout(8);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            //客户端证书文件名  mDevPSK是设备秘钥

            if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
                Loggor.info(TAG, "Using cert stream " + mDevPriv + "  " + mDevCert);
                options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
            } else if (mDevPSK != null && mDevPSK.length() != 0){
                Loggor.info(TAG, "Using PSK");
				// options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 hub-device-java sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
            } else {
                Loggor.info(TAG, "Using cert assets file");
                options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
            }
            mqttconnection = new TXMqttConnection(mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,true, new SelfMqttLogCallBack(), new callBack());
            mqttconnection.connect(options, null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void checkFirmware() {
        try {
            Thread.sleep(2000);
            String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
            mqttconnection.initOTA(workDir, new TXOTACallBack() {
                @Override
                public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                    Loggor.error(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
                    unlock();
                }

                @Override
                public boolean onLastestFirmwareReady(String url, String md5, String version) {
                    Loggor.error(TAG, "MQTTSample onLastestFirmwareReady");
                    return false;
                }

                @Override
                public void onDownloadProgress(int percent, String version) {
                    Loggor.error(TAG, "onDownloadProgress:" + percent);
                }

                @Override
                public void onDownloadCompleted(String outputFile, String version) {
                    Loggor.error(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

                    mqttconnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
                }

                @Override
                public void onDownloadFailure(int errCode, String version) {
                    Loggor.error(TAG, "onDownloadFailure:" + errCode);

                    mqttconnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
                }
            });
            mqttconnection.reportCurrentFirmwareVersion("0.0.1");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LogManager.resetConfiguration();
        LOG.isDebugEnabled();
        PropertyConfigurator.configure(OTASampleTest.class.getResource("/log4j.properties"));

        connect();
    }

    public static class callBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";

            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]", status.name(), reconnect, userContextInfo, msg);
            Loggor.info(TAG, logInfo);
            unlock();
        }

        private TXOTACallBack oTACallBack = new TXOTACallBack() {

            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                Loggor.error(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
                unlock();
            }

            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                Loggor.error(TAG, "onLastestFirmwareReady url=" + url + " version " + version);
                return false; // false 自动触发下载升级文件  true 需要手动触发下载升级文件
            }

            @Override
            public void onDownloadProgress(int percent, String version) {
                Loggor.error(TAG, "onDownloadProgress:" + percent);
            }

            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                Loggor.error(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

                mqttconnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                Loggor.error(TAG, "onDownloadFailure:" + errCode);

                mqttconnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        };

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            Loggor.info(TAG, logInfo);

        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";

            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            Loggor.info(TAG, logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";

            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]", status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            Loggor.debug(TAG, logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";

            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]", status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                Loggor.error(TAG, logInfo);
            } else {
                Loggor.debug(TAG, logInfo);
                if (Arrays.toString(asyncActionToken.getTopics()).contains("ota/update/")){   // 订阅ota相关的topic成功
                    otaSubscribeTopicSuccess = true;
                    unlock();
                }
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";

            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            Loggor.debug(TAG, logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
            Loggor.debug(TAG, logInfo);
        }
    }

    /**
     * 实现TXMqttLogCallBack回调接口
     */
    private static class SelfMqttLogCallBack extends TXMqttLogCallBack {

        @Override
        public String setSecretKey() {
            String secertKey;
            if (mDevPSK != null && mDevPSK.length() != 0) {  //密钥认证
                secertKey = mDevPSK;
                secertKey = secertKey.length() > 24 ? secertKey.substring(0,24) : secertKey;
                return secertKey;
			} else {
                BufferedReader cert;

                if (mDevCert != null && mDevCert.length() != 0) { //动态注册,从DevCert中读取
                    cert = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mDevCert.getBytes(Charset.forName("utf8"))), Charset.forName("utf8")));

                } else { //证书认证，从证书文件中读取

                    try {
                        String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
                        cert=new BufferedReader(new InputStreamReader(new FileInputStream(new File(workDir + mCertFilePath))));
                    } catch (IOException e) {
                        Loggor.error(TAG, "getSecertKey failed, cannot open CRT Files.");
                        return null;
                    }
                }
                //获取密钥
                try {
                	if (cert.readLine().contains("-----BEGIN")) {
                        secertKey = cert.readLine();
                        secertKey = secertKey.length() > 24 ? secertKey.substring(0,24) : secertKey;
                	} else {
                        secertKey = null;
                        Loggor.error(TAG, "Invaild CRT Files.");
                	}
                	cert.close();
                } catch (IOException e) {
                	Loggor.error(TAG, "getSecertKey failed.");
                	return null;
                }
            }

            return secertKey;
        }

        @Override
        public void printDebug(String message){
            Loggor.debug(TAG, message);
        }

        @Override
        public boolean saveLogOffline(String log){

            String logFilePath = mLogPath + mProductID + mDevName + ".log";

            Loggor.debug(TAG, "Save log to " + logFilePath);

            try {
                BufferedWriter wLog = new BufferedWriter(new FileWriter(new File(logFilePath), true));
                wLog.write(log);
                wLog.flush();
                wLog.close();
                return true;
            } catch (IOException e) {
                String logInfo = String.format("Save log to [%s] failed, check the Storage permission!", logFilePath);
                Loggor.error(TAG, logInfo);
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public String readOfflineLog(){

            String logFilePath = mLogPath + mProductID + mDevName + ".log";

            Loggor.debug(TAG, "Read log from %s" + logFilePath);

            try {
                BufferedReader logReader = new BufferedReader(new FileReader(logFilePath));
                StringBuilder offlineLog = new StringBuilder();
                int data;
                while (( data = logReader.read()) != -1 ) {
                    offlineLog.append((char)data);
                }
                logReader.close();
                return offlineLog.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public boolean delOfflineLog(){

            String logFilePath = mLogPath + mProductID + mDevName + ".log";

            File file = new File(logFilePath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    return true;
                }
            }
            return false;
        }

    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);
    private static boolean otaSubscribeTopicSuccess = false;

    private static void lock() {
        latch = new CountDownLatch(COUNT);
        try {
            latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void unlock() {
        latch.countDown();// 回调执行完毕，唤醒主线程
    }

    @Test
    public void testOTA() {
        // Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
        connect();
        lock();
        Loggor.debug(TAG, "after connect");
        assertSame(mqttconnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);

        checkFirmware();
        lock();
        assertTrue(otaSubscribeTopicSuccess);
        Loggor.debug(TAG, "checkFirmware subscribe ota");
    }
}
