package com.tencent.iot.explorer.device.tme;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.kugou.ultimatetv.SongPlayStateListener;
import com.kugou.ultimatetv.UltimateKtvLocalPlayer;
import com.kugou.ultimatetv.UltimateSongPlayer;
import com.kugou.ultimatetv.UltimateTv;
import com.kugou.ultimatetv.api.UltimateSongApi;
import com.kugou.ultimatetv.api.model.Response;
import com.kugou.ultimatetv.constant.ErrorCode;
import com.kugou.ultimatetv.constant.PlayerErrorCode;
import com.kugou.ultimatetv.data.entity.User;
import com.kugou.ultimatetv.entity.Song;
import com.kugou.ultimatetv.entity.SongInfo;
import com.kugou.ultimatetv.entity.SongList;
import com.kugou.ultimatetv.entity.UserAuth;
import com.kugou.ultimatetv.util.KGLog;
import com.kugou.ultimatetv.util.RxUtil;
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
import com.tencent.iot.explorer.device.tme.utils.Utils;
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
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.tme.data_template.TmeDataTemplate.METHOD_KUGOU_QUERY_PID_REPLY;
import static com.tencent.iot.explorer.device.tme.data_template.TmeDataTemplate.METHOD_KUGOU_QUERY_SONG_REPLY;


