package com.tencent.iot.explorer.device.android.app;

import android.Manifest;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.iot.explorer.device.android.app.utils.Watermark;
import com.tencent.iot.explorer.device.broadcast.BroadCastMainActivity;
import com.tencent.iot.explorer.device.central.CentralMainActivity;
import com.tencent.iot.explorer.device.face.AuthActivity;
import com.tencent.iot.explorer.device.rtc.TRTCMainActivity;
import com.tencent.iot.explorer.device.video.VideoEntryActivity;
import com.tencent.xnet.XP2P;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class IoTDemoActivity extends AppCompatActivity {

    private Button mIoTDemoBtn;
    private Button mIoTAiFaceBtn;
    private Button mIoTTrtcBtn;
    private Button mIoTTmeBtn;
    private Button mIoTVideoBtn;
    private Button mIoTWiFiBtn;
    private Button mIoTCentralBtn;
    private Button mIoTBroadCastBtn;

    private static final String TAG = IoTDemoActivity.class.getSimpleName();

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
    };

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iot_demo);

        //日志功能开启写权限
        try {
            for (String ele: PERMISSIONS_STORAGE) {
                int granted = ActivityCompat.checkSelfPermission(this, ele);
                if (granted != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                    break;
                } else {
                    initLogConfigurator();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mIoTDemoBtn = findViewById(R.id.iot_demo);
        mIoTAiFaceBtn = findViewById(R.id.iot_ai_face);
        mIoTTrtcBtn = findViewById(R.id.iot_trtc);
        mIoTTmeBtn = findViewById(R.id.iot_tme);
        mIoTVideoBtn = findViewById(R.id.btn_iot_video);
        mIoTWiFiBtn = findViewById(R.id.btn_wifi);
        mIoTCentralBtn = findViewById(R.id.btn_central);
        mIoTBroadCastBtn = findViewById(R.id.btn_broadcast);

        mIoTVideoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(IoTDemoActivity.this, VideoEntryActivity.class);
            startActivity(intent);
        });

        mIoTDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, IoTMainActivity.class));
            }
        });

        mIoTAiFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, AuthActivity.class));
            }
        });

        mIoTTrtcBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, TRTCMainActivity.class));
            }
        });

        mIoTTmeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Class c = Class.forName("com.tencent.iot.explorer.device.tme.TmeConfigActivity");
                    startActivity(new Intent(IoTDemoActivity.this, c));
                } catch (ClassNotFoundException e) {
                    Toast.makeText(IoTDemoActivity.this, "请线下联系", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        mIoTWiFiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IoTDemoActivity.this, WiFiActivity.class));
            }
        });
        mIoTCentralBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IoTDemoActivity.this, CentralMainActivity.class));
            }
        });
        mIoTBroadCastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IoTDemoActivity.this, BroadCastMainActivity.class));
            }
        });
        Watermark.getInstance(this).show(this, BuildConfig.SDKDemoCommitID + "    xp2p: " + XP2P.getVersion());
    }

    private void initLogConfigurator() {
        // 下面配置是为了让sdk中用log4j记录的日志可以输出至logcat
        LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "explorer-demo.log");
        logConfigurator.configure();
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
                    initLogConfigurator();
                    break;
                }
            }
        }
    }
}