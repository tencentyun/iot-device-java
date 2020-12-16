package com.tencent.iot.explorer.device.trtc.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.trtc.entity.UserEntity;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {
    private ArrayList<UserEntity> mDatas = null;
    private LayoutInflater mInflater = null;

    public UserListAdapter(Context context, ArrayList<UserEntity> datas) {
        this.mDatas = datas;
        this.mInflater = LayoutInflater.from(context);
    }

    // 创建新View，被LayoutManager所调用
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.user_list_item, parent, false);
        ViewHolder vewHolder = new ViewHolder(view);
        return vewHolder;
    }

    // 将数据与界面进行绑定的操作
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final UserEntity user = mDatas.get(position);
        holder.useridTv.setText(String.format("id:%s,name:%s", user.getUserid(), user.getUserName()));
        holder.selectCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.selectCb.isChecked()){
                    user.setIsSelect(true);
                }else {
                    user.setIsSelect(false);
                }
            }
        });
    }

    // 获取数据的数量
    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    // 自定义的ViewHolder，持有每个Item的的所有界面组件
    public class ViewHolder extends RecyclerView.ViewHolder {
        public CheckBox selectCb = null;
        public TextView useridTv = null;

        public ViewHolder(View itemView) {
            super(itemView);

            selectCb = (CheckBox) itemView.findViewById(R.id.cb_select);
            useridTv = (TextView) itemView.findViewById(R.id.tv_userid);
        }
    }
}