package com.tencent.iot.explorer.device.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Watermark {
    private String mText;
    private int mTextColor;
    private float mTextSize;
    private float mRotation;
    private static Context mContext;
    private static Watermark sInstance;

    private Watermark() {
        mText = "";
        mTextColor = 0xAEAEAEAE;
        mTextSize = 14;
        mRotation = 0;
    }

    public static Watermark getInstance(Context context) {
        if (sInstance == null) {
            synchronized (Watermark.class) {
                mContext = context;
                sInstance = new Watermark();
            }
        }
        return sInstance;
    }

    public Watermark setText(String text) {
        mText = text;
        return sInstance;
    }

    public Watermark setTextColor(int color) {
        mTextColor = color;
        return sInstance;
    }

    public Watermark setTextSize(float size) {
        mTextSize = size;
        return sInstance;
    }

    public Watermark setRotation(float degrees) {
        mRotation = degrees;
        return sInstance;
    }

    public void show(Activity activity) {
        show(activity, mText);
    }

    public void show(Activity activity, String text) {
        WatermarkDrawable drawable = new WatermarkDrawable();
        drawable.mText = text;
        drawable.mTextColor = mTextColor;
        drawable.mTextSize = mTextSize;
        drawable.mRotation = mRotation;

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        FrameLayout layout = new FrameLayout(activity);
        layout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setBackground(drawable);
        rootView.addView(layout);
    }

    private class WatermarkDrawable extends Drawable {
        private Paint mPaint;
        private String mText;
        private int mTextColor;
        private float mTextSize;
        private float mRotation;

        private WatermarkDrawable() {
            mPaint = new Paint();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().right;
            int height = getBounds().bottom;
            int diagonal = (int) Math.sqrt(width * width + height * height); // 对角线的长度

            mPaint.setColor(mTextColor);
            mPaint.setTextSize(sp2px(mTextSize));
            mPaint.setAntiAlias(true);
            float textWidth = mPaint.measureText(mText);

            canvas.drawColor(0x00000000);
            canvas.rotate(mRotation);

//            int index = 0;
//            float fromX;
//            for (int positionY = diagonal / 10; positionY <= diagonal; positionY += diagonal / 10) {
//                fromX = -width + (index++ % 2) * textWidth; // 上下两行的X轴起始点不一样，错开显示
//                for (float positionX = fromX; positionX < width; positionX += textWidth * 2) {
//                    canvas.drawText(mText, positionX, positionY, mPaint);
//                }
//            }
            canvas.drawText(mText, 30, height - 30, mPaint);

            canvas.save();
            canvas.restore();
        }

        @Override
        public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private int sp2px(float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spVal, mContext.getResources().getDisplayMetrics());
    }
}
