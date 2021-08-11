package com.tencent.iot.explorer.device.android.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.android.utils.TXLogImpl;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;


public class IoTMainActivity extends AppCompatActivity implements View.OnClickListener {

    public static String TAG = IoTMainActivity.class.getSimpleName();

    /**
     * 用于对Fragment进行管理
     */
    private FragmentManager fragmentManager;

    private IoTDataTemplateFragment mDataTemplateFragment;
    private IoTLightFragment mLightFragment;
    private IoTGatewayFragment mGatewayFragment;

    private Button btnDataTemplate;
    private Button btnDemo;
    private Button btnGateway;

    private int mCurrentFragment = R.id.btn_data_template;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iot_main);
        initComponent();
        //日志功能开启写权限
        try {
            for (String ele: PERMISSIONS_STORAGE) {
                int granted = ActivityCompat.checkSelfPermission(this, ele);
                if (granted != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                    break;
                } else {
                    TXLogImpl.init(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "必要权限申请失败");
                    finish();
                } else {
                    TXLogImpl.init(this);
                    break;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 设置默认的Fragment
        setDefaultFragment();
    }

    /**
     * 设置默认的Fragment
     */
    private void setDefaultFragment() {
        handlerButtonClick(R.id.btn_data_template);
    }

    /**
     * 初始化组件
     */
    private void initComponent() {
        // 初始化控件
        btnDemo = (Button) findViewById(R.id.btn_demo);
        btnDataTemplate = (Button) findViewById(R.id.btn_data_template);
        btnGateway = (Button) findViewById(R.id.btn_gateway);


        btnDemo.setOnClickListener(this);
        btnDataTemplate.setOnClickListener(this);
        btnGateway.setOnClickListener(this);

        fragmentManager = getSupportFragmentManager();
    }

    /**
     * 点击事件
     */
    @Override
    public void onClick(View v) {
        handlerButtonClick(v.getId());
    }

    /**
     * 处理tab点击事件
     *
     * @param id
     */
    private void handlerButtonClick(int id) {
        // 重置按钮状态
        resetButton(id);
        // 开启Fragment事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // 隐藏Fragment
        hideFragments(transaction);

        closeMqttConnection(mCurrentFragment);
        mCurrentFragment = id;

        switch (id) {

            case R.id.btn_data_template:
                if (mDataTemplateFragment == null) {
                    mDataTemplateFragment = new IoTDataTemplateFragment();
                    transaction.add(R.id.fragment_content, mDataTemplateFragment);
                } else {
                    transaction.show(mDataTemplateFragment);
                }
                break;
                
            case R.id.btn_demo:
                if (mLightFragment == null) {
                    mLightFragment = new IoTLightFragment();
                    transaction.add(R.id.fragment_content, mLightFragment);
                } else {
                    transaction.show(mLightFragment);
                }
                break;

            case R.id.btn_gateway:
                if (mGatewayFragment == null) {
                    mGatewayFragment = new IoTGatewayFragment();
                    transaction.add(R.id.fragment_content, mGatewayFragment);
                } else {
                    transaction.show(mGatewayFragment);
                }
                break;
        }
        // 事务提交
        transaction.commit();

    }

    /**
     * 关闭上一个Fragment中开启的mqtt连接
     *
     * @param id
     */
    private void closeMqttConnection(int id) {
        switch (id) {
            case R.id.btn_data_template:
                if (null != mDataTemplateFragment) {
                    mDataTemplateFragment.closeConnection();
                }
                break;
            case R.id.btn_demo:
                if (null != mLightFragment) {
                    mLightFragment.closeConnection();
                }
                break;
            case R.id.btn_gateway:
                if (null != mGatewayFragment) {
                    mGatewayFragment.closeConnection();
                }
                break;
            default:
                break;
        }
    }


    /**
     * 重置button状态
     */
    private void resetButton(int id) {
        switch (id) {
            case R.id.btn_data_template:
                btnDataTemplate.setBackgroundColor(Color.LTGRAY);
                btnDemo.setBackgroundColor(Color.WHITE);
                btnGateway.setBackgroundColor(Color.WHITE);
                break;
            case R.id.btn_demo:
                btnDemo.setBackgroundColor(Color.LTGRAY);
                btnDataTemplate.setBackgroundColor(Color.WHITE);
                btnGateway.setBackgroundColor(Color.WHITE);
                break;
            case R.id.btn_gateway:
                btnGateway.setBackgroundColor(Color.LTGRAY);
                btnDemo.setBackgroundColor(Color.WHITE);
                btnDataTemplate.setBackgroundColor(Color.WHITE);
                break;
        }
    }

    /**
     * 隐藏Fragment
     */
    private void hideFragments(FragmentTransaction transaction) {

        if (null != mDataTemplateFragment) {
            transaction.hide(mDataTemplateFragment);
        }
        if (null != mLightFragment) {
            transaction.hide(mLightFragment);
        }
        if (null != mGatewayFragment) {
            transaction.hide(mGatewayFragment);
        }
    }

    /**
     * 打印日志信息
     *
     * @param logInfo
     */
    protected void printLogInfo(final String tag, final String logInfo, final TextView textView, int logLevel) {
        switch (logLevel) {
            case TXLog.LEVEL_DEBUG:
                TXLog.d(tag, logInfo);
                break;

            case TXLog.LEVEL_INFO:
                TXLog.i(tag, logInfo);
                break;

            case TXLog.LEVEL_ERROR:
                TXLog.e(tag, logInfo);
                break;

            default:
                TXLog.d(tag, logInfo);
                break;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append("==> " + logInfo + "\n\n");
            }
        });
    }

    /**
     * 打印日志信息
     *
     * @param logInfo
     */
    protected void printLogInfo(final String tag, final String logInfo, final TextView textView) {
        printLogInfo(tag, logInfo, textView, TXLog.LEVEL_DEBUG);
    }

}
