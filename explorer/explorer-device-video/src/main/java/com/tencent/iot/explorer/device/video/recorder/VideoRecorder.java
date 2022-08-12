package com.tencent.iot.explorer.device.video.recorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.text.TextUtils;

import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.opengles.view.CameraView;
import com.tencent.iot.explorer.device.video.recorder.opengles.view.base.EGLTextureView;
import com.tencent.iot.explorer.device.video.recorder.param.AudioEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.param.CameraParam;
import com.tencent.iot.explorer.device.video.recorder.param.MicParam;
import com.tencent.iot.explorer.device.video.recorder.param.RecordParam;
import com.tencent.iot.explorer.device.video.recorder.param.RecordThreadParam;
import com.tencent.iot.explorer.device.video.recorder.param.VideoEncodeParam;

import javax.microedition.khronos.egl.EGLContext;

public class VideoRecorder {

    private RecordThread recordThread; // 实际获取视频流、音频流的线程
    private OnRecordListener onRecordListener; // 记录过程回调
    private CameraView cameraView; // 摄像头预览的内容
    private int recorderType = CallingType.TYPE_VIDEO_CALL;
    private OnEncodeListener encodeListener;
    private int frameRate = 0;

    public VideoRecorder(OnEncodeListener listener) {
        encodeListener = listener;
    }

    // 获取实际的摄像头预览对象
    public void attachCameraView(CameraView cameraView) {
        this.cameraView = cameraView;
    }

    /**
     * 开始录像
     * @param path 视频流音频流存储的路径
     * @return 错误码  0：成功
     *               -1：没有相机预览对象
     *               -2: 参数错误
     */
    public int start(String path, OnRecordListener onRecordListener) {
        return start(640, 360, path, onRecordListener);
    }

    public int start(OnRecordListener onRecordListener) {
        return start(640, 360, "", onRecordListener);
    }

    public int start(int recorderType, OnRecordListener onRecordListener) {
        this.recorderType = recorderType;
        return start(onRecordListener);
    }

    public int start(int recorderType, int width, int height, int frameRate, OnRecordListener onRecordListener) {
        this.recorderType = recorderType;
        this.frameRate = frameRate;
        return start(width, height, "", onRecordListener);
    }

    public int startRecord(String path, String audioName, String videoName) {
        if (recordThread == null) {// 没有开启采集线程
            return ErrorCode.ERROR_INSTANCE;
        }
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(audioName) || TextUtils.isEmpty(videoName)) { // 没有正确的保存路径
            return ErrorCode.ERROR_PARAM;
        }
        if (recordThread.isStartStore()) { // 正在保存本地文件
            return ErrorCode.ERROR_STATE;
        }
        recordThread.setStorePath(path);
        recordThread.setAudioName(audioName);
        recordThread.setVideoName(videoName);
        return recordThread.startStore(false);
    }

    public int stopRecord() {
        if (recordThread == null) { // 没有开启采集线程
            return ErrorCode.ERROR_INSTANCE;
        }
        if (!recordThread.isStartStore()) { // 没有处于录像状态
            return ErrorCode.ERROR_STATE;
        }
        recordThread.startStore(false);
        return ErrorCode.SUCCESS;
    }

    public boolean isRecord() {
        if (recordThread == null) {
            return false;
        }
        return recordThread.isStartStore();
    }

    public int start(int width, int height, String path, OnRecordListener onRecordListener) {
        if (cameraView == null) return -1;
        if (width <= 0 || height <= 0) return -2;

        this.onRecordListener = onRecordListener;
        return start(cameraView.getContext(), cameraView.getEglContext(), cameraView.getFboTextureId(),
                cameraView.getCameraManager().getCameraFacing(), width, height, path);
    }

    private int start(Context context, EGLContext eglContext, int textureId, int facing, int width, int height, String path) {
        VideoRender render = new VideoRender(context);
        render.setTextureId(textureId);

        MicParam micParam = new MicParam.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setSampleRateInHz(8000) // 采样率
                .setChannelConfig(AudioFormat.CHANNEL_IN_MONO)
                .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT) // PCM
                .build();
        AudioEncodeParam audioEncodeParam = new AudioEncodeParam.Builder().build();

        CameraParam cameraParam = new CameraParam.Builder()
                .setFacing(facing)
                .setEGLContext(eglContext)
                .setRenderer(render)
                .setRenderMode(EGLTextureView.RENDERMODE_CONTINUOUSLY)
                .build();
        VideoEncodeParam videoEncodeParam = new VideoEncodeParam.Builder()
                .setSize(width, height).build();
        if (frameRate>0)
            videoEncodeParam.setFrameRate(frameRate);

        RecordParam recordParam = new RecordParam(recorderType, path);

        RecordThreadParam recordThreadParam = new RecordThreadParam();
        recordThreadParam.setAudioEncodeParam(audioEncodeParam);
        recordThreadParam.setCameraParam(cameraParam);
        recordThreadParam.setMicParam(micParam);
        recordThreadParam.setRecordParam(recordParam);
        recordThreadParam.setVideoEncodeParam(videoEncodeParam);

        return start(recordThreadParam);
    }

    private int start(RecordThreadParam recordThreadParam) {
        // 清理环境
        cancel();
        recordThread = new RecordThread(recordThreadParam, encodeListener);
        recordThread.setOnRecordListener(onRecordListener);
        recordThread.start();
        return ErrorCode.SUCCESS;
    }

    public void cancel() {
        if (recordThread != null) {
            recordThread.cancelRecord();
            recordThread = null;
        }
    }

    public void stop() {
        stopRecordThread();
    }

    private void stopRecordThread() {
        if (recordThread != null) {
            if (isRecord()) stopRecord();

            recordThread.stopRecord();
            recordThread = null;
        }
    }
}
