package com.tencent.iot.explorer.device.central;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.App;
import com.tencent.iot.explorer.device.android.app.BuildConfig;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.http.ErrorCode;
import com.tencent.iot.explorer.device.android.http.HttpRequest;
import com.tencent.iot.explorer.device.android.http.IoTAuth;
import com.tencent.iot.explorer.device.android.http.callback.MyCallback;
import com.tencent.iot.explorer.device.android.http.response.BaseResponse;
import com.tencent.iot.explorer.device.android.http.utils.RequestCode;
import com.tencent.iot.explorer.device.android.utils.SharePreferenceUtil;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.central.adapter.DeviceListAdapter;
import com.tencent.iot.explorer.device.central.adapter.DevicePropertiesAdapter;
import com.tencent.iot.explorer.device.central.callback.OnGetDeviceListListener;
import com.tencent.iot.explorer.device.central.consts.Common;
import com.tencent.iot.explorer.device.central.data_template.CentralDataTemplateSample;
import com.tencent.iot.explorer.device.central.entity.Device;
import com.tencent.iot.explorer.device.central.entity.DeviceDataEntity;
import com.tencent.iot.explorer.device.central.entity.DeviceDataResponse;
import com.tencent.iot.explorer.device.central.entity.DeviceOnlineEntity;
import com.tencent.iot.explorer.device.central.entity.DeviceOnlineResponse;
import com.tencent.iot.explorer.device.central.message.payload.Payload;
import com.tencent.iot.explorer.device.central.utils.MessageParseUtils;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.rtc.utils.ZXingUtils;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;


public class CentralMainActivity extends AppCompatActivity {

    private static final String TAG = CentralMainActivity.class.getSimpleName();
    private static final String CENTRAL_CONFIG = "central_config";
    private static final String BROKER_URL = "broker_url";
    private static final String PRODUCT_ID = "product_id";
    private static final String DEVICE_NAME = "dev_name";
    private static final String DEVICE_PSK = "dev_psk";

    private static final String JSON_FILE_NAME = "TRTC_watch.json";

    private ImageView mQRCodeImgView;
    private Button mConnectBtn;
    private Button mDisconnectBtn;
    private EditText mBrokerURLEditText;
    private EditText mProductIdEditText;
    private EditText mDevNameEditText;
    private EditText mDevPSKEditText;
    private TextView mLogInfoText;
    private ScrollView mLogInfoScrollView;
    private RecyclerView mDeviceListView;

    //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mBrokerURL = BuildConfig.CENTRAL_BROKER_URL;
    private String mProductID = BuildConfig.CENTRAL_PRODUCT_ID;
    private String mDevName = BuildConfig.CENTRAL_DEVICE_NAME;
    private String mDevPSK  = BuildConfig.CENTRAL_DEVICE_PSK;
    private String mAccessToken = "";
    private String mCurrentDeviceId = "";

    private final ArrayList<Device> mDeviceList = new ArrayList<>();
    private DeviceListAdapter mDeviceListAdapter;

    private CentralDataTemplateSample mDataTemplateSample;

    private boolean mDialogShow = false;
    private IosCenterStyleDialog mDeviceDetailDialog;
    private List<DeviceDataEntity> mDeviceDataEntities;
    private RecyclerView mDeviceDetailListView;
    private DevicePropertiesAdapter mDevicePropertiesAdapter;

    private final OnGetDeviceListListener onGetDeviceListListener = new OnGetDeviceListListener() {
        @Override
        public void onGetDeviceList(List<String> devices) {
            mDeviceList.clear();
            for (int i = 0; i < devices.size(); i++) {
                mDeviceList.add(new Device(devices.get(i), 0));
            }
            HttpRequest.Companion.getInstance().deviceOnlineStatus((ArrayList<String>) devices, mCallback);
        }
    };

    private final MyCallback mCallback = new MyCallback() {
        @Override
        public void fail(@Nullable String msg, int reqCode) {
            TXLog.e(TAG, msg);
            if (ErrorCode.DATA_MSG.ACCESS_TOKEN_ERR.equals(msg)) {
                // Invalid Access Token, 小程序重新扫码绑定中控设备
                TXLog.e(TAG, msg);
                mDataTemplateSample.refreshToken(mAccessToken);
            }
        }

        @Override
        public void success(@NotNull BaseResponse response, int reqCode) {
            TXLog.d(TAG, response.toString());
            if (!response.isSuccess()) return;
            switch (reqCode) {
                case RequestCode.device_data:
                    DeviceDataResponse deviceData = response.parse(DeviceDataResponse.class);
                    if (deviceData != null) {
                        mDeviceDataEntities = deviceData.parseList();
                        runOnUiThread(() -> {
                            showDeviceDetail(mCurrentDeviceId, mDeviceDataEntities);
                        });
                    }
                    break;
                case RequestCode.device_online_status:
                    DeviceOnlineResponse resp = response.parse(DeviceOnlineResponse.class);
                    if (resp != null) {
                        List<DeviceOnlineEntity> deviceStatuses = resp.getDeviceStatuses();
                        if (deviceStatuses != null) {
                            for (int i = 0; i < mDeviceList.size(); i++) {
                                for (int j = 0; j < deviceStatuses.size(); j++) {
                                    if (mDeviceList.get(i).id.equals(deviceStatuses.get(j).getDeviceId())) {
                                        mDeviceList.get(i).status = deviceStatuses.get(j).getOnline();
                                    }
                                }
                            }
                        }
                        runOnUiThread(() -> mDeviceListAdapter.notifyDataSetChanged());
                    }
                    break;
                default:
                    break;
            }

        }
    };

