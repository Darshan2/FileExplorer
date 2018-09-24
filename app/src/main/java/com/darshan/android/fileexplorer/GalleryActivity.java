package com.darshan.android.fileexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;


public class GalleryActivity extends AppCompatActivity implements DirListAdapter.DirClickListener {
    private static final String TAG = GalleryActivity.class.getSimpleName();
    private static final int REQUEST_STORAGE_PERMISSION = 110;

    private final String IMAGE_TYPE = "images";
    private final String VIDEO_TYPE = "videos";

    private ArrayList<Image> mGridImagesList;
    private ArrayList<Image> mDirecList;
    private ArrayList<LoadThumbAsyncTask> mAsyncTaskLists;
    private HashSet<String> mLastSubDirSet;
    private ThumbUtils mThumbUtils;
    //MIME type of wanted files
    private String mRequiredMediaType;
    private boolean isFolderList;
    private int mNumFiles = 0;


    //widgets
    private RecyclerView mImageGridRecyclerView;
    private RecyclerView mDirNameRecyclerView;
    private RelativeLayout mFileSelectToolbar;
    private RelativeLayout mFoldersToolbar;
    private ProgressBar mLoadProgressBar;

    private FileListAdapter mImageGridAdapter;
    private DirListAdapter mDirListAdapter;


    @Override
    public void onDirClick(String dirName) {
        Log.d(TAG, "onDirClick: " + dirName);
        //hide SubDirectory list
        mDirNameRecyclerView.setVisibility(View.GONE);
        mImageGridRecyclerView.setVisibility(View.VISIBLE);

        if (mGridImagesList != null) {
            mGridImagesList.clear();
        }

        mImageGridAdapter.notifyDataSetChanged();
        isFolderList = false;
        //Stop AsyncTask from loading previously selected folder's files thumb images
        stopPreviousAsyncTasks();
        getMediaFiles(dirName);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageGridRecyclerView = findViewById(R.id.gridImage_RV);
        mDirNameRecyclerView = findViewById(R.id.dirName_RV);
        mLoadProgressBar = findViewById(R.id.load_PB);
        mFoldersToolbar = findViewById(R.id.folders_toolbar);
        mFileSelectToolbar = findViewById(R.id.itemSelect_toolbar);

        showFolderSelectBar();

        mLoadProgressBar.setVisibility(View.VISIBLE);

        mLastSubDirSet = new HashSet<>();
        mThumbUtils = new ThumbUtils();
        mAsyncTaskLists = new ArrayList<>();

        //get intent extra
        Intent intent = getIntent();
        if (intent.hasExtra(GalleryConsts.INTENT_MEDIA_TYPE)) {
            mRequiredMediaType = intent.getStringExtra(GalleryConsts.INTENT_MEDIA_TYPE);
        }

        initRecyclerLists();
        checkPermissions();

    }


    private void initRecyclerLists() {
        mGridImagesList = new ArrayList<>();
        mImageGridAdapter = new FileListAdapter(this, mGridImagesList);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        mImageGridRecyclerView.setLayoutManager(gridLayoutManager);
        mImageGridRecyclerView.setHasFixedSize(true);
        mImageGridRecyclerView.setAdapter(mImageGridAdapter);


        mDirecList = new ArrayList<>();
        mDirListAdapter = new DirListAdapter(this, mDirecList, this, mRequiredMediaType);
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(this, 2);
        mDirNameRecyclerView.setLayoutManager(gridLayoutManager1);
        mDirNameRecyclerView.setAdapter(mDirListAdapter);

    }

    private void toggleMediaFolders() {
        switch (mRequiredMediaType) {
            case GalleryConsts.IMAGE_TYPE:
                getAllMediaFolderFromDb(GalleryConsts.IMAGE_TYPE);
                break;
            case GalleryConsts.VIDEO_TYPE:
                getAllMediaFolderFromDb(GalleryConsts.VIDEO_TYPE);
                break;
            case GalleryConsts.IMAGE_VIDEO_TYPE:
                getAllMediaFolderFromDb(GalleryConsts.IMAGE_TYPE);
                getAllMediaFolderFromDb(GalleryConsts.VIDEO_TYPE);
                break;
            default:break;
        }
    }

