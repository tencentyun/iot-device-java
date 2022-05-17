package com.tencent.iot.explorer.device.video.call.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.video.call.entity.ResolutionEntity;

import java.util.ArrayList;

public class ResolutionListAdapter extends RecyclerView.Adapter<ResolutionListAdapter.ViewHolder> {
    private ArrayList<ResolutionEntity> mDatas = null;
    private LayoutInflater mInflater = null;
    private Context mContext;

    public ResolutionListAdapter(Context context, ArrayList<ResolutionEntity> datas) {
        this.mDatas = datas;
        mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.resolution_list_item, parent, false);
        ViewHolder vewHolder = new ViewHolder(view);
        return vewHolder;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final ResolutionEntity entity = mDatas.get(position);
        holder.tv.setText(String.format("%d * %d", entity.getWidth(), entity.getHeight()));
        if (entity.getIsSelect()) {
            holder.selectIv.setBackground(mContext.getDrawable(R.drawable.selected));
        } else {
            holder.selectIv.setBackground(mContext.getDrawable(R.drawable.unselect));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (ResolutionEntity enti : mDatas) {
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
            tv = (TextView) itemView.findViewById(R.id.tv_resolution);
        }
    }

    public ResolutionEntity selectedResolutionEntity() {
        for (ResolutionEntity entity : mDatas) {
            if (entity.getIsSelect()) {
                return entity;
            }
        }
        return null;
    }
}