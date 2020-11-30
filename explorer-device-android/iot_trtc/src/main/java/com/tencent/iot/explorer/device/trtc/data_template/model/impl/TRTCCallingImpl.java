package com.tencent.iot.explorer.device.trtc.data_template.model.impl;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.tencent.iot.explorer.device.trtc.data_template.model.RoomKey;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCalling;
import com.tencent.iot.explorer.device.trtc.data_template.model.TRTCCallingDelegate;
import com.tencent.liteav.beauty.TXBeautyManager;
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
public class TRTCCallingImpl {
    private static final String TAG            = TRTCCallingImpl.class.getSimpleName();
    /**
     * 超时时间，单位秒
     */
    public static final  int    TIME_OUT_COUNT = 30;
    private final Context mContext;

    /**
     * 底层SDK调用实例
     */
    private TRTCCloud mTRTCCloud;
    private boolean mIsUseFrontCamera;
    private TRTCCallingDelegate mTRTCCallingDelegate;

    public void setTRTCCallingDelegate(TRTCCallingDelegate mTRTCCallingDelegate) {
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
//            TRTCLogger.e(TAG, "onError: " + errCode + " " + errMsg);
//            stopCall();
//            if (mTRTCInternalListenerManager != null) {
//                mTRTCInternalListenerManager.onError(errCode, errMsg);
//            }
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onError(errCode, errMsg);
            }
        }

        @Override
        public void onEnterRoom(long result) {
//            TRTCLogger.d(TAG, "onEnterRoom result:" + result);
//            if (result < 0) {
//                stopCall();
//            } else {
//                mIsInRoom = true;
//            }
        }

        @Override
        public void onExitRoom(int reason) {
//            TRTCLogger.d(TAG, "onExitRoom reason:" + reason);
        }

        @Override
        public void onRemoteUserEnterRoom(String userId) {
//            TRTCLogger.d(TAG, "onRemoteUserEnterRoom userId:" + userId);
//            mCurRoomRemoteUserSet.add(userId);
//            // 只有单聊这个时间才是正确的，因为单聊只会有一个用户进群，群聊这个时间会被后面的人重置
//            mEnterRoomTime = System.currentTimeMillis();
//            if (mTRTCInternalListenerManager != null) {
//                mTRTCInternalListenerManager.onUserEnter(userId);
//            }
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserEnter(userId);
            }
        }

        @Override
        public void onRemoteUserLeaveRoom(String userId, int reason) {
//            TRTCLogger.d(TAG, "onRemoteUserLeaveRoom userId:" + userId + ", reason:" + reason);
//            mCurRoomRemoteUserSet.remove(userId);
//            mCurInvitedList.remove(userId);
//            // 远端用户退出房间，需要判断本次通话是否结束
//            if (mTRTCInternalListenerManager != null) {
//                mTRTCInternalListenerManager.onUserLeave(userId);
//            }
//            preExitRoom(userId);
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserLeave(userId);
            }
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
//            TRTCLogger.d(TAG, "onUserVideoAvailable userId:" + userId + ", available:" + available);
//            if (mTRTCInternalListenerManager != null) {
//                mTRTCInternalListenerManager.onUserVideoAvailable(userId, available);
//            }
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserVideoAvailable(userId, available);
            }
        }

        @Override
        public void onUserAudioAvailable(String userId, boolean available) {
//            TRTCLogger.d(TAG, "onUserAudioAvailable userId:" + userId + ", available:" + available);
//            if (mTRTCInternalListenerManager != null) {
//                mTRTCInternalListenerManager.onUserAudioAvailable(userId, available);
//            }
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
//            mTRTCInternalListenerManager.onUserVoiceVolume(volumeMaps);
            if (mTRTCCallingDelegate != null) {
                mTRTCCallingDelegate.onUserVoiceVolume(volumeMaps);
            }
        }
    };


    public TRTCCallingImpl(Context context) {
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

        if (roomKey.getCallType() == TRTCCalling.TYPE_VIDEO_CALL) {
            // 开启基础美颜
            TXBeautyManager txBeautyManager = mTRTCCloud.getBeautyManager();
            // 自然美颜
            txBeautyManager.setBeautyStyle(1);
            txBeautyManager.setBeautyLevel(6);
            // 进房前需要设置一下关键参数
            TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
            encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540;
            encParam.videoFps = 15;
            encParam.videoBitrate = 1000;
            encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
            encParam.enableAdjustRes = true;
            mTRTCCloud.setVideoEncoderParam(encParam);
        }
        Log.i(TAG, "enterTRTCRoom: " + roomKey.getUserId() + " room:" + roomKey.getRoomId());
        TRTCCloudDef.TRTCParams TRTCParams = new TRTCCloudDef.TRTCParams(roomKey.getAppId(),
                roomKey.getUserId(), roomKey.getUserSig(), roomKey.getRoomId(), "", "");
        TRTCParams.role = TRTCCloudDef.TRTCRoleAnchor;
        mTRTCCloud.enableAudioVolumeEvaluation(300);
        mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
        mTRTCCloud.startLocalAudio();
        // 收到来电，开始监听 trtc 的消息
        mTRTCCloud.setListener(mTRTCCloudListener);
        mTRTCCloud.enterRoom(TRTCParams, roomKey.getCallType() == TRTCCalling.TYPE_VIDEO_CALL ? TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL : TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL);
    }

    public void openCamera(boolean isFrontCamera, TXCloudVideoView txCloudVideoView) {
        if (txCloudVideoView == null) {
            return;
        }
        mIsUseFrontCamera = isFrontCamera;
        mTRTCCloud.startLocalPreview(isFrontCamera, txCloudVideoView);
    }

    public void closeCamera() {
        mTRTCCloud.stopLocalPreview();
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

    public void switchCamera(boolean isFrontCamera) {
        if (mIsUseFrontCamera == isFrontCamera) {
            return;
        }
        mIsUseFrontCamera = isFrontCamera;
        mTRTCCloud.switchCamera();
    }

    public void setMicMute(boolean isMute) {
        mTRTCCloud.muteLocalAudio(isMute);
    }

    public void setHandsFree(boolean isHandsFree) {
        if (isHandsFree) {
            mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
        } else {
            mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
        }
    }

}
