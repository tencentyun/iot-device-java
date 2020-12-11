package com.tencent.iot.explorer.device.trtc.data_template.ui.audiocall;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.trtc.R;
import com.tencent.iot.explorer.device.trtc.data_template.model.IntentParams;
import com.tencent.iot.explorer.device.trtc.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCallingDelegate;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCallingParamsCallback;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCUIManager;
import com.tencent.iot.explorer.device.trtc.data_template.model.UserInfo;
import com.tencent.iot.explorer.device.trtc.data_template.model.impl.TRTCCallingImpl;
import com.tencent.iot.explorer.device.trtc.data_template.ui.audiocall.audiolayout.TRTCAudioLayout;
import com.tencent.iot.explorer.device.trtc.data_template.ui.audiocall.audiolayout.TRTCAudioLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.blankj.utilcode.util.CollectionUtils;
//import com.blankj.utilcode.util.ToastUtils;

/**
 * 用于展示语音通话的主界面，通话的接听和拒绝就是在这个界面中完成的。
 */
public class TRTCAudioCallActivity extends AppCompatActivity {
    private static final String TAG = TRTCAudioCallActivity.class.getName();

    public static final String PARAM_TYPE                = "type";
    public static final String PARAM_USER                = "user_model";
    public static final String PARAM_SELF_INFO           = "self_info";
    public static final String PARAM_BEINGCALL_USER      = "beingcall_user_model";
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

    private Runnable mTimeRunnable;
    private int           mTimeCount;
    private Handler mTimeHandler;
    private HandlerThread mTimeHandlerThread;

    private UserInfo              mSelfModel;
    private List<UserInfo> mCallUserInfoList = new ArrayList<>(); // 呼叫方
    private Map<String, UserInfo> mCallUserModelMap = new HashMap<>();
    private UserInfo              mSponsorUserInfo;                      // 被叫方
    private List<UserInfo> mOtherInvitingUserInfoList;
    private int                   mCallType;
    private TRTCCallingImpl mTRTCCalling;
    private boolean               isHandsFree       = true;
    private boolean               isMuteMic         = false;

