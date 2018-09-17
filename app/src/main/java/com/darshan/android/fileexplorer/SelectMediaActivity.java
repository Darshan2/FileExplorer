package com.darshan.android.fileexplorer;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SelectMediaActivity extends AppCompatActivity {
    private RelativeLayout mMediaRL;
    private RelativeLayout mLocationRL;

    private TextView mDeviceMemoryTV;
    private TextView mSDCardTV;
    private ImageView mThumbIV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_media);

        TextView videoTV = findViewById(R.id.videos_TV);
        TextView imagesTV = findViewById(R.id.images_TV);
        mThumbIV = findViewById(R.id.thumbImage);

        mDeviceMemoryTV = findViewById(R.id.deviceMemory_TV);
        mSDCardTV = findViewById(R.id.sdCard_TV);

        mMediaRL = findViewById(R.id.media_RL);
        mLocationRL = findViewById(R.id.location_RL);

        mThumbIV.setImageBitmap(BitmapFactory.decodeFile("/storage/emulated/0/DCIM/.thumbnails/1537171644214.jpg"));

        videoTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMediaOptions();
                selectLocation(getString(R.string.video));
            }
        });

        imagesTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMediaOptions();
                selectLocation(getString(R.string.image));
            }
        });

    }

    private void selectLocation(final String mediaType) {
        mDeviceMemoryTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveForward(mediaType, getString(R.string.device_memory));
            }
        });

        mSDCardTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveForward(mediaType, getString(R.string.sd_card));
            }
        });
    }


    private void moveForward(String mediaType, String locationType) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(getString(R.string.media_type), mediaType);
        intent.putExtra(getString(R.string.location_type), locationType);
        startActivity(intent);
    }

    private void hideMediaOptions() {
        mMediaRL.setVisibility(View.GONE);
        mLocationRL.setVisibility(View.VISIBLE);
    }

    private void showMediaOptions() {
        mMediaRL.setVisibility(View.VISIBLE);
        mLocationRL.setVisibility(View.GONE);
    }

}
