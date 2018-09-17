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
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
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
    private ArrayList<Image> mGridImagesList;
    private ArrayList<String> mDirecNameList;







    private String mDeviceInternalMemoryRootPath;
    private String mSdCardRootPath;

    private String mLocation;
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
        getAllFilesInFolder(dirName, true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageAndThumbMap = new HashMap<>();


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
        mDirListAdapter = new DirListAdapter(this, mDirecNameList, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mDirNameRecyclerView.setLayoutManager(linearLayoutManager);
        mDirNameRecyclerView.setAdapter(mDirListAdapter);

    }

    private void toggleMedia() {
        Intent intent = getIntent();

        if (intent.hasExtra(getString(R.string.location_type))) {
            if (intent.getStringExtra(getString(R.string.location_type)).equals(getString(R.string.device_memory))) {
                mLocation = LOCATION_INTERNAL;
            } else {
                mLocation = LOCATION_SDCard;
            }
        }

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
//        final String orderBy = MediaStore.Images.Media._ID;

        //Get all the images(both in device/SdCard) and store them in cursor
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                null);

        getDirectoriesWithOnly(cursor);

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

        getDirectoriesWithOnly(cursor);

    }


    private void getDirectoriesWithOnly(Cursor cursor) {
        Log.d(TAG, "getDirectoriesWithOnly: ");
        HashSet<String> validDirs = new HashSet<>();

        //Total number of images/videos
        int count = cursor.getCount();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);

            String filePath = "";

            //TODO add further filters to include new media type files.
            if (mMediaType.equals(IMAGE_TYPE)) {
                //Getting types of all images in device
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                filePath = cursor.getString(dataColumnIndex);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                getImageThumbnailAndStore(imageId, filePath);

//                mValidImgExtensions.add(filePath.substring(filePath.lastIndexOf(".")));

            } else if (mMediaType.equals(VIDEO_TYPE)) {
                //Geeing types of all videos in device
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
                filePath = cursor.getString(dataColumnIndex);
//                mValidVideoExtensions.add(filePath.substring(filePath.lastIndexOf(".")));
            }

            //Getting all the folders with images/videos
            String parentPath = filePath.substring(0, filePath.lastIndexOf("/"));
            validDirs.add(parentPath);
        }

        cursor.close();

        mDirecNameList.addAll(mImageAndThumbMap.keySet());
        for(String str : mDirecNameList) {
            Log.d(TAG, "getDirectoriesWithOnly: " + str);
        }
        mDirListAdapter.notifyDataSetChanged();
        displayAllFiles();

