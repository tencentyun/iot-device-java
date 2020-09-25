package com.tencent.iot.hub.device.java.main.scenarized;


import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;

public class Airconditioner {

    private static final Logger LOG = LoggerFactory.getLogger(Airconditioner.class);
	public static final String TAG = "iot.scenarized.Airconditioner";

	/**
	 * 产品ID
	 */
	private static final String PRODUCT_ID = "YOUR_PRODUCT_ID";

	/**
	 * 设备名称
	 */
	protected static final String DEVICE_NAME = "YOUR_DEVICE_NAME";

	/**
	 * 密钥
	 */
	private static final String SECRET_KEY = "YOUR_SECRET_KEY";
	/**
	 * 设备证书名
	 */
	private static final String DEVICE_CERT_NAME = "YOUR_DEVICE_NAME_cert.crt";

	/**
	 * 设备私钥文件名
	 */
	private static final String DEVICE_KEY_NAME = "YOUR_DEVICE_NAME_private.key";

	private TXMqttConnection mqttConnection;

	private MqttConnectOptions options;

	public Airconditioner( TXMqttActionCallBack callBack) {

		mqttConnection = new TXMqttConnection(PRODUCT_ID, DEVICE_NAME, SECRET_KEY, callBack);

		options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(240);
		options.setAutomaticReconnect(true);
		options.setSocketFactory(
				AsymcSslUtils.getSocketFactoryByAssetsFile(DEVICE_CERT_NAME, DEVICE_KEY_NAME));

		mqttConnection.connect(options, null);
		DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
		bufferOptions.setBufferEnabled(true);
		bufferOptions.setBufferSize(1024);
		bufferOptions.setDeleteOldestMessages(true);
		mqttConnection.setBufferOpts(bufferOptions);
	}

	public void subScribeTopic() {
		LOG.debug("{}", "subScribeTopic");
		String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "control");
		mqttConnection.subscribe(topic, TXMqttConstants.QOS1, null);
	}

	public void closeConnection() {
		if (null != mqttConnection) {
			mqttConnection.disConnect(null);
		}
	}
}