    /**
     * 拨号的回调
     */
    private TRTCCallingDelegate mTRTCAudioCallListener = new TRTCCallingDelegate() {
        @Override
        public void onError(int code, String msg) {
            //发生了错误，报错并退出该页面
//            ToastUtils.showLong(getString(R.string.trtccalling_toast_call_error_msg, code, msg));
            removeCallbackAndFinish();
        }

        @Override
        public void onInvited(String sponsor, List<String> userIdList, boolean isFromGroup, int callType) {
        }

        @Override
        public void onGroupCallInviteeListUpdate(List<String> userIdList) {
        }

        @Override
        public void onUserEnter(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCallingView();
                    TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(userId);
                    if (layout != null) {
                        layout.stopLoading();
                    } else {
                        UserInfo model = new UserInfo();
                        model.setUserId(userId);
                        model.userName = userId;
//                        model.userAvatar = "";
                        mCallUserInfoList.add(model);
                        mCallUserModelMap.put(model.getUserId(), model);
                        addUserToManager(model);
                    }
                }
            });
        }

        @Override
        public void onUserLeave(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //1. 回收界面元素
                    mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                    //2. 删除用户model
                    UserInfo userInfo = mCallUserModelMap.remove(userId);
                    if (userInfo != null) {
                        mCallUserInfoList.remove(userInfo);
                    }
                    removeCallbackAndFinish();
                }
            });
        }

        @Override
        public void onReject(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallUserModelMap.containsKey(userId)) {
                        // 进入拒绝环节
                        //1. 回收界面元素
                        mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                        //2. 删除用户model
                        UserInfo userInfo = mCallUserModelMap.remove(userId);
                        if (userInfo != null) {
                            mCallUserInfoList.remove(userInfo);
//                            ToastUtils.showLong(getString(R.string.trtccalling_toast_user_reject_call, userInfo.userName));
                        }
                        removeCallbackAndFinish();
                    }
                }
            });
        }

        @Override
        public void onNoResp(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallUserModelMap.containsKey(userId)) {
                        // 进入无响应环节
                        //1. 回收界面元素
                        mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                        //2. 删除用户model
                        UserInfo userInfo = mCallUserModelMap.remove(userId);
                        if (userInfo != null) {
                            mCallUserInfoList.remove(userInfo);
//                            ToastUtils.showLong(getString(R.string.trtccalling_toast_user_not_response, userInfo.userName));
                        }
                        removeCallbackAndFinish();
                    }
                }
            });
        }

        @Override
        public void onLineBusy(String userId) {
            if (mCallUserModelMap.containsKey(userId)) {
                // 进入无响应环节
                //1. 回收界面元素
                mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                //2. 删除用户model
                UserInfo userInfo = mCallUserModelMap.remove(userId);
                if (userInfo != null) {
                    mCallUserInfoList.remove(userInfo);
//                    ToastUtils.showLong(getString(R.string.trtccalling_toast_user_busy, userInfo.userName));
                }
                removeCallbackAndFinish();
            }
        }

        @Override
        public void onCallingCancel() {
            if (mSponsorUserInfo != null) {
//                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_cancel_call, mSponsorUserInfo.userName));
            }
            removeCallbackAndFinish();
        }

        @Override
        public void onCallingTimeout() {
            if (mSponsorUserInfo != null) {
//                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_timeout, mSponsorUserInfo.userName));
            }
            removeCallbackAndFinish();
        }

        @Override
        public void onCallEnd() {
            if (mSponsorUserInfo != null) {
//                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_end, mSponsorUserInfo.userName));
            }
            removeCallbackAndFinish();
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean isVideoAvailable) {
        }

        @Override
        public void onUserAudioAvailable(String userId, boolean isVideoAvailable) {

        }

        @Override
        public void onUserVoiceVolume(Map<String, Integer> volumeMap) {
            for (Map.Entry<String, Integer> entry : volumeMap.entrySet()) {
                String userId = entry.getKey();
                TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(userId);
                if (layout != null) {
//                    layout.setAudioVolume(entry.getValue());
                }
            }
        }
    };

    /**
     * 主动拨打给某个用户
     *
     * @param context
     * @param roomKey
     */
    public static void startCallSomeone(Context context, RoomKey roomKey, String beingCallUserId) {
        Intent starter = new Intent(context, TRTCAudioCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_CALL);
        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(roomKey));
        UserInfo beingCallUserInfo = new UserInfo();
        beingCallUserInfo.setUserId(beingCallUserId);
        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
        context.startActivity(starter);
    }

    /**
     * 作为用户被叫
     */
    public static void startBeingCall(Context context, RoomKey roomKey, String beingCallUserId) {
        Intent starter = new Intent(context, TRTCAudioCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        UserInfo beingCallUserInfo = new UserInfo();
        beingCallUserInfo.setUserId(beingCallUserId);
        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(roomKey));
        starter.putExtra(PARAM_OTHER_INVITING_USER, new IntentParams(new ArrayList<>()));
        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

//    public static void startBeingCall(Context context, RoomKey roomKey, String beingCallUserId) {
//        Intent starter = new Intent(context, TRTCVideoCallActivity.class);
//        starter.putExtra(PARAM_TYPE, TYPE_BEING_CALLED);
//        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(roomKey));
//        UserInfo beingCallUserInfo = new UserInfo();
//        beingCallUserInfo.setUserId(beingCallUserId);
//        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
//        starter.putExtra(PARAM_OTHER_INVITING_USER, new IntentParams(new ArrayList<>()));
//        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(starter);
//    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.trtccalling_audiocall_activity_call_main);

        TRTCUIManager.getInstance().addCallingParamsCallback(new TRTCCallingParamsCallback() {
            @Override
            public void joinRoom(Integer callingType, String deviceId, RoomKey roomKey) {
                //1.分配自己的画面
                mLayoutManagerTRTC.setMySelfUserId(mSelfModel.getUserId());
                addUserToManager(mSelfModel);
                //2.接听电话
//                mTRTCCalling.accept();
                mTRTCCalling.enterTRTCRoom(roomKey);
                showCallingView();
            }
        });

        initView();
        initData();
        initListener();
    }

    @Override
    public void onBackPressed() {
        // 退出这个界面的时候，需要挂断
//        mTRTCCalling.hangup();
        mTRTCCalling.exitRoom();
        super.onBackPressed();
    }

    private void removeCallbackAndFinish() {
        TRTCUIManager.getInstance().didExitRoom(TRTCCalling.TYPE_AUDIO_CALL, "");
        finish();
        TRTCUIManager.getInstance().isCalling = false;
        TRTCUIManager.getInstance().removeCallingParamsCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mTRTCCalling.removeDelegate(mTRTCAudioCallListener);
        stopTimeCount();
        mTimeHandlerThread.quit();
    }

    private void initListener() {
        mLayoutMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMuteMic = !isMuteMic;
                mTRTCCalling.setMicMute(isMuteMic);
                mImageMute.setActivated(isMuteMic);
//                ToastUtils.showLong(isMuteMic ? R.string.trtccalling_toast_enable_mute : R.string.trtccalling_toast_disable_mute);
            }
        });
        mLayoutHandsFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHandsFree = !isHandsFree;
                mTRTCCalling.setHandsFree(isHandsFree);
                mImageHandsFree.setActivated(isHandsFree);
