package com.tencent.iot.explorer.device.android.youtu.demo.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.percent.PercentRelativeLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.ImageView;

import com.tencent.iot.explorer.device.android.youtu.demo.R;
import com.tencent.iot.explorer.device.android.youtu.demo.widgets.FaceDrawView.DrawableFace;
import com.tencent.youtu.YTFaceTracker.TrackedFace;

import java.util.ArrayList;
import java.util.Collection;

public class FakeCameraActivityViewController extends AbsActivityViewController {

    private ImageView mColorImage;
    private ImageView mIrImage;
    private ImageView mDepthImage;
    private FaceDrawView mTracedFaceDrawViewColor;
    private FaceDrawView mTracedFaceDrawViewIr;
    private FaceDrawView mTracedFaceDrawViewDepth;
    private ViewGroup mPreviewContainer;
    private View mPreview;
    private boolean mIsFirstFrame = true;
    private boolean mIsLandscape;

    public FakeCameraActivityViewController(Context context) {
        super(context);
        mColorImage = new ImageView(context);
        mIrImage = new ImageView(context);
        mDepthImage = new ImageView(context);

        mTracedFaceDrawViewColor = new FaceDrawView(context, false);
        mTracedFaceDrawViewIr = new FaceDrawView(context, false);
        mTracedFaceDrawViewDepth = new FaceDrawView(context, false);
    }

    @Override
    protected View onCreateCameraPreview() {
        mPreviewContainer = (ViewGroup) LayoutInflater.from(mRootView.getContext()).inflate(R.layout.camera_preview_fake, null);
        return mPreviewContainer;
    }

    @Override
    protected void onDrawColorFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight) {
        if (frameWidth != 0 && frameHeight != 0) {
            boolean isLandscape = frameWidth > frameHeight;
            adjustPreviewOrientation(isLandscape);
            adjustPreviewSize(mColorImage, frameWidth, frameHeight);
        }
        mTracedFaceDrawViewColor.onDrawTracedFaces(drawableFaces, frameWidth, frameHeight);
    }

    @Override
    protected void onDrawIrFaces(Collection<DrawableFace> drawableFaces, int frameWidth, int frameHeight) {
        if (frameWidth != 0 && frameHeight != 0) {
            adjustPreviewSize(mIrImage, frameWidth, frameHeight);
        }
        mTracedFaceDrawViewIr.onDrawTracedFaces(drawableFaces, frameWidth, frameHeight);
    }

    public void drawDepthFaces(Collection<TrackedFace> faces, int frameWidth, int frameHeight) {
        mTracedFaceDrawViewDepth.onDrawTracedFaces(makeDrawableFaces(faces), frameWidth, frameHeight);
    }
    
    public void onDrawDepthFaces(Collection<DrawableFace> faces, int frameWidth, int frameHeight) {
        if (frameWidth != 0 && frameHeight != 0) {
            adjustPreviewSize(mDepthImage, frameWidth, frameHeight);
        }
        mTracedFaceDrawViewDepth.onDrawTracedFaces(faces, frameWidth, frameHeight);
    }

    private Collection<DrawableFace> makeDrawableFaces(Collection<TrackedFace> faces) {
        ArrayList<DrawableFace> drawableFaces = new ArrayList<>(faces.size());
        for (TrackedFace face : faces) {
            drawableFaces.add(new DrawableFace(face.faceRect, face.xy5Points, "", Color.YELLOW));
        }
        return drawableFaces;
    }

    public void setPreviewImageColor(final Bitmap bitmap) {
        setImage(mColorImage, bitmap);
    }
    
    public void setPreviewImageIr(final Bitmap bitmap) {
        setImage(mIrImage, bitmap);
    }

    public void setPreviewImageDepth(final Bitmap bitmap) {
        setImage(mDepthImage, bitmap);
    }

    private void setImage(final ImageView imageView, final Bitmap bitmap) {
        imageView.post(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    // 根据图片宽高比调整图片框的宽高比，防止人脸框错位
    private void adjustPreviewSize(final ImageView imageView, final int width, final int height) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) imageView.getLayoutParams();
                params.getPercentLayoutInfo().aspectRatio = (float) width / (float) height;
                imageView.setLayoutParams(params);
            }
        });
    }

    // 根据图片宽高比调整布局样式（横向、纵向的图片对应不同布局）
    private void adjustPreviewOrientation(final boolean isLandscape) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (isLandscape != mIsLandscape || mIsFirstFrame) {
                    View realPreview;
                    if (!mIsFirstFrame) {
                        ((ViewManager) mPreview).removeView(mTracedFaceDrawViewColor);
                        ((ViewManager) mPreview).removeView(mTracedFaceDrawViewIr);
                        ((ViewManager) mPreview).removeView(mTracedFaceDrawViewDepth);
                        ((ViewManager) mPreview).removeView(mColorImage);
                        ((ViewManager) mPreview).removeView(mIrImage);
                        ((ViewManager) mPreview).removeView(mDepthImage);
                        mPreviewContainer.removeView(mPreview);
                    }

                    if (isLandscape) {
                        realPreview = LayoutInflater.from(mPreviewContainer.getContext()).inflate(R.layout.horizontal_camera_preview_fake, null, false);
                    } else {
                        realPreview = LayoutInflater.from(mPreviewContainer.getContext()).inflate(R.layout.vertial_camera_preview_fake, null, false);
                    }
                    mPreview = realPreview;
                    mPreviewContainer.addView(realPreview);


                    replaceView(realPreview.findViewById(R.id.drawViewContainerColor), mTracedFaceDrawViewColor);
                    replaceView(realPreview.findViewById(R.id.drawViewContainerIr), mTracedFaceDrawViewIr);
                    replaceView(realPreview.findViewById(R.id.drawViewContainerDepth), mTracedFaceDrawViewDepth);

                    replaceView(realPreview.findViewById(R.id.imgColor), mColorImage);
                    replaceView(realPreview.findViewById(R.id.imgIr), mIrImage);
                    replaceView(realPreview.findViewById(R.id.imgDepth), mDepthImage);
                    mIsFirstFrame = false;

                    mIsLandscape = isLandscape;
                }
            }
        });
    }
}
