package com.tencent.iot.hub.device.android.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class IoTTidFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = IoTTidFragment.class.getSimpleName();

    private IoTMainActivity mParent;
    private Button mConnectBtn;
    private Button mDisconnectBtn;
    private Button mSubScribeBtn;
    private Button mUnSubscribeBtn;
    private Button mPublishBtn;
    private TextView mLogInfoText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_iot_tid, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        mParent = (IoTMainActivity) this.getActivity();
        mConnectBtn = view.findViewById(R.id.connect);
        mDisconnectBtn = view.findViewById(R.id.disconnect);
        mLogInfoText = view.findViewById(R.id.log_info);
        mSubScribeBtn = view.findViewById(R.id.subscribe_topic);
        mUnSubscribeBtn = view.findViewById(R.id.unSubscribe_topic);
        mPublishBtn = view.findViewById(R.id.publish_topic);
        mConnectBtn.setOnClickListener(this);
        mDisconnectBtn.setOnClickListener(this);
        mSubScribeBtn.setOnClickListener(this);
        mUnSubscribeBtn.setOnClickListener(this);
        mPublishBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                break;
            case R.id.disconnect:
                break;
            case R.id.subscribe_topic:
                break;
            case R.id.unSubscribe_topic:
                break;
            case R.id.publish_topic:
                break;
            default:
        }
    }
}
