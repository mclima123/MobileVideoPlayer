package com.example.mobilevideoplayer;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;

public class CustomGesturesListener implements GestureOverlayView.OnGesturePerformedListener {

    private Activity activity;
    private VideoView videoView;
    private GestureLibrary gestureLibrary;

    public CustomGesturesListener(Activity activity, VideoView videoView) {
        this.activity = activity;
        this.videoView = videoView;

        gestureLibrary = GestureLibraries.fromRawResource(activity, R.raw.gestures);
        if (!gestureLibrary.load()) {
            activity.finish();
        }
    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = gestureLibrary.recognize(gesture);

        for (Prediction prediction : predictions) {
            if (prediction.score > 1.0) {
                Toast.makeText(activity, prediction.name, Toast.LENGTH_SHORT).show();

                switch (prediction.name) {
                    case "restart": // circular gesture sets the timestamp to 0s
                        videoView.seekTo(1);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
