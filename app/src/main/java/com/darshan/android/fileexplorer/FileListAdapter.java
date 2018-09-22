package com.darshan.android.fileexplorer;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Darshan B.S on 13-09-2018.
 */

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
    private static final String TAG = "FileListAdapter";

    private Context mContext;
    private ArrayList<Image> mImageFilesList;
    private GridImageClickListener mListener;
    private HashSet<Image> mSelectedImageSet;
    private boolean selectEnabled = false;

    interface GridImageClickListener {
        void onGridImageClicked(File clickedSubDir);
    }

    public FileListAdapter(Context mContext, ArrayList<Image> mImageFilesList, GridImageClickListener mListener) {
        this.mContext = mContext;
        this.mImageFilesList = mImageFilesList;
        this.mListener = mListener;
        mSelectedImageSet = new HashSet<>();
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
        holder.ivFileImage.setImageBitmap(BitmapFactory.decodeFile(image.getThumbUri()));

        if(!selectEnabled) {
            holder.ivSelectedLogo.setVisibility(View.GONE);
        }

        holder.ivFileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectEnabled = true;
                if(mSelectedImageSet.contains(image)) {
                    mSelectedImageSet.remove(image);
                    deHighlightView(holder.ivSelectedLogo);
                } else {
                    mSelectedImageSet.add(image);
                    highlightView(holder.ivSelectedLogo);
                }
            }

        });

    }


    @Override
    public int getItemCount() {
        return mImageFilesList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
         ImageView ivFileImage;
         ImageView ivSelectedLogo;

         ViewHolder(View itemView) {
            super(itemView);

            ivFileImage = itemView.findViewById(R.id.fileImage_IV);
            ivSelectedLogo = itemView.findViewById(R.id.selectedIcon);
        }
    }

    private void highlightView(ImageView imageView) {
        ((MainActivity)mContext).showSelectBar(mSelectedImageSet.size());

        imageView.setVisibility(View.VISIBLE);
    }

    private void deHighlightView(ImageView imageView) {
        if(mSelectedImageSet.size() == 0) {
            selectEnabled = false;
            ((MainActivity)mContext).hideSelectBar();
        } else {
            ((MainActivity) mContext).showSelectBar(mSelectedImageSet.size());
        }
        imageView.setVisibility(View.GONE);
    }

    public HashSet<Image> getSelectedItems() {
        return mSelectedImageSet;
    }

    public void clearSelectedList() {
        selectEnabled = false;
        mSelectedImageSet.clear();
    }
}
