package com.darshan.android.fileexplorer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Darshan B.S on 19-09-2018.
 */

public class ThumbUtils {
    private static final String TAG = "ThumbUtils";
    /**
     *
     * @param sourceId id of original image/video in their corresponding table
     * @param filePath
     */
    public Image getThumbnail(ContentResolver contentResolver, long sourceId, String filePath) {
        Image image;

        String[] projection = {MediaStore.Images.Thumbnails.DATA};
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
                image = new Image(filePath, imageThumbPath);
            } else {
                Log.d(TAG, "getThumbnail: entry exist, file not " + imageThumbPath);
                //Entry with give sourceId already exist in MediaStore db, so update that row
                image = createImageThumbNail(contentResolver, sourceId, filePath, imageThumbPath);
            }

            cursor.close();

        } else {
            //Entry with give sourceId does not exist in MediaStore db, so insert that row
            image = createImageThumbNail(contentResolver, sourceId, filePath, null);
        }

        return image;

    }


    public Image createImageThumbNail(ContentResolver contentResolver, long imageId,
                                      String imagePath, @Nullable String previousThumbPath) {
        Log.d(TAG, "createImageThumbNail: " + imagePath);
        //Create a bitmap for image.
        Bitmap sourceBm = MediaStore.Images.Thumbnails.getThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null
        );

        Log.d(TAG, "createImageThumbNail: bm " + sourceBm);
        //Store/update that bitmap in MediaStore
        if (sourceBm != null) {
            Image image = storeThumbnail(imagePath, previousThumbPath, contentResolver, sourceBm, imageId, 50F,
                    50F, MediaStore.Images.Thumbnails.MINI_KIND);
            return image;
        } else {
            return null;
        }
    }


     /* Put the thumbnail in MediaStore db,
     * if the row with image_id = image_source_id(Source Image id from Image table) already
      * exist in media store db newThumbEntry = false */

    private Image storeThumbnail(String imagePath, @Nullable String previousThumbPath, ContentResolver cr,
                                Bitmap source, long id, float width, float height, int kind) {
        Image image = new Image(imagePath, previousThumbPath);

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
            Log.d(TAG, "storeThumbnail: no thumb, no previous entry in db");
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
            image = getNewThumbFromDb(cr, imagePath, id);

        } else {
            //i.e previously row exist in thumb table with given sourceId(Image _id from Image table),
            //update that row.
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

        return image;
    }


    private Image getNewThumbFromDb(ContentResolver contentResolver, String imagePath, long imageId) {
        //Query MediaStore again to get the newly created thumb url for the image
        String[] projection = {MediaStore.Images.Thumbnails.DATA};

        Cursor cursor2 = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                projection
        );

        if (cursor2 != null && cursor2.moveToFirst()) {
            String imageThumbPath = cursor2.getString(cursor2.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            Log.d(TAG, "createImageThumbNail: thumbPath " + imageThumbPath);
            cursor2.close();
            Image image = new Image(imagePath, imageThumbPath);
            return image;
        }

        return null;

    }
}
