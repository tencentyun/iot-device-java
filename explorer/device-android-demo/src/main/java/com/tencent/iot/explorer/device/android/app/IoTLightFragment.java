package com.tencent.iot.explorer.device.android.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tencent.iot.explorer.device.android.app.scenarized.LightSample;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import java.util.Map;

public class IoTLightFragment extends Fragment {
    private static final String TAG = "IoTLightFragment";

    private IoTMainActivity mParent;

    private LightSample mLightSample;

    private Button mConnectBtn;
    private Button mCloseConnectBtn;
    private Button mCheckFirmwareBtn;
    private TextView mProperty;

    // Default testing parameters
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = BuildConfig.SUB_PRODUCT_ID;
    private String mDevName = BuildConfig.SUB_DEV_NAME;
    private String mDevPSK  = BuildConfig.SUB_DEV_PSK; //若使用证书验证，设为null

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String

    private final static String mJsonFileName = "light_sample.json";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_light_demo, container, false);

        mParent = (IoTMainActivity) this.getActivity();

        mConnectBtn = view.findViewById(R.id.connect);
        mCloseConnectBtn = view.findViewById(R.id.close_connect);
        mCheckFirmwareBtn = view.findViewById(R.id.check_firmware);
        mProperty =  view.findViewById(R.id.property);

        mLightSample = new LightSample(mParent, mBrokerURL, mProductID, mDevName, mDevPSK, mJsonFileName);

       //mProperty.setText(info);
        new setPropertyText().start();

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLightSample == null)
                    return;
                mLightSample.online();
            }
        });

        mCloseConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLightSample == null)
                    return;
                mLightSample.offline();
            }
        });

        mCheckFirmwareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLightSample == null)
                    return;
                mLightSample.checkFirmware();
            }
        });

        return view;
    }

    /**
     * 显示信息
     */
    private class setPropertyText extends Thread {
        public void run() {
            this.setName(setPropertyText.class.getSimpleName());

            StringBuilder textInfo;
            while (!isInterrupted()) {
                if(mLightSample.isOnline()) {
                    textInfo = new StringBuilder("Status: online");
                } else {
                    textInfo = new StringBuilder("Status: offline");
                }
                textInfo.append("\r\nVersion: ").append(mLightSample.mVersion);

                for(Map.Entry<String, Object> entry: mLightSample.mProperty.entrySet())
                {
                    textInfo.append("\r\n").append(entry.getKey()).append(": ").append(entry.getValue());
                }

                final String text = textInfo.toString();
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProperty.setText(text);
                    }
                });

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
        if (mLightSample == null)
            return;
        mLightSample.offline();
    }
}
