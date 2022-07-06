package com.tencent.iot.explorer.device.rtc.utils;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

public class WifiUtils {

    private static final String TAG = WifiUtils.class.getSimpleName();

    /**
     * 创建 WifiConfiguration，这里创建的是wpa2加密方式的wifi
     *
     * @param ssid     wifi账号
     * @param password wifi密码
     * @return
     */
    public static WifiConfiguration createWifiInfo(String ssid, String password) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";
        if(TextUtils.isEmpty(password)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            Log.i(TAG, "password is ''");
            return config;
        }
        config.preSharedKey = "\"" + password + "\"";
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.status = WifiConfiguration.Status.ENABLED;
        return config;
    }

    public static boolean connectWifiApByName(Context context, String wifiApName, String password) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiConfiguration wifiNewConfiguration = createWifiInfo(wifiApName, password);//使用wpa2的wifi加密方式
        int newNetworkId = mWifiManager.addNetwork(wifiNewConfiguration);
        if (newNetworkId == -1) {
            Log.e(TAG, "操作失败,需要您到手机wifi列表中取消对设备连接的保存");
            return false;
        }
        Log.i(TAG, "newNetworkId is:" + newNetworkId);
        // 如果wifi权限没打开（1、先打开wifi，2，使用指定的wifi
        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
        boolean enableNetwork = mWifiManager.enableNetwork(newNetworkId, true);
        if (!enableNetwork) {
            Log.e(TAG, "切换到指定wifi失败");
            return false;
        } else {
            Log.e(TAG, "切换到指定wifi成功");
            return true;
        }
    }


    public interface WifiConnectCallBack {
        void connectResult(boolean connectResult);
    }

    /**
     * 通过热点用户名和密码连接热点   (因为Android10以上系统限制无法成功连接wifi，故app build.gradle的targetSdkVersion和compileSdkVersion改成了28，使用旧的API可以通过代码连接wifi)
     * @param context    context
     * @param wifiApName wifi账号
     * @param password   wifi密码
     * @param callBack   回调callback
     */
    public static void connectWifiApByNameAndPwd(Context context, String wifiApName, String password, WifiConnectCallBack callBack) {
        if (context == null || callBack == null) {
            Log.i(TAG, "context == null || callBack == null");
            return;
        }
        WifiManager mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiConfiguration wifiNewConfiguration = createWifiInfo(wifiApName, password);//使用wpa2的wifi加密方式
        int newNetworkId = mWifiManager.addNetwork(wifiNewConfiguration);
        if (newNetworkId == -1) {
            Log.i(TAG, "操作失败,需要您到手机wifi列表中取消对设备连接的保存");
            callBack.connectResult(false);
            return;
        }
        Log.i(TAG, "newNetworkId is:" + newNetworkId);
        // 如果wifi权限没打开（1、先打开wifi，2，使用指定的wifi
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        boolean enableNetwork = mWifiManager.enableNetwork(newNetworkId, true);
        if (!enableNetwork) {
            Log.i(TAG, "切换到指定wifi失败");
            callBack.connectResult(false);
            return;
        }
        Log.i(TAG, "切换到指定wifi成功");
        callBack.connectResult(true);
    }
}