    private void getMediaFiles(String folderName) {
        switch (mRequiredMediaType) {
            case GalleryConsts.IMAGE_TYPE:
                getOnlyImageFiles(folderName);
                break;
            case GalleryConsts.VIDEO_TYPE:
                getOnlyVideoFiles(folderName);
                break;
            case GalleryConsts.IMAGE_VIDEO_TYPE:
                getBothImagesVideosFiles(folderName);
                break;
            default:break;
        }
    }

    private int getOnlyImageFiles(String folderName) {
        Cursor cursor = getFolderCursor(folderName, GalleryConsts.IMAGE_TYPE);
        int itemCount = cursor.getCount();
        mNumFiles = itemCount;
        getAllFilesInFolder(cursor, GalleryConsts.IMAGE_TYPE);
        return itemCount;
    }

    private int getOnlyVideoFiles(String folderName) {
        Cursor cursor = getFolderCursor(folderName, GalleryConsts.VIDEO_TYPE);
        int itemCount = cursor.getCount();
        mNumFiles = itemCount;
        getAllFilesInFolder(cursor, GalleryConsts.VIDEO_TYPE);
        return itemCount;
    }

    private void getBothImagesVideosFiles(String folderName) {
        mNumFiles = getOnlyImageFiles(folderName) + getOnlyVideoFiles(folderName);
    }


    private void stopPreviousAsyncTasks() {
        for (LoadThumbAsyncTask asyncTask : mAsyncTaskLists) {
            asyncTask.cancel(true);
        }
        mAsyncTaskLists.clear();
    }


    private void getAllMediaFolderFromDb(String mediaType) {
        Uri mediaUri = null;
        if (mediaType.equals(IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType.equals(VIDEO_TYPE)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        //Get all the images(both in device/SdCard) and store them in cursor
        if (mediaUri != null) {
            Cursor cursor = getContentResolver().query(
                    mediaUri,
                    columns,
                    null,
                    null,
                    null);

            getDirectoriesWithMedia(cursor, mediaType);
        }
    }


    //Cursor will determine which media directories we are getting
    private void getDirectoriesWithMedia(Cursor cursor, String mediaType) {
        Log.d(TAG, "getDirectoriesWithMedia: ");
        isFolderList = true;

        //Total number of images/videos
        int count = cursor.getCount();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            //TODO add further filters to include new media type files.
            if (mediaType.equals(IMAGE_TYPE) || mediaType.equals(VIDEO_TYPE)) {
                //Getting image/video root path and id by querying MediaStore
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(dataColumnIndex);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                //add images parent root path, image id to map so that i can access them in Adapter
                String subDirName = new File(filePath.substring(0, filePath.lastIndexOf("/"))).getName();
                boolean addedFolder = mLastSubDirSet.add(subDirName);
                if (addedFolder) {
                    //Avoiding adding first image of the folder if that file is of 0 size
                    if (new File(filePath).length() == 0) {
                        mLastSubDirSet.remove(subDirName);
                    } else {
                        LoadThumbAsyncTask asyncTask = new LoadThumbAsyncTask(mediaType);
                        mAsyncTaskLists.add(asyncTask);
                        asyncTask.execute(filePath, String.valueOf(imageId));
                    }
                }
            }
        }
        cursor.close();
        mLoadProgressBar.setVisibility(View.GONE);
    }


    //Called when Folder is clicked
    public Cursor getFolderCursor(String folderName, String mediaType) {
        Log.d(TAG, "getFolderCursor: " + folderName);
        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        final String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        final String[] selectionArg = {folderName};

        //Name of several image and video data base columns are same.
        Uri mediaUri = null;
        if (mediaType.equals(IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType.equals(VIDEO_TYPE)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        //Get all the files under the folder store them in cursor
        Cursor cursor = null;
        if (mediaUri != null) {
            cursor = getContentResolver().query(
                    mediaUri,
                    columns,
                    selection,
                    selectionArg,
                    null);
        }
        return cursor;
    }


    private void getAllFilesInFolder(Cursor folderCursor, String mediaType) {
        if (folderCursor != null) {
            int size = folderCursor.getCount();
            for (int i = 0; i < size; i++) {
                folderCursor.moveToPosition(i);
                int dataColumnIndex = folderCursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = folderCursor.getString(dataColumnIndex);

                int imageIdColumnIndex = folderCursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = folderCursor.getLong(imageIdColumnIndex);

//                Log.d(TAG, "getAllFilesInFolder: " + filePath + imageId);
                LoadThumbAsyncTask asyncTask = new LoadThumbAsyncTask(mediaType);
                mAsyncTaskLists.add(asyncTask);
                asyncTask.execute(filePath, String.valueOf(imageId));
            }
            folderCursor.close();
        }
    }


    class LoadThumbAsyncTask extends AsyncTask<String, Void, Image> {
        String mediaType;

        public LoadThumbAsyncTask(String mediaType) {
            this.mediaType = mediaType;
        }

        @Override
        protected Image doInBackground(String... strings) {
            try {
                String filePath = strings[0];
                String imageId = strings[1];

                if (!isCancelled()) {
                    Image image = mThumbUtils.getMediaThumbnail(mediaType, getContentResolver(), Long.valueOf(imageId), filePath);
                    if(image != null) {
                        if (mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
                            image.setVideo(true);
                        } else if (mediaType.equals(GalleryConsts.IMAGE_TYPE)) {
                            image.setVideo(false);
                        }
                    }
                    return image;
                } else {
                    Log.d(TAG, "doInBackground: cancelled");
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "doInBackground: ", e);
                return null;
            }

        }

        @Override
        protected void onPostExecute(Image image) {
            super.onPostExecute(image);
            if (image != null) {
                saveImage(image);
            }
        }
    }


    //Save files related info in lists
    private void saveImage(Image image) {
        if (image != null) {
            if (!isFolderList) {
                int size = mGridImagesList.size();
                if (size > 0) {
                    mGridImagesList.add(size, image);
                } else {
                    mGridImagesList.add(image);
                }
                mImageGridAdapter.notifyItemInserted(size);

            } else {
                int size = mDirecList.size();
                if (size > 0) {
                    mDirecList.add(size, image);
                } else {
                    mDirecList.add(image);
                }
                mDirListAdapter.notifyItemInserted(size);
            }
        }
    }


    public void showFileSelectBar(int itemCount) {
        mFileSelectToolbar.setVisibility(View.VISIBLE);
        mFoldersToolbar.setVisibility(View.GONE);

        TextView itemCountTV = findViewById(R.id.itemCount_TV);
        String countText = itemCount + getString(R.string.items);
        itemCountTV.setText(countText);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshGridImageList();
            }
        });

        TextView selectTV = findViewById(R.id.select_TV);

        selectTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send the result back to calling activity
                HashSet<Image> selectedImages = mImageGridAdapter.getSelectedItems();
                for (Image image : selectedImages) {
                    Log.d(TAG, "onClick: " + image);
                }
                sendResultBack(selectedImages);
//                refreshGridImageList();
            }
        });

