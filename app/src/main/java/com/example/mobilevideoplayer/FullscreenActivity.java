package com.example.mobilevideoplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
    }

    /**
     * TODO
     * Finishes the activity and saves the current state(video uri and timestamp, etc)
     */
    public void onClickExitFullscreen(View view) {


        finish();
    }
}
