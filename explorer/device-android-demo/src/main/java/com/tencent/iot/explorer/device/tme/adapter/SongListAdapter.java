package com.tencent.iot.explorer.device.tme.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.kugou.ultimatetv.entity.Song;
import com.tencent.iot.explorer.device.android.app.R;

import java.util.List;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder>{

    private List<Song> mSongList;
    private LayoutInflater mInflater;
    private ItemClickListener mItemClickLitener;

    public SongListAdapter(Context context, List<Song> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mSongList = data;
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        mItemClickLitener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SongListAdapter.ViewHolder holder, int position) {
        Song song = mSongList.get(position);
        holder.tvSongName.setText(song.songName);
        holder.tvSingerName.setText(song.singerName);

        if (mItemClickLitener != null) {
            holder.btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mItemClickLitener.onItemClick(view, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mSongList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongName;
        TextView tvSingerName;
        Button btnPlay;

        ViewHolder(View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tv_song_name);
            tvSingerName = itemView.findViewById(R.id.tv_singer_name);
            btnPlay = itemView.findViewById(R.id.btn_play);
        }
    }

    public interface ItemClickListener{
        void onItemClick(View view, int position);
    }
}