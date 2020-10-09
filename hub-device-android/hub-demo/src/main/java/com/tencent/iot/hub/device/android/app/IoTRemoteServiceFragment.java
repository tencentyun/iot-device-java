package com.tencent.iot.hub.device.android.app;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.tencent.iot.hub.device.android.app.service.RemoteRequest;
import com.tencent.iot.hub.device.android.app.shadow.ShadowRequest;
import com.tencent.iot.hub.device.android.service.TXMqttClient;
import com.tencent.iot.hub.device.android.service.TXMqttClientOptions;
import com.tencent.iot.hub.device.android.service.TXMqttConnectOptions;
import com.tencent.iot.hub.device.android.service.TXMqttMessage;
import com.tencent.iot.hub.device.android.service.TXShadowClient;
import com.tencent.iot.hub.device.android.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * 远程客户端
 */
public class IoTRemoteServiceFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = IoTRemoteServiceFragment.class.getSimpleName();

    /**
     * 产品ID
     */
    private static final String PRODUCT_ID = BuildConfig.PRODUCT_ID;

    /**
     * 设备名称
     */
    private static final String DEVICE_NAME = BuildConfig.DEVICE_NAME;

    /**
     * 密钥
     */
    private static final String SECRET_KEY = BuildConfig.DEVICE_PSK;
	
    /**
     * 设备证书名
     */
    private static final String DEVICE_CERT_NAME = "YOUR_DEVICE_NAME_cert.crt";

    /**
     * 设备私钥文件名
     */
    private static final String DEVICE_KEY_NAME = "YOUR_DEVICE_NAME_private.key";

    private AtomicInteger mRequestId = new AtomicInteger(0);

    private IoTMainActivity mParent = null;

    /**
     * mqttClient选项
     */
    private TXMqttClientOptions mMqttClientOptions = null;

    /**
     * mqtt远程服务客户端
     */
    private TXMqttClient mMqttClient = null;

    /**
     * shadow远程服务客户端
     */
    private TXShadowClient mShadowClient = null;

    /**
     * mqttAction回调接口
     */
    private TXMqttActionCallBack mMqttActionCallBack = null;

    /**
     * shadowAction回调接口
     */
    private TXShadowActionCallBack mShadowActionCallBack = null;

    /**
     * 远程服务连接回调接口
     */
    private ServiceConnection mServiceConnection = null;

    /**
     * 是否已连接远程服务
     */
    private boolean isServiceConnect = false;

    /**
     * 是否使用shadow功能
     */
    private boolean useShadow = false;

    private List<Button> btnList = null;

    private TextView mLogInfoText = null;

    private View mView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_iot_remote_service, container, false);

        mMqttClientOptions = new TXMqttClientOptions();
        mMqttClientOptions.productId(PRODUCT_ID);
        mMqttClientOptions.deviceName(DEVICE_NAME);
        mMqttClientOptions.secretKey(SECRET_KEY);
        mMqttClientOptions.serverURI(null);

        mParent = (IoTMainActivity) this.getActivity();

        btnList = new ArrayList<>();

        btnList.add((Button) mView.findViewById(R.id.cb_use_shadow));
        btnList.add((Button) mView.findViewById(R.id.btn_start_remote_service));
        btnList.add((Button) mView.findViewById(R.id.btn_stop_remote_service));
        btnList.add((Button) mView.findViewById(R.id.btn_connect));
        btnList.add((Button) mView.findViewById(R.id.btn_disconnect));
        btnList.add((Button) mView.findViewById(R.id.btn_subscribe));
        btnList.add((Button) mView.findViewById(R.id.btn_subscribe_broadcast_topic));
        btnList.add((Button) mView.findViewById(R.id.btn_publish));
        btnList.add((Button) mView.findViewById(R.id.btn_register_property));
        btnList.add((Button) mView.findViewById(R.id.btn_get_shadow));
        btnList.add((Button) mView.findViewById(R.id.btn_update_shadow));
        btnList.add((Button) mView.findViewById(R.id.btn_subscribe_rrpc_topic));

        mLogInfoText = mView.findViewById(R.id.log_info);

        for (Button button : btnList) {
            button.setOnClickListener(this);
        }

        initListener();
        initClient();
        return mView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cb_use_shadow:
                onUseShadow(((CheckBox) view).isChecked());
                break;

            case R.id.btn_start_remote_service:
                onStartRemoteService();
                break;

            case R.id.btn_stop_remote_service:
                onStopRemoteService();
                break;

            case R.id.btn_connect:
                onConnect();
                break;

            case R.id.btn_disconnect:
                onDisconnect();
                break;

            case R.id.btn_subscribe:
                onSubscribe();
                break;
            case R.id.btn_subscribe_broadcast_topic:
                onSubscribeBroadCastTopic();
                break;

            case R.id.btn_publish:
                onPublish();
                break;

            case R.id.btn_register_property:
                onRegisterProperty();
                break;

            case R.id.btn_get_shadow:
                onGetShadow();
                break;

            case R.id.btn_update_shadow:
                onUpdateShadow();
                break;
            case R.id.btn_subscribe_rrpc_topic:
                onSubscribeRRPCTopic();
                break;

            default:
                break;
        }
    }

    /**
     * 是否使用shadow功能。
     * <p>
     * 注：TXShadowClient和TXMqttClient不能共用，切换时需要重新开启远程服务
     */
    public void onUseShadow(boolean useShadow) {
        this.useShadow = useShadow;
        if (useShadow) {
            mView.findViewById(R.id.shadow_operate).setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.shadow_operate).setVisibility(View.GONE);
        }
        initClient();
    }

    /**
     * 开启远程服务，mqtt/shadow功能切换时，需要重新开启远程服务
     */
    public void onStartRemoteService() {
        if (useShadow) {
            mShadowClient.startRemoteService();
        } else {
            mMqttClient.startRemoteService();
        }
    }

    /**
     * 停止远程服务
     */
    public void onStopRemoteService() {
        if (useShadow) {
            mShadowClient.stopRemoteService();
        } else {
            mMqttClient.stopRemoteService();
        }
    }

    /**
     * 与IoT服务器建立连接
     */
    public void onConnect() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        TXMqttConnectOptions connectOptions = new TXMqttConnectOptions();
        connectOptions.setConnectionTimeout(8);
        connectOptions.setKeepAliveInterval(240);
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setDeviceCertName(DEVICE_CERT_NAME);
        connectOptions.setDeviceKeyName(DEVICE_KEY_NAME);
        connectOptions.setSecretKey(SECRET_KEY);

        RemoteRequest remoteRequest = new RemoteRequest(mRequestId.getAndIncrement());

        if (useShadow) {
            Status status = mShadowClient.connect(connectOptions, remoteRequest);
            mParent.printLogInfo(TAG, String.format("connect IoT completed, status[%s]", status.name()), mLogInfoText);
        } else {
            mMqttClient.connect(connectOptions, remoteRequest);
        }
    }

    /**
     * 与IoT服务器断开连接
     */
    public void onDisconnect() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        RemoteRequest remoteRequest = new RemoteRequest(mRequestId.getAndIncrement());
        if (useShadow) {
            mShadowClient.disConnect(remoteRequest);
        } else {
            mMqttClient.disConnect(remoteRequest);
        }
    }

    /**
     * 订阅主题
     */
    public void onSubscribe() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "test_rule");
        int qos = 1;
        ShadowRequest shadowRequest = new ShadowRequest(mRequestId.getAndIncrement());

        if (useShadow) {
            mShadowClient.getMqttClient().subscribe(topic, qos, shadowRequest);
        } else {
            mMqttClient.subscribe(topic, qos, shadowRequest);
        }
    }

    /**
     * 订阅广播主题
     */
    public void onSubscribeBroadCastTopic() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }
        ShadowRequest shadowRequest = new ShadowRequest(mRequestId.getAndIncrement());
        if (useShadow) {
            mShadowClient.getMqttClient().subscribeBroadcastTopic(TXMqttConstants.QOS1, shadowRequest);
        } else {
            mMqttClient.subscribeBroadcastTopic(TXMqttConstants.QOS1, shadowRequest);
        }
    }

    /**
     * 发布主题
     */
    public void onPublish() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "data");

        TXMqttMessage mqttMessage = new TXMqttMessage();
        mqttMessage.setQos(1);

        RemoteRequest remoteRequest = new RemoteRequest(mRequestId.getAndIncrement());

        if (useShadow) {
            mShadowClient.getMqttClient().publish(topic, mqttMessage, remoteRequest);
        } else {
            mMqttClient.publish(topic, mqttMessage, remoteRequest);
        }
    }

    /**
     * 订阅RRPC主题
     */
    public void onSubscribeRRPCTopic() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }
        ShadowRequest shadowRequest = new ShadowRequest(mRequestId.getAndIncrement());
        if (useShadow) {
            mShadowClient.getMqttClient().subscribeRRPCTopic(TXMqttConstants.QOS0, shadowRequest);
        } else {
            mMqttClient.subscribeRRPCTopic(TXMqttConstants.QOS0, shadowRequest);
        }
    }

    /**
     * 注册设备影子属性
     */
    public void onRegisterProperty() {
        if (!useShadow) {
            return;
        }

        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.key("key1").data("value1").dataType(TXShadowConstants.JSONDataType.STRING);
        mShadowClient.registerProperty(deviceProperty);
    }

    /**
     * 获取设备影子
     */
    public void onGetShadow() {
        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        if (useShadow) {
            RemoteRequest remoteRequest = new RemoteRequest(mRequestId.getAndIncrement());
            mShadowClient.get(remoteRequest);
        }
    }

    /**
     * 更新设备影子
     */
    public void onUpdateShadow() {
        if (!useShadow) {
            return;
        }

        if (!isServiceConnect) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        List<DeviceProperty> devicePropertyList = new ArrayList<>();

        DeviceProperty deviceProperty1 = new DeviceProperty();
        deviceProperty1.key("key1").data("value1").dataType(TXShadowConstants.JSONDataType.STRING);
        devicePropertyList.add(deviceProperty1);

        DeviceProperty deviceProperty2 = new DeviceProperty();
        deviceProperty2.key("key2").data("value2").dataType(TXShadowConstants.JSONDataType.STRING);
        devicePropertyList.add(deviceProperty2);

        RemoteRequest remoteRequest = new RemoteRequest(mRequestId.getAndIncrement());
        mShadowClient.update(devicePropertyList, remoteRequest);
    }

    public void closeConnection() {
        onDisconnect();
    }

    /**
     * 根据云端下发的消息更新设备属性
     *
     * @param propertyJSONDocument
     * @param devicePropertyList
     */
    private void updateDeviceProperty(String propertyJSONDocument, List<DeviceProperty> devicePropertyList) {
        mParent.printLogInfo(TAG, "update device property success and report null desired info", mLogInfoText);
        // 在确认delta更新后，调用reportNullDesiredInfo()接口进行上报
        mShadowClient.reportNullDesiredInfo();
    }

    /**
     * 初始化远程服务客户端
     */
    private void initClient() {
        if (useShadow && null == mShadowClient) {
            mShadowClient = new TXShadowClient();
            mShadowClient.init(this.getContext(), mMqttClientOptions);
            mShadowClient.setServiceConnection(mServiceConnection);
            mShadowClient.setShadowActionCallBack(mShadowActionCallBack);
        } else if (null == mMqttClient) {
            mMqttClient = new TXMqttClient();
            mMqttClient.init(this.getContext(), mMqttClientOptions);
            mMqttClient.setServiceConnection(mServiceConnection);
            mMqttClient.setMqttActionCallBack(mMqttActionCallBack);
        }
    }

    private void checkFirmware() {

        mMqttClient.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                TXLog.e(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                TXLog.e(TAG, "IoTRemoteServiceFragment onLastestFirmwareReady");
                return false;
            }

            @Override
            public void onDownloadProgress(int percent, String version) {
                TXLog.e(TAG, "onDownloadProgress:" + percent);
            }

            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                TXLog.e(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

                mMqttClient.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                TXLog.e(TAG, "onDownloadFailure:" + errCode);

                mMqttClient.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mMqttClient.reportCurrentFirmwareVersion("0.0.1");
    }
    /**
     * 初始化监听器
     */
    private void initListener() {

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                String logInfo = String.format("remote service has been started!");
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
                isServiceConnect = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                String logInfo = String.format("remote service has been destroyed!");
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
                isServiceConnect = false;
            }
        };

        mMqttActionCallBack = new TXMqttActionCallBack() {
            @Override
            public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
                String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                        status, reconnect, userContext, msg);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);


                checkFirmware();
            }

            @Override
            public void onConnectionLost(Throwable cause) {
                String logInfo = String.format("onConnectionLost, cause[%s]", cause.getMessage());
                mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
            }

            @Override
            public void onDisconnectCompleted(Status status, Object userContext, String msg) {
                String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]",
                        status, userContext, msg);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
            }

            @Override
            public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                String logInfo = String.format("onPublishCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                        status, Arrays.toString(token.getTopics()), userContext, errMsg);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }

            @Override
            public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                        status, Arrays.toString(token.getTopics()), userContext, errMsg);
                if (Status.ERROR == status) {
                    mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
                } else {
                    mParent.printLogInfo(TAG, logInfo, mLogInfoText);
                }
            }

            @Override
            public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
                String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                        status, Arrays.toString(asyncActionToken.getTopics()), userContext, errMsg);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }

            @Override
            public void onMessageReceived(String topic, MqttMessage message) {
                String logInfo = String.format("onMessageReceived, topic[%s]", topic);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }
        };

        mShadowActionCallBack = new TXShadowActionCallBack() {
            @Override
            public void onRequestCallback(String type, int result, String document) {
                String logInfo = String.format("onRequestCallback, type[%s], result[%d], document[%s]",
                        type, result, document);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }

            @Override
            public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends com.tencent.iot.hub.device.java.core.shadow.DeviceProperty> devicePropertyList) {
                String logInfo = String.format("onDevicePropertyCallback, propertyJSONDocument[%s], devicePropertyList size[%d]",
                        propertyJSONDocument, devicePropertyList.size());
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
                updateDeviceProperty(propertyJSONDocument, (List<DeviceProperty>)devicePropertyList);
            }

            @Override
            public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                String logInfo = String.format("onPublishCompleted, status[%s], topics[%s], userContext[%s]， errMsg[%s]",
                        status, Arrays.toString(token.getTopics()), userContext, errMsg);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }

            @Override
            public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                        status, Arrays.toString(token.getTopics()), userContext, errMsg);
                if (Status.ERROR == status) {
                    mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
                } else {
                    mParent.printLogInfo(TAG, logInfo, mLogInfoText);
                }
            }

            @Override
            public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                        status, Arrays.toString(token.getTopics()), userContext, errMsg);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }

            @Override
            public void onMessageReceived(String topic, MqttMessage message) {
                String logInfo = String.format("onMessageReceived, topic[%s], message[%s]", topic, message);
                mParent.printLogInfo(TAG, logInfo, mLogInfoText);
            }
        };
    }
}
