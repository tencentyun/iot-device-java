package com.tencent.iot.hub.device.android.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;


public class IoTMainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 用于对Fragment进行管理
     */
    private FragmentManager fragmentManager;

    private IoTMqttFragment mMqttFragment;

    private IoTRemoteServiceFragment mRemoteServiceFragment;

    private IoTShadowFragment mShadowFragment;

    private IoTEntryFragment mEntryFragment;

    private Button btnMqtt;

    private Button btnShadow;

    private Button btnRemoteService;

    private Button btnEntry;

    private int mCurrentFragment = R.id.btn_basic_function;

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
        // 下面配置是为了让sdk中用log4j记录的日志可以输出至logcat
        LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "hub-demo.log");
        logConfigurator.configure();
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
        handlerButtonClick(R.id.btn_basic_function);
    }

    /**
     * 初始化组件
     */
    private void initComponent() {
        // 初始化控件
        btnShadow = (Button) findViewById(R.id.btn_shadow);
        btnRemoteService = (Button) findViewById(R.id.btn_remote_service);
        btnEntry = (Button) findViewById(R.id.btn_entry_demo);
        btnMqtt = (Button) findViewById(R.id.btn_basic_function);

        btnShadow.setOnClickListener(this);
        btnRemoteService.setOnClickListener(this);
        btnEntry.setOnClickListener(this);
        btnMqtt.setOnClickListener(this);

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

            case R.id.btn_basic_function:
                if (mMqttFragment == null) {
                    mMqttFragment = new IoTMqttFragment();
                    transaction.add(R.id.fragment_content, mMqttFragment);
                } else {
                    transaction.show(mMqttFragment);
                }
                break;

            case R.id.btn_shadow:

                if (mShadowFragment == null) {
                    mShadowFragment = new IoTShadowFragment();
                    transaction.add(R.id.fragment_content, mShadowFragment);
                } else {
                    transaction.show(mShadowFragment);
                }
                break;

            case R.id.btn_remote_service:

                if (mRemoteServiceFragment == null) {
                    mRemoteServiceFragment = new IoTRemoteServiceFragment();
                    transaction.add(R.id.fragment_content, mRemoteServiceFragment);
                } else {
                    transaction.show(mRemoteServiceFragment);
                }
                break;

            case R.id.btn_entry_demo:

                if (mEntryFragment == null) {
                    mEntryFragment = new IoTEntryFragment();
                    transaction.add(R.id.fragment_content, mEntryFragment);
                } else {
                    transaction.show(mEntryFragment);
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
            case R.id.btn_basic_function:
                if (null != mMqttFragment) {
                    mMqttFragment.closeConnection();
                }
                break;

            case R.id.btn_shadow:
                if (null != mShadowFragment) {
                    mShadowFragment.closeConnection();
                }
                break;

            case R.id.btn_remote_service:
                if (null != mRemoteServiceFragment) {
                    mRemoteServiceFragment.closeConnection();
                }
                break;

            case R.id.btn_entry_demo:
                if (null != mEntryFragment) {
                    mEntryFragment.closeConnection();
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
            case R.id.btn_basic_function:
                btnMqtt.setBackgroundColor(Color.LTGRAY);
                btnEntry.setBackgroundColor(Color.WHITE);
                btnShadow.setBackgroundColor(Color.WHITE);
                btnRemoteService.setBackgroundColor(Color.WHITE);
                break;

            case R.id.btn_entry_demo:
                btnEntry.setBackgroundColor(Color.LTGRAY);
                btnMqtt.setBackgroundColor(Color.WHITE);
                btnShadow.setBackgroundColor(Color.WHITE);
                btnRemoteService.setBackgroundColor(Color.WHITE);
                break;

            case R.id.btn_shadow:
                btnShadow.setBackgroundColor(Color.LTGRAY);
                btnMqtt.setBackgroundColor(Color.WHITE);
                btnEntry.setBackgroundColor(Color.WHITE);
                btnRemoteService.setBackgroundColor(Color.WHITE);
                break;

            case R.id.btn_remote_service:
                btnRemoteService.setBackgroundColor(Color.LTGRAY);
                btnMqtt.setBackgroundColor(Color.WHITE);
                btnShadow.setBackgroundColor(Color.WHITE);
                btnEntry.setBackgroundColor(Color.WHITE);
                break;
        }
    }

    /**
     * 隐藏Fragment
     */
    private void hideFragments(FragmentTransaction transaction) {

        if (null != mMqttFragment) {
            transaction.hide(mMqttFragment);
        }
        if (null != mRemoteServiceFragment) {
            transaction.hide(mRemoteServiceFragment);
        }
        if (null != mShadowFragment) {
            transaction.hide(mShadowFragment);
        }
        if (null != mEntryFragment) {
            transaction.hide(mEntryFragment);
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
