package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TXWebSocketManager {

    private String TAG = TXWebSocketManager.class.getSimpleName();

    private static TXWebSocketManager instance;

    private String defaultUriStr = ".iotcloud.tencentdevices.com:443";

    private String PREFIX = "wss://";

    private static Map<String, TXWebSocketClient> clients = new ConcurrentHashMap<>();

    public static String path;

    private TXWebSocketManager() { }

    public synchronized static TXWebSocketManager getInstance() {
        if (instance == null) {
            instance = new TXWebSocketManager();
        }
        return instance;
    }

    public synchronized TXWebSocketClient getClient(String productId, String devicename) {
        if (isEmpty(productId) || isEmpty(devicename)) {
            System.out.println("productId or devicename empty");
            return null;
        }

        String clientId = productId + devicename;

        String uriStr = PREFIX + productId + defaultUriStr;
        if (clients.containsKey(clientId) && clients.get(clientId) != null) {
            // 集合内已经存在连接对象，不需要对连接对象做任何处理
        } else {    // 集合内不存在连接对象，新创建一个连接对象
            TXWebSocketClient clientRet = null;
            try {
                System.out.println("serverURI=" + uriStr);
                System.out.println("clientId=" + clientId);
                System.out.println("path=" + path);
                clientRet = new TXWebSocketClient(uriStr, clientId, path);
                clients.put(clientId, clientRet);
            } catch (MqttException e) {
                e.printStackTrace();
                System.out.println("e=" + e.toString());
            }

        }
        return clients.get(clientId);
    }

    public synchronized void releaseClient(String productId, String devicename) {
        // 禁止移除默认的连接对象
        if (isEmpty(productId) || isEmpty(devicename)) {
            return;
        }

        // 移除对象默认关闭连接
        String clientId = productId + devicename;
        TXWebSocketClient clientRet = clients.remove(clientId);
        if (clientRet.isConnected()) {
            clientRet.destroy();
        }
    }

    private boolean isEmpty(String src) {
        if (src == null || src.equals("")) {
            return true;
        }

        return false;
    }

}
