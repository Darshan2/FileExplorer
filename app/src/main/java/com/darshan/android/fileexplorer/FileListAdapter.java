package com.darshan.android.fileexplorer;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Darshan B.S on 13-09-2018.
 */

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
    private static final String TAG = "FileListAdapter";

    private Context mContext;
    private ArrayList<Image> mImageFilesList;
    private GridImageClickListener mListener;

    interface GridImageClickListener {
        void onGridImageClicked(File clickedSubDir);
    }

    public FileListAdapter(Context mContext, ArrayList<Image> mImageFilesList, GridImageClickListener mListener) {
        this.mContext = mContext;
        this.mImageFilesList = mImageFilesList;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_gallery_file, null);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Image image = mImageFilesList.get(position);
        Log.d(TAG, "onBindViewHolder: " + image.getImageUri());
        holder.ivFileImage.setImageBitmap(BitmapFactory.decodeFile(image.getThumbUri()));

//        int numSubDir = mFileMap.get("SubDir").size();
//
//        if(position < numSubDir) {
//            File currentFile = mFileMap.get(mContext.getString(R.string.SubDir)).get(position);
//            holder.tvFileName.setText(currentFile.getName());
//
//        } else {
//            File currentFile = mFileMap.get(mContext.getString(R.string.Files)).get(position - numSubDir);
//            holder.tvFileName.setText(currentFile.getAbsolutePath());
//        }

//        holder.tvFileName.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mListener.onGridImageClicked(new File(image.getImageUri()));
//
//            }
//        });


    }


    @Override
    public int getItemCount() {
//        int size = 0;
//        if(mFileMap != null) {
//            if(mFileMap.get("Files") != null) {
//                size = size + mFileMap.get("Files").size();
//            }
//
//            if(mFileMap.get("SubDir") != null) {
//                size = size + mFileMap.get("SubDir").size();
//            }
//            return size;
//        }
//        Log.d(TAG, "getItemCount: " +size);
        return mImageFilesList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

         TextView tvFileName;
         ImageView ivFileImage;

         ViewHolder(View itemView) {
            super(itemView);

//            tvFileName = itemView.findViewById(R.id.fileName_TV);
            ivFileImage = itemView.findViewById(R.id.fileImage_IV);
        }
    }
}
