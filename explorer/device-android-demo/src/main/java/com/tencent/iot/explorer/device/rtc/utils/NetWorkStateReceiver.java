package com.tencent.iot.explorer.device.rtc.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import com.tencent.iot.explorer.device.android.utils.TXLog;

import java.util.ArrayList;
import java.util.List;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;

public class NetWorkStateReceiver extends BroadcastReceiver {

    protected List<NetworkStateReceiverListener> listeners;
    protected Boolean connected;
    private static final String TAG = "NetworkStateReceiver";

    public NetWorkStateReceiver() {
        listeners = new ArrayList<>();
        connected = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        isConnected(context);
        notifyStateToAll();
    }  //After the onReceive() of the receiver class has finished, the Android system is allowed to recycle the receiver

    public boolean isConnected(Context context) {
        TXLog.i(TAG, "网络状态发生变化");
        //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {

            //获得ConnectivityManager对象
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            //获取ConnectivityManager对象对应的NetworkInfo对象
            //获取WIFI连接的信息
            NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(TYPE_WIFI);
            //获取移动数据连接的信息
            NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(TYPE_MOBILE);
            //获取以太网连接的信息
            NetworkInfo ethernetNetworkInfo = connMgr.getNetworkInfo(TYPE_ETHERNET);
            TXLog.i(TAG, String.format("WIFI是否连接:%b,以太网是否连接:%b,移动数据是否连接:%b,",
                    wifiNetworkInfo != null && wifiNetworkInfo.isConnected(),
                    ethernetNetworkInfo != null && ethernetNetworkInfo.isConnected(),
                    dataNetworkInfo != null && dataNetworkInfo.isConnected()));
            if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected()) {
                connected = true;
            } else if (ethernetNetworkInfo != null && ethernetNetworkInfo.isConnected()) {
                connected = true;
            } else if (dataNetworkInfo != null && dataNetworkInfo.isConnected()) {
                connected = true;
            } else {
                connected = false;
            }
            //API大于23时使用下面的方式进行网络监听
        } else {

            TXLog.i(TAG, "API level 大于23");
            //获得ConnectivityManager对象
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            //获取所有网络连接的信息
            Network[] networks = connMgr.getAllNetworks();
            //用于存放网络连接信息
            StringBuilder sb = new StringBuilder();
            //通过循环将网络信息逐个取出来
            for (int i=0; i < networks.length; i++){
                //获取ConnectivityManager对象对应的NetworkInfo对象
                NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
                sb.append(networkInfo.getTypeName() + " connect is " + networkInfo.isConnected());
            }
            if (networks.length == 0) {// 断网获取不到网络状态了
                connected = false;
                sb.append("no network connect~!");
            } else {
                connected = true;
            }
            TXLog.i(TAG, sb.toString());
        }
        return connected;
    }

    /**
     * Notify the state to all needed methods
     */
    private void notifyStateToAll() {
        TXLog.i(TAG, "Notifying state to " + listeners.size() + " listener(s)");
        for(NetworkStateReceiverListener eachNetworkStateReceiverListener : listeners)
            notifyState(eachNetworkStateReceiverListener);
    }

    /**
     * Notify the network state, triggering interface functions based on the current state
     * @param networkStateReceiverListener  Object which implements the NetworkStateReceiverListener interface
     */
    private void notifyState(NetworkStateReceiverListener networkStateReceiverListener) {
        if(connected == null || networkStateReceiverListener == null)
            return;

        if(connected == true) {
            // Triggering function on the interface towards network availability
            networkStateReceiverListener.networkAvailable();
        } else {
            // Triggering function on the interface towards network being unavailable
            networkStateReceiverListener.networkUnavailable();
        }
    }

    /**
     * Adds a listener to the list so that it will receive connection state change updates
     * @param networkStateReceiverListener     Object which implements the NetworkStateReceiverListener interface
     */
    public void addListener(NetworkStateReceiverListener networkStateReceiverListener) {
        TXLog.i(TAG, "addListener() - listeners.add(networkStateReceiverListener) + notifyState(networkStateReceiverListener);");
        listeners.add(networkStateReceiverListener);
        notifyState(networkStateReceiverListener);
    }

    /**
     * Removes listener (when no longer necessary) from the list so that it will no longer receive connection state change updates
     * @param networkStateReceiverListener     Object which implements the NetworkStateReceiverListener interface
     */
    public void removeListener(NetworkStateReceiverListener networkStateReceiverListener) {
        listeners.remove(networkStateReceiverListener);
    }

    /**
     * Inner Interface (i.e. to encapsulate behavior in a generic and re-usable way) which handles connection state changes for classes which registered this receiver (Outer class NetworkStateReceiver)
     * This interface implements the 'Strategy Pattern', where an execution strategy is evaluated and applied internally at runtime
     */
    public interface NetworkStateReceiverListener {
        /**
         * When the connection state is changed and there is a connection, this method is called
         */
        void networkAvailable();

        /**
         * Connection state is changed and there is not a connection, this method is called
         */
        void networkUnavailable();
    }
}
