package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Target;

public class TXAlarmPingSender implements MqttPingSender {
	private static final Logger logger = LoggerFactory.getLogger(TXMqttConnection.class);
	public static final String TAG = TXAlarmPingSender.class.getSimpleName();

	static { Loggor.setLogger(logger); }

	private ClientComms mComms;

	private TXAlarmPingSender that;
	private volatile boolean hasStarted = false;

	public TXAlarmPingSender() {
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.mComms = comms;
	}

	@Override
	public void start() {
		String action = TXMqttConstants.PING_SENDER + mComms.getClient().getClientId();
		Loggor.debug(TAG, "Register alarmreceiver to Context " + action);
		schedule(mComms.getKeepAlive());
		hasStarted = true;
	}

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

	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
		Loggor.debug(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
	}

}
