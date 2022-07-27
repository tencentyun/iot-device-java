package com.tencent.iot.hub.device.java.core.mqtt;


import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket 连接管理器
 */
public class TXWebSocketManager {

    private String TAG = TXWebSocketManager.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TXWebSocketManager.class);
    static { Loggor.setLogger(logger); }

    private static TXWebSocketManager instance;

    private String defaultUriStr = ".ap-guangzhou.iothub.tencentdevices.com";

    private String WSS_PREFIX = "wss://";

    private String WS_PREFIX = "ws://";

    private String WSS_PORT = ":443";

    private String WS_PORT = ":80";

    private static Map<String, TXWebSocketClient> clients = new ConcurrentHashMap<>();

    private TXWebSocketManager() { }

    /**
     * 获取单例
     *
     * @return 单例实体
     */
    public synchronized static TXWebSocketManager getInstance() {
        if (instance == null) {
            instance = new TXWebSocketManager();
        }
        return instance;
    }

    /**
     * 获取连接对象
     *
     * @param wsUrl 服务器 URL
     * @param productId 产品 ID
     * @param devicename 设备名
     * @param secretKey 密钥认证设备该参数为 设备密钥  证书认证设备该参数为 设备私钥
     * @return 连接对象 {@link TXWebSocketClient}
     */
    public synchronized TXWebSocketClient getClient(String wsUrl, String productId, String devicename, String secretKey) {
        if (isEmpty(productId) || isEmpty(devicename)) {
            Loggor.error(TAG, "productId or devicename empty");
            return null;
        }

        String clientId = productId + devicename;

        if (clients.containsKey(clientId) && clients.get(clientId) != null) {
            // 集合内已经存在连接对象，不需要对连接对象做任何处理
        } else {    // 集合内不存在连接对象，新创建一个连接对象
            try {
                if (wsUrl == null || wsUrl.length() == 0) {
                    if (getIsPskDevice(secretKey)) {
                        wsUrl = WS_PREFIX + productId + defaultUriStr + WS_PORT;
                    } else {
                        wsUrl = WSS_PREFIX + productId + defaultUriStr + WSS_PORT;
                    }
                }
                TXWebSocketClient client = new TXWebSocketClient(wsUrl, clientId, secretKey);
                clients.put(clientId, client);
            } catch (MqttException e) {
                e.printStackTrace();
                Loggor.error(TAG, "e=" + e.toString());
            }

        }
        return clients.get(clientId);
    }

    private boolean getIsPskDevice(String secretKey) {
        if (secretKey != null && secretKey.length() != 0) {
            return !secretKey.contains("BEGIN PRIVATE KEY");
        }
        return false;
    }

    /**
     * 获取连接对象
     *
     * @param productId 产品 ID
     * @param devicename 设备名
     * @param secretKey 密钥认证设备该参数为 设备密钥  证书认证设备该参数为 设备私钥
     * @return 连接对象 {@link TXWebSocketClient}
     */
    public synchronized TXWebSocketClient getClient(String productId, String devicename, String secretKey) {
        return getClient(null, productId, devicename, secretKey);
    }

    /**
     * 释放连接对象
     *
     * @param productId 产品 ID
     * @param devicename 设备名
     */
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
