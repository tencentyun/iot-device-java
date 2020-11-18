package com.tencent.iot.explorer.device.face.widgets;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.tencent.iot.explorer.device.android.app.R;

import java.util.concurrent.CountDownLatch;

public class ConfirmRegFaceViewController {

    /** 这是阻塞方法 */
    public ConfirmResult waitConfirm(final Context context, final Bitmap face, final String name) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final ConfirmResult result = new ConfirmResult();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final MyDialog dialog = new MyDialog(context);

                final View view = LayoutInflater.from(context).inflate(R.layout.confirm_reg_face, null);
                view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                ((ImageView) view.findViewById(R.id.img)).setImageBitmap(face);
                final EditText nameText = (EditText) view.findViewById(R.id.name);
                nameText.setText(name);
                view.findViewById(R.id.reg).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = nameText.getText().toString();
                        if (TextUtils.isEmpty(name)) {
                            Toast.makeText(context, "请输入名字", Toast.LENGTH_SHORT).show();
                        } else {
                            result.setResultOkWithName(name);
                            dialog.dismiss();
                            countDownLatch.countDown();
                        }
                    }
                });
                view.findViewById(R.id.next).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        result.setCode(ConfirmResult.RESULT_NEXT);
                        dialog.dismiss();
                        countDownLatch.countDown();
                    }
                });
                view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        result.setCode(ConfirmResult.RESULT_CANCEL);
                        dialog.dismiss();
                        countDownLatch.countDown();
                    }
                });

                dialog.setContentView(view);
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        result.setCode(ConfirmResult.RESULT_CANCEL);
                        dialog.dismiss();
                        countDownLatch.countDown();
                    }
                });
                dialog.show();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static class ConfirmResult {

        public static final int RESULT_CANCEL = 0;
        public static final int RESULT_NEXT = 1;
        public static final int RESULT_OK = 2;
        /**
         * 用户交互结果: {@link #RESULT_CANCEL}, {@link #RESULT_NEXT}, {@link #RESULT_OK}
         */
        public volatile int code = RESULT_CANCEL;
        public volatile String name = "";

        public void setResultOkWithName(String name) {
            this.code = RESULT_OK;
            this.name = name;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

    private static class MyDialog extends Dialog {

        MyDialog(@NonNull Context context) {
            super(context);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        @Override
        public void show() {
            // 设置背景变暗程度, 构造方法时候设置无效
            getWindow().getAttributes().dimAmount = 0.8f;
            super.show();
        }
    }
}
