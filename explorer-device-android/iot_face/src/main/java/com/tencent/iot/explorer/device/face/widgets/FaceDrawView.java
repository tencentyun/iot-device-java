package com.tencent.iot.explorer.device.face.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 高性能的绘制类
 */
public class FaceDrawView extends SurfaceView {

    private final String TAG = this.getClass().getSimpleName();

    private final Object mDataChangeLock = new Object();
    private Paint mRectPaint;
    private Paint mPointsPaint;
    private Paint mTextPaint;
    private SurfaceHolder mHolder;
    private OnDrawDelayListener mOnDrawDelayListener;
    private volatile long mDataUpdateTime;
    private volatile Collection<DrawableFace> mTracedFaces = new ArrayList<>();
    private volatile int mFrameWidth;
    private volatile int mFrameHeight;
    private float mDensity;

    /**
     * @param isOverAnotherSurface 这个 View 是否叠在另一个 SurfaceView 上面, 如果设置错误, 会使这个 View 失去透明效果
     */
    public FaceDrawView(Context context, boolean isOverAnotherSurface) {
        super(context);
        init(isOverAnotherSurface);
    }

    private void init(boolean isOverAnotherSurface) {
        mDensity = getResources().getDisplayMetrics().density;
        
        mRectPaint = new Paint();
        mRectPaint.setStyle(Style.STROKE);
        mRectPaint.setStrokeWidth(1 * mDensity);
        mRectPaint.setColor(Color.GREEN);
        
        mPointsPaint = new Paint();
        mPointsPaint.setStyle(Style.STROKE);
        mPointsPaint.setStrokeWidth(2 * mDensity);
        mPointsPaint.setColor(Color.GREEN);

        mTextPaint = new TextPaint();
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setTextSize(10 * mDensity);

        mHolder = getHolder();

        mHolder.setFormat(PixelFormat.TRANSPARENT);//允许绘制透明色
        setZOrderOnTop(true);//有些机器必须加, 否则看不到绘制效果, 例如华为 waterplay 平板 (HDN-W09)
        
        // 当本 SurfaceView 覆盖在另一个 SurfaceView 上面时, 需要设置为 true, 本 SurfaceView 才有透明效果
        // 但是如果没有覆盖在另一个 SurfaceView 上面时, 则切勿调用此方法, 本 SurfaceView 才有透明效果. 只要调用, 不论入参 true/false, 透明效果都丢失  
        if (isOverAnotherSurface) {
            setZOrderMediaOverlay(true);//有些机器必须加, 否则看不到绘制效果, 例如华为 waterplay 平板 (HDN-W09)
        }

        mHolder.addCallback(new SurfaceHolder.Callback() {
            Thread mThread;

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mThread == null || mThread.isInterrupted() || !mThread.isAlive()) {
                    mThread = new DrawThread();
                    mThread.start();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                synchronized (mDataChangeLock) {//尺寸可能变了, 重新绘制
                    mDataChangeLock.notify();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mThread != null) {
                    mThread.interrupt();
                }
            }
        });
    }

    private class DrawThread extends Thread {

        private volatile boolean isRunning = true;
        private volatile long mDrawDelay = -1;
        private final Handler mUiHandler = new Handler(Looper.getMainLooper());
        private final Runnable mNotifyOnDrawDelayListenerRunnable = new Runnable() {
            @Override
            public void run() {
                if (mOnDrawDelayListener != null) {
                    mOnDrawDelayListener.onDrawDelay(mDrawDelay);
                }
            }
        };

        @Override
        public void run() {
            while (isRunning) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas == null) {
                    continue;
                }
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//擦除画布
                onDrawTracedFaceInner(canvas);
                mHolder.unlockCanvasAndPost(canvas);

                //计算绘制延迟, 并回调
                mDrawDelay = System.currentTimeMillis() - mDataUpdateTime;
                mUiHandler.post(mNotifyOnDrawDelayListenerRunnable);

                //等待下一次绘制
                synchronized (mDataChangeLock) {
                    try {
                        mDataChangeLock.wait();
                    } catch (InterruptedException e) {
                        Log.d(TAG, "TracedFaceSurfaceView.DrawThread Interrupted");
                        break;
                    }
                }
            }
            Log.d(TAG, "TracedFaceSurfaceView.DrawThread is finished.");
        }

        @Override
        public void interrupt() {
            Log.d(TAG, "TracedFaceSurfaceView.DrawThread.interrupt()");
            isRunning = false;
            super.interrupt();
        }

    }

    public void setDrawDelayListener(OnDrawDelayListener l) {
        mOnDrawDelayListener = l;
    }

    public void onDrawTracedFaces(Collection<DrawableFace> tracedFaces, final int frameWidth, final int frameHeight) {
        mTracedFaces.clear();
        mTracedFaces.addAll(tracedFaces);
        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;

        mDataUpdateTime = System.currentTimeMillis();
        synchronized (mDataChangeLock) {
            mDataChangeLock.notify();
        }
    }

    private void onDrawTracedFaceInner(Canvas canvas) {
        if (mTracedFaces == null) {
            return;
        }
        
        Matrix matrix = new Matrix();
        float heightRatio = Math.max(getWidth(), getHeight()) * 1f / Math.max(mFrameWidth, mFrameHeight);
        matrix.postScale(heightRatio, heightRatio);
        
        final Collection<DrawableFace> faces = new ArrayList<>(mTracedFaces);
        for (DrawableFace face : faces) {
            //画人脸框
            RectF rf = new RectF(face.rect);
            matrix.mapRect(rf);
            mRectPaint.setColor(face.color);
            canvas.drawRect(rf, mRectPaint);

            //画人脸关键点
            if (face.points != null && face.points.length > 2) {
                float[] dst = new float[face.points.length];
                matrix.mapPoints(dst, face.points);
                mPointsPaint.setColor(face.color);
                canvas.drawPoints(dst, mPointsPaint);
            }

            //画人脸相关文字说明
            int x = (int) (rf.left + 5 * mDensity);
            int y = (int) (rf.top + 12 * mDensity);
            mTextPaint.setColor(face.color);
            for (String line : face.msg.split("\n")) {
                canvas.drawText(line, x, y, mTextPaint);
                y += mTextPaint.descent() - mTextPaint.ascent();
            }
        }
    }

    /** 回调绘制延迟, 用于性能监控 */
    public interface OnDrawDelayListener {

        /** 在主线程回调 */
        void onDrawDelay(long delayInMs);
    }

    /** 实体类, 需要绘制的人脸信息 */
    public static class DrawableFace {

        final int color;
        final Rect rect;
        final float[] points;
        final String msg;

        public DrawableFace(Rect rect, float[] points, String msg, int color) {
            this.rect = rect;
            this.points = points;
            this.msg = msg;
            this.color = color;
        }
    }
}
