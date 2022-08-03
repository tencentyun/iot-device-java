package com.tencent.iot.explorer.device.central.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.central.entity.DeviceEntity;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder>{

    private List<DeviceEntity> mDeviceList;
    private LayoutInflater mInflater;
    private Context mContext;
    private DeviceListAdapter.ItemClickListener mItemClickLitener;

    public DeviceListAdapter(Context context, List<DeviceEntity> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mDeviceList = data;
        this.mContext = context;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        DeviceEntity device = mDeviceList.get(position);
        holder.tvDeviceName.setText(device.getAliasName());
        holder.tvDeviceId.setText(device.getDeviceId());
        if (device.getOnline() == 0) {
            holder.tvDeviceStatus.setText("离线");
            holder.tvDeviceStatus.setTextColor(mContext.getResources().getColor(R.color.red_eb3d3d));
        } else {
            holder.tvDeviceStatus.setText("在线");
            holder.tvDeviceStatus.setTextColor(mContext.getResources().getColor(R.color.green_0ABF5B));
        }

        if (mItemClickLitener != null) {
            holder.tvDeviceId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mItemClickLitener.onItemClick(view, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceId;
        TextView tvDeviceStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceId = itemView.findViewById(R.id.tv_device_id);
            tvDeviceStatus = itemView.findViewById(R.id.tv_device_status);
        }
    }

    public void setOnItemClickListener(DeviceListAdapter.ItemClickListener listener) {
        mItemClickLitener = listener;
    }

    public interface ItemClickListener{
        void onItemClick(View view, int position);
    }

}
