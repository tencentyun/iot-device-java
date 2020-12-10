package com.tencent.iot.hub.device.android.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tencent.iot.hub.device.android.app.mqtt.MQTTRequest;
import com.tencent.iot.hub.device.android.app.shadow.ShadowSample;
import com.tencent.iot.hub.device.android.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IoTShadowFragment extends Fragment {

    private static final String TAG = IoTShadowFragment.class.getSimpleName();

    private IoTMainActivity mParent;

    private Button mConnectBtn;

    private Button mDocumentBtn;

    private Button mRegisterPropertyBtn;

    private Button mCloseConnectBtn;

    private Button mLoopBtn;

    //Add by fancyxu
    private Button mSubScribeBtn;

    private Button mUnSubscribeBtn;

    private Button mPublishBtn;

    private TextView mLogInfoText;

    private ShadowSample mShadowSample;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_iot_shadow, container, false);
        init(view);
        return view;
    }

    public void closeConnection() {
        mShadowSample.closeConnect();
    }


    /**
     * 初始化
     */
    private void init(final View view) {
        mParent = (IoTMainActivity) this.getActivity();
        mConnectBtn = view.findViewById(R.id.connect);
        mDocumentBtn = view.findViewById(R.id.get_device_document);
        mRegisterPropertyBtn = view.findViewById(R.id.register_property);
        mCloseConnectBtn = view.findViewById(R.id.close_connect);
        mLoopBtn = view.findViewById(R.id.loop);
        mLogInfoText = view.findViewById(R.id.log_info);

        //Add by fancyxu
        mSubScribeBtn = view.findViewById(R.id.subscribe_topic);
        mUnSubscribeBtn = view.findViewById(R.id.unSubscribe_topic);
        mPublishBtn = view.findViewById(R.id.publish_topic);

        mShadowSample = new ShadowSample(this, new ShadowActionCallBack());

        mDocumentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShadowSample.getDeviceDocument();
            }
        });

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShadowSample.connect();
            }
        });

        mRegisterPropertyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShadowSample.registerProperty();
            }
        });

        mCloseConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShadowSample.closeConnect();
            }
        });

        mLoopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShadowSample.loop();
            }
        });

        //Add by fancyxu
        mSubScribeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 在腾讯云控制台增加自定义主题（权限为订阅和发布）：custom_data，用于接收IoT服务端转发的自定义数据。
                // 本例中，发布的自定义数据，IoT服务端会在发给当前设备。
                mShadowSample.subscribeTopic(BuildConfig.SHADOW_TEST_TOPIC);
            }
        });

        mUnSubscribeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShadowSample.unSubscribeTopic(BuildConfig.SHADOW_TEST_TOPIC);
            }
        });

        mPublishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 要发布的数据
                Map<String, String> data = new HashMap<String, String>();
                // 车辆类型
                data.put("car_type", "suv");
                // 车辆油耗
                data.put("oil_consumption", "6.6");
                // 车辆最高速度
                data.put("maximum_speed", "205");

                // 需先在腾讯云控制台，增加自定义主题: data，用于更新自定义数据
                mShadowSample.publishTopic(BuildConfig.SHADOW_TEST_TOPIC, data);
            }
        });
    }

    private class ShadowActionCallBack extends TXShadowActionCallBack {
        @Override
        public void onRequestCallback(String type, int result, String document) {
            super.onRequestCallback(type, result, document);
            String logInfo = String.format("onRequestCallback, type[%s], result[%d], document[%s]", type, result, document);
            printLogInfo(TAG, logInfo);
        }

        @Override
        public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends com.tencent.iot.hub.device.java.core.shadow.DeviceProperty> devicePropertyList) {
            super.onDevicePropertyCallback(propertyJSONDocument, devicePropertyList);
            String logInfo = String.format("onDevicePropertyCallback, propertyJSONDocument[%s], deviceProperty[%s]",
                    propertyJSONDocument, devicePropertyList.toString());
            printLogInfo(TAG, logInfo);
            mShadowSample.updateDeviceProperty(propertyJSONDocument, (List<DeviceProperty>)devicePropertyList);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof MQTTRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            mParent.printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof MQTTRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
            } else {
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof MQTTRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            mParent.printLogInfo(TAG, logInfo, mLogInfoText);
        }
    }

    /**
     * 打印日志
     *
     * @param tag
     * @param logInfo
     */
    public void printLogInfo(final String tag, final String logInfo) {
        mParent.printLogInfo(tag, logInfo, mLogInfoText);
    }
}
