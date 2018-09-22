package com.darshan.android.fileexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements FileListAdapter.GridImageClickListener,
        DirListAdapter.DirClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 110;

    private final String IMAGE_TYPE = "images";
    private final String VIDEO_TYPE = "videos";

    private HashMap<String, ArrayList<Image>> mImageAndThumbMap;
    private ArrayList<Image> mGridImagesList;
    private ArrayList<Image> mDirecList;
    private ArrayList<LoadThumbAsyncTask> mAsyncTaskLists;

    private HashSet<String> mLastSubDirSet;
    private ThumbUtils mThumbUtils;

    //MIME type of wanted files
    private String mMediaType;
    private boolean isFolderList;


    //widgets
    private RecyclerView mImageGridRecyclerView;
    private RecyclerView mDirNameRecyclerView;
    private ProgressBar mLoadProgressBar;
    private Toolbar mGalleryToolbar;
    private ActionBar mGalleryActionBar;


    private FileListAdapter mImageGridAdapter;
    private DirListAdapter mDirListAdapter;


    private TextView mSelectTV;
    private TextView mItemCountTV;
    private RelativeLayout mSelectRL;


    @Override
    public void onGridImageClicked(File subDir) {

    }

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
        getAllFilesInFolder(dirName);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageGridRecyclerView = findViewById(R.id.gridImage_RV);
        mDirNameRecyclerView = findViewById(R.id.dirName_RV);
        mLoadProgressBar = findViewById(R.id.load_PB);
        mGalleryToolbar = findViewById(R.id.gallery_toolbar);

        //set up the tool bar, as action bar
        setSupportActionBar(mGalleryToolbar);
        mGalleryActionBar = getSupportActionBar();
        mGalleryActionBar.setDisplayHomeAsUpEnabled(true);

        hideSelectBar();






//        mSelectTV = findViewById(R.id.select_TV);
//        mItemCountTV = findViewById(R.id.itemCount_TV);
//        mSelectRL = findViewById(R.id.select_RL);

        mLoadProgressBar.setVisibility(View.VISIBLE);

        mImageAndThumbMap = new HashMap<>();
        mLastSubDirSet = new HashSet<>();
        mThumbUtils = new ThumbUtils();
        mAsyncTaskLists = new ArrayList<>();


        initRecyclerLists();
        checkPermissions();

