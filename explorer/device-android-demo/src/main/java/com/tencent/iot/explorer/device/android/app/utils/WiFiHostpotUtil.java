package com.tencent.iot.explorer.device.android.app.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import com.tencent.iot.explorer.device.android.utils.TXLog;

import java.lang.reflect.Method;

public class WiFiHostpotUtil {
    public static String TAG = WiFiHostpotUtil.class.getSimpleName();

    private Context mContext;
    private WifiManager wifiManager;
    private WifiManager.LocalOnlyHotspotReservation localOnlyHotspotReservation;
    private Handler handler = new Handler();

    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public WiFiHostpotUtil(Context context) {
        this.mContext = context;
        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    }
    /**
     * 创建Wifi热点
     */
    public void createWifiHotspot(String hostpotSsid, String password, CreateWifiHotspotCallback createWifiHotspotCallback) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(mContext)) {// 申请ACTION_MANAGE_WRITE_SETTINGS权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                if (createWifiHotspotCallback != null) {
                    createWifiHotspotCallback.onCreateWifiHotspotFailure("-1001", "未申请ACTION_MANAGE_WRITE_SETTINGS权限", new Exception("未申请ACTION_MANAGE_WRITE_SETTINGS权限"));
                }
                return ;
            } else {
                // 申请权限后做的操作
            }
        } else { //不支持android6.0以下使用
            if (createWifiHotspotCallback != null) {
                createWifiHotspotCallback.onCreateWifiHotspotFailure("-1002", "暂不支持android6.0以下使用hotspot", new Exception("暂不支持android6.0以下使用hotspot"));
            }
            return ;
        }

        if (wifiManager.isWifiEnabled()) {
            //如果wifi处于打开状态，则关闭wifi,
            wifiManager.setWifiEnabled(false);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //8.0以上 startLocalOnlyHotspot创建热点

            //日志功能开启写权限
            try {
                for (String ele: PERMISSIONS_LOCATION) {
                    int granted = ActivityCompat.checkSelfPermission(mContext, ele);
                    if (granted != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS_LOCATION, 102);
                        if (createWifiHotspotCallback != null) {
                            createWifiHotspotCallback.onCreateWifiHotspotFailure("-1003", "未获取到ACCESS_FINE_LOCATION权限", new Exception("未获取到ACCESS_FINE_LOCATION权限"));
                        }
                        return ;
                    }
                }
            } catch (Exception e) {
                if (createWifiHotspotCallback != null) {
                    createWifiHotspotCallback.onCreateWifiHotspotFailure("-1004", "获取到ACCESS_FINE_LOCATION权限失败", e);
                }
                e.printStackTrace();
            }

            try {
                wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                    @TargetApi(Build.VERSION_CODES.O)
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        super.onStarted(reservation);
                        localOnlyHotspotReservation = reservation;
                        WifiConfiguration wifiConfiguration = reservation.getWifiConfiguration();
                        String ssid = wifiConfiguration.SSID;
                        String pwd = wifiConfiguration.preSharedKey;
                        if (createWifiHotspotCallback != null) {
                            createWifiHotspotCallback.onCreateWifiHotspotSuccess(ssid, pwd);
                        }
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        if (createWifiHotspotCallback != null) {
                            createWifiHotspotCallback.onCreateWifiHotspotFailure("-1005", "Android8.0以上停止热点", new Exception("Android8.0以上停止热点"));
                        }
                    }

                    @Override
                    public void onFailed(int reason) {
                        super.onFailed(reason);
                        if (createWifiHotspotCallback != null) {
                            createWifiHotspotCallback.onCreateWifiHotspotFailure("-1006", "Android8.0以上开启热点失败"+reason, new Exception("Android8.0开启热点失败"+reason));
                        }
                    }

                }, handler);
            } catch (Exception e) {
                e.printStackTrace();
                TXLog.e(TAG, "开启LocalOnlyHotspot热点失败");
                if (e.getMessage().equals("Caller already has an active LocalOnlyHotspot request")) {
                    if (createWifiHotspotCallback != null) {
                        createWifiHotspotCallback.onCreateWifiHotspotFailure("-1009", "已开启了LocalOnlyHotspot热点", e);
                    }
                } else {
                    if (createWifiHotspotCallback != null) {
                        createWifiHotspotCallback.onCreateWifiHotspotFailure("-1010", "开启LocalOnlyHotspot热点失败", e);
                    }
                }
            }

        } else { //8.0以下
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = hostpotSsid;
            config.preSharedKey = password;
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
                    TXLog.d(TAG, "热点已开启 SSID:" + hostpotSsid + " password:" + password);
                    if (createWifiHotspotCallback != null) {
                        createWifiHotspotCallback.onCreateWifiHotspotSuccess(hostpotSsid, password);
                    }
                    return ;
                } else {
                    TXLog.e(TAG, "创建热点失败");
                    if (createWifiHotspotCallback != null) {
                        createWifiHotspotCallback.onCreateWifiHotspotFailure("-1007", "Android8.0以下开启热点失败，热点不可用", new Exception("Android8.0以下开启热点失败，热点不可用"));
                    }
                    return ;
                }
            } catch (Exception e) {
                e.printStackTrace();
                TXLog.e(TAG, "创建热点失败");
                if (createWifiHotspotCallback != null) {
                    createWifiHotspotCallback.onCreateWifiHotspotFailure("-1008", "Android8.0以下开启热点失败", e);
                }
                return;
            }
        }
    }

    /**
     * 关闭WiFi热点
     */
    public void closeWifiHotspot(CloseWifiHotspotCallback closeWifiHotspotCallback) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //8.0以上 LocalOnlyHotspotReservation创建热点
            if (localOnlyHotspotReservation != null) {
                localOnlyHotspotReservation.close();
                localOnlyHotspotReservation = null;
                TXLog.d(TAG, "Turned off hotspot");
            }

        } else {
            try {
                Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
                Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(wifiManager, config, false);
            } catch (Exception e) {
                e.printStackTrace();
                if (closeWifiHotspotCallback != null) {
                    closeWifiHotspotCallback.onCloseWifiHotspotFailure(e.toString(), e);
                }
                return;
            }
            if (closeWifiHotspotCallback != null) {
                closeWifiHotspotCallback.onCloseWifiHotspotSuccess();
            }
        }

    }
}
