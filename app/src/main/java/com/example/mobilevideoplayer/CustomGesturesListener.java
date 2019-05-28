package com.example.mobilevideoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.media.AudioManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.VideoView;

import java.util.ArrayList;

public class CustomGesturesListener implements
        GestureOverlayView.OnGesturePerformedListener,
        View.OnTouchListener {

    private VideoView videoView;
    private GestureLibrary gestureLibrary;
    private AudioManager audioManager;
    private static final int MIN_DISTANCE = 200; // minimum distance to consider a swipe
    private static final int videoSeekForwardJump = 15000; // 15s jumps
    private static final int videoSeekBackwardJump = 5000; // 5s jumps
    private static final long doubleTapInterval = 200; // interval to detect double tap
    private float downX, downY;
    private long prevTapTime = 0;

    public CustomGesturesListener(Activity activity, VideoView videoView) {
        gestureLibrary = GestureLibraries.fromRawResource(activity, R.raw.gestures);
        if (!gestureLibrary.load()) {
            activity.finish();
        }

        this.videoView = videoView;
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = gestureLibrary.recognize(gesture);

        for (Prediction prediction : predictions) {
            // increased accuracy level, so this gesture doesn't get detected on accident.
            if (prediction.score > 4.0) {
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

    /**
     * Forwards video 15s on right swipe.
     */
    private void onRightSwipe() {
        int currentTimestamp = videoView.getCurrentPosition();

        if (currentTimestamp + videoSeekForwardJump < videoView.getDuration()) {
            videoView.seekTo(currentTimestamp + videoSeekForwardJump);
        } else {
            videoView.seekTo(videoView.getDuration());
        }
    }

    /**
     * Backwards video 15s on right swipe.
     */
    private void onLeftSwipe() {
        int currentTimestamp = videoView.getCurrentPosition();

        if (currentTimestamp - videoSeekBackwardJump > 0) {
            videoView.seekTo(currentTimestamp - videoSeekBackwardJump);
        } else {
            videoView.seekTo(1);
        }
    }

    /**
     * Decreases media volume on up swipe.
     */
    private void onUpSwipe() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    /**
     * Decreases media volume on down swipe.
     */
    private void onDownSwipe() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    /**
     * Plays/pauses the video on double tap.
     */
    private void onDoubleTap() {
        if (videoView.isPlaying())
            videoView.pause();
        else
            videoView.start();
    }

    /**
     * Detects onTouch event (press and release) and what swipe direction.
     * Detects double tap.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (System.currentTimeMillis() - prevTapTime <= doubleTapInterval) {
                    onDoubleTap();
                    return true;
                }
                downX = event.getX();
                downY = event.getY();
                prevTapTime = System.currentTimeMillis();
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
                            onRightSwipe();
                            return true;
                        }
                        if (deltaX < 0) {
                            onLeftSwipe();
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
                            onDownSwipe();
                            return true;
                        }
                        if (deltaY > 0) {
                            onUpSwipe();
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
