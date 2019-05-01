package com.example.mobilevideoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.Objects;

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
    private boolean isVideoReady = false;
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
    private FrameLayout frameLayout;
    private View decorView;
    private ConstraintLayout constraintLayout;
    private ConstraintSet constraintSet;
    private Intent serviceIntent;
    private SensorBroadcastReceiver receiver;
    private IntentFilter filter;

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
    public void onPause() {
        // Unregister sensors to save battery
        super.onPause();
        unregisterReceiver(receiver);
        stopService(serviceIntent); // ends the service so it doesn't run in the background
    }

    @Override
    public void onResume() {
        super.onResume();
        // Initialize sensors service
        serviceIntent = new Intent(this, SensorService.class);
        startService(serviceIntent);
        filter = new IntentFilter();
        filter.addAction("GET_PROXIMITY_GRAVITY_ACTION");
        receiver = new SensorBroadcastReceiver(videoView);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // On result from the file chooser activity
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
        frameLayout = findViewById(R.id.video_container_layout);
        constraintLayout = findViewById(R.id.main_constraint_layout);

        // Initialize variables
        mediaController = new MediaController(this);
        decorView = getWindow().getDecorView();
        constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout); // cache portrait layout constraints
    }

    /**
     * Initializes the custom gestures listeners.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setGesturesListeners() {
        CustomGesturesListener customGesturesListener = new CustomGesturesListener(this, videoView);

        gestureOverlayView.addOnGesturePerformedListener(customGesturesListener); // custom gestures
        gestureOverlayView.setOnTouchListener(customGesturesListener); // double tap and swipes
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

                // keep screen on while there is a video loaded
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                isVideoReady = true;
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
                isVideoReady = false;
                if (isFullscreen) exitFullscreen();

                // remove flags that keep screen on
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                return false;
            }
        });

        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
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
                .setType("video/*"); //shows only video files

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
        if (!isFullscreen && isVideoReady) {
            float ratio = (float) videoView.getWidth() / videoView.getHeight();
            enterFullscreen(ratio);
        } else if (isFullscreen) {
            exitFullscreen();
        }
    }

    // endregion

    /**
     * Enters fullscreen.
     * Rotates screen to landscape if video is wider, stays in portrait otherwise.
     * https://stackoverflow.com/questions/18268218/change-screen-orientation-programmatically-using-a-button
     */
    private void enterFullscreen(float ratio) {
        //hideSystemUI();
        fullscreenButton.setImageResource(R.drawable.fullscreen_exit_icon); //sets appropriate icon

        // is video landscape?
        if (ratio > 1)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setVideoFullscreen();
        isFullscreen = true;
    }

    /**
     * Exits fullscreen.
     */
    private void exitFullscreen() {
        //showSystemUI();
        fullscreenButton.setImageResource(R.drawable.fullscreen_icon); // sets appropriate icon

        // if screen is in landscape, rotate to portrait
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setVideoNormalScreen();
        isFullscreen = false;
    }

    /**
     * Sets the frameLayout containing the video to fill the screen.
     * https://stackoverflow.com/questions/12728255/in-android-how-do-i-set-margins-in-dp-programmatically
     * Hides the action bar.
     * https://stackoverflow.com/questions/31152069/app-crashes-on-hiding-actionbar
     */
    private void setVideoFullscreen() {
        Objects.requireNonNull(getSupportActionBar()).hide(); // avoids potential exception
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(0, 0, 0, 0);
        frameLayout.setLayoutParams(params);
    }

    /**
     * Sets the frameLayout containing the video to its normal size
     * Restores previous layout.
     * https://stackoverflow.com/questions/45263159/constraintlayout-change-constraints-programmatically
     */
    private void setVideoNormalScreen() {
        Objects.requireNonNull(getSupportActionBar()).show(); // avoids potential exception

        constraintSet.applyTo(constraintLayout); // restore previous layout constraints
    }

    /**
     * Sets the background of the video source that is currently loaded.
     * Indicates to the user which source the video is from.
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


    // region hide system UI - TODO

    /**
     * Hides system UI, making activity fullscreen.
     * https://developer.android.com/training/system-ui/immersive
     */
    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /**
     * Shows the system bars by removing all the flags.
     * except for the ones that make the content appear under the system bars.
     * https://developer.android.com/training/system-ui/immersive
     */
    private void showSystemUI() {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE);
    }

    // endregion
}