//        separateDirectoriesBasedOnStorage(validDirs);

    }


    private void getImageThumbnailAndStore(long imageId, String imagePath) {
        Log.d(TAG, "getImageThumbnailAndStore: ");
        String[] projection = {MediaStore.Images.Thumbnails.DATA};
        ContentResolver contentResolver = getContentResolver();

        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                projection
        );

        String imageThumbPath = "";
        if (cursor != null && cursor.moveToFirst()) {
            imageThumbPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            cursor.close();

            File file = new File(imageThumbPath);

            if (file.exists()) {
                Image image = new Image(imagePath, imageThumbPath);
                saveImage(imagePath, image);
            } else {
                createAndStoreThumbNail(imagePath, imageId);
            }

        } else {
            createAndStoreThumbNail(imagePath, imageId);
        }

    }


    //Create a bitmap for image and store it in MediaStore db
    private void createAndStoreThumbNail(String imagePath, long imageId) {
        Log.d(TAG, "createAndStoreThumbNail: ");
        String[] projection = {MediaStore.Images.Thumbnails.DATA};
        ContentResolver contentResolver = getContentResolver();

        //Create a bitmap for image and store it in MediaStore db
        Bitmap sourceBm = MediaStore.Images.Thumbnails.getThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null
        );

        Log.d(TAG, "createAndStoreThumbNail: imagePath " + imagePath);
        Log.d(TAG, "createAndStoreThumbNail: bm " + sourceBm);

        //Store that bitmap in MediaStore
        if (sourceBm != null) {
            storeThumbnail(contentResolver, sourceBm, imageId, 50F,
                    50F, MediaStore.Images.Thumbnails.MINI_KIND);
        }

        //Query MediaStore again to get the newly created thumb url for the image
        Cursor cursor2 = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                projection
        );

        if (cursor2 != null && cursor2.moveToFirst()) {
            String imageThumbPath = cursor2.getString(cursor2.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            Log.d(TAG, "createAndStoreThumbNail: thumbPath " + imageThumbPath);
            cursor2.close();
            Image image = new Image(imagePath, imageThumbPath);
            saveImage(imagePath, image);
        }


    }


    //Put the thumbnail in MediaStore db
    private void storeThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {
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

        Uri url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);

            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
        }
        catch (FileNotFoundException ex) {
            Log.e(TAG, "StoreThumbnail: ", ex);
        }
        catch (IOException ex) {
            Log.e(TAG, "StoreThumbnail: ", ex );
        }
    }

    //Save image related info in map
    private void saveImage(String imagePath, Image image) {
        String lastSubDirName = new File(imagePath).getParentFile().getName();

        if (mImageAndThumbMap.containsKey(lastSubDirName)) {
            ArrayList<Image> imagesUnderFolder = mImageAndThumbMap.get(lastSubDirName);
            imagesUnderFolder.add(image);
        } else {
            ArrayList<Image> imagesUnderFolder = new ArrayList<>();
            imagesUnderFolder.add(image);
            mImageAndThumbMap.put(lastSubDirName, imagesUnderFolder);
        }
    }


    private void displayAllFiles() {
//        Set<String> keySet = mImageAndThumbMap.keySet();
//        for (String str : keySet) {
//            Log.d(TAG, "displayAllFiles: " + str);
//            ArrayList<Image> images = mImageAndThumbMap.get(str);
//            for (Image img : images) {
//                Log.d(TAG, "displayAllFiles: " + img);
//            }
//        }
    }


    private void getAllFilesInFolder(String folderPath, boolean push) {
        Log.d(TAG, "getAllFilesInFolder: " + folderPath);

        ArrayList<Image> imgesList = mImageAndThumbMap.get(folderPath);
        if(imgesList.size() > 0) {
            mGridImagesList.addAll(imgesList);
            for(Image image : imgesList) {
                Log.d(TAG, "getAllFilesInFolder: " + image);
            }
        }

        mImageGridAdapter.notifyDataSetChanged();

    }













