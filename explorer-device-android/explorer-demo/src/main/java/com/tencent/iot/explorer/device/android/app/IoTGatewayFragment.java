package com.tencent.iot.explorer.device.android.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.app.gateway.GatewaySample;
import com.tencent.iot.explorer.device.android.app.gateway.ProductAirconditioner;
import com.tencent.iot.explorer.device.android.app.gateway.ProductLight;
import com.tencent.iot.explorer.device.android.common.Status;
import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.android.gateway.TXGatewaySubdev;
import com.tencent.iot.explorer.device.android.utils.TXLog;

import java.util.Map;

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
    private String mProductID = "FCUGSP3YL8";
    private String mDevName = "dev01";
    private String mDevPSK  = "ZIQWRtYq3UahYeTQlHITAA=="; //若使用证书验证，设为null
    private String mSubDev1ProductId = "TSV6COGWKO";
    private String mSubDev1DeviceName = "dev001";

    private String mSubDev2ProductId = "5E2VQA1DLI";
    private String mSubDev2DeviceName = "dev001";

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

        mGatewaySample = new GatewaySample(mParent, mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mJsonFileName, mSubDev1ProductId, mSubDev2ProductId);

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
