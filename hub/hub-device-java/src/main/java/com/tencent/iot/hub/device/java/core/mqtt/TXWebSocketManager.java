package com.tencent.iot.hub.device.java.core.mqtt;


import org.eclipse.paho.client.mqttv3.MqttException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TXWebSocketManager {

    private String TAG = TXWebSocketManager.class.getSimpleName();

    private static TXWebSocketManager instance;

    private String defaultUriStr = ".iotcloud.tencentdevices.com:443";

    private String WSS_PREFIX = "wss://";

    private String WS_PREFIX = "ws://";

    private static Map<String, TXWebSocketClient> clients = new ConcurrentHashMap<>();

    private TXWebSocketManager() { }

    public synchronized static TXWebSocketManager getInstance() {
        if (instance == null) {
            instance = new TXWebSocketManager();
        }
        return instance;
    }

    public synchronized TXWebSocketClient getClient(String wsUrl, String productId, String devicename, String secretKey) {
        if (isEmpty(productId) || isEmpty(devicename)) {
            System.out.println("productId or devicename empty");
            return null;
        }

        String clientId = productId + devicename;

        if (clients.containsKey(clientId) && clients.get(clientId) != null) {
            // 集合内已经存在连接对象，不需要对连接对象做任何处理
        } else {    // 集合内不存在连接对象，新创建一个连接对象
            try {
                if (wsUrl == null || wsUrl.length() == 0) {
//                    if (secretKey != null && secretKey.length() != 0) {
//                        wsUrl = WS_PREFIX + productId + defaultUriStr;
//                    } else {
                        wsUrl = WSS_PREFIX + productId + defaultUriStr;
//                    }
                }
                TXWebSocketClient client = new TXWebSocketClient(wsUrl, clientId, secretKey);
                clients.put(clientId, client);
            } catch (MqttException e) {
                e.printStackTrace();
                System.out.println("e=" + e.toString());
            }

        }
        return clients.get(clientId);
    }

    public synchronized TXWebSocketClient getClient(String productId, String devicename, String secretKey) {
        return getClient(null, productId, devicename, secretKey);
    }

    public synchronized void releaseClient(String productId, String devicename) {

        // 移除对象默认关闭连接
        String clientId = productId + devicename;
        TXWebSocketClient clientRet = clients.remove(clientId);

        // 不处于断开连接状态的对象需要被销毁
        if (clientRet.getConnectionState() != ConnectionState.DISCONNECTED &&
                clientRet.getConnectionState() != ConnectionState.DISCONNECTING) {
            try {
                clientRet.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isEmpty(String src) {
        if (src == null || src.equals("")) {
            return true;
        }

        return false;
    }

}
