package com.tencent.iot.explorer.device.android.app;

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

import com.tencent.iot.explorer.device.android.app.data_template.DataTemplateSample;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;

public class IoTDataTemplateFragment extends Fragment {

    private static final String TAG = "TXDataTemplateFragment";

    private IoTMainActivity mParent;

    private DataTemplateSample mDataTemplateSample;

    private Button mConnectBtn;
    private Button mCloseConnectBtn;
    private Button mSubScribeBtn;
    private Button mUnSubscribeBtn;

    private Button mPropertyReportBtn;
    private Button mGetStatusBtn;
    private Button mInfoReportBtn;
    private Button mClearControlBtn;

    private Button mEventPostBtn;
    private Button mEventsPostBtn;

    private Button mCheckFirmwareBtn;

    private Spinner mSpinner;
    private TextView mLogInfoText;

    // Default testing parameters
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = BuildConfig.SUB_PRODUCT_ID;
    private String mDevName = BuildConfig.SUB_DEV_NAME;
    private String mDevPSK  = BuildConfig.SUB_DEV_PSK; //若使用证书验证，设为null

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
        View view = inflater.inflate(R.layout.fragment_iot_data_template, container, false);

        mParent = (IoTMainActivity) this.getActivity();

        mConnectBtn = view.findViewById(R.id.connect);
        mCloseConnectBtn = view.findViewById(R.id.close_connect);
        mSubScribeBtn = view.findViewById(R.id.subscribe_topic);
        mUnSubscribeBtn = view.findViewById(R.id.unSubscribe_topic);
        mPropertyReportBtn = view.findViewById(R.id.property_report);
        mGetStatusBtn = view.findViewById(R.id.get_status);
        mInfoReportBtn = view.findViewById(R.id.report_info);
        mClearControlBtn = view.findViewById(R.id.clear_control);
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

                mDataTemplateSample = new DataTemplateSample(mParent, mBrokerURL, mProductID, mDevName, mDevPSK, new SelfMqttActionCallBack(), mJsonFileName, new SelfDownStreamCallBack());
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
                // 订阅相关的主题
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
                JSONObject property = new JSONObject();
                try {
                    property.put("power_switch",0);
                    property.put("color",0);
                    property.put("brightness",0);
                    property.put("name","test");
                } catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct property json failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return;
                }

                if(Status.OK != mDataTemplateSample.propertyReport(property, null)) {
                    mParent.printLogInfo(TAG, "property report failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }
            }
        });

        mGetStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                //get status
                if(Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
                    mParent.printLogInfo(TAG, "property get status failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }

                if(Status.OK != mDataTemplateSample.propertyGetStatus("control", false)) {
                    mParent.printLogInfo(TAG, "property get status failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }
            }
        });

        mInfoReportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                //report info
                JSONObject params = new JSONObject();
                try {
                    JSONObject label = new JSONObject();  //device label
                    label.put("version", "v1.0.0");
                    label.put("company", "tencent");

                    params.put("module_hardinfo", "v1.0.0");
                    params.put("module_softinfo", "v1.0.0");
                    params.put("fw_ver", "v1.0.0");
                    params.put("imei", "0");
                    params.put("mac", "00:00:00:00");
                    params.put("device_label", label);
                }catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return;
                }
                if(Status.OK != mDataTemplateSample.propertyReportInfo(params)) {
                    mParent.printLogInfo(TAG, "property report failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }

            }
        });

        mClearControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                //clear control
                if(Status.OK !=  mDataTemplateSample.propertyClearControl()){
                    mParent.printLogInfo(TAG, "clear control failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }
            }
        });

        mEventPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                String eventId = "status_report";
                String type = "info";
                JSONObject params = new JSONObject();
                try {
                    params.put("status",0);
                    params.put("message","");
                } catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }
                if(Status.OK != mDataTemplateSample.eventSinglePost(eventId, type, params)){
                    mParent.printLogInfo(TAG, "single event post failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }
            }
        });

        mEventsPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDataTemplateSample == null)
                    return;
                JSONArray events = new JSONArray();

                //event:status_report
                try {
                    JSONObject event = new JSONObject();
                    event.put("eventId","status_report");
                    event.put("type", "info");
                    event.put("timestamp", System.currentTimeMillis());

                    JSONObject params = new JSONObject();
                    params.put("status",0);
                    params.put("message","");

                    event.put("params", params);

                    events.put(event);
                } catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return;
                }

                //event:low_voltage
                try {
                    JSONObject event = new JSONObject();
                    event.put("eventId","low_voltage");
                    event.put("type", "alert");
                    event.put("timestamp", System.currentTimeMillis());

                    JSONObject params = new JSONObject();
                    params.put("voltage",1.000000f);

                    event.put("params", params);

                    events.put(event);
                } catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return;
                }

                //event:hardware_fault
                try {
                    JSONObject event = new JSONObject();
                    event.put("eventId","hardware_fault");
                    event.put("type", "fault");
                    event.put("timestamp", System.currentTimeMillis());

                    JSONObject params = new JSONObject();
                    params.put("name","");
                    params.put("error_code",1);

                    event.put("params", params);

                    events.put(event);
                } catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return;
                }

                if(Status.OK != mDataTemplateSample.eventsPost(events)){
                    mParent.printLogInfo(TAG, "events post failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                }
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
     * 实现下行消息处理的回调接口
     */
    private class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            Log.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //可根据自己需求进行处理状态和控制信息的获取结果
            Log.d(TAG, "event down stream message received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            Log.d(TAG, "control down stream message received : " + msg);
            //do something

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                return null;
            }
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            TXLog.d(TAG, "action [%s] received, input:" + params, actionId);
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    Iterator<String> it = params.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        TXLog.d(TAG,"Input parameter[%s]:" + params.get(key), key);
                    }
                    //construct result
                    JSONObject result = new JSONObject();
                    result.put("code",0);
                    result.put("status", "some message wher errorsome message when error");

                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);

                    result.put("response", response);
                    return result;
                } catch (JSONException e) {
                    mParent.printLogInfo(TAG, "Construct params failed!", mLogInfoText, TXLog.LEVEL_ERROR);
                    return null;
                }
            } else if (actionId.equals("YOUR ACTION")) {
                //do your action
            }
            return null;
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
