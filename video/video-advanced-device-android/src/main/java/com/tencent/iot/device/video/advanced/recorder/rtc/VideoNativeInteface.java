package com.tencent.iot.device.video.advanced.recorder.rtc;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.tencent.iot.explorer.device.common.stateflow.entity.RoomKey;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.tencent.trtc.TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG;

public class VideoNativeInteface {
    private String TAG = VideoNativeInteface.class.getSimpleName();
    private Context mContext;

    /**
     * 底层SDK调用实例
     */
    private TRTCCloud mRTCCloud;
    private boolean mIsUseFrontCamera;
    private XP2PCallback mXP2PCallback;

    private static VideoNativeInteface instance = null;

    public static synchronized VideoNativeInteface getInstance() {
        if (instance == null) {
            instance = new VideoNativeInteface();
        }
        return instance;
    }

    public void initWithDevice(Context context) {
        this.mContext = context;
        mRTCCloud = TRTCCloud.sharedInstance(mContext);
        mRTCCloud.setListener(mRTCCloudListener);
        enableAGC(false);
        enableAEC(true);
        enableANS(true);
    }

    public void setCallback(XP2PCallback mXP2PCallback) {
        this.mXP2PCallback = mXP2PCallback;
    }

    public TRTCCloudListener getRTCCloudListener() {
        return mRTCCloudListener;
    }

    /**
     * TRTC的监听器
     */
    private TRTCCloudListener mRTCCloudListener = new TRTCCloudListener() {
        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.e(TAG, "errorCode = " + errCode + ", errMsg: " + errMsg + ", extraInfo: " + extraInfo.toString());
            if (mXP2PCallback != null) {
                mXP2PCallback.onError(errCode, errMsg);
            }
        }

        @Override
        public void onEnterRoom(long result) {
            if (mXP2PCallback != null) {
                mXP2PCallback.onConnect(result);
            }
        }

        @Override
        public void onExitRoom(int reason) {
            if (mXP2PCallback != null) {
                mXP2PCallback.onRelease(reason);
            }
        }

        @Override
        public void onRemoteUserEnterRoom(String userId) {
            if (mXP2PCallback != null) {
                mXP2PCallback.onUserEnter(userId);
            }
        }

        @Override
        public void onRemoteUserLeaveRoom(String userId, int reason) {
            stopRemoteView(userId);
            if (mXP2PCallback != null) {
                mXP2PCallback.onUserLeave(userId);
            }
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
            if (mXP2PCallback != null) {
                mXP2PCallback.onUserVideoAvailable(userId, available);
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
            if (mXP2PCallback != null) {
                mXP2PCallback.onUserVoiceVolume(volumeMaps);
            }
        }

        @Override
        public void onRecvCustomCmdMsg(String userId, int cmdID, int seq, byte[] message) {
            String msg = new String(message);
            if (mXP2PCallback != null && !msg.isEmpty()) {
                mXP2PCallback.onRecvCustomCmdMsg(userId, msg);
            }
        }

        @Override
        public void onFirstVideoFrame(String userId, int streamType, int width, int height) {
            if (mXP2PCallback != null) {
                mXP2PCallback.onFirstVideoFrame(userId, width, height);
            }
        }
    };

