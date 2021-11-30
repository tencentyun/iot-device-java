package com.tencent.iot.explorer.device.rtc.ui.audiocall;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.rtc.data_template.model.IntentParams;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.rtc.impl.TRTCCallingDelegate;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCCallingParamsCallback;
import com.tencent.iot.explorer.device.rtc.data_template.model.TRTCUIManager;
import com.tencent.iot.explorer.device.rtc.data_template.model.UserInfo;
import com.tencent.iot.explorer.device.rtc.impl.TRTCCallingImpl;
import com.tencent.iot.explorer.device.rtc.ui.audiocall.audiolayout.TRTCAudioLayout;
import com.tencent.iot.explorer.device.rtc.ui.audiocall.audiolayout.TRTCAudioLayoutManager;
import com.tencent.iot.explorer.device.rtc.utils.NetWorkStateReceiver;
import com.tencent.trtc.TRTCCloudDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 用于展示语音通话的主界面，通话的接听和拒绝就是在这个界面中完成的。
 */
public class TRTCAudioCallActivity extends AppCompatActivity implements NetWorkStateReceiver.NetworkStateReceiverListener {

    private static final String TAG = TRTCAudioCallActivity.class.getName();

    public static final String PARAM_TYPE                = "type";
    public static final String PARAM_USER                = "user_model";
    public static final String PARAM_SELF_INFO           = "self_info";
    public static final String PARAM_BEINGCALL_USER      = "beingcall_user_model";
    public static final String PARAM_AGENT               = "agent";
    public static final String PARAM_OTHER_INVITING_USER = "other_inviting_user_model";
    public static final  int    TYPE_BEING_CALLED         = 1;
    public static final  int    TYPE_CALL                 = 2;
    private static final int    MAX_SHOW_INVITING_USER    = 2;

    private ImageView mImageMute;
    private ImageView mImageHangup;
    private LinearLayout mLayoutMute;
    private LinearLayout mLayoutHangup;
    private ImageView mImageHandsFree;
    private LinearLayout mLayoutHandsFree;
    private ImageView mImageDialing;
    private LinearLayout mLayoutDialing;
    private TRTCAudioLayoutManager mLayoutManagerTRTC;
    private Group mGroupInviting;
    private LinearLayout mLayoutImgContainer;
    private TextView mTextTime;
    private TextView mStatusView;
    private TextView mSponsorUserNameTv;
    private TextView mSponsorAudioTagTv;

    private Runnable mTimeRunnable;
    private int           mTimeCount;
    private Handler mTimeHandler;
    private HandlerThread mTimeHandlerThread;

    private UserInfo mSelfModel;
    private List<UserInfo> mCallUserInfoList = new ArrayList<>(); // 呼叫方
    private Map<String, UserInfo> mCallUserModelMap = new HashMap<>();
    private UserInfo              mSponsorUserInfo;                      // 被叫方
    private String                mUserId;                               // 被叫Id
    private List<UserInfo> mOtherInvitingUserInfoList;
    private int                   mCallType;
    private TRTCCallingImpl mTRTCCalling;
    private boolean               isHandsFree       = true;
    private boolean               isMuteMic         = false;
    private volatile boolean      mIsEnterRoom      = false;
    private volatile boolean      mIsExitRoom       = false;

    private TimerTask otherEnterRoomTask = null;
    private TimerTask enterRoomTask = null;

    private NetWorkStateReceiver netWorkStateReceiver;

