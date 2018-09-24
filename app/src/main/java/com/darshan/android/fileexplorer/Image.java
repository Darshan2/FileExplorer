package com.darshan.android.fileexplorer;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Darshan B.S on 15-09-2018.
 */

public class Image implements Parcelable {
    private String imageUri;
    private String thumbUri;
    private boolean selected;
    private boolean video;

    public Image() {
    }

    public Image(String imageUri, String thumbUri) {
        this.imageUri = imageUri;
        this.thumbUri = thumbUri;
    }

    protected Image(Parcel in) {
        imageUri = in.readString();
        thumbUri = in.readString();
        selected = in.readByte() != 0;
        video = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageUri);
        dest.writeString(thumbUri);
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeByte((byte) (video ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isVideo() {
        return video;
    }

    public void setVideo(boolean video) {
        this.video = video;
    }

    @Override
    public String toString() {
        return "Image{" +
                "imageUri='" + imageUri + '\'' +
                ", thumbUri='" + thumbUri + '\'' +
                '}';
    }
}