    private void showDeviceDetail(String deviceId, List<DeviceDataEntity> deviceDatas) {
        mDeviceDetailDialog = new IosCenterStyleDialog(this, R.layout.custom_dialog, true) {
            @Override
            public void initView() {
                super.initView();
                TextView titleView = view.findViewById(R.id.dialog_device_id);
                titleView.setText(deviceId);
                mDeviceDetailListView = view.findViewById(R.id.rv_device_properties_list);
                mDeviceDetailListView.setLayoutManager(new LinearLayoutManager(CentralMainActivity.this, LinearLayoutManager.VERTICAL, false));
                mDeviceDetailListView.addItemDecoration(new DividerItemDecoration(CentralMainActivity.this, DividerItemDecoration.VERTICAL));
                mDevicePropertiesAdapter = new DevicePropertiesAdapter(CentralMainActivity.this, deviceDatas);
                mDevicePropertiesAdapter.setOnItemClickListener((view, position) -> {
                    runOnUiThread(() -> {
                        Toast.makeText(CentralMainActivity.this, deviceDatas.get(position).getId() + " is clicked.", Toast.LENGTH_SHORT).show();
                        String productId = mCurrentDeviceId.split("/")[0];
                        String deviceName = mCurrentDeviceId.split("/")[1];
                        int propertyValue = Integer.parseInt(deviceDatas.get(position).getValue()) == 0 ? 1 : 0;
                        String data = String.format("{\"%s\":\"%s\"}", deviceDatas.get(position).getId(), propertyValue);
                        HttpRequest.Companion.getInstance().controlDevice(productId, deviceName, data, new MyCallback() {
                            @Override
                            public void fail(@Nullable String msg, int reqCode) {
                                TXLog.d(TAG, msg);
                            }

                            @Override
                            public void success(@NotNull BaseResponse response, int reqCode) {
                                TXLog.d(TAG, response.toString());
                                if (!response.isSuccess()) return;
                                deviceDatas.get(position).setValue(propertyValue + "");
                                mDevicePropertiesAdapter.notifyDataSetChanged();
                            }
                        });
                    });
                });
                mDeviceDetailListView.setAdapter(mDevicePropertiesAdapter);
            }
        };
        mDeviceDetailDialog.setOnDismissListener(dialog -> {
            mDialogShow = false;
            mCurrentDeviceId = "";
        });
        mDeviceDetailDialog.show();
        mDialogShow = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central_main);

        mQRCodeImgView = findViewById(R.id.iv_qrcode);
        mConnectBtn = findViewById(R.id.btn_connect);
        mDisconnectBtn = findViewById(R.id.btn_disconnect);
        mLogInfoText = findViewById(R.id.log_info);
        mLogInfoScrollView = findViewById(R.id.sv_log_info);

        mBrokerURLEditText = findViewById(R.id.et_broker_url);
        mProductIdEditText = findViewById(R.id.et_productId);
        mDevNameEditText = findViewById(R.id.et_deviceName);
        mDevPSKEditText = findViewById(R.id.et_devicePsk);

        mDeviceListView = findViewById(R.id.rv_device_list);
        mDeviceListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDeviceListView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        mDeviceListAdapter = new DeviceListAdapter(this, mDeviceList);
        mDeviceListAdapter.setOnItemClickListener((view, position) -> {
            mCurrentDeviceId = mDeviceList.get(position).id;
            String productId = mCurrentDeviceId.split("/")[0];
            String deviceName = mCurrentDeviceId.split("/")[1];
            Toast.makeText(this, mDeviceList.get(position).id + " is clicked.", Toast.LENGTH_SHORT).show();
            HttpRequest.Companion.getInstance().deviceData(productId, deviceName, mCallback);
        });
        mDeviceListView.setAdapter(mDeviceListAdapter);