    /**
     * 拨号的回调
     */
    private TRTCCallingDelegate mTRTCAudioCallListener = new TRTCCallingDelegate() {
        @Override
        public void onError(int code, String msg) {
            //发生了错误，报错并退出该页面
            removeCallbackAndFinish();
        }

        @Override
        public void onInvited(String sponsor, List<String> trtc_uidList, boolean isFromGroup, int callType) {
        }

        @Override
        public void onGroupCallInviteeListUpdate(List<String> trtc_uidList) {
        }

        @Override
        public void onUserEnter(final String trtc_uid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!mIsExitRoom) {
                        TRTCUIManager.getInstance().startOnThePhone(TRTCCalling.TYPE_AUDIO_CALL, mUserId, mSponsorUserInfo.getAgent());
                    }
                    removeOtherIsEnterRoom15secondsTask();
                    TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(mUserId);
                    if (layout != null) {
                        layout.stopLoading();
                    } else {
                        UserInfo model = new UserInfo();
                        model.setUserId(mUserId);
                        model.userName = mUserId;
                        mCallUserInfoList.add(model);
                        mCallUserModelMap.put(model.getUserId(), model);
                        addUserToManager(model);
                    }
                    mIsEnterRoom = true;
                    showTimeCount();
                    mStatusView.setText(R.string.trtccalling_dialed_is_busy);
                }
            });
        }

        @Override
        public void onUserLeave(final String trtc_uid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //1. 回收界面元素
                    mLayoutManagerTRTC.recyclerAudioCallLayout(mUserId);
                    //2. 删除用户model
                    UserInfo userInfo = mCallUserModelMap.remove(mUserId);
                    if (userInfo != null) {
                        mCallUserInfoList.remove(userInfo);
                    }
                    if (mIsExitRoom) {
                        Toast.makeText(getApplicationContext(), "对方已挂断", Toast.LENGTH_LONG).show();
                    }
                    removeCallbackAndFinish();
                }
            });
        }

        @Override
        public void onReject(final String trtc_uid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallUserModelMap.containsKey(mUserId)) {
                        // 进入拒绝环节
                        //1. 回收界面元素
                        mLayoutManagerTRTC.recyclerAudioCallLayout(mUserId);
                        //2. 删除用户model
                        UserInfo userInfo = mCallUserModelMap.remove(mUserId);
                        if (userInfo != null) {
                            mCallUserInfoList.remove(userInfo);
                        }
                        removeCallbackAndFinish();
                    }
                }
            });
        }

        @Override
        public void onNoResp(final String trtc_uid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallUserModelMap.containsKey(mUserId)) {
                        // 进入无响应环节
                        //1. 回收界面元素
                        mLayoutManagerTRTC.recyclerAudioCallLayout(mUserId);
                        //2. 删除用户model
                        UserInfo userInfo = mCallUserModelMap.remove(mUserId);
                        if (userInfo != null) {
                            mCallUserInfoList.remove(userInfo);
                        }
                        removeCallbackAndFinish();
                    }
                }
            });
        }

        @Override
        public void onLineBusy(String trtc_uid) {
            if (mCallUserModelMap.containsKey(mUserId)) {
                // 进入无响应环节
                //1. 回收界面元素
                mLayoutManagerTRTC.recyclerAudioCallLayout(mUserId);
                //2. 删除用户model
                UserInfo userInfo = mCallUserModelMap.remove(mUserId);
                if (userInfo != null) {
                    mCallUserInfoList.remove(userInfo);
                }
                removeCallbackAndFinish();
            }
        }

        @Override
        public void onCallingCancel() {
            removeCallbackAndFinish();
        }

        @Override
        public void onCallingTimeout() {
            removeCallbackAndFinish();
        }

        @Override
        public void onCallEnd() {
            removeCallbackAndFinish();
        }

        @Override
        public void onUserVideoAvailable(String trtc_uid, boolean isVideoAvailable) {
        }

        @Override
        public void onUserAudioAvailable(String trtc_uid, boolean isVideoAvailable) {

        }

        @Override
        public void onUserVoiceVolume(Map<String, Integer> volumeMap) {

        }

        @Override
        public void onNetworkQuality(TRTCCloudDef.TRTCQuality trtcQuality, ArrayList<TRTCCloudDef.TRTCQuality> arrayList) {

        }
    };

    /**
     * 主动拨打给某个用户
     *
     * @param context
     * @param agent
     */
    public static void startCallSomeone(Context context, String agent, String beingCallUserId) {
        Intent starter = new Intent(context, TRTCAudioCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_CALL);
        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(new RoomKey()));
        UserInfo beingCallUserInfo = new UserInfo();
        beingCallUserInfo.setUserId(beingCallUserId);
        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
        starter.putExtra(PARAM_AGENT, agent);
        context.startActivity(starter);
    }

    /**
     * 作为用户被叫
     */
    public static void startBeingCall(Context context, RoomKey roomKey, String beingCallUserId, String agent) {
        Intent starter = new Intent(context, TRTCAudioCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        UserInfo beingCallUserInfo = new UserInfo();
        beingCallUserInfo.setUserId(beingCallUserId);
        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
        starter.putExtra(PARAM_AGENT, agent);
        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(roomKey));
        starter.putExtra(PARAM_OTHER_INVITING_USER, new IntentParams(new ArrayList<UserInfo>()));
        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    private void checkoutOtherIsEnterRoom15seconds() {
        if (otherEnterRoomTask == null) {
            otherEnterRoomTask = new TimerTask(){
                public void run(){
                    //自己已进入房间15秒内对方没有进入房间 则显示对方已挂断，并主动退出，进入了就取消timertask
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "对方已挂断", Toast.LENGTH_LONG).show();
                            removeCallbackAndFinish();
                        }
                    });
                }
            };
            Timer timer = new Timer();
            timer.schedule(otherEnterRoomTask, 15000);
        }
    }

    private void removeOtherIsEnterRoom15secondsTask() {
        if (otherEnterRoomTask != null) {
            otherEnterRoomTask.cancel();
            otherEnterRoomTask = null;
        }
    }

    private void checkoutIsEnterRoom60seconds(String message) {
        if (enterRoomTask == null) {
            enterRoomTask = new TimerTask(){
                public void run(){
                    //呼叫了60秒，对方未接听 显示对方无人接听，并退出，进入了就取消timertask
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            removeCallbackAndFinish();
                        }
                    });
                }
            };
            Timer timer = new Timer();
            timer.schedule(enterRoomTask, 60000);
        }
    }

    private void removeIsEnterRoom60secondsTask() {
        if (enterRoomTask != null) {
            enterRoomTask.cancel();
            enterRoomTask = null;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.trtccalling_audiocall_activity_call_main);
        startNetworkBroadcastReceiver(this);

        TRTCUIManager.getInstance().addCallingParamsCallback(new TRTCCallingParamsCallback() {
            @Override
            public void joinRoom(Integer callingType, String deviceId, RoomKey roomKey) {
                //1.分配自己的画面
                mLayoutManagerTRTC.setMySelfUserId(mSelfModel.getUserId());
                //2.接听电话
                TRTCUIManager.getInstance().callingUserId = roomKey.getUserId();
                mTRTCCalling.enterTRTCRoom(roomKey);
                if (roomKey != null) {
                    removeIsEnterRoom60secondsTask();
                }
                checkoutOtherIsEnterRoom15seconds();
            }

            @Override
            public void refuseEnterRoom() {
                removeCallbackAndFinish();
            }
        });


        initView();
        initData();
        initListener();
        checkoutIsEnterRoom60seconds("对方无人接听");
    }

    @Override
    public void onBackPressed() {
        // 退出这个界面的时候，需要挂断
        mTRTCCalling.exitRoom();
        super.onBackPressed();
    }

    private void removeCallbackAndFinish() {
        mTRTCCalling.exitRoom();
        mIsExitRoom = true;
        TRTCUIManager.getInstance().didExitRoom(TRTCCalling.TYPE_AUDIO_CALL, mUserId);
        finish();
        TRTCUIManager.getInstance().isCalling = false;
        TRTCUIManager.getInstance().callMobile = false;
        TRTCUIManager.getInstance().callingUserId = "";
        TRTCUIManager.getInstance().removeCallingParamsCallback();
        removeIsEnterRoom60secondsTask();
        removeOtherIsEnterRoom15secondsTask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimeCount();
        mTimeHandlerThread.quit();
    }

    //在onResume()方法注册
    @Override
    protected void onResume() {
        registerNetworkBroadcastReceiver(this);
        TXLog.e(TAG, "注册netWorkStateReceiver");
        super.onResume();
    }

    //onPause()方法注销
    @Override
    protected void onPause() {
        unregisterNetworkBroadcastReceiver(this);
        TXLog.e(TAG, "注销netWorkStateReceiver");
        super.onPause();
    }

    public void startNetworkBroadcastReceiver(Context currentContext) {
        netWorkStateReceiver = new NetWorkStateReceiver();
        netWorkStateReceiver.addListener((NetWorkStateReceiver.NetworkStateReceiverListener) currentContext);
        registerNetworkBroadcastReceiver(currentContext);
    }

    /**
     * Register the NetworkStateReceiver with your activity
     *
     * @param currentContext
     */
    public void registerNetworkBroadcastReceiver(Context currentContext) {
        currentContext.registerReceiver(netWorkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Unregister the NetworkStateReceiver with your activity
     *
     * @param currentContext
     */
    public void unregisterNetworkBroadcastReceiver(Context currentContext) {
        currentContext.unregisterReceiver(netWorkStateReceiver);
    }

    @Override
    public void networkAvailable() {
        TXLog.e(TAG, "networkAvailable");
        if (mIsEnterRoom) {
            removeIsEnterRoom60secondsTask();
        }
    }

    @Override
    public void networkUnavailable() {
        TXLog.e(TAG, "networkUnavailable");
        checkoutIsEnterRoom60seconds("网络异常，请稍后重试");
    }

    private void initListener() {
        mLayoutMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMuteMic = !isMuteMic;
                mTRTCCalling.setMicMute(isMuteMic);
                mImageMute.setActivated(isMuteMic);
            }
        });
        mLayoutHandsFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHandsFree = !isHandsFree;
                mTRTCCalling.setHandsFree(isHandsFree);
                mImageHandsFree.setActivated(isHandsFree);
            }
        });
        mImageMute.setActivated(isMuteMic);
        mImageHandsFree.setActivated(isHandsFree);
    }

    private void initData() {
        // 初始化成员变量
        mTRTCCalling = new TRTCCallingImpl(this);
        mTRTCCalling.setTRTCCallingDelegate(mTRTCAudioCallListener);
        mTimeHandlerThread = new HandlerThread("tencent-time-count-thread");
        mTimeHandlerThread.start();
        mTimeHandler = new Handler(mTimeHandlerThread.getLooper());
        // 初始化从外界获取的数据
        Intent intent = getIntent();
        //自己的资料
        String roomKeyStr = intent.getStringExtra(PARAM_SELF_INFO);
        if (TextUtils.isEmpty(roomKeyStr)) return;
        RoomKey roomKey = JSON.parseObject(roomKeyStr, RoomKey.class);
        mSelfModel = new UserInfo();
        mSelfModel.setUserId(roomKey.getUserId());

        mCallType = intent.getIntExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        mSponsorUserInfo = (UserInfo) intent.getSerializableExtra(PARAM_BEINGCALL_USER);
        mUserId = mSponsorUserInfo.getUserId();
        mSponsorUserInfo.agent = intent.getStringExtra(PARAM_AGENT);
        if (mCallType == TYPE_BEING_CALLED) {
            // 作为被叫
            IntentParams params = (IntentParams) intent.getSerializableExtra(PARAM_OTHER_INVITING_USER);
            if (params != null) {
                mOtherInvitingUserInfoList = params.mUserInfos;
            }
            showWaitingResponseView();
        } else {
            // 主叫方
            if (roomKey != null) {
                showInvitingView();
            }
        }
    }

    private void startInviting(RoomKey roomKey) {
        mTRTCCalling.enterTRTCRoom(roomKey);
    }

    private void initView() {
        mImageMute = (ImageView) findViewById(R.id.img_mute);
        mLayoutMute = (LinearLayout) findViewById(R.id.ll_mute);
        mImageHangup = (ImageView) findViewById(R.id.img_hangup);
        mLayoutHangup = (LinearLayout) findViewById(R.id.ll_hangup);
        mImageHandsFree = (ImageView) findViewById(R.id.img_handsfree);
        mLayoutHandsFree = (LinearLayout) findViewById(R.id.ll_handsfree);
        mImageDialing = (ImageView) findViewById(R.id.img_dialing);
        mLayoutDialing = (LinearLayout) findViewById(R.id.ll_dialing);
        mLayoutManagerTRTC = (TRTCAudioLayoutManager) findViewById(R.id.trtc_layout_manager);
        mGroupInviting = (Group) findViewById(R.id.group_inviting);
        mLayoutImgContainer = (LinearLayout) findViewById(R.id.ll_img_container);
        mTextTime = (TextView) findViewById(R.id.tv_time);
        mStatusView = (TextView) findViewById(R.id.tv_status);
        mSponsorUserNameTv = (TextView) findViewById(R.id.tv_sponsor_user_name);
        mSponsorAudioTagTv = (TextView) findViewById(R.id.tv_sponsor_audio_tag);
    }


    /**
     * 等待接听界面
     */
    public void showWaitingResponseView() {
        //1. 展示对方的画面
        TRTCAudioLayout layout = mLayoutManagerTRTC.allocAudioCallLayout(mSelfModel.getUserId());
        layout.setUserId(mSelfModel.getUserId());
        mSponsorUserNameTv.setText(mSponsorUserInfo.userName);

        //2. 展示电话对应界面
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutDialing.setVisibility(View.VISIBLE);
        mLayoutHandsFree.setVisibility(View.GONE);
        mLayoutMute.setVisibility(View.GONE);
        //3. 设置对应的listener
        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCallbackAndFinish();
            }
        });
        mLayoutDialing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TRTCUIManager.getInstance().didAcceptJoinRoom(TRTCCalling.TYPE_AUDIO_CALL, mUserId, mSponsorUserInfo.getAgent());
                showCallingView();
            }
        });
        //4. 展示其他用户界面
        showOtherInvitingUserView();
    }

    /**
     * 展示邀请列表
     */
    public void showInvitingView() {
        //1. 展示自己的界面
        mLayoutManagerTRTC.setMySelfUserId(mSelfModel.getUserId());
        addUserToManager(mSelfModel);
        //2. 展示对方的画面
        for (UserInfo userInfo : mCallUserInfoList) {
            TRTCAudioLayout layout = addUserToManager(userInfo);
            layout.startLoading();
        }
        //3. 设置底部栏
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.exitRoom();
                removeCallbackAndFinish();
            }
        });
        mLayoutDialing.setVisibility(View.GONE);
        mLayoutHandsFree.setVisibility(View.VISIBLE);
        mLayoutMute.setVisibility(View.VISIBLE);
        mSponsorUserNameTv.setVisibility(View.GONE);
        mSponsorAudioTagTv.setVisibility(View.GONE);
        //4. 隐藏中间他们也在界面
        hideOtherInvitingUserView();
    }

    /**
     * 展示通话中的界面
     */
    public void showCallingView() {
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutDialing.setVisibility(View.GONE);
        mLayoutHandsFree.setVisibility(View.VISIBLE);
        mLayoutMute.setVisibility(View.VISIBLE);
        mSponsorUserNameTv.setVisibility(View.GONE);
        mSponsorAudioTagTv.setVisibility(View.GONE);

        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.exitRoom();
                removeCallbackAndFinish();
            }
        });
        hideOtherInvitingUserView();
        mStatusView.setText(R.string.trtccalling_waiting_to_hear);
    }

    private void showTimeCount() {
        if (mTimeRunnable != null) {
            return;
        }
        mTimeCount = 0;
        mTextTime.setText(getShowTime(mTimeCount));
        if (mTimeRunnable == null) {
            mTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    mTimeCount++;
                    if (mTextTime != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextTime.setText(getShowTime(mTimeCount));
                            }
                        });
                    }
                    mTimeHandler.postDelayed(mTimeRunnable, 1000);
                }
            };
        }
        mTimeHandler.postDelayed(mTimeRunnable, 1000);
    }

    private void stopTimeCount() {
        mTimeHandler.removeCallbacks(mTimeRunnable);
        mTimeRunnable = null;
    }

    private String getShowTime(int count) {
        return getString(R.string.trtccalling_called_time_format, count / 60, count % 60);
    }

    private void showOtherInvitingUserView() {
        mGroupInviting.setVisibility(View.VISIBLE);
        int squareWidth = getResources().getDimensionPixelOffset(R.dimen.trtccalling_small_image_size);
        int leftMargin  = getResources().getDimensionPixelOffset(R.dimen.trtccalling_small_image_left_margin);
        for (int index = 0; index < mOtherInvitingUserInfoList.size() && index < MAX_SHOW_INVITING_USER; index++) {
            UserInfo                  userInfo     = mOtherInvitingUserInfoList.get(index);
            ImageView imageView    = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(squareWidth, squareWidth);
            if (index != 0) {
                layoutParams.leftMargin = leftMargin;
            }
            imageView.setLayoutParams(layoutParams);
            mLayoutImgContainer.addView(imageView);
        }
    }

    private void hideOtherInvitingUserView() {
        mGroupInviting.setVisibility(View.GONE);
    }

    private TRTCAudioLayout addUserToManager(UserInfo userInfo) {
        TRTCAudioLayout layout = mLayoutManagerTRTC.allocAudioCallLayout(userInfo.getUserId());
        if (layout == null) {
            return null;
        }
        layout.setUserId(userInfo.getUserId());
        return layout;
    }
}