//                ToastUtils.showLong(isHandsFree ? R.string.trtccalling_toast_use_speaker : R.string.trtccalling_toast_use_handset);
            }
        });
        mImageMute.setActivated(isMuteMic);
        mImageHandsFree.setActivated(isHandsFree);
    }

    private void initData() {
        // 初始化成员变量
        mTRTCCalling = new TRTCCallingImpl(this);//TRTCCalling.sharedInstance(this);
//        mTRTCCalling.addDelegate(mTRTCAudioCallListener);
        mTRTCCalling.setTRTCCallingDelegate(mTRTCAudioCallListener);
        mTimeHandlerThread = new HandlerThread("time-count-thread");
        mTimeHandlerThread.start();
        mTimeHandler = new Handler(mTimeHandlerThread.getLooper());
        // 初始化从外界获取的数据
        Intent intent = getIntent();
        //自己的资料
//        mSelfModel = (UserInfo) intent.getSerializableExtra(PARAM_SELF_INFO);
        String roomKeyStr = intent.getStringExtra(PARAM_SELF_INFO);
        if (TextUtils.isEmpty(roomKeyStr)) return;
        RoomKey roomKey = JSON.parseObject(roomKeyStr, RoomKey.class);
        mSelfModel = new UserInfo();
        mSelfModel.setUserId(roomKey.getUserId());

        mCallType = intent.getIntExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        if (mCallType == TYPE_BEING_CALLED) {
            // 作为被叫
            mSponsorUserInfo = (UserInfo) intent.getSerializableExtra(PARAM_BEINGCALL_USER);
            IntentParams params = (IntentParams) intent.getSerializableExtra(PARAM_OTHER_INVITING_USER);
            if (params != null) {
                mOtherInvitingUserInfoList = params.mUserInfos;
            }
            showWaitingResponseView();
            mStatusView.setText(mSponsorUserInfo.getUserId()+"邀请您进行语音通话");
        } else {
            // 主叫方
            if (roomKey != null) {
//                mCallUserInfoList = params.mUserInfos;
//                for (UserInfo userInfo : mCallUserInfoList) {
//                    mCallUserModelMap.put(userInfo.getUserId(), userInfo);
//                }
//                startInviting(roomKey);
                showInvitingView();
            }
        }
    }

    private void startInviting(RoomKey roomKey) {
//        for (UserInfo userInfo : mCallUserInfoList) {
//            mTRTCCalling.call(userInfo.userId, TRTCCalling.TYPE_AUDIO_CALL);
//        }
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
    }


    /**
     * 等待接听界面
     */
    public void showWaitingResponseView() {
        //1. 展示对方的画面
        TRTCAudioLayout layout = mLayoutManagerTRTC.allocAudioCallLayout(mSponsorUserInfo.getUserId());
        layout.setUserId(mSponsorUserInfo.getUserId());
//        Picasso.get().load(mSponsorUserInfo.userAvatar).into(layout.getImageView());
        //2. 展示电话对应界面
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutDialing.setVisibility(View.VISIBLE);
        mLayoutHandsFree.setVisibility(View.GONE);
        mLayoutMute.setVisibility(View.GONE);
        //3. 设置对应的listener
        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mTRTCCalling.reject();
                mTRTCCalling.exitRoom();
                removeCallbackAndFinish();
            }
        });
        mLayoutDialing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TRTCUIManager.getInstance().didAcceptJoinRoom(TRTCCalling.TYPE_AUDIO_CALL, mSponsorUserInfo.getUserId());
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
//                mTRTCCalling.hangup();
                mTRTCCalling.exitRoom();
                removeCallbackAndFinish();
            }
        });
        mLayoutDialing.setVisibility(View.GONE);
        mLayoutHandsFree.setVisibility(View.GONE);
        mLayoutMute.setVisibility(View.GONE);
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

        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mTRTCCalling.hangup();
                mTRTCCalling.exitRoom();
                removeCallbackAndFinish();
            }
        });
        showTimeCount();
        hideOtherInvitingUserView();
        mStatusView.setText(R.string.trtccalling_dialed_is_busy);
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
//        if (CollectionUtils.isEmpty(mOtherInvitingUserInfoList)) {
//            return;
//        }
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
//            Picasso.get().load(userInfo.userAvatar).into(imageView);
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
//        Picasso.get().load(userInfo.userAvatar).into(layout.getImageView());
        return layout;
    }
}
