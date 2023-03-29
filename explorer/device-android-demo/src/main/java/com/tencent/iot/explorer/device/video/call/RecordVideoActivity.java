package com.tencent.iot.explorer.device.video.call;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import com.alibaba.fastjson.JSON;
import com.squareup.picasso.Picasso;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.iot.explorer.device.rtc.ui.videocall.videolayout.TRTCVideoLayout;
import com.tencent.iot.explorer.device.rtc.ui.videocall.videolayout.TRTCVideoLayoutManager;
import com.tencent.iot.device.video.advanced.recorder.rtc.XP2PCallback;
import com.tencent.iot.device.video.advanced.recorder.rtc.VideoNativeInteface;
import com.tencent.iot.device.video.advanced.recorder.rtc.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordVideoActivity extends AppCompatActivity {

    public static final int TYPE_BEING_CALLED = 1;
    public static final int TYPE_CALL         = 2;

    public static final String PARAM_TYPE                = "type";
    public static final String PARAM_SELF_INFO           = "self_info";

    private ImageView mMuteImg;
    private LinearLayout mMuteLl;
    private ImageView mHangupImg;
    private LinearLayout mHangupLl;
    private ImageView mHandsfreeImg;
    private LinearLayout mHandsfreeLl;
    private ImageView mSwitchCameraImg;
    private LinearLayout mSwitchCameraLl;
    private ImageView mDialingImg;
    private LinearLayout mDialingLl;
    private TRTCVideoLayoutManager mLayoutManagerTrtc;
    private Group mInvitingGroup;
    private LinearLayout mImgContainerLl;
    private TextView mTimeTv;
    private TextView mStatusView;
    private ImageView mSponsorAvatarImg;
    private TextView mSponsorUserNameTv;
    private Group                  mSponsorGroup;
    private Runnable mTimeRunnable;
    private int                    mTimeCount;
    private Handler mTimeHandler;
    private HandlerThread mTimeHandlerThread;
    private RoomKey mRoomKey;

    /**
     * 拨号相关成员变量
     */
    private UserInfo mSelfModel;
    private List<UserInfo> mCallUserInfoList = new ArrayList<>(); // 呼叫方
    private Map<String, UserInfo> mCallUserModelMap = new HashMap<>();
    private int                   mCallType;
    private boolean               isHandsFree       = true;
    private boolean               isMuteMic         = false;
    private boolean               mIsFrontCamera    = true;
    private String TAG = RecordVideoActivity.class.getSimpleName();
    private String mRtc_uid = null;
    private long showVideoTime ;
    private long startShowVideoTime ;
    private boolean showTip = false;
    private volatile boolean endCall = false;

    /**
     * 拨号的回调
     */
    private XP2PCallback mXP2PCallback = new XP2PCallback() {
        @Override
        public void onError(int code, String msg) {
            //发生了错误，报错并退出该页面
            stopCameraAndFinish();
        }

        @Override
        public void onConnect(long result) {

        }

        @Override
        public void onRelease(int reason) {

        }

        @Override
        public void onUserEnter(final String rtc_uid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 对方已进入房间，记录下对方id
                    mRtc_uid = rtc_uid;
                }
            });
        }

        @Override
        public void onUserLeave(final String rtc_uid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //1. 回收界面元素
                    mLayoutManagerTrtc.recyclerCloudViewView(rtc_uid);
                    //2. 删除用户model
                    UserInfo userInfo = mCallUserModelMap.remove(rtc_uid);
                    if (userInfo != null) {
                        mCallUserInfoList.remove(userInfo);
                    }
                    if (!endCall) {
                        Toast.makeText(getApplicationContext(), "对方已挂断", Toast.LENGTH_LONG).show();
                    }
                    stopCameraAndFinish();
                }
            });
        }

        @Override
        public void onUserVideoAvailable(final String rtc_uid, final boolean isVideoAvailable) {
            //有用户的视频开启了
            TRTCVideoLayout layout = mLayoutManagerTrtc.findCloudViewView(rtc_uid);
            if (layout != null) {
                layout.setVideoAvailable(isVideoAvailable);
                if (isVideoAvailable) {
                    startShowVideoTime = System.currentTimeMillis();
                    VideoNativeInteface.getInstance().startRemoteView(mRtc_uid, layout.getVideoView());
                }
            } else {

            }
        }

        @Override
        public void onUserVoiceVolume(Map<String, Integer> volumeMap) {
            for (Map.Entry<String, Integer> entry : volumeMap.entrySet()) {
                String userId = entry.getKey();
                TRTCVideoLayout layout = mLayoutManagerTrtc.findCloudViewView(userId);
                if (layout != null) {
                    layout.setAudioVolumeProgress(entry.getValue());
                }
            }
        }

        @Override
        public void onRecvCustomCmdMsg(String userId, String message) {
            Log.e(TAG, String.format("onRecvCustomCmdMsg userId:%s, message:%s", userId , message));
            if (!message.isEmpty() && message.equals("1001")) { // 设备接听了，信令可双方协商自定义
                showCallingView();
                startShowVideoTime = System.currentTimeMillis();
                if (mRtc_uid != null && !mRtc_uid.isEmpty()) {
                    //1.将对方用户mtrtc_uid添加到屏幕上
                    UserInfo model = new UserInfo();
                    model.setUserId(mRtc_uid);
                    model.userName = mRtc_uid;
                    model.userAvatar = "";
                    mCallUserInfoList.add(model);
                    mCallUserModelMap.put(model.getUserId(), model);
                    TRTCVideoLayout videoLayout = addUserToManager(model);
                    if (videoLayout == null) {
                        return;
                    }
                    videoLayout.setVideoAvailable(false);
                    VideoNativeInteface.getInstance().sendStreamToServer();
                } else {
                    Toast.makeText(getApplicationContext(), "对方未进房", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onFirstVideoFrame(String userId, int width, int height) {
            if (!showTip  && startShowVideoTime > 0) {
                showVideoTime = System.currentTimeMillis() - startShowVideoTime;
                Toast.makeText(getApplicationContext(), String.format("出图: %s(ms)", showVideoTime), Toast.LENGTH_LONG).show();
                showTip = true;
            }
        }
    };

    /**
     * 主动拨打给某个用户
     *
     * @param context
     */
    public static void startCallSomeone(Context context, RoomKey roomKey) {
        Intent starter = new Intent(context, RecordVideoActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_CALL);
        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(roomKey));
        context.startActivity(starter);
    }

    /**
     * 作为用户被叫
     *
     * @param context
     * @param roomKey
     */
    public static void startBeingCall(Context context, RoomKey roomKey) {
        Intent starter = new Intent(context, RecordVideoActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        starter.putExtra(PARAM_SELF_INFO, JSON.toJSONString(roomKey));
        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.trtccalling_videocall_activity_call_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        initView();
        initData();
        initListener();
    }

    private void initView() {
        mMuteImg = (ImageView) findViewById(R.id.iv_mute);
        mMuteLl = (LinearLayout) findViewById(R.id.ll_mute);
        mHangupImg = (ImageView) findViewById(R.id.iv_hangup);
        mHangupLl = (LinearLayout) findViewById(R.id.ll_hangup);
        mHandsfreeImg = (ImageView) findViewById(R.id.iv_handsfree);
        mHandsfreeLl = (LinearLayout) findViewById(R.id.ll_handsfree);
        mSwitchCameraImg = (ImageView) findViewById(R.id.iv_switch_camera);
        mSwitchCameraLl = (LinearLayout) findViewById(R.id.ll_switch_camera);
        mDialingImg = (ImageView) findViewById(R.id.iv_dialing);
        mDialingLl = (LinearLayout) findViewById(R.id.ll_dialing);
        mLayoutManagerTrtc = (TRTCVideoLayoutManager) findViewById(R.id.trtc_layout_manager);
        mInvitingGroup = (Group) findViewById(R.id.group_inviting);
        mImgContainerLl = (LinearLayout) findViewById(R.id.ll_img_container);
        mTimeTv = (TextView) findViewById(R.id.tv_time);
        mSponsorAvatarImg = (ImageView) findViewById(R.id.iv_sponsor_avatar);
        mSponsorUserNameTv = (TextView) findViewById(R.id.tv_sponsor_user_name);
        mSponsorGroup = (Group) findViewById(R.id.group_sponsor);
        mStatusView = (TextView) findViewById(R.id.tv_status);
    }

    private void initData() {
        // 初始化从外界获取的数据
        Intent intent = getIntent();
        String roomKeyStr = intent.getStringExtra(PARAM_SELF_INFO);
        if (TextUtils.isEmpty(roomKeyStr)) return;
        RoomKey roomKey = JSON.parseObject(roomKeyStr, RoomKey.class);
        mRoomKey = roomKey;
        mSelfModel = new UserInfo();
        mSelfModel.setUserId(roomKey.getUserId());
        mCallType = intent.getIntExtra(PARAM_TYPE, TYPE_BEING_CALLED);

        // 初始化成员变量
        VideoNativeInteface.getInstance().initWithDevice(this, roomKey);
        VideoNativeInteface.getInstance().setCallback(mXP2PCallback);
        mTimeHandlerThread = new HandlerThread("tencent-time-count-thread");
        mTimeHandlerThread.start();
        mTimeHandler = new Handler(mTimeHandlerThread.getLooper());
        //calltype主叫和被叫处理
        if (mCallType == TYPE_BEING_CALLED) {
            // 被叫 展示等待对方回应的视图
            showWaitingResponseView();
        } else {
            // 主叫 展示邀请对方的视图
            showInvitingView();
        }
    }

    private void initListener() {
        mMuteLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMuteMic = !isMuteMic;
                VideoNativeInteface.getInstance().setMicMute(isMuteMic);
                mMuteImg.setActivated(isMuteMic);
            }
        });
        mHandsfreeLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHandsFree = !isHandsFree;
                VideoNativeInteface.getInstance().setHandsFree(isHandsFree);
                mHandsfreeImg.setActivated(isHandsFree);
            }
        });
        mSwitchCameraLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsFrontCamera = !mIsFrontCamera;
                VideoNativeInteface.getInstance().switchCamera(mIsFrontCamera);
                mSwitchCameraImg.setActivated(mIsFrontCamera);
            }
        });
        mMuteImg.setActivated(isMuteMic);
        mHandsfreeImg.setActivated(isHandsFree);
        mSwitchCameraImg.setActivated(mIsFrontCamera);
    }

    @Override
    public void onBackPressed() {
        stopCameraAndFinish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimeCount();
        mTimeHandlerThread.quit();
    }

    private void stopCameraAndFinish() {
        VideoNativeInteface.getInstance().release();
        finish();
    }

    /**
     * 等待接听界面
     */
    public void showWaitingResponseView() {
        //1. 展示自己的画面
        mLayoutManagerTrtc.setMySelfUserId(mSelfModel.getUserId());
        TRTCVideoLayout videoLayout = addUserToManager(mSelfModel);
        if (videoLayout == null) {
            return;
        }
        videoLayout.setVideoAvailable(true);
        VideoNativeInteface.getInstance().openCamera(mIsFrontCamera, videoLayout.getVideoView());

        //2. 展示对方的头像和蒙层
        mSponsorGroup.setVisibility(View.VISIBLE);
        mSponsorUserNameTv.setText("123");

        //3. 展示电话对应界面
        mHangupLl.setVisibility(View.VISIBLE);
        mDialingLl.setVisibility(View.VISIBLE);
        mSwitchCameraLl.setVisibility(View.GONE);
        mHandsfreeLl.setVisibility(View.GONE);
        mMuteLl.setVisibility(View.GONE);
        //3. 设置对应的listener
        mHangupLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCameraAndFinish();
            }
        });
        mDialingLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCallingView();

                VideoNativeInteface.getInstance().sendMsgToPeer("1001");
                if (mRtc_uid != null && !mRtc_uid.isEmpty()) {
                    //1.将对方用户mtrtc_uid添加到屏幕上
                    UserInfo model = new UserInfo();
                    model.setUserId(mRtc_uid);
                    model.userName = mRtc_uid;
                    model.userAvatar = "";
                    mCallUserInfoList.add(model);
                    mCallUserModelMap.put(model.getUserId(), model);
                    TRTCVideoLayout videoLayout = addUserToManager(model);
                    if (videoLayout == null) {
                        return;
                    }
                    videoLayout.setVideoAvailable(false);
                    VideoNativeInteface.getInstance().sendStreamToServer();
                } else {
                    Toast.makeText(getApplicationContext(), "对方未进房", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * 展示邀请列表
     */
    public void showInvitingView() {
        //1. 展示自己的界面
        mLayoutManagerTrtc.setMySelfUserId(mSelfModel.getUserId());
        TRTCVideoLayout videoLayout = addUserToManager(mSelfModel);
        if (videoLayout == null) {
            return;
        }
        videoLayout.setVideoAvailable(true);
        VideoNativeInteface.getInstance().openCamera(mIsFrontCamera, videoLayout.getVideoView());
        //2. 设置底部栏
        mHangupLl.setVisibility(View.VISIBLE);
        mHangupLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall = true;
                stopCameraAndFinish();
            }
        });
        mDialingLl.setVisibility(View.GONE);
        mSwitchCameraLl.setVisibility(View.VISIBLE);
        mHandsfreeLl.setVisibility(View.VISIBLE);
        mMuteLl.setVisibility(View.VISIBLE);
        //3. 隐藏中间他们也在界面
        hideOtherInvitingUserView();
        //4. sponsor画面也隐藏
        mSponsorGroup.setVisibility(View.GONE);
    }

    /**
     * 展示通话中的界面
     */
    public void showCallingView() {
        //1. 蒙版消失
        mSponsorGroup.setVisibility(View.GONE);
        //2. 底部状态栏
        mHangupLl.setVisibility(View.VISIBLE);
        mDialingLl.setVisibility(View.GONE);
        mSwitchCameraLl.setVisibility(View.VISIBLE);
        mHandsfreeLl.setVisibility(View.VISIBLE);
        mMuteLl.setVisibility(View.VISIBLE);

        mHangupLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall = true;
                stopCameraAndFinish();
            }
        });
        hideOtherInvitingUserView();
        showTimeCount();
        mStatusView.setText(R.string.trtccalling_dialed_is_busy);
    }

    private void showTimeCount() {
        if (mTimeRunnable != null) {
            return;
        }
        mTimeCount = 0;
        mTimeTv.setText(getShowTime(mTimeCount));
        if (mTimeRunnable == null) {
            mTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    mTimeCount++;
                    if (mTimeTv != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTimeTv.setText(getShowTime(mTimeCount));
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

    private void hideOtherInvitingUserView() {
        mInvitingGroup.setVisibility(View.GONE);
    }

    private TRTCVideoLayout addUserToManager(UserInfo userInfo) {
        TRTCVideoLayout layout = mLayoutManagerTrtc.allocCloudVideoView(userInfo.getUserId());
        if (layout == null) {
            return null;
        }
        layout.getUserNameTv().setText(userInfo.userName);
        if (!TextUtils.isEmpty(userInfo.userAvatar)) {
            Picasso.get().load(userInfo.userAvatar).into(layout.getHeadImg());
        }
        return layout;
    }

}