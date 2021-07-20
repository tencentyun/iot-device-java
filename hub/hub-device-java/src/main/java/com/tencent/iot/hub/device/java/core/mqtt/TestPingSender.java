package com.tencent.iot.hub.device.java.core.mqtt;

import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class TestPingSender implements MqttPingSender  {
    private static final String CLASS_NAME = TestPingSender.class.getName();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestPingSender.class);
    private ClientComms comms;
    private Timer timer;

    @Override
    public void init(ClientComms comms) {
        logger.info("============ init ============= ");
        if (comms == null) {
            throw new IllegalArgumentException("ClientComms cannot be null.");
        }
        this.comms = comms;
    }

    @Override
    public void start() {
        final String methodName = "start";
        String clientid = comms.getClient().getClientId();

        //@Trace 659=start timer for client:{0}
        logger.info(CLASS_NAME, methodName, "659", new Object[]{clientid});
        logger.info("============ start ============= ");

        timer = new Timer("MQTT Ping: " + clientid);
        //Check ping after first keep alive interval.
        timer.schedule(new TestPingSender.PingTask(), comms.getKeepAlive());
    }

    @Override
    public void stop() {
        final String methodName = "stop";
        //@Trace 661=stop
        logger.info("============ stop ============= ");
        logger.info(CLASS_NAME, methodName, "661", null);
        if(timer != null){
            logger.info("============ timer.cancel ============= ");
            timer.cancel();
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        logger.info("============ schedule ============= " + System.currentTimeMillis());
        timer.schedule(new TestPingSender.PingTask(), delayInMilliseconds);
    }

    private class PingTask extends TimerTask {
        private static final String methodName = "PingTask.run";

        @Override
        public void run() {
            //@Trace 660=Check schedule at {0}
            logger.info(CLASS_NAME, methodName, "660", new Object[]{new Long(System.currentTimeMillis())});
            logger.info("============ PingTask.run ============= " + System.currentTimeMillis());
            comms.checkForActivity();
        }
    }
}
