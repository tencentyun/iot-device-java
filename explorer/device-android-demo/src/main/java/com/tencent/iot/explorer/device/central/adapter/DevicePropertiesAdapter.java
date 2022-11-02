package com.tencent.iot.explorer.device.central.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.central.entity.DeviceDataEntity;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class DevicePropertiesAdapter extends RecyclerView.Adapter<DevicePropertiesAdapter.ViewHolder>{

    private List<DeviceDataEntity> mDeviceDataList;
    private LayoutInflater mInflater;
    private Context mContext;
    private DeviceListAdapter.ItemClickListener mItemClickLitener;

    public DevicePropertiesAdapter(Context context, List<DeviceDataEntity> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mDeviceDataList = data;
        this.mContext = context;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_device_property, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        DeviceDataEntity deviceData = mDeviceDataList.get(position);
        holder.tvPropertyKey.setText(deviceData.getId());
        holder.tvPropertyValue.setText(deviceData.getValue());

        if (mItemClickLitener != null) {
            holder.tvPropertyKey.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mItemClickLitener.onItemClick(view, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDeviceDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPropertyKey;
        TextView tvPropertyValue;

        ViewHolder(View itemView) {
            super(itemView);
            tvPropertyKey = itemView.findViewById(R.id.tv_device_property_key);
            tvPropertyValue = itemView.findViewById(R.id.tv_device_property_value);
        }
    }

    public void setOnItemClickListener(DeviceListAdapter.ItemClickListener listener) {
        mItemClickLitener = listener;
    }

    public interface ItemClickListener{
        void onItemClick(View view, int position);
    }

}
