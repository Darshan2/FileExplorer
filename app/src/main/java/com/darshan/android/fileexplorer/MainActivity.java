package com.darshan.android.fileexplorer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements FileListAdapter.GridImageClickListener,
        DirListAdapter.DirClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 110;

    private final String IMAGE_TYPE = "images";
    private final String VIDEO_TYPE = "videos";
    private final String LOCATION_INTERNAL = "Device internal memory";
    private final String LOCATION_SDCard = "SD card memory";


    private HashMap<String, ArrayList<Image>> mImageAndThumbMap;
    private HashMap<String, Long> mFolderAndIdMap;
    private ArrayList<Image> mGridImagesList;
    private ArrayList<String> mDirecNameList;

    private HashSet<String> mLastSubDirSet;
    private ThumbUtils mThumbUtils;






    private String mDeviceInternalMemoryRootPath;
    private String mSdCardRootPath;


    //MIME type of wanted files
    private String mMediaType;

    private ArrayList<String> mDirsInDevice;
    private ArrayList<String> mDirsInSdCard;

    private Stack<File> mParentStack;
    private HashMap<String, ArrayList<File>> mFileMap;

    private HashSet<String> mValidImgExtensions;
    private HashSet<String> mValidVideoExtensions;

    //widgets
    private RecyclerView mImageGridRecyclerView;
    private RecyclerView mDirNameRecyclerView;
    private FileListAdapter mImageGridAdapter;
    private DirListAdapter mDirListAdapter;


    @Override
    public void onGridImageClicked(File subDir) {

    }

    @Override
    public void onDirClick(String dirName) {
        Log.d(TAG, "onDirClick: " + dirName);
        mDirNameRecyclerView.setVisibility(View.GONE);
        mImageGridRecyclerView.setVisibility(View.VISIBLE);
        if(mGridImagesList != null ) {
            mGridImagesList.clear();
        }
        getAllFilesInFolder(dirName);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageAndThumbMap = new HashMap<>();
        mLastSubDirSet = new HashSet<>();
        mFolderAndIdMap = new HashMap<>();
        mThumbUtils = new ThumbUtils();



        mFileMap = new HashMap<>();
        mParentStack = new Stack<>();
        mValidImgExtensions = new HashSet<>();
        mValidVideoExtensions = new HashSet<>();

        mImageGridRecyclerView = findViewById(R.id.gridImage_RV);
        mDirNameRecyclerView = findViewById(R.id.dirName_RV);

        initRecyclerLists();
        checkPermissions();

    }


    private void initRecyclerLists() {
        mGridImagesList = new ArrayList<>();
        mImageGridAdapter = new FileListAdapter(this, mGridImagesList, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        mImageGridRecyclerView.setLayoutManager(gridLayoutManager);
        mImageGridRecyclerView.setAdapter(mImageGridAdapter);

        mDirecNameList = new ArrayList<>();
        mDirListAdapter = new DirListAdapter(this, getContentResolver(), mFolderAndIdMap, this);
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(this, 3);
        mDirNameRecyclerView.setLayoutManager(gridLayoutManager1);
        mDirNameRecyclerView.setAdapter(mDirListAdapter);

    }

    private void toggleMedia() {
        Intent intent = getIntent();

        if (intent.hasExtra(getString(R.string.media_type))) {
            if (intent.getStringExtra(getString(R.string.media_type)).equals(getString(R.string.image))) {
                getAllImages();
            } else {
                getAllVideos();
            }
        }
    }

    private void getAllImages() {
        mMediaType = IMAGE_TYPE;
        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        //Get all the images(both in device/SdCard) and store them in cursor
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                null);

        getDirectoriesWithMedia(cursor);

    }

    private void getAllVideos() {
        mMediaType = VIDEO_TYPE;
        String[] projection = {MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.Media._ID};

        Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        getDirectoriesWithMedia(cursor);

    }


    private void getDirectoriesWithMedia(Cursor cursor) {
        Log.d(TAG, "getDirectoriesWithMedia: ");

        //Total number of images/videos
        int count = cursor.getCount();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);

            //TODO add further filters to include new media type files.
            if (mMediaType.equals(IMAGE_TYPE)) {
                //Getting image root path and id by querying MediaStore
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(dataColumnIndex);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                //add images parent root path, image id to map so that i can access them in Adapter
                String subDirPath = filePath.substring(0, filePath.lastIndexOf("/"));
                boolean addedFolder = mLastSubDirSet.add(subDirPath);
                if(addedFolder) {
                    mFolderAndIdMap.put(subDirPath, imageId);
                }





            } else if (mMediaType.equals(VIDEO_TYPE)) {
                //Geeing types of all videos in device
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
                String filePath = cursor.getString(dataColumnIndex);

                int videoIdColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                long videoId = cursor.getLong(videoIdColumnIndex);
            }
        }

        cursor.close();
        mDirListAdapter.notifyDataSetChanged();
    }


    private void getAllFilesInFolder(String folderName) {
//        Log.d(TAG, "getAllFilesInFolder: " + folderName);
        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        final String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        final String[] selectionArg = {folderName};

        //Get all the images under the folder store them in cursor
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                selection,
                selectionArg,
                null);

        if(cursor != null) {
            int size = cursor.getCount();
            for(int i = 0 ; i < size; i++) {
                cursor.moveToPosition(i);
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(dataColumnIndex);
                Log.d(TAG, "getAllFilesInFolder: file " + filePath);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                //TODO do this in worker threads to reduce time
                Image image = mThumbUtils.getThumbnail(getContentResolver(), imageId, filePath);
                saveImage(image);

//                getThumbnail(imageId, filePath);
            }
        }

        mImageGridAdapter.notifyDataSetChanged();
    }



    /**
     *
     * @param sourceId id of original image/video in their corresponding table
     * @param filePath
     */
    private void getThumbnail(long sourceId, String filePath) {
        String[] projection = {MediaStore.Images.Thumbnails.DATA};
        ContentResolver contentResolver = getContentResolver();

        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                contentResolver,
                sourceId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                projection
        );

        if (cursor != null && cursor.moveToFirst()) {
            String imageThumbPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            File file = new File(imageThumbPath);
            //Check if the thumb file pointed by imageThumbPath exist in device(user may have deleted them)
            if (file.exists()) {
//                Log.d(TAG, "getThumbnail: file exists");
                Image image = new Image(filePath, imageThumbPath);
                saveImage(image);
            } else {
//                Log.d(TAG, "getThumbnail: entry exist, file not " + imageThumbPath);
                //Entry with give sourceId already exist in MediaStore db, so update that row
                createImageThumbNail(sourceId, filePath, imageThumbPath);
            }

            cursor.close();

        } else {
            //Entry with give sourceId does not exist in MediaStore db, so insert that row
            createImageThumbNail(sourceId, filePath, null);
        }

        mImageGridAdapter.notifyDataSetChanged();
    }


    //Create a bitmap for image and store its uri in MediaStore db
    private void createImageThumbNail(long imageId, String imagePath, @Nullable String previousThumbPath) {
        ContentResolver contentResolver = getContentResolver();
        //Create a bitmap for image. Do it in worker thread
        Bitmap sourceBm = MediaStore.Images.Thumbnails.getThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null
        );

        Log.d(TAG, "createImageThumbNail: imagePath " + imagePath);
        Log.d(TAG, "createImageThumbNail: bm " + sourceBm);

        //Store/update that bitmap in MediaStore
        if (sourceBm != null) {
             storeThumbnail(imagePath, previousThumbPath, contentResolver, sourceBm, imageId, 50F,
                    50F, MediaStore.Images.Thumbnails.MINI_KIND);
        }
    }


    /* Put the thumbnail in MediaStore db,
     * if the row with image_id = image_source_id(Source Image id from Image table) already
      * exist in media store db newThumbEntry = false */

    private void storeThumbnail(String imagePath, @Nullable String previousThumbPath, ContentResolver cr,
                                Bitmap source, long id, float width, float height, int kind) {
        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Thumbnails.KIND,     kind);
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, (int)id);
        values.put(MediaStore.Images.Thumbnails.HEIGHT,   thumb.getHeight());
        values.put(MediaStore.Images.Thumbnails.WIDTH,    thumb.getWidth());

        File thumbFile;
        if(previousThumbPath == null) {
//            Log.d(TAG, "storeThumbnail: no thumb, no previous entry in db");
            //i.e previously no row exist in thumb table with given sourceId(Image _id from Image table )
            //Uri pointing to where the thumb is stored in device
            Uri thumbUri = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

            try {
                OutputStream thumbOut = cr.openOutputStream(thumbUri);
                thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
                thumbOut.close();

            } catch (FileNotFoundException ex) {
                Log.e(TAG, "StoreThumbnail: ", ex);
            }
            catch (IOException ex) {
                Log.e(TAG, "StoreThumbnail: ", ex);
            }

            //You have to re-query the MediaStore to get the thumb path.
            getNewThumbFromDb(imagePath, id);

        } else {
            //i.e previously row exist in thumb table with given sourceId(Image _id from Image table)
            int result = cr.update(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    values,
                    "image_id = " + id,
                    null);
            Log.d(TAG, "storeThumbnail: update " + result);

            thumbFile = new File(previousThumbPath);

            //Putting the generated bitmap into device storage, pointed by above url
            try {
                OutputStream thumbOut = new FileOutputStream(thumbFile);

                thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
                thumbOut.close();
            }
            catch (FileNotFoundException ex) {
                Log.e(TAG, "StoreThumbnail: ", ex);
            }
            catch (IOException ex) {
                Log.e(TAG, "StoreThumbnail: ", ex);
            }
        }
    }


    private void getNewThumbFromDb(String imagePath, long imageId) {
        //Query MediaStore again to get the newly created thumb url for the image
        String[] projection = {MediaStore.Images.Thumbnails.DATA};

        Cursor cursor2 = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                getContentResolver(),
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                projection
        );

        if (cursor2 != null && cursor2.moveToFirst()) {
            String imageThumbPath = cursor2.getString(cursor2.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            Log.d(TAG, "createImageThumbNail: thumbPath " + imageThumbPath);
            Image image = new Image(imagePath, imageThumbPath);
            saveImage(image);
            cursor2.close();
        }

    }


    //Save image related info in map
    private void saveImage(Image image) {
        int size = mGridImagesList.size();
        if(size > 0) {
            mGridImagesList.add(size , image);
        } else {
            mGridImagesList.add(image);
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
        } else {
            super.onBackPressed();
        }
    }


}
