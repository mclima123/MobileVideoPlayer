package com.example.mobilevideoplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.VideoView;

public class SensorBroadcastReceiver extends BroadcastReceiver {

    private VideoView videoView;

    public SensorBroadcastReceiver(VideoView videoView) {
        this.videoView = videoView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean pauseVideo = intent.getBooleanExtra("PAUSE_VIDEO", false);

        // Pause the video
        if (pauseVideo && videoView.isPlaying()) videoView.pause();
    }
}