package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ping 发送器类
 */
public class TXAlarmPingSender implements MqttPingSender {
	private static final Logger logger = LoggerFactory.getLogger(TXMqttConnection.class);
	/**
	 * 类标记
	 */
	public static final String TAG = TXAlarmPingSender.class.getSimpleName();

	static { Loggor.setLogger(logger); }

	private ClientComms mComms;

	private TXAlarmPingSender that;
	private volatile boolean hasStarted = false;

	/**
	 * 构造函数
	 */
	public TXAlarmPingSender() {
		that = this;
	}

	/**
	 * 初始化
	 * @param comms {@link ClientComms}
	 */
	@Override
	public void init(ClientComms comms) {
		this.mComms = comms;
	}

	/**
	 * 启动发送器
	 */
	@Override
	public void start() {
		String action = TXMqttConstants.PING_SENDER + mComms.getClient().getClientId();
		Loggor.debug(TAG, "Register alarmreceiver to Context " + action);
		schedule(mComms.getKeepAlive());
		hasStarted = true;
	}

	/**
	 * 停止发送器
	 */
	@Override
	public void stop() {
		Loggor.debug(TAG, "Unregister alarmreceiver to Context " + mComms.getClient().getClientId());
		if (hasStarted) {
			hasStarted = false;
			try {
			} catch (IllegalArgumentException e) {
				// Ignore unregister errors.
			}
		}
	}

	/**
	 * 定时任务
	 * @param delayInMilliseconds 定时周期
	 */
	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
		Loggor.debug(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
	}

}