        SharedPreferences settings = getSharedPreferences(CENTRAL_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        mBrokerURL = settings.getString(BROKER_URL, mBrokerURL);
        mProductID = settings.getString(PRODUCT_ID, mProductID);
        mDevName = settings.getString(DEVICE_NAME, mDevName);
        mDevPSK = settings.getString(DEVICE_PSK, mDevPSK);
        editor.apply();

        if (!TextUtils.isEmpty(mBrokerURL)) {
            mBrokerURLEditText.setText(mBrokerURL);
        }

        if (!TextUtils.isEmpty(mProductID)) {
            mProductIdEditText.setText(mProductID);
        }

        if (!TextUtils.isEmpty(mDevName)) {
            mDevNameEditText.setText(mDevName);
        }

        if (!TextUtils.isEmpty(mDevPSK)) {
            mDevPSKEditText.setText(mDevPSK);
        }

        mConnectBtn.setOnClickListener(view -> {
            if (!checkInput()) {
                return;
            }
            SharedPreferences settings1 = getSharedPreferences(CENTRAL_CONFIG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = settings1.edit();
            editor1.putString(BROKER_URL, mBrokerURL);
            editor1.putString(PRODUCT_ID, mProductID);
            editor1.putString(DEVICE_NAME, mDevName);
            editor1.putString(DEVICE_PSK, mDevPSK);
            editor1.apply();

            if (mDataTemplateSample != null) return;
            mDataTemplateSample = new CentralDataTemplateSample(CentralMainActivity.this,
                    mBrokerURL, mProductID, mDevName, mDevPSK,
                    new SelfMqttActionCallBack(), JSON_FILE_NAME,
                    new SelfDownStreamCallBack(), onGetDeviceListListener);
            mDataTemplateSample.connect();
        });

        mDisconnectBtn.setOnClickListener(view -> {
            if (mDataTemplateSample == null)
                return;
            mDataTemplateSample.disconnect();
            mDataTemplateSample = null;
        });

        mAccessToken = IoTAuth.INSTANCE.getToken();
    }


    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        public SelfMqttActionCallBack() { }

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
            if (Status.ERROR == status) {
                runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, "上线失败，请检查设备三元组信息是否正确或网络是否正常", Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, "上线成功", Toast.LENGTH_LONG).show());
                if (mDataTemplateSample != null) {
                    if (!reconnect) {
                        mDataTemplateSample.subscribeTopic();
                        runOnUiThread(() -> {
                            // 设置适配器，刷新展示用户列表
                            mQRCodeImgView.setImageBitmap(ZXingUtils.createQRCodeBitmap(mDataTemplateSample.generateDeviceQRCodeContent(), 200, 200,"UTF-8","H", "1", Color.BLACK, Color.WHITE));
                        });
                    }
                }
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
            if (mDataTemplateSample != null) {
                mDataTemplateSample.unSubscribeTopic();
            }
            runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, "下线成功", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String topics = Arrays.toString(asyncActionToken.getTopics());
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), topics, userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_ERROR);

            if (Status.OK == status) {
                if (topics.contains(TOPIC_SERVICE_DOWN_PREFIX)) {
                    if (!TextUtils.isEmpty(mAccessToken)) {
                        mDataTemplateSample.requestDeviceList(mAccessToken);
                    }
                }
            }

            if (Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
                printLogInfo(TAG, "property get status failed!", mLogInfoText, TXLog.LEVEL_ERROR);
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg, Throwable cause) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("onMessageReceived, topic[%s], message[%s]", topic, message.toString());
            // 解析设备上报的ws消息
            if (message.toString().contains("ws_message") && message.toString().contains("Report")) {
                //解析设备上报的ws消息并更新设备控制面板
                Payload payload = MessageParseUtils.parseMessage(message.toString());
                if (mDialogShow && mCurrentDeviceId.equals(payload.getDeviceId())) {
                    com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(payload.getData());
                    if (jsonObject != null) {
                        for (int i = 0; i < mDeviceDataEntities.size(); i++) {
                            String id = mDeviceDataEntities.get(i).getId();
                            if (jsonObject.containsKey(id)) {
                                mDeviceDataEntities.get(i).setValue(jsonObject.getString(id));
                            }
                        }
                        runOnUiThread(() -> {
                            mDevicePropertiesAdapter.notifyDataSetChanged();
                        });
                    }
                }
            }

            // 解析设备上下线的ws消息
            if (message.toString().contains("ws_message") && message.toString().contains("StatusChange")) {
                //解析设备上报的ws消息并更新设备控制面板
                Payload payload = MessageParseUtils.parseMessage(message.toString());
                if (payload != null) {
                    for (int i = 0; i < mDeviceList.size(); i++) {
                        String id = mDeviceList.get(i).id;
                        if (mDeviceList.get(i).id.equals(payload.getDeviceId())) {
                            mDeviceList.get(i).status = payload.getSubtype().equals("Online") ? 1 : 0;
                        }
                    }
                    runOnUiThread(() -> {
                        mDeviceListAdapter.notifyDataSetChanged();
                    });
                }
            }

            // 设备别名变更或设备房间变更
            if (message.toString().contains("ws_message") && message.toString().contains("AppUpdate")) {
                Payload payload = MessageParseUtils.parseMessage(message.toString());
                if ("DeviceAliasUpdate".equals(payload.getSubtype())) {
                    runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, payload.getPayload(), Toast.LENGTH_SHORT).show());
                } else if ("DeviceRoomUpdate".equals(payload.getSubtype())) {
                    runOnUiThread(() -> Toast.makeText(CentralMainActivity.this, payload.getPayload(), Toast.LENGTH_SHORT).show());
                }
            }

            printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_DEBUG);
        }
    }


    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            TXLog.d(TAG, "onReplyCallBack : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            TXLog.d(TAG, "onGetStatusReplyCallBack : " + data.toString());
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            TXLog.d(TAG, "onControlCallBack : " + msg);
            // 更新AccessToken以及过期时间
            updateToken(msg);
            JSONObject result = new JSONObject();
            try {
                result.put("code",0);
                result.put("status", "success");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        public JSONObject onActionCallBack(String actionId, JSONObject params) {
            TXLog.d(TAG, String.format("onActionCallBack: actionId=[%s], params=[%s]", actionId, params.toString()));

            JSONObject result = new JSONObject();
            try {
                result.put("code", 0);
                result.put("status", "success");
                JSONObject response = new JSONObject();
                response.put("Code", 0);
                result.put("response", response);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            TXLog.d(TAG, "onUnbindDeviceCallBack : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            TXLog.d(TAG, "onBindDeviceCallBack : " + msg);
        }
    }


    private boolean checkInput() {
        String inputBrokerURL = String.valueOf(mBrokerURLEditText.getText());
        if (TextUtils.isEmpty(inputBrokerURL)) {
            mBrokerURL = null;
        } else {
            mBrokerURL = inputBrokerURL;
        }

        String inputProductId = String.valueOf(mProductIdEditText.getText());
        if (TextUtils.isEmpty(inputProductId)) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入productId", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mProductID = inputProductId;
        }

        String inputDevName = String.valueOf(mDevNameEditText.getText());
        if (TextUtils.isEmpty(inputDevName)) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入deviceName", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mDevName = inputDevName;
        }

        String inputDevPSK = String.valueOf(mDevPSKEditText.getText());
        if (TextUtils.isEmpty(inputDevPSK)) {
            Toast toast = Toast.makeText(getApplicationContext(), "请输入devicePsk", Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else {
            mDevPSK = inputDevPSK;
        }
        return true;
    }

    /**
     * 打印日志信息
     *
     * @param logInfo
     */
    protected void printLogInfo(final String tag, final String logInfo, final TextView textView, int logLevel) {
        switch (logLevel) {
            case TXLog.LEVEL_DEBUG:
                TXLog.d(tag, logInfo);
                break;

            case TXLog.LEVEL_INFO:
                TXLog.i(tag, logInfo);
                break;

            case TXLog.LEVEL_ERROR:
                TXLog.e(tag, logInfo);
                break;

            default:
                TXLog.d(tag, logInfo);
                break;
        }

        runOnUiThread(() -> {
            textView.setMovementMethod(ScrollingMovementMethod.getInstance());
            textView.append(logInfo + "\n");
            mLogInfoScrollView.post(() -> mLogInfoScrollView.smoothScrollTo(0, textView.getBottom()));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDataTemplateSample != null) {
            mDataTemplateSample.disconnect();
        }
        if (mDeviceDetailDialog != null && mDeviceDetailDialog.isShowing()) {
            mDeviceDetailDialog.dismiss();
            mDeviceDetailDialog = null;
        }
    }

    private void updateToken(JSONObject msg) {
        if (msg != null) {
            try {
                String str = msg.getString(Common.PROPERTY_ACCESS_TOKEN); // {token;expiredTime}
                if (!TextUtils.isEmpty(str)) {
                    String[] info = str.split(";");
                    if (info.length == 2) {
                        String token = info[0];
                        long expiredTime = Long.parseLong(info[1]);
                        SharePreferenceUtil.saveString(this, App.CONFIG, App.ACCESS_TOKEN, token);
                        SharePreferenceUtil.saveLong(this, App.CONFIG, App.TOKEN_EXPIRED_TIME, expiredTime);
                        IoTAuth.INSTANCE.init(token, expiredTime);
                        mAccessToken = token;
                        mDataTemplateSample.requestDeviceList(mAccessToken);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            TXLog.e(TAG, "updateToken msg is null");
        }
    }
}