package com.qcloud.iot.samples;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.qcloud.iot.R;
import com.qcloud.iot.samples.data_template.DataTemplateSample;
import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.data_template.TXDataTemplateDownCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttRequest;
import com.qcloud.iot_explorer.utils.TXLog;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;

public class IoTMqttFragment extends Fragment {

    private static final String TAG = "TXMQTT";

    private IoTMainActivity mParent;

    private DataTemplateSample mDataTemplateSample;

    private Button mConnectBtn;
    private Button mCloseConnectBtn;
    private Button mSubScribeBtn;
    private Button mUnSubscribeBtn;

    private Button mPropertyReportBtn;
    private Button mGetStatusBtn;

    private Button mEventPostBtn;
    private Button mEventsPostBtn;

    private Button mCheckFirmwareBtn;

    private Spinner mSpinner;
    private TextView mLogInfoText;

    // Default testing parameters
    private String mBrokerURL = "ssl://111.230.126.244:8883";
    private String mProductID = "3INKNQFTGV";
    private String mDevName = "test";
    private String mDevPSK  = "GgsH+Dxb2hwQ5wS9EOZWbw=="; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String

    private final static String mJsonFileName = "light_sample.json";

    private EditText mItemText;

    private final static String BROKER_URL = "broker_url";
    private final static String PRODUCT_ID = "product_id";
    private final static String DEVICE_NAME = "dev_name";
    private final static String DEVICE_PSK = "dev_psk";
    private final static String DEVICE_CERT = "dev_cert";
    private final static String DEVICE_PRIV  = "dev_priv";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_iot_mqtt, container, false);

        mParent = (IoTMainActivity) this.getActivity();

        mConnectBtn = view.findViewById(R.id.connect);
        mCloseConnectBtn = view.findViewById(R.id.close_connect);
        mSubScribeBtn = view.findViewById(R.id.subscribe_topic);
        mUnSubscribeBtn = view.findViewById(R.id.unSubscribe_topic);
        mPropertyReportBtn = view.findViewById(R.id.property_report);
        mGetStatusBtn = view.findViewById(R.id.get_status);
        mEventPostBtn = view.findViewById(R.id.event_report);
        mEventsPostBtn = view.findViewById(R.id.events_report);
        mCheckFirmwareBtn = view.findViewById(R.id.check_firmware);
        mSpinner = view.findViewById(R.id.spinner4);
        mLogInfoText = view.findViewById(R.id.log_info);
        mItemText = view.findViewById(R.id.editText2);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           @Override
           public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               String[] items = getResources().getStringArray(R.array.setup_items);
               String paraStr = mItemText.getText().toString();

               if (position == 0) {
                   return;
               }

               if (paraStr.equals("")) {
                   return;
               }

               Log.d("TXMQTT", "Set " + items[position] + " to " + paraStr);
               Toast toast = Toast.makeText(mParent, "Set " + items[position] + " to " + paraStr, Toast.LENGTH_LONG);
               toast.show();
               SharedPreferences sharedPreferences =  mParent.getSharedPreferences("config",Context.MODE_PRIVATE);
               SharedPreferences.Editor editor = sharedPreferences.edit();
               switch(position) {
                   case 1:
                       mBrokerURL = paraStr;
                       editor.putString(BROKER_URL, mBrokerURL);
                       break;
                   case 2:
                       mProductID = paraStr;
                       editor.putString(PRODUCT_ID, mProductID);
                   case 3:
                       mDevName = paraStr;
                       editor.putString(DEVICE_NAME, mDevName);
                       break;
                   case 4:
                       mDevPSK = paraStr;
                       editor.putString(DEVICE_PSK, mDevPSK);
                       break;
                   default:
                       break;
               }
               editor.commit();
           }

           @Override
           public void onNothingSelected(AdapterView<?> parent) {
           }
        });

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = mParent.getSharedPreferences("config", Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(BROKER_URL, mBrokerURL);
                editor.putString(PRODUCT_ID, mProductID);
                editor.putString(DEVICE_NAME, mDevName);
                editor.putString(DEVICE_PSK, mDevPSK);
                editor.commit();

                mBrokerURL = settings.getString(BROKER_URL, mBrokerURL);
                mProductID = settings.getString(PRODUCT_ID, mProductID);
                mDevName = settings.getString(DEVICE_NAME, mDevName);
                mDevCert = settings.getString(DEVICE_CERT, mDevCert);
                mDevPriv  = settings.getString(DEVICE_PRIV, mDevPriv);

                mDataTemplateSample = new DataTemplateSample(mParent, mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName);
                mDataTemplateSample.connect();
            }
        });

        mCloseConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                mDataTemplateSample.disconnect();
            }
        });

        mSubScribeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 订阅explorer相关的主题
                if (mDataTemplateSample == null)
                    return;
                mDataTemplateSample.subscribeTopic();
            }
        });

        mUnSubscribeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                mDataTemplateSample.unSubscribeTopic();
            }
        });

        mPropertyReportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //property report
                if (mDataTemplateSample == null)
                    return;
                String params = "{\"power_switch\":0,\"color\":0,\"brightness\":0,\"name\":\"test\"}";
                mDataTemplateSample.propertyReport(params, null, new SelfReportReplyCallback());
            }
        });

        mGetStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                //get status
            }
        });

        mEventPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                String eventId = "status_report";
                String type = "info";
                String params = "{\"status\":0,\"message\":\"\"}";
                mDataTemplateSample.eventSinglePost(eventId, type, params, new SelfEventReplyCallback());
            }
        });

        mEventsPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                String events =  "[{\"eventId\":\"status_report\", \"type\":\"info\", \"timestamp\":" + System.currentTimeMillis() + ", \"params\":{\"status\":0,\"message\":\"\"}}," +
                                 "{\"eventId\":\"low_voltage\", \"type\":\"alert\", \"timestamp\":"  + System.currentTimeMillis() + ", \"params\":{\"voltage\":1.26}}," +
                                 "{\"eventId\":\"hardware_fault\", \"type\":\"fault\", \"timestamp\":" + System.currentTimeMillis() + ", \"params\":{\"name\":\"\",\"error_code\":1}}]";
                mDataTemplateSample.eventsPost(events, new SelfEventReplyCallback());
            }
        });

        mCheckFirmwareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                mDataTemplateSample.checkFirmware();
            }
        });

        return view;
    }

    public void closeConnection() {
        if (mDataTemplateSample == null)
            return;
        mDataTemplateSample.disconnect();
    }

    /**
     * 实现属性上报回复的回调接口
     */
    private class SelfReportReplyCallback extends TXDataTemplateDownCallBack {
        @Override
        public void onDownStreamCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报回复，此处只打印
            Log.d(TAG, "event down stream message received : " + replyMsg);
        }
    }

    /**
     * 实现事件回复的回调接口
     */
    private class SelfEventReplyCallback extends TXDataTemplateDownCallBack {
        @Override
        public void onDownStreamCallBack(String replyMsg) {
            //可根据自己需求进行处理事件回复，此处只打印
            Log.d(TAG, "event down stream message received : " + replyMsg);
        }
    }

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            mParent.printLogInfo(TAG, logInfo, mLogInfoText, TXLog.LEVEL_INFO);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            mParent.printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
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
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            mParent.printLogInfo(TAG, logInfo, mLogInfoText);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            mParent.printLogInfo(TAG, logInfo, mLogInfoText);
        }
    }
}
