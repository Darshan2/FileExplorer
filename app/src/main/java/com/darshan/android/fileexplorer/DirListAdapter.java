package com.darshan.android.fileexplorer;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Darshan B.S on 15-09-2018.
 */

public class DirListAdapter extends RecyclerView.Adapter<DirListAdapter.DirecListViewHolder> {
    private static final String TAG = "DirListAdapter";
    private Context mContext;
    private ContentResolver mContentResolver;
    private ThumbUtils mThumbUtils;
    private HashMap<String, Long> mFolderMap;
    private ArrayList<String> mDirecList;
    private DirClickListener mDirClickListener;

    public DirListAdapter(Context mContext, ContentResolver contentResolver, HashMap<String, Long> mFolderMap, DirClickListener mDirClickListener) {
        this.mContext = mContext;
        this.mFolderMap = mFolderMap;
        this.mDirClickListener = mDirClickListener;
        this.mContentResolver = contentResolver;
        mThumbUtils = new ThumbUtils();

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
        String imageFilePath = mDirecList.get(position);
        final String dirName = new File(imageFilePath).getName();

        Image image = mThumbUtils.getThumbnail(mContentResolver, mFolderMap.get(imageFilePath), imageFilePath);

        if(image != null) {
            holder.ivFolderThumb.setImageBitmap(
                    BitmapFactory.decodeFile(image.getThumbUri()));
        }

        holder.tvDirName.setText(dirName);
        holder.rvFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirClickListener.onDirClick(dirName);
            }
        });
    }

    @Override
    public int getItemCount() {
        mDirecList = new ArrayList<>(mFolderMap.keySet());
        int size = mDirecList.size();
        return size;

    }


    class DirecListViewHolder extends RecyclerView.ViewHolder {
        TextView tvDirName;
        ImageView ivFolderThumb;
        RelativeLayout rvFolder;

        public DirecListViewHolder(View itemView) {
            super(itemView);

            tvDirName = itemView.findViewById(R.id.dirName_TV);
            ivFolderThumb = itemView.findViewById(R.id.folderThumb_IV);
            rvFolder = itemView.findViewById(R.id.folder_RV);
        }
    }
}
