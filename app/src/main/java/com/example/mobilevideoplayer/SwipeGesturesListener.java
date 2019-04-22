package com.example.mobilevideoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

/**
 * https://stackoverflow.com/questions/937313/fling-gesture-detection-on-grid-layout
 */

public class SwipeGesturesListener implements View.OnTouchListener {

    private Activity activity;
    private VideoView videoView;
    private static final int MIN_DISTANCE = 100;
    private float downX, downY;

    public SwipeGesturesListener(Activity activity, VideoView videoView) {
        this.activity = activity;
        this.videoView = videoView;
    }

    /**
     * Forwards video 15s on right swipe
     */
    private void onRightSwipe() {
        int currentTimestamp = videoView.getCurrentPosition();

        if(currentTimestamp + 15000 < videoView.getDuration()){
            videoView.seekTo(currentTimestamp + 15000);
        }
        else{
            videoView.seekTo(videoView.getDuration());
        }
    }

    /**
     * Backwards video 15s on right swipe
     */
    private void onLeftSwipe() {
        if(videoView.getCurrentPosition() - 15000 > 0){
            videoView.seekTo(videoView.getCurrentPosition() - 15000);
        }
        else{
            videoView.seekTo(1);
        }
    }

    private void onDownSwipe() {
        Toast.makeText(activity, "down swipe", Toast.LENGTH_SHORT).show();
    }

    private void onUpSwipe() {
        Toast.makeText(activity, "up swipe", Toast.LENGTH_SHORT).show();
    }

    /**
     * Detects onTouch event (press and release) and what swipe direction.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                float upX = event.getX();
                float upY = event.getY();

                float deltaX = upX - downX;
                float deltaY = downY - upY;

                // swipe horizontal?
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (Math.abs(deltaX) > MIN_DISTANCE) {
                        // left or right
                        if (deltaX > 0) {
                            this.onRightSwipe();
                            return true;
                        }
                        if (deltaX < 0) {
                            this.onLeftSwipe();
                            return true;
                        }
                    } else {
                        return false; // We don't consume the event
                    }
                }
                // swipe vertical?
                else {
                    if (Math.abs(deltaY) > MIN_DISTANCE) {
                        // top or down
                        if (deltaY < 0) {
                            this.onDownSwipe();
                            return true;
                        }
                        if (deltaY > 0) {
                            this.onUpSwipe();
                            return true;
                        }
                    } else {
                        return false; // We don't consume the event
                    }
                }

                return true;
            }
        }
        return false;
    }
}
