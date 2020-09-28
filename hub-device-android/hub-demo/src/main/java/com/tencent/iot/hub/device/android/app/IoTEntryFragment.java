package com.tencent.iot.hub.device.android.app;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.android.app.scenarized.Airconditioner;
import com.tencent.iot.hub.device.android.app.scenarized.Door;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.atomic.AtomicInteger;

public class IoTEntryFragment extends Fragment {

    private static final String TAG = "IoTEntryActivity";

    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    private IoTMainActivity mParent;

    private Door mDoor;

    private Airconditioner mAir;

    private Button enterButton, outButton;

    private TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_iot_entry, container, false);

        mParent = (IoTMainActivity) this.getActivity();

        mDoor = new Door(IoTEntryFragment.this.getContext());
        mAir = new Airconditioner(this.getContext(), new AirMqttActionCallBack());

        enterButton = view.findViewById(R.id.button);
        outButton = view.findViewById(R.id.button2);

        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDoor.enterRoom();
            }
        });

        outButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDoor.leaveRoom();
            }
        });

        textView = view.findViewById(R.id.textView);

        return view;
    }

    public void closeConnection() {
        mAir.closeConnection();
        mDoor.closeConnection();
    }

    private class AirMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            TXLog.i(TAG, msg);

            if (status.equals(Status.OK)) {
                TXLog.i(TAG, msg);
                mAir.subScribeTopic();
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String logInfo = String.format("onDisconnectCompleted, status[%s], msg[%s]", status.name(), msg);
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
            String logInfo = String.format("onSubscribeCompleted, status[%s], message[%s]", status.name(), msg);
            if (Status.ERROR == status) {
                TXLog.e(TAG, logInfo);
            } else {
                TXLog.i(TAG, logInfo);
            }
        }

        @Override
        public void onMessageReceived(String topic, MqttMessage message) {
            String logInfo;
            if (message.toString().contains("come_home")) {
                logInfo = "receive command: open airconditioner, count: " + atomicInteger.getAndIncrement();
            } else {
                logInfo = "receive command: close airconditioner, count: " + atomicInteger.getAndIncrement();
            }
            mParent.printLogInfo(TAG, logInfo, textView);
        }
    }

}
