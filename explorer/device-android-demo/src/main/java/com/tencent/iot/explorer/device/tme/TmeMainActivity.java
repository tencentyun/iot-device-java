package com.tencent.iot.explorer.device.tme;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.kugou.ultimatetv.SongPlayStateListener;
import com.kugou.ultimatetv.UltimateSongPlayer;
import com.kugou.ultimatetv.api.UltimateSongApi;
import com.kugou.ultimatetv.api.model.Response;
import com.kugou.ultimatetv.entity.Song;
import com.kugou.ultimatetv.entity.SongInfo;
import com.kugou.ultimatetv.entity.SongList;
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
import com.tencent.iot.explorer.device.rtc.utils.ZXingUtils;
import com.tencent.iot.explorer.device.tme.adapter.SongListAdapter;
import com.tencent.iot.explorer.device.tme.callback.AuthCallback;
import com.tencent.iot.explorer.device.tme.consts.Common;
import com.tencent.iot.explorer.device.tme.consts.TmeConst;
import com.tencent.iot.explorer.device.tme.consts.TmeErrorEnum;
import com.tencent.iot.explorer.device.tme.data_template.TmeDataTemplateSample;
import com.tencent.iot.explorer.device.tme.utils.SharePreferenceUtil;
import com.tencent.iot.explorer.device.tme.utils.Utils;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;


