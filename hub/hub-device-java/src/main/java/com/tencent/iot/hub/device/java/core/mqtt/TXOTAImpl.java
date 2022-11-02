package com.tencent.iot.hub.device.java.core.mqtt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
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

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.device.CA;
import com.tencent.iot.hub.device.java.utils.Loggor;

/**
 * OTA 实现类
 */
public class TXOTAImpl {
	private static final String TAG = TXOTAImpl.class.getName();
	private static final Logger logger = LoggerFactory.getLogger(TXOTAImpl.class);

	static { Loggor.setLogger(logger); }
	
	private TXMqttConnection mConnection;
	private TXOTACallBack mCallback;

	private final String OTA_UPDATE_TOPIC;
	private final String OTA_REPORT_TOPIC;
	private final String OTA_SUB_DEV_UPDATE_TOPIC;
	private final String OTA_SUB_DEV_REPORT_TOPIC;
	private final String mStoragePath;

	private volatile static boolean mDownloadThreadRunning = false;
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
					Loggor.error(TAG, "prepareOTAServerCA error:" + e);
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
	 * 构造 OTA 对象
	 *
	 * @param connection MQTT 连接
	 * @param storagePath 用于保存固件的路径（调用者须保证目录已存在，并具有写权限）
	 * @param cosServerCaCrtList OTA 升级包下载服务器的 CA 证书链
	 * @param callback OTA 事件回调
	 */
	public TXOTAImpl(TXMqttConnection connection, String storagePath, String[] cosServerCaCrtList, TXOTACallBack callback) {
		this.mConnection = connection;
		this.mStoragePath = storagePath;
		this.mCallback = callback;

		OTA_UPDATE_TOPIC = "$ota/update/" + mConnection.mProductId + "/" + mConnection.mDeviceName;
		OTA_REPORT_TOPIC = "$ota/report/" + mConnection.mProductId + "/" + mConnection.mDeviceName;

		OTA_SUB_DEV_UPDATE_TOPIC = "$ota/update/" + mConnection.getSubProductID() + "/" + mConnection.getSubDevName();
		OTA_SUB_DEV_REPORT_TOPIC = "$ota/report/" + mConnection.getSubProductID() + "/" + mConnection.getSubDevName();

		if (cosServerCaCrtList != null && cosServerCaCrtList.length > 0) {
			mCosServerCaCrtList = cosServerCaCrtList;
		}

		prepareOTAServerCA();

		subscribeTopic();  // 提前订阅主题
		if (mConnection.getSubProductID() != null) { // 设置子设备时
			subscribeSubDevTopic();  //网关子设备订阅
		}
	}

	/**
	 * 构造 OTA 对象
	 *
	 * @param connection MQTT 连接
	 * @param storagePath 用于保存固件的路径（调用者须保证目录已存在，并具有写权限）
	 * @param callback OTA 事件回调
	 */
	public TXOTAImpl(TXMqttConnection connection, String storagePath, TXOTACallBack callback) {
		this(connection, storagePath, null, callback);
	}

	/**
	 * 设置 OTA TOPIC 订阅是否成功的标记
	 *
	 * @param state true：表示订阅成功；false: 表示订阅失败
	 */
	public void setSubscribedState(boolean state) {
		this.mSubscribedState = state;
	}