//        mGalleryActionBar.setTitle(itemCount + " items");
//        mGalleryActionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.lokal_color)));
    }

    public void showFolderSelectBar() {
        mFileSelectToolbar.setVisibility(View.GONE);
        mFoldersToolbar.setVisibility(View.VISIBLE);

        ImageView backArrow = findViewById(R.id.backArrow_yellow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


//        mGalleryActionBar.setTitle("Select items");
//        mGalleryActionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.white)));

    }

    public int getNumFiles() {
        return mNumFiles;
    }

    private void refreshGridImageList() {
        showFolderSelectBar();
        mImageGridAdapter.clearSelectedList();
        mImageGridAdapter.notifyDataSetChanged();
    }

    //Send result back to calling activity
    private void sendResultBack(HashSet<Image> selectedImages) {
        ArrayList<Image> selectedImageList = new ArrayList<>(selectedImages);
        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra(GalleryConsts.INTENT_SELECT_GALLERY_ITEMS, selectedImageList);
        setResult(RESULT_OK, resultIntent);
        finish();
    }









    //-------------------------------------------------------------------------------------------//
    private void checkPermissions() {
        Log.d(TAG, "checkPermissions: ");
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            //Permission is granted proceed
            toggleMediaFolders();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, proceed
                    toggleMediaFolders();

                } else {
                    // If you do not get permission, show a Toast and exit from app.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (mDirNameRecyclerView.getVisibility() != View.VISIBLE) {
            mDirNameRecyclerView.setVisibility(View.VISIBLE);
            mImageGridRecyclerView.setVisibility(View.GONE);

            refreshGridImageList();

        } else {
            super.onBackPressed();
        }
    }

}
