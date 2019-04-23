package com.example.mobilevideoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    /**
     * Software Architecture for User Interfaces project
     * <p>
     * Get test video Url's from this site:
     * https://www.sample-videos.com/
     */

    private enum FileSource {
        LOCAL, URL
    }

    private FileSource fileSource;
    private boolean isFullscreen = false;
    private static final int READ_REQUEST_CODE = 42;
    private TextView filePathTextView;
    private TextView urlPathTextView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private MediaController mediaController;
    private ConstraintLayout urlBackground;
    private ConstraintLayout fileBackground;
    private GestureOverlayView gestureOverlayView;
    private ImageButton fullscreenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeVariables();
        setVideoViewListeners();
        setUrlTextViewListener();
        setGesturesListeners();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                if (uri != null) {
                    filePathTextView.setText(uri.toString());
                    filePathTextView.setTextColor(Color.WHITE);
                }
            }
        }
    }

    /**
     * Finds the UI views and initializes variables.
     */
    private void initializeVariables() {
        // Find views
        filePathTextView = findViewById(R.id.file_text_view);
        urlPathTextView = findViewById(R.id.url_text_view);
        videoView = findViewById(R.id.video_view);
        progressBar = findViewById(R.id.video_view_progress_bar);
        urlBackground = findViewById(R.id.url_container_layout);
        fileBackground = findViewById(R.id.file_container_layout);
        gestureOverlayView = findViewById(R.id.gestures_overlay);
        fullscreenButton = findViewById(R.id.fullscreen_button);

        // Initialize variables
        mediaController = new MediaController(this);
    }

    /**
     * Initializes the normal and custom gestures listeners.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setGesturesListeners() {
        gestureOverlayView.addOnGesturePerformedListener(new CustomGesturesListener(this,
                videoView));

        gestureOverlayView.setOnTouchListener(new SwipeGesturesListener(this, videoView));
    }

    /**
     * Overrides VideoView listeners for a customized behaviour.
     * https://stackoverflow.com/questions/3686729/mediacontroller-positioning-over-videoview/24529711
     */
    private void setVideoViewListeners() {
        /*
         * Sets the position of the media controller upon video loaded.
         * MediaController needs to be positioned after the video is ready, otherwise it gets anchored
         * to the bottom of the parent layout.
         * Creates a thumbnail after video loaded.
         */
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.setMediaController(mediaController);
                mediaController.setAnchorView(gestureOverlayView); // anchor the media controls
                progressBar.setVisibility(View.GONE); // hide progress bar
                videoView.start(); // sometimes thumbnail doesn't show without this
                videoView.seekTo(1); // create thumbnail
                videoView.pause(); // video plays instantly without this

                setSourceTextViewColors();
            }
        });

        // Sets the video back to the beginning upon completion.
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.seekTo(1);
            }
        });

        // Called when there is an error playing or setting up the video.
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                progressBar.setVisibility(View.GONE); // hides the progress bar
                videoView.setVideoURI(null); // prevents error dialog from showing multiple times
                urlBackground.setBackgroundColor(Color.TRANSPARENT); // no video source selected
                fileBackground.setBackgroundColor(Color.TRANSPARENT); // no video source selected
                return false;
            }
        });
    }

    /**
     * Hides the media controls when editing the Url to prevent overlapping of views.
     */
    private void setUrlTextViewListener() {
        urlPathTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaController.hide();
            }
        });
    }

    // region onClick functions for choosing files and loading video.

    /**
     * Fires an intent to spin up the "file chooser" UI and select a video.
     * https://developer.android.com/guide/topics/providers/document-provider
     */
    public void onClickFileSearch(View view) {
        videoView.setVideoURI(null); //stops playing current video
        progressBar.setVisibility(View.GONE); //hides the progress bar in case it's till visible

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("video/*"); //shows only supported video files

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Loads the video into the videoView from the specified storage path
     */
    public void onClickLoadFile(View view) {
        fileSource = FileSource.LOCAL;
        progressBar.setVisibility(View.VISIBLE); // shows progress bar while video loads
        videoView.setVideoURI(Uri.parse(filePathTextView.getText().toString()));
    }

    /**
     * Loads the video into the videoView from the specified url
     */
    public void onClickLoadUrl(View view) {
        fileSource = FileSource.URL;
        progressBar.setVisibility(View.VISIBLE); // shows progress bar while video loads
        videoView.setVideoURI(Uri.parse(urlPathTextView.getText().toString()));
    }

    /**
     * Handles click on fullscreen button.
     */
    public void onClickFullscreen(View view) {
        if (!isFullscreen) {
            float ratio = (float) videoView.getWidth() / videoView.getHeight();
            enterFullscreen(ratio);
        } else {
            exitFullscreen();
        }
    }

    // endregion

    /**
     * Sets the background of the video source that is currently loaded.
     */
    private void setSourceTextViewColors() {
        // if video source is local storage, highlight only local storage
        if (fileSource == FileSource.LOCAL) {
            urlBackground.setBackgroundColor(Color.TRANSPARENT);
            fileBackground.setBackgroundResource(R.drawable.rounded_corners_background_padding);
        }
        // else, highlight only url fields
        else {
            fileBackground.setBackgroundColor(Color.TRANSPARENT);
            urlBackground.setBackgroundResource(R.drawable.rounded_corners_background_padding);
        }
    }

    /**
     * Enters fullscreen.
     * Rotates screen to landscape if video is wider, stays in portrait otherwise.
     */
    private void enterFullscreen(float ratio) {
        fullscreenButton.setImageResource(R.drawable.fullscreen_exit_icon);

        // is video landscape?
        if (ratio > 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //fazer coisas
        //fazer coisas
        //fazer coisas

        isFullscreen = true;
    }

    /**
     * Exits fullscreen.
     */
    private void exitFullscreen() {
        fullscreenButton.setImageResource(R.drawable.fullscreen_icon); // sets appropriate icon
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //sets portrait orientation

        //fazer coisas
        //fazer coisas
        //fazer coisas

        isFullscreen = false;
    }
}
