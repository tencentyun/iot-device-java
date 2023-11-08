package com.tencent.iot.explorer.device.broadcast.impl;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;


import com.tencent.iot.explorer.device.broadcast.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.rtc.impl.TRTCCallingDelegate;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 视频/语音通话的具体实现
 */
public class BroadcastCallingImpl {
    private static final String TAG = BroadcastCallingImpl.class.getSimpleName();
    /**
     * 超时时间，单位秒
     */
    public static final int TIME_OUT_COUNT = 30;
    private final Context mContext;

    /**
     * 底层SDK调用实例
     */
    private TRTCCloud mTRTCCloud;
    private boolean mIsUseFrontCamera;
    private BroadcastCallingDelegate mTRTCCallingDelegate;

    public void setTRTCCallingDelegate(BroadcastCallingDelegate mTRTCCallingDelegate) {
        this.mTRTCCallingDelegate = mTRTCCallingDelegate;
    }

    public TRTCCloudListener getmTRTCCloudListener() {
        return mTRTCCloudListener;
    }

    /**
     * TRTC的监听器
     */
    private TRTCCloudListener mTRTCCloudListener = new TRTCCloudListener() {
        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.e(TAG, "errorCode = " + errCode + ", errMsg: " + errMsg);
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onError(errCode, errMsg);
            }
        }

        @Override
        public void onEnterRoom(long result) {

        }

        @Override
        public void onExitRoom(int reason) {

        }

        @Override
        public void onRemoteUserEnterRoom(String userId) {
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserEnter(userId);
            }
        }

        @Override
        public void onRemoteUserLeaveRoom(String userId, int reason) {
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserLeave(userId);
            }
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserVideoAvailable(userId, available);
            }
        }

        @Override
        public void onUserAudioAvailable(String userId, boolean available) {
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserAudioAvailable(userId, available);
            }
        }

        @Override
        public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
            Map<String, Integer> volumeMaps = new HashMap<>();
            for (TRTCCloudDef.TRTCVolumeInfo info : userVolumes) {
                String userId = "";
                if (info.userId == null) {
                    userId = "";
                } else {
                    userId = info.userId;
                }
                volumeMaps.put(userId, info.volume);
            }
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserVoiceVolume(volumeMaps);
            }
        }

        @Override
        public void onNetworkQuality(TRTCCloudDef.TRTCQuality trtcQuality, ArrayList<TRTCCloudDef.TRTCQuality> arrayList) {
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onNetworkQuality(trtcQuality, arrayList);
            }
        }
    };


    public BroadcastCallingImpl(Context context) {
        mContext = context;
        mTRTCCloud = TRTCCloud.sharedInstance(context);
        mTRTCCloud.setListener(mTRTCCloudListener);
    }

    public void destroy() {
        //必要的清除逻辑
        mTRTCCloud.stopLocalPreview();
        mTRTCCloud.stopLocalAudio();
        mTRTCCloud.exitRoom();
    }

    /**
     * trtc 退房
     */
    public void exitRoom() {
        mTRTCCloud.stopLocalPreview();
        mTRTCCloud.stopLocalAudio();
        mTRTCCloud.exitRoom();
    }

    /**
     * trtc 进房
     */
    public void enterTRTCRoom(RoomKey roomKey) {
        if (roomKey == null) return;

        Log.i(TAG, "enterTRTCRoom: " + roomKey.getUserId() + " room:" + roomKey.getRoomId());
        TRTCCloudDef.TRTCParams TRTCParams = new TRTCCloudDef.TRTCParams(roomKey.getAppId(),
                roomKey.getUserId(), roomKey.getUserSig(), roomKey.getRoomId(), "", "");
        TRTCParams.role = TRTCCloudDef.TRTCRoleAudience;
        // 收到来电，开始监听 trtc 的消息
        mTRTCCloud.setListener(mTRTCCloudListener);
        mTRTCCloud.enterRoom(TRTCParams, TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL);
    }

    public void startRemoteView(String userId, TXCloudVideoView txCloudVideoView) {
        if (txCloudVideoView == null) {
            return;
        }
        mTRTCCloud.startRemoteView(userId, txCloudVideoView);
    }

    public void stopRemoteView(String userId) {
        mTRTCCloud.stopRemoteView(userId);
    }

}