public class TmeMainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = TmeMainActivity.class.getSimpleName();
    private static final String JSON_FILE_NAME = "tme_speaker.json";
    private static final int TYPE_SEARCH = 0;
    private static final int TYPE_SONG_LIST = 1;
    private static final int MSG_REFRESH_VIEW = 1;
    private static final int MSG_POPUP_QRCODE = 2;
    private static final String[] qualityStrArray = { "标准","高清","无损" };
    private static final int[] qualities = { SongInfo.QUALITY_STANDARD, SongInfo.QUALITY_HIGH, SongInfo.QUALITY_SUPER };

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
    private Spinner mSpinner;

    private TmeDataTemplateSample mDataTemplateSample;
    private SongListAdapter mAdapter;
    private volatile List<Song> mSongList = new ArrayList<>();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private int page = 1;
    private int pageSize = 20;
    private String currentSongId = "";
    private int mDuration = 0;
    private boolean mIsSeekBarTouching = false;
    private int seekValue = 0;
    private int volumeSeekValue = 0;
    private int maxVolume = 0;
    private int currentVolume = 0;
    private boolean isGetStatusReply = false;
    private String currentPlayType = "";
    private int currentTopId = -1;
    private String currentSongListId = "";

    private Disposable mLoadSongDisposable;
    private Disposable mSearchSongDisposable;

    private int operationType = -1;
    private String keyword = "";
    private int playMode = UltimateSongPlayer.PLAY_MODE_CYCLE;
    private String songListId = "";
    private String songListType = "";

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
                        mTimeTv.setText(Utils.toProgress(position, mDuration));
                        int pos = mSeekBar.getProgress();
                        if (pos != position && !mIsSeekBarTouching) {
                            mSeekBar.setProgress(position);
                            reportProperty(Common.PROPERTY_PLAY_POSITION, position);
                        }
                    }
                    mMainHandler.removeMessages(MSG_REFRESH_VIEW);
                    mMainHandler.sendEmptyMessageDelayed(MSG_REFRESH_VIEW, 500);
                    break;
                case MSG_POPUP_QRCODE:
                    Bitmap qrcode = ZXingUtils.createQRCodeBitmap(mDataTemplateSample.getAuthQrcode(), 200, 200,
                            "UTF-8", "H", "1", Color.BLACK, Color.WHITE);
                    showDialog(TmeMainActivity.this, qrcode);
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
        Intent intent = getIntent();
        String preBrokerUrl = intent.getStringExtra(TmeConst.TME_BROKER_URL);
        if (!TextUtils.isEmpty(preBrokerUrl)) {
            mBrokerURL = preBrokerUrl;
        }
        initView();
        UltimateSongPlayer.getInstance().init();
        UltimateSongPlayer.getInstance().addSongPlayStateListener(mSongPlayStateListener);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVolumeSeekBar.setMax(maxVolume);
        mVolumeSeekBar.setProgress(currentVolume);
        mProductID = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_PRODUCT_ID);
        mDevName = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_DEVICE_NAME);
        mDevPSK = SharePreferenceUtil.getString(this, TmeConst.TME_CONFIG, TmeConst.TME_DEVICE_PSK);
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
        mSpinner = findViewById(R.id.sp_quality);
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
                playSong(position);
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
                    getSongListById(songListId, songListType);
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
                if (seekValue >= 60) {
                    SongInfo songInfo = UltimateSongPlayer.getInstance().getSongInfo();
                    if (songInfo != null && songInfo.isTryListen()) {
                        TmeMainActivity.this.runOnUiThread(() -> ToastUtil.showS("当前歌曲只支持试听60秒~"));
                    }
                }
                UltimateSongPlayer.getInstance().seekTo(seekValue * 1000);
                mIsSeekBarTouching = false;
                reportProperty(Common.PROPERTY_PLAY_POSITION, seekValue);
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
                    mDataTemplateSample = new TmeDataTemplateSample(TmeMainActivity.this,
                            mBrokerURL, mProductID, mDevName, mDevPSK,
                            new SelfMqttActionCallBack(mProductID, mDevName), JSON_FILE_NAME,
                            new SelfDownStreamCallBack(),
                            mAuthCallback);
                    mDataTemplateSample.connect();
                } else {
                    if (mDataTemplateSample == null) return;
                    mDataTemplateSample.disconnect();
                    mDataTemplateSample = null;
                }
            }
        });

        mSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, qualityStrArray));
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isGetStatusReply) {
                    UltimateSongPlayer.getInstance().changeQuality(qualities[position]);
                    isGetStatusReply = false;
                } else {
                    if (UltimateSongPlayer.getInstance().getCurrentPlayQuality() == qualities[position]) {
                        return;
                    }
                    //当前歌曲详情
                    SongInfo songInfo = UltimateSongPlayer.getInstance().getSongInfo();
                    if (songInfo == null) {
                        ToastUtil.showS("无法切换音质");
                        parent.setSelection(0);
                        return;
                    }
                    if (songInfo.isTryListen()) {
                        ToastUtil.showS("试听中，无法切换音质");
                        parent.setSelection(0);
                        return;
                    }
                    if (songInfo.getSupportQualities().contains(qualities[position])) {
                        UltimateSongPlayer.getInstance().changeQuality(qualities[position]);
                        reportProperty(Common.PROPERTY_RECOMMEND_QUALITY, position);
                    } else {
                        parent.setSelection(0);
                        ToastUtil.showS(String.format("当前歌曲不支持%s音质", qualityStrArray[position]));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.request_song: {
                operationType = TYPE_SONG_LIST;
                resetState();
                getSongListById(songListId, songListType);
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
                reportProperty(Common.PROPERTY_PRE_NEXT, Common.PROPERTY_PRE);
            }
            break;
            case R.id.iv_next: {
                ToastUtil.showS("next");
                UltimateSongPlayer.getInstance().next();
                // 通过mqtt上报切歌动作
                reportProperty(Common.PROPERTY_PRE_NEXT, Common.PROPERTY_NEXT);
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
            if (Status.ERROR == status) {
                ToastUtil.showS("上线失败，请检查设备三元组信息是否正确或网络是否正常");
                mSwitch.setChecked(false);
            } else {
                ToastUtil.showS("上线成功");
                if (mDataTemplateSample != null) {
                    if (!reconnect) {
                        mDataTemplateSample.subscribeTopic();
                    }
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
            updateControlState(data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            TXLog.d(TAG, "======onControlCallBack : " + msg);
            onControlMsgReceived(msg);
            JSONObject result = new JSONObject();
            try {
                result.put("code",0);
                result.put("status", "success");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            TXLog.d(TAG, String.format("======onActionCallBack: actionId=[%s], params=[%s]", actionId, params.toString()));
            if (Common.ACTION_REFRESH_TOKEN.equals(actionId)) {
                mDataTemplateSample.requestUserInfo();
            }
            JSONObject result = new JSONObject();
            try {
                result.put("code", 0);
                result.put("status", "success");
                JSONObject response = new JSONObject();
                response.put("result", 0);
                result.put("response", response);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            TXLog.d(TAG, "======onUnbindDeviceCallBack : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            TXLog.d(TAG, "======onBindDeviceCallBack : " + msg);
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
            String currSongId = UltimateSongPlayer.getInstance().getCurPlaySong().songId;
            reportProperty(Common.PROPERTY_CUR_SONG_ID, currSongId);
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
            mAdapter.setCurrentPos(index);
            TmeMainActivity.this.runOnUiThread(() -> mAdapter.notifyDataSetChanged());
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
            String tip = String.format("播放出错，error: %d, msg: %s", what, msg);
            TXLog.d(TAG, "======" + tip);
            TmeErrorEnum error = TmeErrorEnum.byCode(what);
            if (error != null) {
                switch (error) {
                    case KPLAYER_ERROR_SONG_OVERSEAS:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_OVERSEAS.msg();
                        break;
                    case KPLAYER_ERROR_SONG_NO_COPYRIGHT:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_NO_COPYRIGHT.msg();
                        break;
                    case KPLAYER_ERROR_SONG_NEED_VIP:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_NEED_VIP.msg();
                        break;
                    case KPLAYER_ERROR_SONG_NEED_PAY:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_NEED_PAY.msg();
                        break;
                    case KPLAYER_ERROR_SONG_NIU_NEED_VIP:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_NIU_NEED_VIP.msg();
                        break;
                    case KPLAYER_ERROR_SONG_PLATFORM_NO_COPYRIGHT:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_PLATFORM_NO_COPYRIGHT.msg();
                        break;
                    case KPLAYER_ERROR_SONG_UNKNOWN:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_UNKNOWN.msg();
                        break;
                    case KPLAYER_ERROR_SONG_NETWORK_ERROR:
                        tip = TmeErrorEnum.KPLAYER_ERROR_SONG_NETWORK_ERROR.msg();
                        break;
                    case PLAY_NET_MUSIC_ERR:
                        tip = TmeErrorEnum.PLAY_NET_MUSIC_ERR.msg();
                        break;
                    case NO_AVALID_NET:
                        tip = TmeErrorEnum.NO_AVALID_NET.msg();
                        break;
                    case INAVALID_AREA:
                        tip = TmeErrorEnum.INAVALID_AREA.msg();
                        break;
                    case TRACKER_URL_ERROR:
                        tip = TmeErrorEnum.TRACKER_URL_ERROR.msg();
                        break;
                    case NO_SDCARD:
                        tip = TmeErrorEnum.NO_SDCARD.msg();
                        break;
                    case NO_ENOUGH_SPACE:
                        tip = TmeErrorEnum.NO_ENOUGH_SPACE.msg();
                        break;
                    case MAKE_STREAM_FAIL:
                        tip = TmeErrorEnum.MAKE_STREAM_FAIL.msg();
                        break;
                    case UNDEFINED_ERROR_CODE:
                        tip = TmeErrorEnum.UNDEFINED_ERROR_CODE.msg();
                        break;
                    case PARAMETER_ERROR:
                        tip = TmeErrorEnum.PARAMETER_ERROR.msg();
                        break;
                    case SYSTEM_BUSY:
                        tip = TmeErrorEnum.SYSTEM_BUSY.msg();
                        break;
                    case AUTHENTICATION_INFORMATION_OUT_OF_DATE_OR_WRONG:
                        tip = TmeErrorEnum.AUTHENTICATION_INFORMATION_OUT_OF_DATE_OR_WRONG.msg();
                        mMainHandler.sendEmptyMessage(MSG_POPUP_QRCODE);
                        break;
                    case CODE_DEVICE_NOTACTIVATE:
                        tip = TmeErrorEnum.CODE_DEVICE_NOTACTIVATE.msg();
                        break;
                    case SYSTEM_ERROR:
                        tip = TmeErrorEnum.SYSTEM_ERROR.msg();
                        break;
                    case NO_RIGHT_TO_CALL_THIS_INTERFACE:
                        tip = TmeErrorEnum.NO_RIGHT_TO_CALL_THIS_INTERFACE.msg();
                        break;
                    default:
                        break;
                }
            }
            ToastUtil.showS("播放出错, " + tip);
            int index = UltimateSongPlayer.getInstance().getCurrentIndex();
            Song song = mSongList.get(index);
            TmeMainActivity.this.runOnUiThread(() -> {
                mCurrentSongTv.setText(song.songName + "-" + song.singerName);
            });
            currentSongId = song.songId;
            mAdapter.setCurrentPos(index);
            TmeMainActivity.this.runOnUiThread(() -> mAdapter.notifyDataSetChanged());
            mMainHandler.removeMessages(MSG_REFRESH_VIEW);
        }
    };

    private final AuthCallback mAuthCallback = new AuthCallback() {
        @Override
        public void expired() {
            mMainHandler.sendEmptyMessage(MSG_POPUP_QRCODE);
        }

        @Override
        public void refreshed() {
            ToastUtil.showS("Refresh Token");
            if (dialog != null && dialog.isShowing()) {
                dismissDialog();
            }
        }
    };

    private void onControlMsgReceived(final JSONObject msg) {
        int value = -1;
        int controlSeq = -1;
        String strValue = "";
        try {
            if (msg.has(Common.PROPERTY_CONTROL_SEQ)) {
                controlSeq = msg.getInt(Common.PROPERTY_CONTROL_SEQ);
            }
            if (msg.has(Common.PROPERTY_PAUSE_PLAY)) { //播放暂停控制
                value = msg.getInt(Common.PROPERTY_PAUSE_PLAY);
                TXLog.d(TAG, "pause_play = " + value);
                if (TextUtils.isEmpty(currentSongId)) {
                    if (mSongList != null && mSongList.size() > 0) {
                        if (value == 1) {
                            UltimateSongPlayer.getInstance().play(mSongList);
                        }
                    }
                } else {
                    if (value == 0) {
                        UltimateSongPlayer.getInstance().pause();
                    } else if (value == 1){
                        UltimateSongPlayer.getInstance().play();
                    }
                }
                reportProperty(Common.PROPERTY_PAUSE_PLAY, value, controlSeq);
            } else if (msg.has(Common.PROPERTY_PRE_NEXT)) { //上一首下一首
                value = msg.getInt(Common.PROPERTY_PRE_NEXT);
                TXLog.d(TAG, "pre_next = " + value);
                if (value == 1) { //上一首
                    UltimateSongPlayer.getInstance().previous();
                } else if (value == 2) { //下一首
                    UltimateSongPlayer.getInstance().next();
                }
                reportProperty(Common.PROPERTY_PRE_NEXT, value, controlSeq);
            } else if (msg.has(Common.PROPERTY_PLAY_MODE)) { //播放模式
                value = msg.getInt(Common.PROPERTY_PLAY_MODE);
                TXLog.d(TAG, "play_mode = " + value);
                updatePlayMode(value);
                reportProperty(Common.PROPERTY_PLAY_MODE, value, controlSeq);
            } else if (msg.has(Common.PROPERTY_VOLUME)) { //播放音量
                value = msg.getInt(Common.PROPERTY_VOLUME);
                TXLog.d(TAG, "volume = " + value);
                updateVolume(value);
                reportProperty(Common.PROPERTY_VOLUME, value, controlSeq);
            } else if (msg.has(Common.PROPERTY_PLAY_POSITION)) { //播放进度
                value = msg.getInt(Common.PROPERTY_PLAY_POSITION);
                TXLog.d(TAG, "play_position = " + value);
                UltimateSongPlayer.getInstance().seekTo(value * 1000);
                reportProperty(Common.PROPERTY_PLAY_POSITION, value, controlSeq);
            } else if (msg.has(Common.PROPERTY_RECOMMEND_QUALITY)) { //播放质量
                value = msg.getInt(Common.PROPERTY_RECOMMEND_QUALITY);
                TXLog.d(TAG, "recommend_quality = " + value);
                updateQuality(value);
                reportProperty(Common.PROPERTY_RECOMMEND_QUALITY, value, controlSeq);
            } else if (msg.has(Common.PROPERTY_CUR_PLAY_LIST)) { //播放列表
                strValue = msg.getString(Common.PROPERTY_CUR_PLAY_LIST);
                TXLog.d(TAG, "cur_play_list = " + strValue);
                updatePlayList(strValue);
                reportProperty(Common.PROPERTY_CUR_PLAY_LIST, strValue, controlSeq);
            } else if (msg.has(Common.PROPERTY_CUR_SONG_ID)) { //当前播放的歌曲ID
                strValue = msg.getString(Common.PROPERTY_CUR_SONG_ID);
                int songIndex = msg.getInt(Common.PROPERTY_SONG_INDEX);
                TXLog.d(TAG, "cur_song_id = " + strValue);
                playSong(songIndex);
                reportProperty(Common.PROPERTY_CUR_SONG_ID, strValue, controlSeq);
            }
        } catch (JSONException e) {
            ToastUtil.showS("JSON解析错误");
            e.printStackTrace();
        }
    }

    private void playSong(int index) {
        if (index >= mSongList.size()) {
            int count = (((index+1) - mSongList.size()) / pageSize) + 1;
            while (count-- > 0) {
                // 加载更多
                countDownLatch = new CountDownLatch(1);
                page++;
                getSongList(currentPlayType, songListId, currentTopId);
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        UltimateSongPlayer.getInstance().play(mSongList, index, true);
    }

    private int getIndexBySongId(String songId) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mSongList == null || mSongList.isEmpty() || TextUtils.isEmpty(songId)) {
            return -1;
        }
        for (int i = 0; i < mSongList.size(); i++) {
            if (mSongList.get(i).songId.equals(songId)) {
                return i;
            }
        }
        return -1;
    }

    private void getSongList(String _type, String _id, int _topId) {
        Consumer<Response<? extends SongList>> consumer = response -> {
            if (response.isSuccess() && response.getData() != null) {
                List<Song> songs = response.getData().getList();
                mSongList.addAll(songs);
                UltimateSongPlayer.getInstance().enqueue(songs, true);
                if (mAdapter != null) mAdapter.notifyDataSetChanged();
            } else {
                if (page > 1) {
                    page--;
                }
                ToastUtil.showS("加载出错");
            }
            countDownLatch.countDown();
        };
        Consumer<Throwable> throwable = _throwable -> {
            if (page>1) {
                page--;
            }
            _throwable.printStackTrace();
            ToastUtil.showS("加载出错");
        };
        RxUtil.d(mLoadSongDisposable);
        if (!TextUtils.isEmpty(_id)) {
            if (Common.PLAY_TYPE_ALBUM.equals(_type)) { // 专辑
                mLoadSongDisposable = UltimateSongApi.getAlbumInfoList(_id, page, pageSize)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(consumer, throwable);
            } else if (Common.PLAY_TYPE_PLAYLIST.equals(_type)){ // 歌单
                mLoadSongDisposable = UltimateSongApi.getSongList(_id, page, pageSize)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(consumer, throwable);
            }
        } else {
            if (Common.PLAY_TYPE_EVERYDAY.equals(_type)) { // 日推
                mLoadSongDisposable = UltimateSongApi.getDailyRecommendList()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(consumer, throwable);
            } else if (Common.PLAY_TYPE_NEWSONG.equals(_type)) { // 新歌首发
                mLoadSongDisposable = UltimateSongApi.getFirstPublishSongList(page, pageSize, _topId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(consumer, throwable);
            } else {
                 ToastUtil.showS("歌单为空，请在腾讯连连App或小程序下发歌单");
             }
        }
    }

    private void getSongListById(String id, String type) {
        getSongList(type, id, 0);
    }

    private void doSearch(String word) {
        RxUtil.d(mSearchSongDisposable);
        mSearchSongDisposable = UltimateSongApi.getSearchSongList(page, pageSize, word)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songListResponse -> {
                            if (songListResponse.isSuccess() && songListResponse.getData() != null) {
                                mSongList.addAll(songListResponse.getData().getList().subList(0, pageSize));
                                if (mAdapter != null) mAdapter.notifyDataSetChanged();
                                KGLog.i(TAG, "搜索歌曲：" + mSongList.get(0).toString());
                            } else {
                                ToastUtil.showS("加载出错");
                            }
                        },
                        throwable -> ToastUtil.showS("加载出错"));
    }

    private void resetState() {
        page = 1;
        currentSongId = "";
        countDownLatch = new CountDownLatch(1);
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

    private void updateControlState(JSONObject data) {
        if (data.has("reported")) {
            try {
                int volume = 0;
                int playMode = 0;
                int quality = 0;
                String currentPlayList = "";
                JSONObject obj = data.getJSONObject("reported");
                if (obj.has(Common.PROPERTY_VOLUME)) { //恢复现场-音量
                    volume = obj.getInt(Common.PROPERTY_VOLUME);
                    updateVolume(volume);
                }
                if (obj.has(Common.PROPERTY_PLAY_MODE)) { //恢复现场-播放模式
                    playMode = obj.getInt(Common.PROPERTY_PLAY_MODE);
                    updatePlayMode(playMode);
                }
                if (obj.has(Common.PROPERTY_CUR_PLAY_LIST)) { //恢复现场-歌单列表
                    currentPlayList = obj.getString(Common.PROPERTY_CUR_PLAY_LIST);
                    updatePlayList(currentPlayList);
                }
                if (obj.has(Common.PROPERTY_RECOMMEND_QUALITY)) { //恢复现场-音质
                    quality = obj.getInt(Common.PROPERTY_RECOMMEND_QUALITY);
                    mSpinner.setSelection(quality, true);
                    isGetStatusReply = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateVolume(int val) {
        int volume = (int) (maxVolume * (val/(Common.TOTOAL_VOLUME_DURATION * 1.000)));
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
        mVolumeSeekBar.setProgress(volume);
    }

    private void updatePlayList(String msg) {
        operationType = TYPE_SONG_LIST;
        if (UltimateSongPlayer.getInstance().isPlaying()) {
            UltimateSongPlayer.getInstance().pause();
        }
        resetState();
        try {
            JSONObject obj = new JSONObject(msg);
            String[] items = obj.getString(Common.PLAY_TYPE).split("_");
            if (items != null && items.length == 2) {
                songListType = items[0];
                JSONObject playParams = obj.getJSONObject(Common.PLAY_PARAMS);

                if (Common.PLAY_TYPE_AWESOME.equals(songListType)) { // 新歌或日推
                    if (Common.PLAY_TYPE_NEWSONG.equals(items[1])) { // 新歌
                        int _topId = playParams.getInt(Common.PLAY_TOP_ID);
                        currentPlayType = Common.PLAY_TYPE_NEWSONG;
                        currentTopId = _topId;
                        songListId = "";
                        getSongList(Common.PLAY_TYPE_NEWSONG, "", _topId);
                    } else if (Common.PLAY_TYPE_EVERYDAY.equals(items[1])) { // 日推
                        currentPlayType = Common.PLAY_TYPE_EVERYDAY;
                        songListId = "";
                        getSongList(Common.PLAY_TYPE_EVERYDAY, "", 0);
                    }
                } else { // 热门歌单或专辑
                    if (Common.PLAY_TYPE_ALBUM.equals(songListType)) {
                        currentPlayType = Common.PLAY_TYPE_ALBUM;
                        songListId = playParams.getString(Common.ALBUM_ID);
                    } else if (Common.PLAY_TYPE_PLAYLIST.equals(songListType)) {
                        currentPlayType = Common.PLAY_TYPE_PLAYLIST;
                        songListId = playParams.getString(Common.PLAYLIST_ID);
                    }
                    getSongListById(songListId, songListType);
                }
            } else {
                TXLog.e(TAG, "歌单错误");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    private boolean updateQuality(int quality) {
        //当前歌曲详情
        SongInfo songInfo = UltimateSongPlayer.getInstance().getSongInfo();
        if (songInfo == null) {
            ToastUtil.showS("无法切换音质");
            return false;
        }
        if (songInfo.isTryListen()) {
            ToastUtil.showS("试听中，无法切换音质");
            return false;
        }
        if (songInfo.getSupportQualities().contains(qualities[quality])) {
            UltimateSongPlayer.getInstance().changeQuality(qualities[quality]);
            mSpinner.setSelection(quality, true);
            ToastUtil.showS(qualityStrArray[quality]);
            return true;
        } else {
            ToastUtil.showS(String.format("当前歌曲不支持%s音质", qualityStrArray[quality]));
            return false;
        }
    }

    private void reportProperty(String key, int value) {
        reportProperty(key, value, -1);
    }

    private void reportProperty(String key, int value, int seq) {
        JSONObject params = new JSONObject();
        try {
            params.put(key, value);
            if (seq >= 0 ) {
                params.put(Common.PROPERTY_CONTROL_SEQ, seq);
            }
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

    private void reportProperty(String key, String value) {
        reportProperty(key, value, -1);
    }

    private void reportProperty(String key, String value, int seq) {
        JSONObject params = new JSONObject();
        try {
            params.put(key, value);
            if (seq >= 0 ) {
                params.put(Common.PROPERTY_CONTROL_SEQ, seq);
            }
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
        if (dialog == null) {
            dialog = new Dialog(context, R.style.iOSDialog);
            dialog.setContentView(R.layout.qrcode_dialog);
            ImageView imageView = dialog.findViewById(R.id.iv_qrcode);
            imageView.setImageBitmap(bitmap);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener((dialog, keyCode, event) -> true);
            dialog.show();
        } else {
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }
    }

    private void dismissDialog(){
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //彻底不需要使用歌曲播放了，释放资源
        if (mDataTemplateSample != null) mDataTemplateSample.disconnect();
        if (dialog != null) dialog = null;
        UltimateSongPlayer.getInstance().removeSongPlayStateListener(mSongPlayStateListener);
        UltimateSongPlayer.getInstance().clearPlayQueue();
        UltimateSongPlayer.getInstance().release();
    }
}