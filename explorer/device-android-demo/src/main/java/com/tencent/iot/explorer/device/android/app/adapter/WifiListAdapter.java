package com.tencent.iot.explorer.device.android.app.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tencent.iot.explorer.device.android.app.R;


/**
 * Created by 坤 on 2016/9/6.
 */
public class WifiListAdapter extends ArrayAdapter<ScanResult> {

    private final LayoutInflater mInflater;
    private int mResource;

    public WifiListAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.wifi_name);
        TextView signl = (TextView) convertView.findViewById(R.id.wifi_signal);

        ScanResult scanResult = getItem(position);
        name.setText(scanResult.SSID);

        int level = scanResult.level;
        if (level <= 0 && level >= -50) {
            signl.setText("信号很好");
        } else if (level < -50 && level >= -70) {
            signl.setText("信号较好");
        } else if (level < -70 && level >= -80) {
            signl.setText("信号一般");
        } else if (level < -80 && level >= -100) {
            signl.setText("信号较差");
        } else {
            signl.setText("信号很差");
        }

        return convertView;
    }

}
