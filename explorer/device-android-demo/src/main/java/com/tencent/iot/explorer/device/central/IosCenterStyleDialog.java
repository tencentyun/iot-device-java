package com.tencent.iot.explorer.device.central;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import com.tencent.iot.explorer.device.android.app.R;

public class IosCenterStyleDialog extends Dialog {

    protected DisplayMetrics displayMetrics;
    protected View view;
    private Context mContext;
    private int layoutId;
    private boolean showAnimation;

    public IosCenterStyleDialog(Context context, int layoutId) {
        this(context, layoutId, true);
    }

    public IosCenterStyleDialog(Context context, int layoutId, boolean showAnimation) {
        super(context, R.style.iOSDialog);
        mContext = context;
        displayMetrics = context.getResources().getDisplayMetrics();
        this.layoutId = layoutId;
        this.showAnimation = showAnimation;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = View.inflate(mContext, this.layoutId, null);
        //设置view 弹出的平移动画，从底部-100% 平移到自身位置
        if (showAnimation) {
            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                    0f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.setDuration(350);
            animation.setStartOffset(150);
            view.setAnimation(animation);//设置动画
        }

        initView();
    }

    public void initView() { }

    @Override
    public void show() {
        // 如果显示对话框的 activity 已经回收，不再显示对话框
        if(((Activity) mContext).isFinishing() || ((Activity) mContext).isDestroyed()) return;

        super.show();
        // 设置dialog的宽高是全屏，注意：一定要放在show的后面，否则不是全屏显示
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = displayMetrics.widthPixels;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;
        getWindow().setAttributes(params);
        getWindow().setContentView(view);
        if (onShowed != null) {
            onShowed.onShowed();
        }
    }

    protected interface OnShowed {
        void onShowed();
    }

    private OnShowed onShowed = null;

    protected void setOnShowed(OnShowed onShowed) {
        this.onShowed = onShowed;
    }

}
