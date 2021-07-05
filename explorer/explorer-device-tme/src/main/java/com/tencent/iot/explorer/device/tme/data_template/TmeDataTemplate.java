package com.tencent.iot.explorer.device.tme.data_template;

import android.content.Context;

import com.kugou.ultimatetv.UltimateTv;
import com.kugou.ultimatetv.constant.ErrorCode;
import com.kugou.ultimatetv.data.entity.User;
import com.kugou.ultimatetv.entity.SongInfo;
import com.kugou.ultimatetv.entity.UserAuth;
import com.kugou.ultimatetv.util.ToastUtil;
import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.tme.callback.ExpiredCallback;
import com.tencent.iot.explorer.device.tme.entity.UserInfo;
import com.tencent.iot.explorer.device.tme.event.SDKInitEvent;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;

public class TmeDataTemplate extends TXDataTemplate {

    public static final String METHOD_KUGOU_QUERY_PID = "kugou_query_pid";
    public static final String METHOD_KUGOU_QUERY_SONG = "kugou_query_song";
    public static final String METHOD_KUGOU_QUERY_PID_REPLY = "kugou_query_pid_reply";
    public static final String METHOD_KUGOU_QUERY_SONG_REPLY = "kugou_query_song_reply";

    private TXMqttConnection mConnection;

    private String mServiceDownStreamTopic;

    private String mServiceUptreamTopic;

    private Context mContext;

    private String mProductID;

    private String mDevName;

    private ExpiredCallback mExpiredCallback;

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品ID
     * @param deviceName         设备名
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     * @param expiredCallback    TME用户信息过期回调函数
     */
    public TmeDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, ExpiredCallback expiredCallback) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
        this.mServiceDownStreamTopic = TOPIC_SERVICE_DOWN_PREFIX + productId + "/" + deviceName;
        this.mServiceUptreamTopic = TOPIC_SERVICE_UP_PREFIX + productId + "/" + deviceName;
        this.mContext = context;
        this.mProductID = productId;
        this.mDevName = deviceName;
        this.mExpiredCallback = expiredCallback;
    }

    public Status requestUserInfo() {
        //构造发布信息
        JSONObject object = new JSONObject();
        String clientToken = UUID.randomUUID().toString();
        try {
            object.put("method", METHOD_KUGOU_QUERY_PID);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(object.toString().getBytes());
        return mConnection.publish(mServiceUptreamTopic, message, null);
    }

    public Status requestSongInfoById(String id) {
        //构造发布信息
        JSONObject object = new JSONObject();
        JSONObject param = new JSONObject();
        String clientToken = UUID.randomUUID().toString();
        try {
            param.put("song_id", id);
            object.put("params", param);
            object.put("method", METHOD_KUGOU_QUERY_SONG);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(object.toString().getBytes());
        return mConnection.publish(mServiceUptreamTopic, message, null);
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        TXLog.d(TAG, message.toString());
        if (topic.equals(mServiceDownStreamTopic)) {
            onServiceMessageReceived(message);
        }
    }

    private void onServiceMessageReceived(final MqttMessage message) {
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (METHOD_KUGOU_QUERY_PID_REPLY.equals(method)) {
                // user info reply
                int code = jsonObj.getInt("code");
                String pid, pkey, userId, token;
                long expire;
                if (code == 0) {
                    JSONObject response = jsonObj.getJSONObject("data");
                    pid = response.getString("pid");
                    pkey = response.getString("pkey");
                    userId = response.getString("user_id");
                    token = response.getString("token");
                    expire = response.getLong("expire");
                    UserInfo user = new UserInfo(pid, pkey, userId, token, expire);
                    doAuth(user, mExpiredCallback);
                } else {
                    ToastUtil.showS(String.format("query user info error, code=%d", code));
                }
            }
        } catch (Exception e) {
            TXLog.e(TAG, "onServiceMessageArrivedCallBack: invalid message: " + message);
        }
    }

    private void doAuth(UserInfo userInfo , ExpiredCallback expiredCallback) {
        UltimateTv.Callback callback = new UltimateTv.Callback() {
            @Override
            public void onInitResult(int code, String msg) {
                if (code == ErrorCode.CODE_SUCCESS) {
                    EventBus.getDefault().post(new SDKInitEvent());
                    TXLog.d(TAG, "init sdk success, " + msg);
                }
            }
            @Override
            public void onRefreshToken(UserAuth userAuth) {
                if (expiredCallback != null) {
                    expiredCallback.expired();
                }
            }
        };
        //开启日志
        UltimateTv.enableLog(true);
        //配置域名
        HashMap<Integer, String> baseUrlProxyMap = new HashMap<>();
        UltimateTv.Config config = new UltimateTv.Config()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(3000, TimeUnit.MILLISECONDS)
                .forceMvPlayerDeCodeType(0)//默认，自适配
                .defaultSongQuality(SongInfo.QUALITY_SUPER) //无损音质
                .baseUrlProxyMap(baseUrlProxyMap);
        UltimateTv.getInstance().setConfig(config);
        try {
            String deviceId = mProductID + "/" + mDevName;
            User user = new User();
            user.userId = userInfo.getUserId();
            user.token = userInfo.getToken();
            user.expireTime = userInfo.getExpire();
            UltimateTv.getInstance().init(mContext, userInfo.getPid(), userInfo.getPkey(), deviceId, user, callback);
            new CheckTokenThread(user.expireTime, expiredCallback).start();
        } catch (IllegalArgumentException e) {
            TXLog.e(TAG, "初始化失败" + e.getMessage());
        }
    }

    class CheckTokenThread extends Thread {

        private long expiredTime;
        private ExpiredCallback callback;

        public CheckTokenThread(long expiredTime, ExpiredCallback callback) {
            this.expiredTime = expiredTime;
            this.callback = callback;
        }

        @Override
        public void run() {
            while (expiredTime * 1000 > System.currentTimeMillis()) {
                try {
                    Thread.sleep(10000);
                    TXLog.e(TAG, "===== check token");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (callback != null) {
                callback.expired();
            }
        }
    }
}