    /**
     * rtc 进房
     */
    public void enterRoom(RoomKey roomKey) {
        if (roomKey == null) return;

        // 进房前需要设置一下关键参数
        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_360;
        encParam.videoFps = 15;
        encParam.videoBitrate = 1000;
        encParam.minVideoBitrate = 50;
        encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        encParam.enableAdjustRes = true;
        mRTCCloud.setVideoEncoderParam(encParam);

        Log.i(TAG, "enterTRTCRoom: " + roomKey.getUserId() + " room:" + roomKey.getRoomId());
        TRTCCloudDef.TRTCParams TRTCParams = new TRTCCloudDef.TRTCParams(roomKey.getAppId(),
                roomKey.getUserId(), roomKey.getUserSig(), roomKey.getRoomId(), "", "");
        TRTCParams.role = TRTCCloudDef.TRTCRoleAnchor;
        mRTCCloud.enableAudioVolumeEvaluation(300);
        mRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
        mRTCCloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH);
        // 收到来电，开始监听 trtc 的消息
        mRTCCloud.setListener(mRTCCloudListener);
        mRTCCloud.muteLocalAudio(true);
        mRTCCloud.enterRoom(TRTCParams, TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL);
    }

    /**
     * 断开链接
     */
    public void release() {
        mRTCCloud.stopLocalPreview();
        closeCamera();
        mRTCCloud.stopLocalAudio();
        mRTCCloud.exitRoom();
    }

    public boolean sendMsgToPeer(String msg) {
        if (mRTCCloud != null) {
            return mRTCCloud.sendCustomCmdMsg(1, msg.getBytes(), true, true);
        }
        return false;
    }

    public void openCamera(boolean isFrontCamera, TXCloudVideoView txCloudVideoView) {
        if (txCloudVideoView == null) {
            return;
        }
        mIsUseFrontCamera = isFrontCamera;
        mRTCCloud.muteLocalVideo(0, true);
        mRTCCloud.startLocalPreview(isFrontCamera, txCloudVideoView);
    }

    public void closeCamera() {
        mRTCCloud.stopLocalPreview();
    }

    public void sendStreamToServer() {
        mRTCCloud.muteLocalAudio(false);
        mRTCCloud.muteLocalVideo(0, false);
    }

    public void startRemoteView(String userId, TXCloudVideoView txCloudVideoView) {
        if (txCloudVideoView == null) {
            return;
        }
        mRTCCloud.startRemoteView(userId, TRTC_VIDEO_STREAM_TYPE_BIG, txCloudVideoView);
    }

    private void stopRemoteView(String userId) {
        mRTCCloud.stopRemoteView(userId, TRTC_VIDEO_STREAM_TYPE_BIG);
    }

    public void switchCamera(boolean isFrontCamera) {
        if (mIsUseFrontCamera == isFrontCamera) {
            return;
        }
        mIsUseFrontCamera = isFrontCamera;
        mRTCCloud.switchCamera();
    }

    public void setMicMute(boolean isMute) {
        mRTCCloud.muteLocalAudio(isMute);
    }

    public void setHandsFree(boolean isHandsFree) {
        if (isHandsFree) {
            mRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
        } else {
            mRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
        }
    }

    /**
     * 是否开启自动增益补偿功能, 可以自动调麦克风的收音量到一定的音量水平
     *
     * @param enable
     */
    public void enableAGC(boolean enable) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api", "enableAudioAGC");
            JSONObject params = new JSONObject();
            params.put("enable", enable ? 1 : 0);
            params.put("level", enable ? 100 : 0);//支持的取值有: 0、100，0 表示关闭 AGC
            jsonObject.put("params", params);
            mRTCCloud.callExperimentalAPI(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * 回声消除器，可以消除各种延迟的回声
     *
     * @param enable
     */
    public void enableAEC(boolean enable) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api", "enableAudioAEC");
            JSONObject params = new JSONObject();
            params.put("enable", enable ? 1 : 0);
            params.put("level", 100);//支持的取值有: 0、30、60、100，0 表示关闭 AEC
            jsonObject.put("params", params);
            mRTCCloud.callExperimentalAPI(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 背景噪音抑制功能，可探测出背景固定频率的杂音并消除背景噪音
     *
     * @param enable
     */
    public void enableANS(boolean enable) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api", "enableAudioANS");
            JSONObject params = new JSONObject();
            params.put("enable", enable ? 1 : 0);
            params.put("level", 100);//支持的取值有: 0、20、40、60、100，0 表示关闭 ANS
            jsonObject.put("params", params);
            mRTCCloud.callExperimentalAPI(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
