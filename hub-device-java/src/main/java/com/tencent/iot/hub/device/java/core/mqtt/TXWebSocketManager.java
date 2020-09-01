package com.tencent.iot.hub.device.java.core.mqtt;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TXWebSocketManager {

    private String TAG = TXWebSocketManager.class.getSimpleName();

    private static TXWebSocketManager instance;

    private String defaultUriStr = ".iotcloud.tencentdevices.com:443";

    private String PREFIX = "wss://";

    private static Map<String, TXWebSocketClient> clients = new ConcurrentHashMap<>();

    private TXWebSocketManager() { }

    public synchronized static TXWebSocketManager getInstance() {
        if (instance == null) {
            instance = new TXWebSocketManager();
        }
        return instance;
    }

    public synchronized TXWebSocketClient getClientByProduct(String productId) {

        String uriStr = PREFIX + productId + defaultUriStr;
        if (clients.containsKey(uriStr) && clients.get(uriStr) != null) {
            // 集合内已经存在连接对象，不需要对连接对象做任何处理
        } else {    // 集合内不存在连接对象，新创建一个连接对象
            URI uri = URI.create(uriStr);
            TXWebSocketClient clientRet = new TXWebSocketClient(uri);
            clients.put(uriStr, clientRet);
        }
        return clients.get(uriStr);
    }

    public synchronized void releaseClient(String uriStr) {
        // 禁止移除默认的连接对象
        if (uriStr == null || uriStr.equals("")) {
            return;
        }

        // 移除对象默认关闭连接
        TXWebSocketClient clientRet = clients.remove(uriStr);
        if (clientRet.isConnected()) {
            clientRet.destroy();
        }
    }

}
