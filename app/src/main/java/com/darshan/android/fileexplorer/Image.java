package com.darshan.android.fileexplorer;

import android.graphics.Bitmap;

/**
 * Created by Darshan B.S on 15-09-2018.
 */

public class Image {
    private String imageUri;
    private String thumbUri;

    public Image() {
    }

    public Image(String imageUri, String thumbUri) {
        this.imageUri = imageUri;
        this.thumbUri = thumbUri;
    }


    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getThumbUri() {
        return thumbUri;
    }

    public void setThumbUri(String thumbUri) {
        this.thumbUri = thumbUri;
    }

    @Override
    public String toString() {
        return "Image{" +
                "imageUri='" + imageUri + '\'' +
                ", thumbUri='" + thumbUri + '\'' +
                '}';
    }
}
