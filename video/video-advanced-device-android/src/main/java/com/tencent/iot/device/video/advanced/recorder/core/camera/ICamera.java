package com.tencent.iot.device.video.advanced.recorder.core.camera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import com.tencent.iot.device.video.advanced.recorder.data.base.Size;


public interface ICamera {

    void openCamera(Activity activity, SurfaceTexture surfaceTexture); // 打开相机
    void openCamera(int facing, Activity activity, SurfaceTexture surfaceTexture); // 打开相机
    void closeCamera(); // 关闭相机
    void switchCamera(); // 切换相机
    void switchCamera(int facing); // 切换相机
    void setCameraFacing(int facing); // 设置 Facing
    int getCameraFacing(); // 获取 Facing
    void setPreviewSize(Size cameraSize); // 设置预览尺寸
    Size getPreviewSize(); // 获取预览尺寸
    void setDisplayOrientation(int displayOrientation); // 设置显示旋转角度
    int getDisplayOrientation(); // 获取显示旋转角度
    void releaseCamera(); // 释放相机
    void addOnCameraListener(OnCameraListener onCameraListener); // 添加相机回调
    void removeOnCameraListener(OnCameraListener onCameraListener); // 移除相机回调
    void removeAllOnCameraListener(); // 移除所有回调
}
