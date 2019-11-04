package com.qcloud.iot.samples;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qcloud.iot.R;
import com.qcloud.iot_explorer.utils.TXLog;


public class IoTMainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 用于对Fragment进行管理
     */
    private FragmentManager fragmentManager;

    private IoTDataTemplateFragment mDataTemplateFragment;
    private IoTLightFragment mLightFragment;

    private Button btnDataTemplate;
    private Button btnDemo;

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
            int permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        btnDemo.setOnClickListener(this);
        btnDataTemplate.setOnClickListener(this);

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
                break;
            case R.id.btn_demo:
                btnDemo.setBackgroundColor(Color.LTGRAY);
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
                textView.append(logInfo + "\n");
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
