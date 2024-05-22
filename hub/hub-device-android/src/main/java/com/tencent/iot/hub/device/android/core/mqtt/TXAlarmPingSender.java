package com.tencent.iot.hub.device.android.core.mqtt;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

/**
 * 心跳包发送器
 */
public class TXAlarmPingSender implements MqttPingSender {

    /**
     * 类标记
     */
    private static final String TAG = "iot.TXAlarmPingSender";

    private ClientComms mComms;
    private Context mContext;
    private BroadcastReceiver mAlarmReceiver;
    private TXAlarmPingSender that;
    private PendingIntent pendingIntent;
    private volatile boolean hasStarted = false;

    /**
     * 构造函数
     *
     * @param context 上下文 {@link Context}
     */
    public TXAlarmPingSender(Context context) {
        this.mContext = context;
        that = this;
    }

    /**
     * 初始化
     *
     * @param comms {@link ClientComms}
     */
    @Override
    public void init(ClientComms comms) {
        this.mComms = comms;
        this.mAlarmReceiver = new AlarmReceiver();
    }

    /**
     * 启动心跳发送器
     */
    @Override
    public void start() {
        String action = TAG + TXMqttConstants.PING_SENDER + mComms.getClient().getClientId();
        TXLog.d(TAG, "Register alarmreceiver to Context " + action);
        if (mContext != null && mAlarmReceiver != null) {
            mContext.registerReceiver(mAlarmReceiver, new IntentFilter(action));
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(action), flags);

        schedule(mComms.getKeepAlive());
        hasStarted = true;
    }

    /**
     * 停止心跳发送器
     */
    @Override
    public void stop() {

        TXLog.d(TAG, "Unregister alarmreceiver to Context " + mComms.getClient().getClientId());
        if(hasStarted){
            if(pendingIntent != null){
                // Cancel Alarm.
                AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                try {
                    alarmManager.cancel(pendingIntent);
                } catch (Exception e) {
                    TXLog.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }

            hasStarted = false;
            try{
                mContext.unregisterReceiver(mAlarmReceiver);
            }catch(IllegalArgumentException e){
                //Ignore unregister errors.			
            }
        }
    }

    /**
     * 定时发送心跳
     *
     * @param delayInMilliseconds 定时周期
     */
    @Override
    public void schedule(long delayInMilliseconds) {
        long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
        TXLog.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        if(Build.VERSION.SDK_INT >= 23){
            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
            // the device to run this task whilst dosing.
            TXLog.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            TXLog.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        }
    }

    /**
     * PingReq发送类
     */
    class AlarmReceiver extends BroadcastReceiver {

        private PowerManager.WakeLock wakelock;

        private final String wakeLockTag = TAG + TXMqttConstants.PING_WAKELOCK + that.mComms.getClient().getClientId();

        @Override
        @SuppressLint("Wakelock")
        public void onReceive(Context context, Intent intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.

            TXLog.d(TAG, "Sending Ping at: " + System.currentTimeMillis());

            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
            try { // 唤醒锁获取失败，抛出权限异常，同时避免后续的释放过程
                wakelock.acquire();
            } catch (Exception e) {
                TXLog.e(TAG, "wakelock without permission.WAKE_LOCK return");
                e.printStackTrace();
                return;
            }

            // Assign new callback to token to execute code after PingResq
            // arrives. Get another wakelock even receiver already has one,
            // release it until ping response returns.
            IMqttToken token = mComms.checkForActivity(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    TXLog.d(TAG, "Success. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    wakelock.release();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TXLog.d(TAG, "Failure. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    wakelock.release();
                }
            });


            if (token == null && wakelock.isHeld()) {
                wakelock.release();
            }
        }
    }
}