	/**
	 * 处理从服务器收到的 MQTT 应答消息，如果 OTA 订阅成功的应答消息则处理，不是 OTA 消息则忽略
	 *
	 * @param status {@link Status}
	 * @param token mqtt 连接 token {@link IMqttToken}
	 * @param userContext 用户上下文
	 * @param msg 平台返回消息
	 */
	public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
		Loggor.debug(TAG, "onSubscribeCompleted status " + status);
		if (status == Status.OK) {
			String[] topics = token.getTopics();
			if (topics != null) {
				for (int i = 0; i < topics.length; i++) {
					Loggor.debug(TAG, "onSubscribeCompleted topic " + topics[i]);
					if (topics[i].startsWith("$ota/")) {
						mSubscribedState = true;
					}
				}
			}
		}
	}

	/**
	 * 处理从服务器收到的 MQTT 消息，如果是 OTA 消息则处理，不是 OTA 消息则忽略
	 *
	 * @param topic 来自哪个 TOPIC 的消息
	 * @param message MQTT 消息
	 * @return 返回 true, 表示此消息已由 OTA 模块处理；返回 false，表示些消息不是 OTA 消息
	 */
	public boolean processMessage(String topic, MqttMessage message) {
		if (!topic.startsWith("$ota/")) {
			return false;
		}
		if (mCallback == null) {
			Loggor.debug(TAG, "mCallback=" + mCallback);
		}
		try {
			byte[] payload = message.getPayload();
			JSONObject jsonObject = new JSONObject(new String(payload));

			String type = jsonObject.getString("type");
			if (type.equalsIgnoreCase("update_firmware")) {
				String firmwareURL = jsonObject.getString("url");
				String md5Sum = jsonObject.getString("md5sum");
				String version = jsonObject.getString("version");

				Loggor.debug(TAG, "mStoragePath=" + mStoragePath);

				if (mCallback != null && !mCallback.onLastestFirmwareReady(firmwareURL, md5Sum, version)) {
					downloadFirmware(firmwareURL, mStoragePath + "/" + md5Sum, md5Sum, version);
				}
			} else if (type.equalsIgnoreCase("report_version_rsp")) {
				String resultCode = String.valueOf(jsonObject.getInt("result_code"));
				String resultMsg = jsonObject.getString("result_msg");
				String version = jsonObject.getString("version");

				if (mCallback != null) {
					mCallback.onReportFirmwareVersion(Integer.valueOf(resultCode), version, resultMsg);
				}
			}
		} catch (JSONException e) {

		}

		return true;
	}

	/**
	 * 上报设备当前版本信息到后台服务器
	 *
	 * @param currentFirmwareVersion 设备当前版本信息
	 * @return 发送请求成功时返回Status.OK；其它返回值表示发送请求失败
	 */
	public Status reportCurrentFirmwareVersion(String currentFirmwareVersion) {
		return reportDevVersion(OTA_REPORT_TOPIC, currentFirmwareVersion);
	}

	public Status gatewaySubdevReportVer(String currentVersion) {
		return reportDevVersion(OTA_SUB_DEV_REPORT_TOPIC, currentVersion);
	}

	/**
	 * 上报设备当前版本信息到后台服务器
	 *
	 * @param currentVersion 设备当前版本信息
	 * @return 发送请求成功时返回Status.OK；其它返回值表示发送请求失败
	 */
	public Status reportDevVersion(String topic, String currentVersion) {
		if (!mSubscribedState) {
			subscribeTopic(10000);
		}

		MqttMessage message = new MqttMessage();

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("type", "report_version");

			JSONObject obj = new JSONObject();
			obj.put("version", currentVersion);

			jsonObject.put("report", obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		message.setPayload(jsonObject.toString().getBytes());

		Status status = mConnection.publish(topic, message, null);
		Loggor.debug(TAG, "reportDevVersion status " + status);

		return status;
	}

	/**
	 * 停止下载ota固件
	 */
	public void stopDownloadOTATask () {
		if (mDownloadThread != null) {
			mDownloadThread.interrupt();
			mDownloadThread = null;
		}
	}

	/**
	 * 上报设备升级状态到后台服务器
	 *
	 * @param state 状态
	 * @param resultCode 结果码
	 * @param resultMsg 平台返回消息
	 * @param version 目标固件版本号
	 * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
	 */
	public Status reportUpdateFirmwareState(String state, int resultCode, String resultMsg, String version) {
		return reportMessage(OTA_REPORT_TOPIC, "report_progress", state, resultCode, resultMsg, version);
	}

	/**
	 * 上报失败信息
	 *
	 * @param errorCode 错误码
	 * @param errorMsg 错误信息
	 * @param version 目标固件版本号
	 * @return 操作结果 {@link Status}
	 */
	public Status reportFailedMessage(int errorCode, String errorMsg, String version) {
		return reportMessage(OTA_SUB_DEV_REPORT_TOPIC, "report_progress", "fail", errorCode, errorMsg, version);
	}

	/**
	 * 上报成功信息
	 *
	 * @param version 目标固件版本号
	 * @return 操作结果 {@link Status}
	 */
	public Status reportSuccessMessage(String version) {
		return reportMessage(OTA_SUB_DEV_REPORT_TOPIC, "report_progress", "done", 0, "", version);
	}

	/**
	 * 上报烧录信息
	 *
	 * @param version 目标固件版本号
	 * @return 操作结果 {@link Status}
	 */
	public Status reportBurnngMessage(String version) {
		return reportMessage(OTA_SUB_DEV_REPORT_TOPIC, "report_progress", "burning", 0, "", version);
	}

	/**
	 * 上报设备升级状态到后台服务器
	 *
	 * @param topic 主题
	 * @param type 类型
	 * @param state 状态
	 * @param resultCode 结果码
	 * @param resultMsg 结果信息
	 * @param version 版本号
	 * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
	 */
	private Status reportMessage(String topic, String type, String state, int resultCode, String resultMsg, String version) {
		MqttMessage message = new MqttMessage();

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("type", type);

			JSONObject reportJsonObject = new JSONObject();
			JSONObject progressJsonObject = new JSONObject();

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
	 * 上报下载进度消息到后台服务器
	 *
	 * @param percent 进度
	 * @param version 目标固件版本号
	 * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
	 */
	private Status reportProgressMessage(String topic, int percent, String version) {
		MqttMessage message = new MqttMessage();

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("type", "report_progress");

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
	 * 订阅用于 OTA 升级的 TOPIC
	 *
	 * @param timeout 超时时间(必须大于0)；单位：毫秒
	 * @return Status.OK：表示订阅成功时；其它返回值表示订阅失败
	 */
	private Status subscribeTopic(int timeout) {
		Status tag = mConnection.subscribe(OTA_UPDATE_TOPIC, TXMqttConstants.QOS1, null);
		Loggor.debug(TAG, "tag " + tag);
		long beginTime = System.currentTimeMillis();
//		while (!mSubscribedState) {
//
//			try {
//				Thread.sleep(100);
//			} catch (Exception e) {
//			}
//
//			if (System.currentTimeMillis() - beginTime > timeout)
//				break;
//		}

		if (mSubscribedState) {
			return Status.OK;
		}

		return Status.ERROR_TOPIC_UNSUBSCRIBED;
	}

	/**
	 * 订阅 OTA 主题
	 *
	 * @return 操作结果 {@link Status}
	 */
	public Status subscribeTopic() {
		return mConnection.subscribe(OTA_UPDATE_TOPIC, TXMqttConstants.QOS1, null);
	}

	/**
	 * 订阅子设备 OTA 主题
	 *
	 * @return 操作结果 {@link Status}
	 */
	public Status subscribeSubDevTopic() {
		return mConnection.subscribe(OTA_SUB_DEV_UPDATE_TOPIC, TXMqttConstants.QOS1, null);
	}

	/**
	 * 根据 URL 创建对应的 HTTP 或 HTTPS 连接对象
	 *
	 * @param firmwareURL 固件 URL
	 * @return HttpURLConnection 或 HttpsURLConnection 对象
	 */
	private HttpURLConnection createURLConnection(String firmwareURL) throws Exception {

		if (firmwareURL.toLowerCase().startsWith("https://")) {
			URL url = new URL(firmwareURL);

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			SSLContext sslContext = SSLContext.getInstance("SSL");
			TrustManager[] tm = { new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
						throws CertificateException {
					// Do nothing. We only want to check server side
					// certificate.
					Loggor.warn(TAG,  "checkClientTrusted");
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
						Loggor.info(TAG,  "checkServerTrusted OK!!!");
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

		URL url = new URL(firmwareURL);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		return conn;
	}

	/**
	 * 下载固件
	 *
	 * @param firmwareURL 固件 URL
	 * @param outputFile 输出文件
	 * @param md5Sum md5 值
	 * @param version 版本号
	 */
	public void gatewayDownSubdevApp(String firmwareURL, String outputFile, String md5Sum, String version) {
		downloadFirmware(firmwareURL, outputFile, md5Sum, version);
	}

	/**
	 * 网关上报子设备进度
	 *
	 * @param percent 进度
	 * @param version 目标子设备固件版本号
	 * @return 操作结果 {@link Status}
	 */
	public Status gatewaySubdevReportProgress(int percent, String version) {
		return reportProgressMessage(OTA_SUB_DEV_REPORT_TOPIC, percent, version);
	}

	/**
	 * 开启线程下载固件
	 *
	 * @param firmwareURL 固件 URL
	 * @param outputFile 固件要保存的全路径及文件名
	 * @param md5Sum 用于下载完成后做校验的 MD5
	 */
	private void downloadFirmware(final String firmwareURL, final String outputFile, final String md5Sum,
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
						Loggor.debug(TAG,  "fileLength " + fos.length() + " bytes");

						long downloadBytes = fos.length();
						int lastPercent = 0;
						long lastReportProcentTimestamp = 0;

						if (downloadBytes > 0) {
							fos.seek(downloadBytes);
						}

						Loggor.debug(TAG,  "connect: " + firmwareURL);
						HttpURLConnection conn = createURLConnection(firmwareURL);

						conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
						conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
						conn.setRequestProperty("Range", "bytes=" + downloadBytes + "-");

						conn.connect();

						int totalLength = conn.getContentLength()+Long.valueOf(downloadBytes).intValue();
						Loggor.debug(TAG,  "totalLength " + totalLength + " bytes");

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
							long currentTimestamp = System.currentTimeMillis();

							if (percent != lastPercent) {
								lastPercent = percent;

								if (mCallback != null) {
									mCallback.onDownloadProgress(percent, version);
								}

								if (lastReportProcentTimestamp == 0 || (currentTimestamp - lastReportProcentTimestamp >= 100)) { //间隔100ms上报一次process进度，防止每秒上报超过30次
									Loggor.debug(TAG,  "download " + downloadBytes + " bytes. percent:" + percent);
									lastReportProcentTimestamp = currentTimestamp;
									reportProgressMessage(OTA_REPORT_TOPIC, percent, version);
								}
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
							Loggor.error(TAG,  "md5 checksum not match!!! calculated md5:" + calcMD5);

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
						if (e instanceof InterruptedIOException) {//主动interrupt线程
							break;
						} else {
							new File(outputFile).delete(); // delete
						}

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

		mDownloadThread.setName("tencent-ota-imp-download-thread");
		mDownloadThread.start();
	}

	/**
	 * 计算文件的 MD5 摘要值
	 *
	 * @param filePath 全路径文件名
	 * @return 以 16 进制字符表示的摘要字符串
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
	 * @param digestBytes 二进制摘要值
	 * @return 以 16 进制字符表示的摘要字符串
	 */
	private static String convertHashToString(byte[] digestBytes) {
		String returnVal = "";

		for (int i = 0; i < digestBytes.length; i++) {
			returnVal += Integer.toString((digestBytes[i] & 0xff) + 0x100, 16).substring(1);
		}

		return returnVal.toLowerCase();
	}
}