public class TmeMainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = TmeMainActivity.class.getSimpleName();
    private static final String JSON_FILE_NAME = "tme_speaker.json";
    private static final int TYPE_SEARCH = 0;
    private static final int TYPE_SONG_LIST = 1;
    private static final int MSG_REFRESH_VIEW = 1;

    //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mBrokerURL = null;
    private String mProductID = "";
    private String mDevName = "";
    private String mDevPSK = ""; //若使用证书验证，设为null

    private String mDevCert = ""; // Cert String
    private String mDevPriv = ""; // Priv String

    private Button mGetSongBtn;
    private ImageView mSearchIv;
    private ImageView mPreIv;
    private ImageView mNextIv;
    private ImageView mPlayIv;
    private ImageView mPlayModeIv;
    private EditText mInputEt;
    private TextView mCurrentSongTv;
    private RecyclerView mPlayList;
    private SmartRefreshLayout mSmartRefreshLayout;
    private TextView mTimeTv;
    private SeekBar mSeekBar;
    private SeekBar mVolumeSeekBar;
    private ToggleButton mSwitch;


    private TmeDataTemplateSample mDataTemplateSample;
    private SongListAdapter mAdapter;
    private List<Song> mSongList = new ArrayList<>();

    private int page = 1;
    private int pageSize = 10;
    private String currentSongId = "";
    private int mDuration = 0;
    private boolean mIsSeekBarTouching = false;
    private int seekValue = 0;
    private int volumeSeekValue = 0;
    private int maxVolume = 0;
    private int currentVolume = 0;

    private Disposable mLoadSongDisposable;
    private Disposable mSearchSongDisposable;

    private int operationType = -1;
    private String keyword = "";
    private int playMode = UltimateSongPlayer.PLAY_MODE_CYCLE;

    private Dialog dialog;

    private AudioManager mAudioManager;

    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_REFRESH_VIEW:
                    if (UltimateSongPlayer.getInstance().isPlaying()) {
                        int position = (int) UltimateSongPlayer.getInstance().getPlayPositionMs() / 1000;
                        mTimeTv.setText(Utils.toProgress((position), mDuration));
                        int pos = mSeekBar.getProgress();
                        if (pos != position && !mIsSeekBarTouching) {
                            mSeekBar.setProgress(position);
                        }
                    }
                    mMainHandler.removeMessages(MSG_REFRESH_VIEW);
                    mMainHandler.sendEmptyMessageDelayed(MSG_REFRESH_VIEW, 500);
                    break;
                default:
                    break;
            }

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tme_main);
        initView();
        UltimateSongPlayer.getInstance().init();
        UltimateSongPlayer.getInstance().addSongPlayStateListener(mSongPlayStateListener);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVolumeSeekBar.setMax(maxVolume);
        mVolumeSeekBar.setProgress(currentVolume);
    }

    private void initView() {
        mGetSongBtn = findViewById(R.id.request_song);
        mSearchIv = findViewById(R.id.search);
        mPreIv = findViewById(R.id.iv_pre);
        mNextIv = findViewById(R.id.iv_next);
        mPlayIv = findViewById(R.id.iv_play);
        mPlayModeIv = findViewById(R.id.iv_play_mode);
        mInputEt = findViewById(R.id.input);
        mCurrentSongTv = findViewById(R.id.tv_current_song);
        mPlayList = findViewById(R.id.play_list);
        mSmartRefreshLayout = findViewById(R.id.smart_refreshLayout);
        mTimeTv = findViewById(R.id.tv_time);
        mSeekBar = findViewById(R.id.sb_seek_bar);
        mVolumeSeekBar = findViewById(R.id.sb_volume);
        mSwitch = findViewById(R.id.tb_switch);
        mSmartRefreshLayout.setEnableLoadMore(true);
        mSmartRefreshLayout.setRefreshFooter(new ClassicsFooter(this));
        mGetSongBtn.setOnClickListener(this);
        mSearchIv.setOnClickListener(this);
        mPreIv.setOnClickListener(this);
        mNextIv.setOnClickListener(this);
        mPlayIv.setOnClickListener(this);
        mPlayModeIv.setOnClickListener(this);
        mPlayList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mPlayList.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        mAdapter = new SongListAdapter(this, mSongList);
        mAdapter.setOnItemClickListener(new SongListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Song song = mSongList.get(position);
                mCurrentSongTv.setText(song.songName + "-" + song.singerName);
                playSong(song.songId);
            }
        });
        mPlayList.setAdapter(mAdapter);

        mSmartRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                refreshLayout.finishLoadMore();
                page++;
                if (operationType == TYPE_SEARCH) {
                    doSearch(keyword);
                } else if (operationType == TYPE_SONG_LIST) {
                    getSongListById("", "");
                }
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    TXLog.d(TAG, "seek =" + seekValue);
                    seekValue = progress;
                    UltimateSongPlayer.getInstance().seekTo(seekValue * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsSeekBarTouching = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                UltimateSongPlayer.getInstance().seekTo(seekValue * 1000);
                mIsSeekBarTouching = false;
            }
        });
        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    TXLog.d(TAG, "seek =" + volumeSeekValue);
                    volumeSeekValue = progress;
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int volume = (int) ((volumeSeekValue/(maxVolume * 1.00)) * Common.TOTOAL_VOLUME_DURATION);
                reportProperty(Common.PROPERTY_VOLUME, volume);
            }
        });
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (mDataTemplateSample != null) return;
                    buttonView.setText("ONLINE");
                    mDataTemplateSample = new TmeDataTemplateSample(TmeMainActivity.this,
                            mBrokerURL, mProductID, mDevName, mDevPSK,
                            new SelfMqttActionCallBack(mProductID, mDevName), JSON_FILE_NAME, new SelfDownStreamCallBack());
                    mDataTemplateSample.connect();
                } else {
                    buttonView.setText("OFFLINE");
                    if (mDataTemplateSample == null) return;
                    mDataTemplateSample.disconnect();
                    mDataTemplateSample = null;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.request_song: {
                operationType = TYPE_SONG_LIST;
                resetState();
                getSongListById("", Common.PLAY_TYPE_ALBUM);
            }
            break;
            case R.id.search: {
                keyword = mInputEt.getText().toString();
                if (!TextUtils.isEmpty(keyword)) {
                    ToastUtil.showS(keyword);
                    operationType = TYPE_SEARCH;
                    resetState();
                    doSearch(keyword);
                } else {
                    ToastUtil.showS("请输入有效关键词");
                }
            }
            break;
            case R.id.iv_pre: {
                ToastUtil.showS("pre");
                UltimateSongPlayer.getInstance().previous();
                // 通过mqtt上报切歌动作
                reportProperty(Common.PROPERTY_PRE_NEXT, 1);
            }
            break;
            case R.id.iv_next: {
                ToastUtil.showS("next");
                UltimateSongPlayer.getInstance().next();
                // 通过mqtt上报切歌动作
                reportProperty(Common.PROPERTY_PRE_NEXT, 2);
            }
            break;
            case R.id.iv_play: {
                if (TextUtils.isEmpty(currentSongId)) {
                    if (mSongList != null && mSongList.size() > 0) {
                        UltimateSongPlayer.getInstance().play(mSongList);
                    }
                } else {
                    UltimateSongPlayer.getInstance().toggle();
                }
            }
            break;
            case R.id.iv_play_mode: {
                switchPlayMode();
            }
            break;
            default:
                break;
        }
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
            TXLog.d(TAG, logInfo);
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
            String topics = Arrays.toString(asyncActionToken.getTopics());
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), topics, userContextInfo, errMsg);
            if (Status.ERROR == status) {
                TXLog.e(TAG, logInfo);
            } else {
                TXLog.d(TAG, logInfo);
                if (topics.contains(TOPIC_SERVICE_DOWN_PREFIX)) {
                    mDataTemplateSample.requestUserInfo();
                }
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
            TXLog.d(TAG, "======onReplyCallBack : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            TXLog.d(TAG, "======onGetStatusReplyCallBack : " + data.toString());
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            TXLog.d(TAG, "======onControlCallBack : " + msg);
            onControlMsgReceived(msg);
            return null;
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            TXLog.d(TAG, String.format("======onActionCallBack: actionId=[%s], params=[%s]", actionId, params.toString()));
            return null;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            TXLog.d(TAG, "======onUnbindDeviceCallBack : " + msg);
        }
    }

    private final SongPlayStateListener mSongPlayStateListener = new SongPlayStateListener() {

        @Override
        public void onPrepared() {
            TXLog.d(TAG, "======onPrepared");
            mDuration = (int) UltimateSongPlayer.getInstance().getPlayDurationMs() / 1000;
            TmeMainActivity.this.runOnUiThread(() -> {
                mSeekBar.setMax(mDuration);
            });
        }

        @Override
        public void onBufferingStart() {
            TXLog.d(TAG, "======onBufferingStart");
        }

        @Override
        public void onBufferingUpdate(int percent) {
            TXLog.d(TAG, "======onBufferingUpdate");
        }

        @Override
        public void onBufferingEnd() {
            TXLog.d(TAG, "======onBufferingEnd");
        }

        @Override
        public void onPlay() {
            TXLog.d(TAG, "======onPlay");
            int index = UltimateSongPlayer.getInstance().getCurrentIndex();
            Song song = mSongList.get(index);
            TmeMainActivity.this.runOnUiThread(() -> {
                mPlayIv.setImageResource(R.drawable.icon_pause);
                mCurrentSongTv.setText(song.songName + "-" + song.singerName);
            });
            mMainHandler.sendEmptyMessageDelayed(MSG_REFRESH_VIEW, 500);
            // 通过mqtt上报播放
            reportProperty(Common.PROPERTY_PAUSE_PLAY, 1);
            currentSongId = song.songId;
        }

        @Override
        public void onPause() {
            TXLog.d(TAG, "======onPause");
            TmeMainActivity.this.runOnUiThread(() -> mPlayIv.setImageResource(R.drawable.icon_play));
            mMainHandler.removeMessages(MSG_REFRESH_VIEW);
            // 通过mqtt上报暂停
            reportProperty(Common.PROPERTY_PAUSE_PLAY, 0);
        }

        @Override
        public void onSeekComplete() {
            TXLog.d(TAG, "======onSeekComplete");
        }

        @Override
        public void onCompletion() {
            TmeMainActivity.this.runOnUiThread(() -> {
                mPlayIv.setImageResource(R.drawable.icon_play);
                mSeekBar.setMax(0);
            });
            mMainHandler.removeMessages(MSG_REFRESH_VIEW);
            TXLog.d(TAG, "======onCompletion");
        }

        @Override
        public void onError(int what, String msg) {
            //根据错误码自定义提示信息
            String tip = "播放出错，error: " + what;
            TXLog.d(TAG, "======" + tip);
            switch (what) {
                case PlayerErrorCode.KPLAYER_ERROR_SONG_OVERSEAS:
                    tip = "海外地区不能播放";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_NO_COPYRIGHT:
                    tip = "歌曲无版权不能播放";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_NEED_VIP:
                    tip = "会员歌曲，非会员不能播放";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_NEED_PAY:
                    tip = "付费内容，须购买才可播放";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_NIU_NEED_VIP:
                    tip = "牛方案策略，非会员不能播放";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_PLATFORM_NO_COPYRIGHT:
                    tip = "因定向版权下架不能播放（针对APP有权但设备端无权的情况）";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_UNKNOWN:
                    tip = "未知原因,无权播放";
                    break;
                case PlayerErrorCode.KPLAYER_ERROR_SONG_NETWORK_ERROR:
                    tip = "网络错误，请检查网络后重试";
                    break;
                case PlayerErrorCode.PLAY_NET_MUSIC_ERR:
                    tip = "播放网络音乐错误";
                    break;
                case PlayerErrorCode.NO_AVALID_NET:
                    tip = "未找到可用的网络连接";
                    break;
                case PlayerErrorCode.INAVALID_AREA:
                    tip = "该地区无法播放";
                    break;
                case PlayerErrorCode.TRACKER_URL_ERROR:
                    tip = "播放链接错误";
                    break;
                case PlayerErrorCode.NO_SDCARD:
                    tip = "已经拨出SD卡,暂时无法使用";
                    break;
                case PlayerErrorCode.NO_ENOUGH_SPACE:
                    tip = "SD卡未插入或SD卡空间不足";
                    break;
                case PlayerErrorCode.MAKE_STREAM_FAIL:
                    tip = "流转换失败";
                    break;
                case PlayerErrorCode.NO_SUCH_FILE:
                    tip = "文件不存在";
                    break;
                default:
                    break;
            }
            ToastUtil.showS("播放出错, " + tip);
            mMainHandler.removeMessages(MSG_REFRESH_VIEW);
        }
    };

    private void onControlMsgReceived(final JSONObject msg) {
        int value = -1;
        String strValue = "";
        try {
            if (msg.has(Common.PROPERTY_PAUSE_PLAY)) { //播放暂停控制
                value = msg.getInt(Common.PROPERTY_PAUSE_PLAY);
                TXLog.d(TAG, "pause_play = " + value);
                if (TextUtils.isEmpty(currentSongId)) {
                    if (mSongList != null && mSongList.size() > 0) {
                        UltimateSongPlayer.getInstance().play(mSongList);
                    }
                } else {
                    UltimateSongPlayer.getInstance().toggle();
                }
            } else if (msg.has(Common.PROPERTY_PRE_NEXT)) { //上一首下一首
                value = msg.getInt(Common.PROPERTY_PRE_NEXT);
                TXLog.d(TAG, "pre_next = " + value);
                if (value == 1) { //上一首
                    UltimateSongPlayer.getInstance().previous();
                } else if (value == 2) { //下一首
                    UltimateSongPlayer.getInstance().next();
                }
            } else if (msg.has(Common.PROPERTY_PLAY_MODE)) { //播放模式
                value = msg.getInt(Common.PROPERTY_PLAY_MODE);
                TXLog.d(TAG, "play_mode = " + value);
                updatePlayMode(value);
            } else if (msg.has(Common.PROPERTY_VOLUME)) { //播放音量
                value = msg.getInt(Common.PROPERTY_VOLUME);
                TXLog.d(TAG, "volume = " + value);
                int volume = (int) (maxVolume * (value/(Common.TOTOAL_VOLUME_DURATION * 1.000)));
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
                mVolumeSeekBar.setProgress(volume);
            } else if (msg.has(Common.PROPERTY_PLAY_POSITION)) { //播放进度
                value = msg.getInt(Common.PROPERTY_PLAY_POSITION);
                TXLog.d(TAG, "play_position = " + value);
                int position = (int) (mDuration * (value/(Common.TOTOAL_DURATION * 1.000)) * 1000);
                UltimateSongPlayer.getInstance().seekTo(position);
            } else if (msg.has(Common.PROPERTY_RECOMMEND_QUALITY)) { //播放质量
                value = msg.getInt(Common.PROPERTY_RECOMMEND_QUALITY);
                TXLog.d(TAG, "recommend_quality = " + value);
            } else if (msg.has(Common.PROPERTY_CUR_PLAY_LIST)) { //播放列表
                strValue = msg.getString(Common.PROPERTY_CUR_PLAY_LIST);
                TXLog.d(TAG, "cur_play_list = " + strValue);
                updatePlayList(strValue);
            } else if (msg.has(Common.PROPERTY_CUR_SONG_ID)) { //当前播放的歌曲ID
                strValue = msg.getString(Common.PROPERTY_CUR_SONG_ID);
                TXLog.d(TAG, "cur_song_id = " + strValue);
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
                String pid, pkey, userId, token;
                if (code == 0) {
                    JSONObject response = jsonObj.getJSONObject("data");
                    pid = response.getString("pid");
                    pkey = response.getString("pkey");
                    userId = response.getString("user_id");
                    token = response.getString("access_token");
                    // init kugou sdk
                    initKugouSDK(pid, pkey, userId, token);
                } else {
                    ToastUtil.showS(String.format("kugou query user info error, code=%d", code));
                }
            } else if (METHOD_KUGOU_QUERY_SONG_REPLY.equals(method)) {
                // kugou music reply
            }
        } catch (Exception e) {
            TXLog.e(TAG, "onServiceMessageArrivedCallBack: invalid message: " + message);
        }
    }

    private void initKugouSDK(String pid, String pkey, String userId, String token) {
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
            String deviceId = mProductID+"/"+mDevName;
            User user = new User();
            user.userId = userId;
            user.token = token;
            user.expireTime = 1639282332; // 2020/12/12 12:12:12
            UltimateTv.getInstance().init(this, pid, pkey, deviceId, user, callback);
        } catch (IllegalArgumentException e) {
            TXLog.e(TAG, "初始化失败" + e.getMessage());
        }
    }

    private void getSongListById(String id, String type) {
        Consumer<Response<? extends SongList>> consumer = response -> {
            if (response.isSuccess() && response.getData() != null) {
                mSongList.addAll(response.getData().getList());
                if (mAdapter != null) mAdapter.notifyDataSetChanged();
            } else {
                if (page > 1) {
                    page--;
                }
                ToastUtil.showS("加载出错");
            }
        };
        Consumer<Throwable> throwable = _throwable -> {
            if (page>1) {
                page--;
            }
            _throwable.printStackTrace();
            ToastUtil.showS("加载出错");
        };
        RxUtil.d(mLoadSongDisposable);
        if (Common.PLAY_TYPE_ALBUM.equals(type)) {
            mLoadSongDisposable = UltimateSongApi.getAlbumInfoList(id, page, pageSize)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer, throwable);
        } else {
            mLoadSongDisposable = UltimateSongApi.getSongList(id, page, pageSize)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer, throwable);
        }

    }

    private void doSearch(String word) {
        RxUtil.d(mSearchSongDisposable);
        mSearchSongDisposable = UltimateSongApi.getSearchSongList( page, pageSize, word)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songListResponse -> {
                            if (songListResponse.isSuccess() && songListResponse.getData() != null) {
                                mSongList.addAll(songListResponse.getData().getList().subList(0,pageSize));
                                if (mAdapter != null) mAdapter.notifyDataSetChanged();
                                KGLog.i(TAG, "搜索歌曲：" + mSongList.get(0).toString());
                            } else {
                                ToastUtil.showS("加载出错");
                            }
                        },
                        throwable -> ToastUtil.showS("加载出错"));
    }

    private void playSong(String songId) {
        Song song = new Song();
        song.setSongId(songId);
        UltimateSongPlayer.getInstance().insertPlay(song, true);
    }

    private void resetState() {
        page = 1;
        currentSongId = "";
        mSongList.clear();
    }

    private void updatePlayMode(int value) {
        if (value == 0) { //顺序播放
            playMode = UltimateSongPlayer.PLAY_MODE_CYCLE;
            TmeMainActivity.this.runOnUiThread(() -> mPlayModeIv.setImageResource(R.drawable.icon_repeat));
        } else if (value == 1) { //单曲循环
            playMode = UltimateSongPlayer.PLAY_MODE_SINGLE;
            TmeMainActivity.this.runOnUiThread(() -> mPlayModeIv.setImageResource(R.drawable.icon_repeat_one));
        } else if (value == 2) { //随机播放
            playMode = UltimateSongPlayer.PLAY_MODE_RANDOM;
            TmeMainActivity.this.runOnUiThread(() -> mPlayModeIv.setImageResource(R.drawable.icon_shuffle));
        }
        UltimateSongPlayer.getInstance().setPlayMode(playMode);
    }

    private void updatePlayList(String msg) {
        String type = "";
        String id = "";
        try {
            JSONObject obj = new JSONObject(msg);
            type = obj.getString(Common.PLAY_TYPE);
            id = obj.getString(Common.PLAY_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getSongListById(id, type);
    }

    private void switchPlayMode() {
        if (++playMode > 3) {
            playMode = UltimateSongPlayer.PLAY_MODE_CYCLE;
        }
        if (playMode == UltimateSongPlayer.PLAY_MODE_CYCLE) {
            TmeMainActivity.this.runOnUiThread(() -> mPlayModeIv.setImageResource(R.drawable.icon_repeat));
        } else if (playMode == UltimateSongPlayer.PLAY_MODE_SINGLE) {
            TmeMainActivity.this.runOnUiThread(() -> mPlayModeIv.setImageResource(R.drawable.icon_repeat_one));
        } else if (playMode == UltimateSongPlayer.PLAY_MODE_RANDOM) {
            TmeMainActivity.this.runOnUiThread(() -> mPlayModeIv.setImageResource(R.drawable.icon_shuffle));
        }
        UltimateSongPlayer.getInstance().setPlayMode(playMode);
        // 通过mqtt上报最新播放模式给后台
        reportProperty(Common.PROPERTY_PLAY_MODE, playMode - 1);
    }

    private void reportProperty(String key, int value) {
        JSONObject params = new JSONObject();
        try {
            params.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mDataTemplateSample != null) {
            if (Status.OK != mDataTemplateSample.propertyReport(params, null)) {
                TXLog.e(TAG, key + " property report failed!");
            }
        } else {
            TXLog.e(TAG, "mDataTemplateSample is null!");
        }
    }

    private void showDialog(Context context, Bitmap bitmap){
        dialog = new Dialog(context, R.style.iOSDialog);
        dialog.setContentView(R.layout.qrcode_dialog);
        ImageView imageView = dialog.findViewById(R.id.iv_qrcode);
        imageView.setImageBitmap(bitmap);
        dialog.show();
        //选择true的话点击其他地方可以使dialog消失，为false的话不会消失
        dialog.setCanceledOnTouchOutside(false);
        Window w = dialog.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.x = 0;
        lp.y = 40;
        dialog.onWindowAttributesChanged(lp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //彻底不需要使用歌曲播放了，释放资源
        mDataTemplateSample.disconnect();
        UltimateSongPlayer.getInstance().removeSongPlayStateListener(mSongPlayStateListener);
        UltimateSongPlayer.getInstance().release();
    }
}