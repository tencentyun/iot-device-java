package com.tencent.iot.explorer.device.android.youtu.demo.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraView.Callback;
import com.tencent.cloud.ai.fr.camera.ICameraManager;

/**
 * 如果使用不同的相机, 重新实现这个类即可
 */
public class AndroidCameraManager implements ICameraManager {

    private final String TAG = this.getClass().getSimpleName();
    
    private final CameraView mCameraView;
    private int mFacing = CameraView.FACING_FRONT;
    private int mFrameWidth;
    private int mFrameHeight;
    private OnFrameArriveListener mOnFrameArriveListener;
    private Camera mCamera;
    private boolean isJustResume = true;

    public AndroidCameraManager(Context context) {
        mCameraView = new CameraView(context);
        mCameraView.setFacing(mFacing);
        mCameraView.setAspectRatio(AspectRatio.of(16, 9));
        mCameraView.addCallback(new Callback() {

            private Size mPreviewSize;
            private int mCachedExifOrientation = 0;

            @SuppressLint("LongLogTag")
            @Override
            public void onCameraOpened(CameraView cameraView) {
                Log.d(TAG, "onCameraOpened");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onCameraClosed(CameraView cameraView) {
                Log.d(TAG, "onCameraClosed");
            }

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                mCamera = camera;//始终刷新相机引用, 保证 CameraFrameRecycler 能正确回收帧
                
                if (isJustResume || mPreviewSize == null || mCachedExifOrientation == 0) {
                    isJustResume = false;
                    Parameters parameters = camera.getParameters();
                    
                    mPreviewSize = parameters.getPreviewSize();
                    mFrameWidth = mPreviewSize.width;
                    mFrameHeight = mPreviewSize.height;
                    
                    String rotation = parameters.get("rotation");//这是耗时操作, 避免频繁调用
                    int r = Integer.parseInt(rotation);
                    mCachedExifOrientation = calcExifOrientation(r, mFacing == CameraView.FACING_FRONT);//切换前后摄像头的时候, ExifOrientation 会变
                }
                
                if (mOnFrameArriveListener != null) {
                    mOnFrameArriveListener.onFrameArrive(data, mPreviewSize.width, mPreviewSize.height, mCachedExifOrientation);
                } else {//帧没有被接管, 直接回收
                    onRecycleFrame(data);
                }
            }
        });
    }

    public void switchCamera() {
        pauseCamera();
        mFacing = (mFacing == CameraView.FACING_FRONT ? CameraView.FACING_BACK : CameraView.FACING_FRONT);
        mCameraView.setFacing(mFacing);
        resumeCamera();
    }

    /** 计算帧旋转角度 */
    private int calcExifOrientation(int cameraRotation, boolean isMirrored) {
        int exifOrientation = 1;
        if (cameraRotation == 0) {
            exifOrientation = isMirrored ? 2 : 1;
        } else if (cameraRotation == 90) {
            exifOrientation = isMirrored ? 5 : 6;
        } else if (cameraRotation == 180) {
            exifOrientation = isMirrored ? 4 : 3;
        } else if (cameraRotation == 270) {
            exifOrientation = isMirrored ? 7 : 8;
        } else {
            throw new IllegalArgumentException("cameraRotation can only be 0, 90, 180, 270");
        }
        Log.d(TAG, String.format("calcExifOrientation(): exifOrientation = %s, cameraRotation = %s, isMirrored = %s", exifOrientation, cameraRotation, isMirrored));
        return exifOrientation;
    }

    private int mStartCameraFailCount = 0;

    @Override
    public void resumeCamera() {
        if (mCameraView != null) {
            try {
                mCameraView.start();//启动或亮屏时, 启动相机
                isJustResume = true;
            } catch (Exception e) {
                if (mStartCameraFailCount < 2) {
                    mStartCameraFailCount++;
                    Toast.makeText(mCameraView.getContext(), "打开摄像头失败, 重试次数" + mStartCameraFailCount, Toast.LENGTH_SHORT).show();
                    switchCamera();
                } else {
                    e.printStackTrace();
                    Toast.makeText(mCameraView.getContext(), "打开摄像头失败, 重试失败.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void pauseCamera() {
        if (mCameraView != null) {
            mCameraView.stop();//锁屏或者退出时, 暂停相机
        }
    }

    @Override
    public void destroyCamera() {
    }

    @Override
    public void setOnFrameArriveListener(OnFrameArriveListener l) {
        mOnFrameArriveListener = l;
    }

    @Override
    public View getPreview() {
        return mCameraView;
    }

    @Override
    public void onRecycleFrame(byte[] frameDataToBeRecycled) {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(frameDataToBeRecycled);
        }
    }
}
