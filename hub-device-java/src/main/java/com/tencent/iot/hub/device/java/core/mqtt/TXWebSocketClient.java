package com.tencent.iot.hub.device.java.core.mqtt;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TXWebSocketClient extends WebSocketClient {

    private String productId = "";

    // 开始连接和未连接两种状态
    private volatile boolean isConnected = false;
    private Thread keepLiveThread = null;
    private boolean heartBack = true;
    private long delayMillis = 60 * 1000L;      // 固定的心跳间隔一分钟
    private TXWebSocketActionCallback connectListener;
    private String heartBeatParam = "{\"action\":\"Hello\",\"reqId\":-1}";

    TXWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    public void setTXWebSocketActionCallback(TXWebSocketActionCallback connectListener) {
        this.connectListener = connectListener;
    }

    public TXWebSocketActionCallback getTXWebSocketActionCallback() {
        return this.connectListener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("onOpen");
    }

    @Override
    public void connect() {
        super.connect();
    }

    @Override
    public void onMessage(String message) {
        System.out.println("onMessage message " + message);
        // ping 的心跳包返回数据后，进行下一次心跳
        isConnected = true;   // 连接成功后认为处于连接状态，除非主动断开连接
        heartBack = true;   // 前一次的 ping 的心跳得到响应
        keepLive();  // 能收到消息的响应就进行心跳保活
        connected();
    }

    private void connected() {
        if (connectListener != null) {
            connectListener.onConnected();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("onClose code " + code + ", reason " + reason + ", remote " + remote);
        disconnect(remote);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("onError ex " + ex);
    }

    // 主动断开连接
    public synchronized void destroy() {
        isConnected = false;
        removeKeepLive();  // 结束连接的时候，中止保活线程
        this.close();
    }

    // 获取连接状态 true:上线 false:掉线
    public boolean isConnected() {
        return isConnected;
    }

    private synchronized void removeKeepLive() {
        // 中止保活线程
        if (keepLiveThread != null) {
            keepLiveThread.interrupt();
            keepLiveThread = null;
        }
    }

    private synchronized void keepLive() {
        if (keepLiveThread == null) {
            keepLiveThread = new Thread(new KeepLiveRunnable());
            keepLiveThread.start();
        }
    }

    private void disconnect(boolean remote) {
        if (connectListener != null) {
            connectListener.onDisconnect(remote);
        }
    }

    // 发送心跳包
    private void heartBeat() {
        this.send(heartBeatParam);
    }

    class KeepLiveRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (isConnected) {   // 处于连接状态，就一直维护心跳
                    if (!heartBack) {
                        disconnect(true);  // 心跳失败认为是远端失败
                    }
                    heartBack = false;  // 下一次的 ping 的响应会修改本次的状态
                    heartBeat();
                    Thread.sleep(delayMillis);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
