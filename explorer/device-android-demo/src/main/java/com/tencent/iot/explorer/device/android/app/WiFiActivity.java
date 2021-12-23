package com.tencent.iot.explorer.device.android.app;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tencent.iot.explorer.device.android.app.adapter.WifiListAdapter;
import com.tencent.iot.explorer.device.android.app.thread.ConnectThread;
import com.tencent.iot.explorer.device.android.app.thread.ListenerThread;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WiFiActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView listView;
    private Button btn_create_hostspot;
    private Button btn_close_hostspot;
    private Button btn_send;
    private Button btn_search;
    private TextView textview;
    private TextView text_state;

    private WifiManager wifiManager;
    private WifiListAdapter wifiListAdapter;
    private WifiConfiguration config;
    private int wcgID;

    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "TEST";
    /**
     * 端口号
     */
    private static final int PORT = 54321;

    private static final int WIFICIPHER_NOPASS = 1;
    private static final int WIFICIPHER_WEP = 2;
    private static final int WIFICIPHER_WPA = 3;

    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED = 2;//有设备连上热点
    public static final int SEND_MSG_SUCCSEE = 3;//发送消息成功
    public static final int SEND_MSG_ERROR = 4;//发送消息失败
    public static final int GET_MSG = 6;//获取新消息

    private final String[] locationPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION};

    /**
     * 连接线程
     */
    private ConnectThread connectThread;

    /**
     * 监听线程
     */
    private ListenerThread listenerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        initVIew();
        initBroadcastReceiver();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        listenerThread = new ListenerThread(PORT, handler);
        listenerThread.start();
    }

    private void initVIew() {
        listView = (ListView) findViewById(R.id.listView);
        btn_create_hostspot = (Button) findViewById(R.id.btn_create_hostspot);
        btn_close_hostspot = (Button) findViewById(R.id.btn_close_hostspot);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_search = (Button) findViewById(R.id.btn_search);
        textview = (TextView) findViewById(R.id.textview);
        text_state = (TextView) findViewById(R.id.text_state);

        btn_create_hostspot.setOnClickListener(this);
        btn_close_hostspot.setOnClickListener(this);
        btn_send.setOnClickListener(this);
        btn_search.setOnClickListener(this);

        wifiListAdapter = new WifiListAdapter(this, R.layout.wifi_list_item);
        listView.setAdapter(wifiListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                wifiManager.disconnect();
                final ScanResult scanResult = wifiListAdapter.getItem(position);
                String capabilities = scanResult.capabilities;
                int type = WIFICIPHER_WPA;
                if (!TextUtils.isEmpty(capabilities)) {
                    if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                        type = WIFICIPHER_WPA;
                    } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                        type = WIFICIPHER_WEP;
                    } else {
                        type = WIFICIPHER_NOPASS;
                    }
                }
                config = isExsits(scanResult.SSID);
                if (config == null) {
                    if (type != WIFICIPHER_NOPASS) {//需要密码
                        final EditText editText = new EditText(WiFiActivity.this);
                        final int finalType = type;
                        new AlertDialog.Builder(WiFiActivity.this).setTitle("请输入Wifi密码").setIcon(
                                android.R.drawable.ic_dialog_info).setView(
                                editText).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.w("AAA", "editText.getText():" + editText.getText());
                                config = createWifiInfo(scanResult.SSID, editText.getText().toString(), finalType);
                                connect(config);
                            }
                        })
                                .setNegativeButton("取消", null).show();
                        return;
                    } else {
                        config = createWifiInfo(scanResult.SSID, "", type);
                        connect(config);
                    }
                } else {
                    connect(config);
                }
            }
        });
    }

    private void connect(WifiConfiguration config) {
        text_state.setText("连接中...");
        wcgID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(wcgID, true);
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        registerReceiver(receiver, intentFilter);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_create_hostspot:
                createWifiHotspot();
                break;
            case R.id.btn_close_hostspot:
                closeWifiHotspot();
                break;
            case R.id.btn_send:
                if (connectThread != null) {
                    connectThread.sendData("这是来自Wifi热点的消息");
                } else {
                    Log.w("AAA", "connectThread == null");
                }
                break;
            case R.id.btn_search:
                search();
                break;
        }
    }

    /**
     * 创建Wifi热点
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createWifiHotspot() {

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        } else {
            // 申请权限后做的操作
        }

        if (wifiManager.isWifiEnabled()) {
            //如果wifi处于打开状态，则关闭wifi,
            wifiManager.setWifiEnabled(false);
        }
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WIFI_HOTSPOT_SSID;
        config.preSharedKey = "123456789";
        config.hiddenSSID = false;
        config.allowedAuthAlgorithms
                .set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        //通过反射调用设置热点
        try {
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManager, config, true);
            if (enable) {
                textview.setText("热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:123456789");
            } else {
                textview.setText("创建热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            textview.setText("创建热点失败");
        }
    }

    /**
     * 关闭WiFi热点
     */
    public void closeWifiHotspot() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
            Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(wifiManager, config, false);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        textview.setText("热点已关闭");
        text_state.setText("wifi已关闭");
    }

    /**
     * 获取连接到热点上的手机ip
     *
     * @return
     */
    private ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    /**
     * 搜索wifi热点
     */
    private void search() {
        if (!wifiManager.isWifiEnabled()) {
            //开启wifi
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    connectThread = new ConnectThread(listenerThread.getSocket(), handler);
                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    textview.setText("设备连接成功");
                    break;
                case SEND_MSG_SUCCSEE:
                    textview.setText("发送消息成功:" + msg.getData().getString("MSG"));
                    break;
                case SEND_MSG_ERROR:
                    textview.setText("发送消息失败:" + msg.getData().getString("MSG"));
                    break;
                case GET_MSG:
                    textview.setText("收到消息:" + msg.getData().getString("MSG"));
                    break;
            }
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.w("BBB", "SCAN_RESULTS_AVAILABLE_ACTION");
                // wifi已成功扫描到可用wifi。
                List<ScanResult> scanResults = wifiManager.getScanResults();
                wifiListAdapter.clear();
                wifiListAdapter.addAll(scanResults);
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                Log.w("BBB", "WifiManager.WIFI_STATE_CHANGED_ACTION");
                int wifiState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        //获取到wifi开启的广播时，开始扫描
                        wifiManager.startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        //wifi关闭发出的广播
                        break;
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.w("BBB", "WifiManager.NETWORK_STATE_CHANGED_ACTION");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    text_state.setText("连接已断开");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    text_state.setText("已连接到网络:" + wifiInfo.getSSID());
                    Log.w("AAA", "wifiInfo.getSSID():" + wifiInfo.getSSID() + "  WIFI_HOTSPOT_SSID:" + WIFI_HOTSPOT_SSID);
                    if (wifiInfo.getSSID().equals(WIFI_HOTSPOT_SSID)) {
                        //如果当前连接到的wifi是热点,则开启连接线程
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ArrayList<String> connectedIP = getConnectedIP();
                                    for (String ip : connectedIP) {
                                        if (ip.contains(".")) {
                                            Log.w("AAA", "IP:" + ip);
                                            Socket socket = new Socket(ip, PORT);
                                            connectThread = new ConnectThread(socket, handler);
                                            connectThread.start();
                                        }
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == state.CONNECTING) {
                        text_state.setText("连接中...");
                    } else if (state == state.AUTHENTICATING) {
                        text_state.setText("正在验证身份信息...");
                    } else if (state == state.OBTAINING_IPADDR) {
                        text_state.setText("正在获取IP地址...");
                    } else if (state == state.FAILED) {
                        text_state.setText("连接失败");
                    }
                }

            }
           /* else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                    text_state.setText("连接已断开");
                    wifiManager.removeNetwork(wcgID);
                } else {
                    WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    text_state.setText("已连接到网络:" + wifiInfo.getSSID());
                }
            }*/
        }
    };

    /**
     * 判断当前wifi是否有保存
     *
     * @param SSID
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private WifiConfiguration isExsits(String SSID) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(locationPermissions, 102);
            return null;
        }
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    public WifiConfiguration createWifiInfo(String SSID, String password,
                                            int type) {
        Log.w("AAA", "SSID = " + SSID + "password " + password + "type ="
                + type);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement
                    .set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            return null;
        }
        return config;
    }

}
