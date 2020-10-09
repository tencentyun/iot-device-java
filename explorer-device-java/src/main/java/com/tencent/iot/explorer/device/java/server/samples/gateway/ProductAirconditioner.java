package com.tencent.iot.explorer.device.java.server.samples.gateway;

import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.gateway.TXGatewayClient;
import com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdev;
import com.tencent.iot.explorer.device.java.gateway.TXGatewaySubdevActionCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.*;

public class ProductAirconditioner {
    private static final String TAG = "TXProductLight";

    public TXGatewaySubdev mGatewaySubdev;
    private final static String mSubDev1JsonFileName = "subdev2.json";
    private static final Logger LOG = LoggerFactory.getLogger(ProductAirconditioner.class);

    /**上报周期*/
    private int reportPeriod = 10 * 1000;
    private reportPropertyPeriodically mReportThread = null;

    //light property
    public ConcurrentHashMap<String, Object> mProperty = null;

    public ProductAirconditioner(TXGatewayClient connection,  String productId, String deviceName) {
        //初始化模板数据
        initTemplateData();
        mGatewaySubdev = new TXGatewaySubdev(connection,  productId, deviceName, mSubDev1JsonFileName,
                new ActionCallBack(), new DownStreamCallBack());
    }

    /**
     * 初始化属性
     */
    private void initTemplateData(){
        //初始化属性
        mProperty = new ConcurrentHashMap<String, Object>();
        mProperty.put("power_switch",0);
        mProperty.put("Temperature",1);
        mProperty.put("Mode",0);
        mProperty.put("Wind",1);
    }

    /**
     * 根据接收的json文件设置属性值,开关改变时触发事件
     * @param params 属性描述文件
     */
    private void setPropertyBaseOnJson(JSONObject params) {
        Iterator<String> it = params.keys();
        while(it.hasNext()){
            String key = it.next();
            if(mProperty.containsKey(key)) {
                try {
                    if(key.equals("power_switch") &&  mProperty.get(key) != params.get(key)) {
                        LOG.error(TAG,mProperty.get(key).toString() + params.get(key).toString());
                    }
                    mProperty.put(key, params.get(key));
                } catch (JSONException e) {
                    LOG.error(TAG, "setPropertyBaseOnJson: failed! Invalid json!");
                    return;
                }
            }
        }
    }

    /**
     * 获取最后上报的信息以及未处理的control
     */
    private void StatusGet() {
        if(Status.OK != mGatewaySubdev.propertyGetStatus("report", false)) {
            LOG.error(TAG, "property get statuts failed!");
        }

        if(Status.OK != mGatewaySubdev.propertyGetStatus("control", false)) {
            LOG.error(TAG, "property get statuts failed!");
        }
    }

    /**
     * 上报设备的基本信息
     */
    private void InfoReport() {
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
        } catch (JSONException e) {
            LOG.error(TAG, "Construct light info failed!");
            return;
        }
        if(Status.OK != mGatewaySubdev.propertyReportInfo(params)) {
            LOG.error(TAG, "light info report failed!");
        }
    }

    /**
     * 根据LightStatusGet中发送control的回复的参数判断需不需要处理control,如果处理完毕，则清除control
     */
    private void ControlClear() {
        if (Status.OK != mGatewaySubdev.propertyClearControl()){
            LOG.error(TAG, "clear control failed!");
        }
    }

    /**
     * 周期性上报属性
     */
    private class reportPropertyPeriodically extends Thread {
        public void run() {
            while (!isInterrupted()) {
                JSONObject property = new JSONObject();
                for(Map.Entry<String, Object> entry: mProperty.entrySet())
                {
                    try {
                        property.put(entry.getKey(),entry.getValue());
                    } catch (JSONException e) {
                        LOG.error(TAG, "construct property failed!");
                    }
                }

                if (Status.OK != mGatewaySubdev.propertyReport(property, null)) {
                    LOG.error(TAG, "report property failed!");
                    break;
                }

                try {
                    Thread.sleep(reportPeriod);
                } catch (InterruptedException e) {
                    LOG.error(TAG, "The thread has been interrupted");
                    break;
                }
            }
        }
    }

    /**
     * 实现子设备上下线，订阅成功接口
     */
    private class ActionCallBack extends TXGatewaySubdevActionCallBack {

        /**上线后，订阅相关主题*/
        @Override
        public void onSubDevOnline() {
            LOG.debug(TAG, "dev[%s] online!", mGatewaySubdev.mDeviceName);
            if (Status.OK != mGatewaySubdev.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)) {
                LOG.error(TAG, "subscribeTopic: subscribe property down stream topic failed!");
            }
            if (Status.OK != mGatewaySubdev.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
                LOG.error(TAG, "subscribeTopic: subscribe event down stream topic failed!");
            }
            if (Status.OK != mGatewaySubdev.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
                LOG.error(TAG, "subscribeTopic: subscribe event down stream topic failed!");
            }
        }

        @Override
        public void onSubDevOffline() {

        }

        /**订阅属性下行主题成功则获取状态和上报信息，启动周期性上报属性线程*/
        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                LOG.error(TAG,logInfo);
            } else {
                String topic = Arrays.toString(asyncActionToken.getTopics());
                if(topic.substring(1,topic.length()-1).equals(mGatewaySubdev.mPropertyDownStreamTopic)) {
                    InfoReport(); //发送基本信息
                    StatusGet();  //获取最后的状态以及未处理的control信息
                    if(null == mReportThread) {
                        mReportThread = new reportPropertyPeriodically(); //周期性上报
                        mReportThread.start();
                    }
                }
                LOG.debug(TAG,logInfo);
            }
        }

    }

    /**
     * 实现下行消息处理的回调接口
     */
    public class DownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //Just print
            LOG.debug(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //根据控制和状态信息设置属性
            try {
                Iterator<String> it = data.keys();
                if(it.hasNext()) {
                    String key = it.next();
                    if (key.equals("reported")) { //report
                        JSONObject params = data.getJSONObject("reported");
                        setPropertyBaseOnJson(params);
                    } else { //control
                        JSONObject params = data.getJSONObject("control");
                        setPropertyBaseOnJson(params);
                        ControlClear();
                    }
                }
            } catch (JSONException e) {
                LOG.error(TAG,"get status reply  params invalid!");
            }
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            //解析控制信息，此处根据参数设置相应的属性
            //set property
            setPropertyBaseOnJson(msg);

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            LOG.debug(TAG, "action [%s] received, input:" + params, actionId);
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    int period;
                    period = params.getInt("period");

                    //实现闪烁
                    mProperty.put("color", 1);
                    TimeUnit.MILLISECONDS.sleep(period*1000);
                    mProperty.put("color", 0);
                    TimeUnit.MILLISECONDS.sleep(period*1000);
                    mProperty.put("color", 1);

                    //construct result
                    JSONObject result = new JSONObject();
                    result.put("code",0);
                    result.put("status", "some message when error");
                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);
                    result.put("response", response);
                    return result;
                } catch (Exception e0) {
                    try {
                        //construct error result
                        JSONObject result = new JSONObject();
                        result.put("code", 1);
                        result.put("status", "action execute failed!");
                        // response based on output
                        JSONObject response = new JSONObject();
                        response.put("result", 1);
                        result.put("response", response);
                        return result;
                    } catch (Exception e1) {
                        return  null;
                    }
                }
            }
            return null;
        }
    }
}
