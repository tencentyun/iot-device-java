package com.tencent.iot.explorer.device.video.recorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

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
    private int recorderType = VideoCalling.TYPE_VIDEO_CALL;

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
                .setSampleRateInHz(44100) // 采样率
                .setChannelConfig(AudioFormat.CHANNEL_IN_STEREO)
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

        Log.e("XXX", "pass recorderType");
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
        recordThread = new RecordThread(recordThreadParam);
        recordThread.setOnRecordListener(onRecordListener);
        recordThread.start();
        return 0;
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
            recordThread.stopRecord();
            recordThread = null;
        }
    }
}
