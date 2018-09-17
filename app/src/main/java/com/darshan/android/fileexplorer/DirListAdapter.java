package com.darshan.android.fileexplorer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Darshan B.S on 15-09-2018.
 */

public class DirListAdapter extends RecyclerView.Adapter<DirListAdapter.DirecListViewHolder> {
    private static final String TAG = "DirListAdapter";
    private Context mContext;
    private ArrayList<String> mDirecList;
    private DirClickListener mDirClickListener;

    public DirListAdapter(Context mContext, ArrayList<String> mDirecList, DirClickListener mDirClickListener) {
        this.mContext = mContext;
        this.mDirecList = mDirecList;
        this.mDirClickListener = mDirClickListener;
    }

    interface DirClickListener {
        void onDirClick(String dirName);
    }

    @NonNull
    @Override
    public DirecListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_gallery_direc, null);

        return new DirecListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DirecListViewHolder holder, final int position) {
        final String dirName = mDirecList.get(position);
        holder.tvDirName.setText(dirName);
        holder.tvDirName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirClickListener.onDirClick(dirName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDirecList.size();
    }


    class DirecListViewHolder extends RecyclerView.ViewHolder {
        TextView tvDirName;

        public DirecListViewHolder(View itemView) {
            super(itemView);

            tvDirName = itemView.findViewById(R.id.dirName_TV);
        }
    }
}
