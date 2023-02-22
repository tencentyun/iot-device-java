package com.tencent.iot.explorer.device.video.call.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.video.call.entity.FrameRateEntity;

import java.util.ArrayList;

public class FrameRateListAdapter extends RecyclerView.Adapter<FrameRateListAdapter.ViewHolder> {
    private ArrayList<FrameRateEntity> mDatas = null;
    private LayoutInflater mInflater = null;
    private Context mContext;

    public FrameRateListAdapter(Context context, ArrayList<FrameRateEntity> datas) {
        this.mDatas = datas;
        mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.frame_rate_list_item, parent, false);
        ViewHolder vewHolder = new ViewHolder(view);
        return vewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final FrameRateEntity entity = mDatas.get(position);
        holder.tv.setText(String.format("%d", entity.getRate()));
        if (entity.getIsSelect()) {
            holder.selectIv.setBackground(ContextCompat.getDrawable(mContext,R.drawable.selected));
        } else {
            holder.selectIv.setBackground(ContextCompat.getDrawable(mContext,R.drawable.unselect));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (FrameRateEntity enti : mDatas) {
                    enti.setIsSelect(false);
                }
                mDatas.get(position).setIsSelect(true);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView selectIv = null;
        public TextView tv = null;

        public ViewHolder(View itemView) {
            super(itemView);

            selectIv = (ImageView) itemView.findViewById(R.id.iv_select);
            tv = (TextView) itemView.findViewById(R.id.tv_frame_rate);
        }
    }

    public FrameRateEntity selectedFrameRateEntity() {
        for (FrameRateEntity entity : mDatas) {
            if (entity.getIsSelect()) {
                return entity;
            }
        }
        return null;
    }
}