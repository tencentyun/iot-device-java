package com.tencent.iot.explorer.device.android.youtu.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.iot.explorer.device.android.youtu.demo.business.thread.AIThreadPool;
import com.tencent.cloud.ai.fr.sdksupport.Auth;
import com.tencent.cloud.ai.fr.sdksupport.Auth.AuthResult;
import com.tencent.cloud.ai.fr.utils.PermissionHandler;
import com.tencent.cloud.ai.fr.utils.PermissionHandler.GetPermissionsException;

import java.util.Arrays;

public class AuthActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);

        try {
            mPermissionHandler.start();// 先申请系统权限
        } catch (GetPermissionsException e) {
            e.printStackTrace();
            Toast.makeText(this, "GetPermissionsException: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);// 必须有这个调用, mPermissionHandler 才能正常工作
    }

    private AuthResult auth(Context context, String appId, String secretKey) {
        AuthResult authResult = Auth.authWithDeviceSn(context, appId, secretKey);
        String msg = String.format("授权%s, appId=%s, %s", authResult.isSucceeded() ? "成功" : "失败", appId, authResult.toString());
        showMessage(msg);
        return authResult;
    }

    private AuthResult auth(Context context, String licenceFileName) {
        AuthResult authResult = Auth.authWithLicence(context, licenceFileName);
        String msg = String.format("授权%s, licenceFileName=%s, %s", authResult.isSucceeded() ? "成功" : "失败", licenceFileName, authResult.toString());
        showMessage(msg);
        return authResult;
    }

    private void showMessage(final String msg) {
        Log.d(TAG, msg);
        ((TextView) findViewById(R.id.tips)).setText(msg);
    }

    private final PermissionHandler mPermissionHandler = new PermissionHandler(this) {
        @Override
        protected boolean shouldIgnore(String permission) {
            return false;
            // return permission.equals(Manifest.permission.WRITE_SETTINGS) //API 23 或以上, 无法通过授权对话框获得授权, 忽略之
        }

        @Override
        protected void onPermissionsDecline(String[] permissions) {
            String msg = "没有获得系统权限: " + Arrays.toString(permissions);
            showMessage(msg);
        }

        @Override
        protected void onAllPermissionGranted() {
            // 请修改人脸识别 SDK 授权信息
             AuthResult authResult = auth(AuthActivity.this, ""/*修改APPID为实际的值*/, ""/*修改SECRET_KEY为实际的值*/);

            if (authResult.isSucceeded()) {//授权成功

                AIThreadPool.instance().init(AuthActivity.this);//提前全局初始化, 后续的 Activity 就不必再执行初始化了

                addButton("1:N 注册(Android相机)", RegWithAndroidCameraActivity.class);
                addButton("1:N 注册(图片文件)", RegWithFileActivity.class);
                addButton("1:N 搜索(Android相机)", RetrieveWithAndroidCameraActivity.class);
            }
        }
    };

    private void addButton(String buttonText, final Class targetActivity) {
        Button button = new Button(this);
        button.setText(buttonText);
        button.setAllCaps(false);
        button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AuthActivity.this, targetActivity));
            }
        });
        ((ViewGroup) findViewById(R.id.button_container)).addView(button);
    }

}
