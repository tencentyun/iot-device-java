package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

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

    public synchronized TXWebSocketClient getClientByProduct(String productId, String devicename) {
        if (isEmpty(productId) || isEmpty(devicename)) {
            System.out.println("productId or devicename empty");
            return null;
        }

        String clientId = productId + devicename;

        String uriStr = PREFIX + productId + defaultUriStr;
        if (clients.containsKey(clientId) && clients.get(clientId) != null) {
            // 集合内已经存在连接对象，不需要对连接对象做任何处理
        } else {    // 集合内不存在连接对象，新创建一个连接对象
            System.out.println("uriStr=" + uriStr);
            URI uri = URI.create(uriStr);
            TXWebSocketClient clientRet = new TXWebSocketClient(uri);
            clients.put(clientId, clientRet);
        }
        return clients.get(clientId);
    }

    private String generateOptionInfo(String clientId, boolean automicReconnect, String secretKey) {

        Long timestamp;
        if (automicReconnect) {
            timestamp = (long) Integer.MAX_VALUE;
        } else {
            timestamp = System.currentTimeMillis() / 1000 + 600;
        }

        String userNameStr = clientId + ";" + TXMqttConstants.APPID + ";" + getConnectId() + ";" + timestamp;

        if (secretKey != null) {
            try {
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(),
                        Base64.decode(secretKey, Base64.DEFAULT)) + ";hmacsha256";
            } catch (IllegalArgumentException e) {
                System.out.println("Failed to set password");
            }
        }
        return null;
    }

    protected String getConnectId() {
        StringBuffer connectId = new StringBuffer();
        for (int i = 0; i < TXMqttConstants.MAX_CONN_ID_LEN; i++) {
            int flag = (int) (Math.random() * Integer.MAX_VALUE) % 3;
            int randNum = (int) (Math.random() * Integer.MAX_VALUE);
            switch (flag) {
                case 0:
                    connectId.append((char) (randNum % 26 + 'a'));
                    break;
                case 1:
                    connectId.append((char) (randNum % 26 + 'A'));
                    break;
                case 2:
                    connectId.append((char) (randNum % 10 + '0'));
                    break;
            }
        }

        return connectId.toString();
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
