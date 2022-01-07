package com.tencent.iot.explorer.device.android.softAp;

import android.content.Context;
import android.util.Log;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SoftApConfigWiFi {
    private int PORT = 24; //hostpot热点的本地端口号为24，默认ip为"192.168.43.1"
    private TXMqttConnection connection = null;
    private SoftApConfigWiFiCallback mCallback;
    private static final String protoVersion = "2.0"; // 协议版本
    private Context mContext;
    private ServerSocketThread serverSocketThread = null;


    //创建一个DatagramSocket对象，并指定监听端口。（UDP使用DatagramSocket）
    private DatagramSocket socket;
    private InetAddress inetAddress;
    private Integer packetPort;
    private SoftApEntity softApEntity = null;

    public SoftApConfigWiFi(Context context, int PORT, TXMqttConnection connection, SoftApConfigWiFiCallback callback) {
        this.mContext = context;
        this.PORT = PORT;
        this.connection = connection;
        this.mCallback = callback;
    }

    public void startUdpConnect() {
        if (serverSocketThread == null) {
            serverSocketThread = new ServerSocketThread();
            serverSocketThread.start();
        }
    }

    public void stopUdpConnect() {
        if (serverSocketThread != null) {
            serverSocketThread.cancel();
            serverSocketThread = null;
        }
    }

    public Status appBindToken() {
        if (connection != null && softApEntity != null && connection.getConnectStatus() == TXMqttConstants.ConnectStatus.kConnected) {
            return connection.appBindToken(softApEntity.getToken());
        } else if (connection.getConnectStatus() != TXMqttConstants.ConnectStatus.kConnected) {
            return Status.MQTT_NO_CONN;
        } else if (softApEntity == null) {
            return Status.PARAMETER_INVALID;
        } else {
            return Status.ERROR;
        }
    }

    class ServerSocketThread extends Thread {
        private volatile boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        @Override
        public void run() {

            try {
                socket = new DatagramSocket(PORT);
                //创建一个byte类型的数组，用于存放接收到得数据
                byte data[] = new byte[4 * 1024];
                //创建一个DatagramPacket对象，并指定DatagramPacket对象的大小
                DatagramPacket packet = new DatagramPacket(data, data.length);
                while (!cancelled) {
                    //读取接收到得数据
                    socket.receive(packet);
                    //把客户端发送的数据转换为字符串。
                    //使用三个参数的String方法。参数一：数据包 参数二：起始位置 参数三：数据包长
                    String result = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    Log.d("WiFiActivity", "service result = " + result);
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.has("cmdType")) {
                        int cmdType = jsonObject.getInt("cmdType");
                        if (cmdType == 1 && softApEntity == null) { //第一步接收的指令包含ssid pwd token
                            softApEntity = new SoftApEntity(result);
                            inetAddress = packet.getAddress();
                            packetPort = packet.getPort();
                            sendUdpPacketWithDeviceInfo();
                        }
                    }
                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        /**
         * 通过 udp 报文发送 wifi 的账号/密码
         */
        private void sendUdpPacketWithDeviceInfo() {
            try {
                // {"cmdType":2,"productId":"OSPB5ASRWT","deviceName":"dev_01","protoVersion":"2.0"}
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("cmdType", 2);
                jsonObject.put("productId", connection.mProductId);
                jsonObject.put("deviceName", connection.mDeviceName);
                jsonObject.put("protoVersion", protoVersion);
                byte [] jsonStringByte = jsonObject.toString().getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(jsonStringByte, jsonStringByte.length, inetAddress, packetPort);
                socket.send(datagramPacket);
                mCallback.requestConnectWifi(softApEntity.getSsid(), softApEntity.getPassword());

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