//-------------------------------------------------------------------------------------------------------//
    private void separateDirectoriesBasedOnStorage(HashSet<String> directories) {
        mDirsInDevice = new ArrayList<>();
        mDirsInSdCard = new ArrayList<>();

        mDeviceInternalMemoryRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d(TAG, "separateDirectoriesBasedOnStorage: devvvvvvvvv" + mDeviceInternalMemoryRootPath);

        //Separate folders into Device and SdCard memory.
        for(String str: directories) {
            if(str.contains(mDeviceInternalMemoryRootPath)) {
                mDirsInDevice.add(str);
            } else {
                mDirsInSdCard.add(str);
            }
        }

        mSdCardRootPath = getSdCardRootPath(mDirsInSdCard);

        for(String str : mDirsInDevice) {
            Log.d(TAG, "separateDirectoriesBasedOnStorage: devDirs " + str);
        }

        for(String str : mDirsInSdCard) {
            Log.d(TAG, "separateDirectoriesBasedOnStorage: sdFiles " +str);
        }

        //Toggle between storages here
        if(mLocation.equals(LOCATION_INTERNAL)) {
            loadAllFilesFromDeviceMemory();
        } else if (mLocation.equals(LOCATION_SDCard)) {
             loadAllFilesFromSdCard();
        }

    }


    /**
     * No std. method to get the root directory path of SdCard storage.
     * This root directory vary from device to device based on the manufacturer.
     * @param directoriesInSdCard
     * @return SdCard Root Directory(relative not absolute)
     */
    private String getSdCardRootPath(ArrayList<String> directoriesInSdCard) {
        String sdCardRootPath = "";

        if(directoriesInSdCard.size() > 1) {
            int minSplits = 90000;
            String shortestPath = "";
            //minimum requirement is second shortest, if string is longer than second shortest no problem
            String secondShortestPath = "";
            for (String str : directoriesInSdCard) {
//                Log.d(TAG, "split lengths " + str.split("/").length);
                if (str.split("/").length < minSplits) {
                    minSplits = str.split("/").length;
                    secondShortestPath = shortestPath;
                    shortestPath = str;
                }
            }

            String[] splitShortestPath = shortestPath.split("/");
            String[] splitSecondShortestPath = secondShortestPath.split("/");

            StringBuffer sb = new StringBuffer();
            for(int i=0 ; i<splitShortestPath.length; i++) {
                if(splitShortestPath[i].equals(splitSecondShortestPath[i])) {
                    sb.append("/" + splitShortestPath[i]);
                }
            }

            sdCardRootPath = sb.toString().substring(1);

        } else if(directoriesInSdCard.size() == 1) {
            sdCardRootPath = directoriesInSdCard.get(0);
        }

        Log.d(TAG, "getSdCardRootPath: " + sdCardRootPath);
        return sdCardRootPath;
    }


    private void loadAllFilesFromDeviceMemory() {
//        mLocation = LOCATION_INTERNAL;
        File internalMemoryRoot = new File(mDeviceInternalMemoryRootPath);
        getAllFilesUnder(internalMemoryRoot, false);
    }


    private void loadAllFilesFromSdCard() {
//        mLocation = LOCATION_SDCard;
        File sdCardRoot = new File(mSdCardRootPath);
        getAllFilesUnder(sdCardRoot, false);
    }


    /**
     * Lists files and SubDir under passed directory
     * @param dirName file Name
     * @param push if true passed directories absolute path added to parent stack
     */
    public void getAllFilesUnder(File dirName, boolean push) {
        HashSet<File> subDirHashSet = new HashSet<>();
        ArrayList<File> listFiles = new ArrayList<>();

        if(push) {
            mParentStack.push(dirName.getParentFile());
        }

        if(mFileMap.get("SubDir") != null && mFileMap.get("Files") != null) {
            mFileMap.get(getString(R.string.SubDir)).clear();
            mFileMap.get(getString(R.string.Files)).clear();
        }

        File[] filesList = dirName.listFiles();

        if(filesList != null) {
            for (File f : filesList) {
                if (f.isDirectory()) {
                    String dirPath = f.getAbsolutePath();
                    if (mLocation.equals(LOCATION_INTERNAL)) {
                        //get subDirs in device internal storage, which contains selected media files
                        for (String str : mDirsInDevice) {
                            if (str.contains(dirPath)) {
                                subDirHashSet.add(f);
                            }
                        }
                    } else if (mLocation.equals(LOCATION_SDCard)) {
                        //get subDirs in sdCard storage, which contains selected media files
                        for (String str : mDirsInSdCard) {
                            if (str.contains(dirPath)) {
                                subDirHashSet.add(f);
                            }
                        }
                    }

                } else {
                    if (isFileOfCorrectMedia(f)) {
                        listFiles.add(f);
                    }
                }
            }
        }

        ArrayList<File> listSubDirs = new ArrayList<>(subDirHashSet);

        mFileMap.put(getString(R.string.Files), listFiles);
        mFileMap.put(getString(R.string.SubDir), listSubDirs);

        mImageGridAdapter.notifyDataSetChanged();

    }


    /**
     * Check if the submitted file is of selected media type
     * @param file
     * @return true if file is of selected media
     */
    private boolean isFileOfCorrectMedia(File file) {
        String filePath = file.getAbsolutePath();
        if(filePath.contains(".")) {
            String extension = filePath.substring(filePath.lastIndexOf("."));

            //TODO add further filters to include new media type
            if(mMediaType.equals(IMAGE_TYPE)) {
                if (mValidImgExtensions.contains(extension)) return true;
            } else if (mMediaType.equals(VIDEO_TYPE)) {
                if (mValidVideoExtensions.contains(extension)) return true;
            }
        }

        return false;
    }




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
//            getAllFilesUnder(parentDir, false);
        } else {
            super.onBackPressed();
        }
    }


}
