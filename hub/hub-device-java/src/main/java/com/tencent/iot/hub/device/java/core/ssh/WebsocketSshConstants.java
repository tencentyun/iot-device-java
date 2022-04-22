package com.tencent.iot.hub.device.java.core.ssh;

public interface WebsocketSshConstants {

    /**
     * 腾讯云 ssh websocket 认证唯一连接地址前缀
     */
    String SSH_PREFIX = "ws://";

    /**
     * 腾讯云 ssh websocket 连接地址 url
     */
    String SSH_WS_SERVER_URL = "ap-guangzhou.gateway.tencentdevices.com";

    /**
     * 腾讯云 ssh websocket 连接地址 path
     */
    String REMOTE_WS_SSH_PATH = "/ssh/device";

}
