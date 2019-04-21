package com.example.mobilevideoplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
    }

    @Override
    protected void onPause(){
        super.onPause();
        Toast.makeText(this, "fullscreen paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume(){
        super.onResume();
        Toast.makeText(this, "fullscreen resumed", Toast.LENGTH_SHORT).show();
    }

    /**
     * TODO
     * Finishes the activity and saves the current state(video uri and timestamp, etc)
     */
    public void onClickExitFullscreen(View view) {


        finish();
    }
}
