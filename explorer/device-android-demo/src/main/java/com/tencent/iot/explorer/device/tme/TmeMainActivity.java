package com.tencent.iot.explorer.device.tme;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kugou.ultimatetv.UltimateSongPlayer;
import com.kugou.ultimatetv.UltimateTv;
import com.kugou.ultimatetv.api.UltimateSongApi;
import com.kugou.ultimatetv.constant.ErrorCode;
import com.kugou.ultimatetv.entity.Playlist;
import com.kugou.ultimatetv.entity.Song;
import com.kugou.ultimatetv.entity.SongInfo;
import com.kugou.ultimatetv.entity.UserAuth;
import com.kugou.ultimatetv.util.ToastUtil;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.tme.adapter.SongListAdapter;
import com.tencent.iot.explorer.device.tme.consts.Common;
import com.tencent.iot.explorer.device.tme.data_template.TmeDataTemplateSample;
import com.tencent.iot.explorer.device.tme.event.SDKInitEvent;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.tme.data_template.TmeDataTemplate.METHOD_KUGOU_QUERY_PID_REPLY;
import static com.tencent.iot.explorer.device.tme.data_template.TmeDataTemplate.METHOD_KUGOU_QUERY_SONG_REPLY;


public class TmeMainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = TmeMainActivity.class.getSimpleName();
    private final static String JSON_FILE_NAME = "tme_speaker.json";

    //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mBrokerURL = null;
    private String mProductID = "";
    private String mDevName = "";
    private String mDevPSK = ""; //若使用证书验证，设为null

    private String mDevCert = ""; // Cert String
    private String mDevPriv = ""; // Priv String

    private Button mOnlineBtn;
    private Button mOfflineBtn;
    private Button mGetPidBtn;
    private Button mGetSongBtn;
    private Button mSearchBtn;
    private EditText mInputEt;
    private RecyclerView mPlayList;
    private SmartRefreshLayout mSmartRefreshLayout;


    private TmeDataTemplateSample mDataTemplateSample;
    private SongListAdapter mAdapter;
    private List<Song> mSongList = new ArrayList<>();

    private int page = 1;
    private int pageSize = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tme_main);
        initView();
        UltimateSongPlayer.getInstance().init();
    }

    private void initView() {
        mOnlineBtn = findViewById(R.id.online);
        mOfflineBtn = findViewById(R.id.offline);
        mGetPidBtn = findViewById(R.id.request_pid);
        mGetSongBtn = findViewById(R.id.request_song);
        mSearchBtn = findViewById(R.id.search);
        mInputEt = findViewById(R.id.input);
        mPlayList = findViewById(R.id.play_list);
        mSmartRefreshLayout = findViewById(R.id.smart_refreshLayout);
        mSmartRefreshLayout.setEnableLoadMore(true);
        mSmartRefreshLayout.setRefreshFooter(new ClassicsFooter(this));
        mOnlineBtn.setOnClickListener(this);
        mOfflineBtn.setOnClickListener(this);
        mGetPidBtn.setOnClickListener(this);
        mGetSongBtn.setOnClickListener(this);
        mSearchBtn.setOnClickListener(this);
        mPlayList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mPlayList.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        mAdapter = new SongListAdapter(this, mSongList);
        mAdapter.setOnItemClickListener(new SongListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(TmeMainActivity.this, mSongList.get(position).songName, Toast.LENGTH_SHORT).show();
                playSong(mSongList.get(position).songId);
            }
        });
        mPlayList.setAdapter(mAdapter);

        mSmartRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                refreshLayout.finishLoadMore();
                page++;
                getSongListById("");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.online: {
                if (mDataTemplateSample != null) return;
                mDataTemplateSample = new TmeDataTemplateSample(TmeMainActivity.this,
                        mBrokerURL, mProductID, mDevName, mDevPSK,
                        new SelfMqttActionCallBack(mProductID, mDevName), JSON_FILE_NAME, new SelfDownStreamCallBack());
                mDataTemplateSample.connect();
            }
            break;
            case R.id.offline: {
                if (mDataTemplateSample == null) return;
                mDataTemplateSample.disconnect();
                mDataTemplateSample = null;
            }
            break;
            case R.id.request_pid: {
                initKugouSDK("", "");
            }
            break;
            case R.id.request_song: {
                getSongListById("");
            }
            break;
            default:
                break;
        }
    }

    private void playSong(String songId) {
        Song song = new Song();
        song.setSongId(songId);
        UltimateSongPlayer.getInstance().insertPlay(song, true);
    }


    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        private String productId;
        private String deviceName;

        public SelfMqttActionCallBack(String productId, String deviceName) {
            this.productId = productId;
            this.deviceName = deviceName;
        }

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            if (mDataTemplateSample != null) {
                if (!reconnect) {
                    mDataTemplateSample.subscribeTopic();
                }
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            TXLog.e(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            TXLog.d(TAG, logInfo);
            if (mDataTemplateSample != null) {
                mDataTemplateSample.unSubscribeTopic();
            }
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            TXLog.d(TAG, logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                TXLog.e(TAG, logInfo);
            } else {
                TXLog.d(TAG, logInfo);
            }
            if (Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
                TXLog.e(TAG, "property get status failed!");
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            TXLog.d(TAG, logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("onMessageReceived, topic[%s], message[%s]", topic, message.toString());
            TXLog.d(TAG, logInfo);
            if (topic.equals(TOPIC_SERVICE_DOWN_PREFIX + productId + "/" + deviceName)) {
                onServiceMessageReceived(message);
            }
        }
    }

    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            TXLog.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {

        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            TXLog.d(TAG, "control down stream message received : " + msg);
            onControlMsgReceived(msg);
            return null;
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            TXLog.d(TAG, String.format("onActionCallBack : actionId=[%s], params=[%s]", actionId, params.toString()));
            return null;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            Log.d(TAG, "unbind device received : " + msg);
        }
    }

    private void onControlMsgReceived(final JSONObject msg) {
        int value = -1;
        try {
            if (msg.has(Common.PAUSE_PLAY)) {
                value = msg.getInt(Common.PAUSE_PLAY);
                TXLog.d(TAG, "pause_play = " + value);
                if (UltimateSongPlayer.getInstance().isPlaying()) {
                    if (value == 0) {  //暂停
                        UltimateSongPlayer.getInstance().pause();
                    }
                } else {
                    if (value == 1) {  //播放
                        UltimateSongPlayer.getInstance().play();
                    }
                }
            } else if (msg.has(Common.PRE_NEXT)) {
                value = msg.getInt(Common.PRE_NEXT);
                TXLog.d(TAG, "pre_next = " + value);
            } else if (msg.has(Common.PLAY_MODE)) {
                value = msg.getInt(Common.PLAY_MODE);
                TXLog.d(TAG, "play_mode = " + value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onServiceMessageReceived(final MqttMessage message) {
        //根据method进行相应处理
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (METHOD_KUGOU_QUERY_PID_REPLY.equals(method)) {
                // user info reply
                int code = jsonObj.getInt("code");
                String pid, pkey;
                if (code == 0) {
                    JSONObject response = jsonObj.getJSONObject("data");
                    pid = response.getString("pid");
                    pkey = response.getString("pkey");
                    // init kugou sdk
                    initKugouSDK(pid, pkey);
                }
            } else if (METHOD_KUGOU_QUERY_SONG_REPLY.equals(method)) {
                // kugou music reply
            }
        } catch (Exception e) {
            TXLog.e(TAG, "onServiceMessageArrivedCallBack: invalid message: " + message);
        }
    }

    private void initKugouSDK(String pid, String pkey) {
        UltimateTv.Callback callback = new UltimateTv.Callback() {
            @Override
            public void onInitResult(int code, String msg) {
                if (code == ErrorCode.CODE_SUCCESS) {
                    EventBus.getDefault().post(new SDKInitEvent());
                    TXLog.d(TAG, "init kugou sdk success, " + msg);
                }
            }
            @Override
            public void onRefreshToken(UserAuth userAuth) {

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
            UltimateTv.getInstance().init(TmeMainActivity.this, pid, pkey, callback);
        } catch (IllegalArgumentException e) {
            TXLog.e(TAG, "初始化失败" + e.getMessage());
        }
    }

    private void getSongListById(String id) {
        UltimateSongApi.getSongList(id, page, pageSize)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                mSongList.addAll(response.getData().getList());
                                if (mAdapter != null) mAdapter.notifyDataSetChanged();
                            } else {
                                if (page>1) {
                                    page--;
                                }
                                ToastUtil.showS("加载出错");
                            }
                        },
                        throwable -> {
                            if (page>1) {
                                page--;
                            }
                            throwable.printStackTrace();
                            ToastUtil.showS("加载出错");
                        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //彻底不需要使用歌曲播放了，释放资源
        UltimateSongPlayer.getInstance().release();
    }
}