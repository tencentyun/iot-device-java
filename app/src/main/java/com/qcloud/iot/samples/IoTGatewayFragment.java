package com.qcloud.iot.samples;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.qcloud.iot.R;
import com.qcloud.iot.samples.gateway.GatewaySample;
import com.qcloud.iot.samples.gateway.ProductAirconditioner;
import com.qcloud.iot.samples.gateway.ProductLight;
import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.data_template.TXDataTemplateDownStreamCallBack;
import com.qcloud.iot_explorer.gateway.TXGatewaySubdev;
import com.qcloud.iot_explorer.utils.TXLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class IoTGatewayFragment extends Fragment {
    private static final String TAG = "IoTGatewayFragment";

    private IoTMainActivity mParent;

    private GatewaySample mGatewaySample;

    private Button mConnectBtn;
    private Button mCloseConnectBtn;

    private Button maddSubDev1;
    private Button mdelSubDev1;
    private Button monlineSubDev1;
    private Button mofflineSubDev1;

    private Button maddSubDev2;
    private Button mdelSubDev2;
    private Button monlineSubDev2;
    private Button mofflineSubDev2;

    private TextView mProperty;

    // Default testing parameters
    private String mBrokerURL = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
    private String mProductID = "YOUR_PRODUCT_ID";
    private String mDevName = "YOUR_DEVICE_NAME";
    private String mDevPSK  = "YOUR_DEVICE_PSK"; //若使用证书验证，设为null
    private String mSubDev1ProductId = "YOUR_LIGHT_PRODUCT_ID";
    private String mSubDev1DeviceName = "YOUR_LIGHT_DEVICE_NAME";

    private String mSubDev2ProductId = "YOUR_AIRCONDITIONER_PRODUCT_ID";
    private String mSubDev2DeviceName = "YOUR_AIRCONDITIONER_DEVICE_NAME";

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String

    private ProductLight mSubDev1 = null;
    private ProductAirconditioner mSubDev2 = null;
    private final static String mJsonFileName = "gateway.json";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frament_gateway_demo, container, false);

        mParent = (IoTMainActivity) this.getActivity();

        mConnectBtn = view.findViewById(R.id.connect);
        mCloseConnectBtn = view.findViewById(R.id.disconnect);
        maddSubDev1= view.findViewById(R.id.addSubDev1);
        mdelSubDev1= view.findViewById(R.id.delSubDev1);
        monlineSubDev1= view.findViewById(R.id.onlineSubDev1);
        mofflineSubDev1= view.findViewById(R.id.offlineSubDev1);
        maddSubDev2= view.findViewById(R.id.addSubDev2);
        mdelSubDev2= view.findViewById(R.id.delSubDev2);
        monlineSubDev2= view.findViewById(R.id.onlineSubDev2);
        mofflineSubDev2= view.findViewById(R.id.offlineSubDev2);

        mProperty =  view.findViewById(R.id.subDevProperty);

        mGatewaySample = new GatewaySample(mParent, mBrokerURL, mProductID, mDevName, mDevPSK, mJsonFileName, mSubDev1ProductId, mSubDev2ProductId);

        new IoTGatewayFragment.setPropertyText().start();

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.online();
            }
        });

        mCloseConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.offline();
            }
        });

        maddSubDev1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                Object obj =  mGatewaySample.addSubDev(mSubDev1ProductId, mSubDev1DeviceName);
                if(null != obj) {
                    mSubDev1 = (ProductLight)obj;
                }
            }
        });

        mdelSubDev1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.delSubDev(mSubDev1ProductId, mSubDev1DeviceName);
                mSubDev1 = null;
            }
        });

        monlineSubDev1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.onlineSubDev(mSubDev1ProductId, mSubDev1DeviceName);
            }
        });

        mofflineSubDev1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.offlineSubDev(mSubDev1ProductId, mSubDev1DeviceName);
            }
        });


        maddSubDev2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                Object obj =  mGatewaySample.addSubDev(mSubDev2ProductId, mSubDev2DeviceName);
                if(null != obj) {
                    mSubDev2 = (ProductAirconditioner)obj;
                }
            }
        });

        mdelSubDev2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.delSubDev(mSubDev2ProductId, mSubDev2DeviceName);
                mSubDev2 = null;
            }
        });

        monlineSubDev2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.onlineSubDev(mSubDev2ProductId, mSubDev2DeviceName);
            }
        });

        mofflineSubDev2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGatewaySample == null)
                    return;
                mGatewaySample.offlineSubDev(mSubDev2ProductId, mSubDev2DeviceName);
            }
        });

        return view;
    }

    /**
     * 显示信息
     */
    private class setPropertyText extends Thread {
        public void run() {
            StringBuilder textInfo;
            while (!isInterrupted()) {
                textInfo = new StringBuilder("subdev property:\r\n");
                if(null != mSubDev1) {
                    textInfo.append("subdev1(Light):");
                    if(Status.SUBDEV_STAT_ONLINE == mGatewaySample.getSubDevStatus(mSubDev1ProductId, mSubDev1DeviceName)) {
                        textInfo.append("Status: online");
                    } else {
                        textInfo.append("Status: offline");
                    }

                    for(Map.Entry<String, Object> entry: mSubDev1.mProperty.entrySet())
                    {
                        textInfo.append("\r\n").append(entry.getKey()).append(": ").append(entry.getValue());
                    }
                    textInfo.append("\r\n");
                }

                if(null != mSubDev2) {
                    textInfo.append("subdev1(Airconditioner):");
                    if(Status.SUBDEV_STAT_ONLINE == mGatewaySample.getSubDevStatus(mSubDev2ProductId, mSubDev2DeviceName)) {
                        textInfo.append("Status: online");
                    } else {
                        textInfo.append("Status: offline");
                    }
                    for(Map.Entry<String, Object> entry: mSubDev2.mProperty.entrySet())
                    {
                        textInfo.append("\r\n").append(entry.getKey()).append(": ").append(entry.getValue());
                    }
                }

                mProperty.setText(textInfo.toString());

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    TXLog.e(TAG, "The thread has been interrupted");
                    break;
                }
            }
        }
    }

    public void closeConnection() {
        if (mGatewaySample == null)
            return;
        mGatewaySample.offline();
    }
}
