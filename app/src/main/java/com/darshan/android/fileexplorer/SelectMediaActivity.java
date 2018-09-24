package com.darshan.android.fileexplorer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SelectMediaActivity extends AppCompatActivity {
    private static final String TAG = "SelectMediaActivity";
    private RelativeLayout mMediaRL;
    private static final int GALLERY_ACTIVITY_REQUEST_CODE = 13025;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_media);

        final EditText mediaTypeET = findViewById(R.id.mediaType_ET);
        Button submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mediaType = mediaTypeET.getText().toString();
                String mediaConst = "";
                switch (mediaType) {
                    case "1" : mediaConst = GalleryConsts.IMAGE_TYPE; break;
                    case "2" : mediaConst = GalleryConsts.VIDEO_TYPE; break;
                    case "3" : mediaConst = GalleryConsts.IMAGE_VIDEO_TYPE; break;
                    default:break;
                }

                loadFoldersOfMedia(mediaConst);
            }
        });

    }

    private void loadFoldersOfMedia(String mediaType) {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryConsts.INTENT_MEDIA_TYPE, mediaType);
        startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
    }


//    private void moveForward(String mediaType) {
//        Intent intent = new Intent(this, GalleryActivity.class);
//        intent.putExtra(GalleryConsts.INTENT_MEDIA_TYPE, mediaType);
//        startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
//    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == GALLERY_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<Image> selectedImages = intent.getParcelableArrayListExtra(GalleryConsts.INTENT_SELECT_GALLERY_ITEMS);
            for(Image image : selectedImages) {
                Log.d(TAG, "onActivityResult: " + image);
            }
        }
    }
}