//        mSelectTV.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                HashSet<Image> selectedImages = mImageGridAdapter.getSelectedItems();
//                for(Image image: selectedImages) {
//                    Log.d(TAG, "onClick: " + image);
//                }
//                mItemCountTV.setText(selectedImages.size() + "items");
//                refreshGridImageList();
//            }
//        });

    }


    private void initRecyclerLists() {
//        mLoadProgressBar.setVisibility(View.VISIBLE);

        mGridImagesList = new ArrayList<>();
        mImageGridAdapter = new FileListAdapter(this, mGridImagesList, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        mImageGridRecyclerView.setLayoutManager(gridLayoutManager);
        mImageGridRecyclerView.setHasFixedSize(true);
        mImageGridRecyclerView.setAdapter(mImageGridAdapter);


        mDirecList = new ArrayList<>();
        mDirListAdapter = new DirListAdapter(this, getContentResolver(), mDirecList, this);
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(this, 2);
        mDirNameRecyclerView.setLayoutManager(gridLayoutManager1);
        mDirNameRecyclerView.setAdapter(mDirListAdapter);

    }

    private void toggleMedia() {
        Intent intent = getIntent();

        if (intent.hasExtra(getString(R.string.media_type))) {
            if (intent.getStringExtra(getString(R.string.media_type)).equals(getString(R.string.image))) {
                mMediaType = IMAGE_TYPE;
            } else {
                mMediaType = VIDEO_TYPE;
            }
            getAllMediaFilesFromDb();
        }
    }


    private void stopPreviousAsyncTasks() {
        for (LoadThumbAsyncTask asyncTask : mAsyncTaskLists) {
            asyncTask.cancel(true);
        }
        mAsyncTaskLists.clear();
    }


    private void getAllMediaFilesFromDb() {
        Uri mediaUri = null;
        if (mMediaType.equals(IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mMediaType.equals(VIDEO_TYPE)) {
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

            getDirectoriesWithMedia(cursor);
        }
    }


    private void getDirectoriesWithMedia(Cursor cursor) {
        Log.d(TAG, "getDirectoriesWithMedia: ");
        isFolderList = true;

        //Total number of images/videos
        int count = cursor.getCount();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            //TODO add further filters to include new media type files.
            if (mMediaType.equals(IMAGE_TYPE) || mMediaType.equals(VIDEO_TYPE)) {
                //Getting image/video root path and id by querying MediaStore
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(dataColumnIndex);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                //add images parent root path, image id to map so that i can access them in Adapter
                String subDirName = new File(filePath.substring(0, filePath.lastIndexOf("/"))).getName();
                boolean addedFolder = mLastSubDirSet.add(subDirName);
                if (addedFolder) {
                    Log.d(TAG, "getDirectoriesWithMedia: folders " + subDirName);
                    //Avoiding adding first image of the folder if that file is of 0 size
                    if (new File(filePath).length() == 0) {
                        mLastSubDirSet.remove(subDirName);
                    } else {
                        LoadThumbAsyncTask asyncTask = new LoadThumbAsyncTask();
                        mAsyncTaskLists.add(asyncTask);
                        asyncTask.execute(filePath, String.valueOf(imageId));
                    }
                }
            }
        }
        cursor.close();
        mLoadProgressBar.setVisibility(View.GONE);
    }


    private void getAllFilesInFolder(String folderName) {
        Log.d(TAG, "getAllFilesInFolder: ");
//        Log.d(TAG, "getAllFilesInFolder: " + folderName);
        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        final String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        final String[] selectionArg = {folderName};

        //Name of several image and video data base columns are same.
        Uri mediaUri = null;
        if (mMediaType.equals(IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mMediaType.equals(VIDEO_TYPE)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        //Get all the files under the folder store them in cursor
        Cursor cursor = getContentResolver().query(
                mediaUri,
                columns,
                selection,
                selectionArg,
                null);

        if (cursor != null) {
            int size = cursor.getCount();
            for (int i = 0; i < size; i++) {
                cursor.moveToPosition(i);
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(dataColumnIndex);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                Log.d(TAG, "getAllFilesInFolder: " + filePath + imageId);
                LoadThumbAsyncTask asyncTask = new LoadThumbAsyncTask();
                mAsyncTaskLists.add(asyncTask);
                asyncTask.execute(filePath, String.valueOf(imageId));
            }
            cursor.close();
        }
    }


    class LoadThumbAsyncTask extends AsyncTask<String, Void, Image> {

        @Override
        protected Image doInBackground(String... strings) {
            try {
                String filePath = strings[0];
                String imageId = strings[1];

                if (!isCancelled()) {
                    Image image = mThumbUtils.getMediaThumbnail(mMediaType, getContentResolver(), Long.valueOf(imageId), filePath);
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


    private void displayAllFiles() {
        Set<String> keySet = mImageAndThumbMap.keySet();
        for (String str : keySet) {
            Log.d(TAG, "displayAllFiles: " + str);
            ArrayList<Image> images = mImageAndThumbMap.get(str);
            for (Image img : images) {
                Log.d(TAG, "displayAllFiles: " + img);
            }
        }
    }

    public void showSelectBar(int itemCount) {
        mGalleryActionBar.setTitle(itemCount + " items");
        mGalleryActionBar.show();
    }

    public void hideSelectBar() {
        mGalleryActionBar.setTitle("Select items");
//        mGalleryActionBar.hide();
    }

    private void refreshGridImageList() {
        hideSelectBar();
        mImageGridAdapter.clearSelectedList();
        mImageGridAdapter.notifyDataSetChanged();
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
            toggleMedia();
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
                    toggleMedia();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_select_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int itemId = item.getItemId();
//        if(itemId == R.id.action_select) {
//            hideSelectBar();
//            HashSet<Image> selectedImages = mImageGridAdapter.getSelectedItems();
//            for (Image image : selectedImages) {
//                Log.d(TAG, "onClick: " + image);
//            }
//            refreshGridImageList();
//            return true;
//        }

//        return super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.action_select : {
                HashSet<Image> selectedImages = mImageGridAdapter.getSelectedItems();
                for (Image image : selectedImages) {
                    Log.d(TAG, "onClick: " + image);
                }
                refreshGridImageList();
                return true;
            }

//            case R.id.homeAsUp : {
//                onBackPressed();
//                return true;
//            }

            default:return super.onOptionsItemSelected(item);
        }

    }
}
