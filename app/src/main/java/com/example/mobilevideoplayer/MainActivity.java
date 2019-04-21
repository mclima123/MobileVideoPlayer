package com.example.mobilevideoplayer;

import android.app.Activity;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GestureOverlayView.OnGesturePerformedListener {

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
    private static final int READ_REQUEST_CODE = 42;
    private TextView filePathTextView;
    private TextView urlPathTextView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private MediaController mediaController;
    private ConstraintLayout urlBackground;
    private ConstraintLayout fileBackground;
    private GestureOverlayView gestureOverlayView;
    private GestureLibrary gestureLib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeVariables();
        setVideoViewListeners();
        setUrlTextViewListener();
        initializeCustomGestures();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                filePathTextView.setText(uri.toString());
                filePathTextView.setTextColor(Color.BLACK);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "main activity paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "main activity resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = gestureLib.recognize(gesture);

        for (Prediction prediction : predictions) {
            if (prediction.score > 1.0) {
                Toast.makeText(this, prediction.name, Toast.LENGTH_SHORT).show();

                switch (prediction.name) {
                    case "restart": // circular gesture sets the timestamp to 0s
                        videoView.seekTo(0);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Finds the UI views and initializes variables.
     */
    private void initializeVariables() {
        filePathTextView = findViewById(R.id.mainActivityFileTextView);
        urlPathTextView = findViewById(R.id.urlTextView);
        videoView = findViewById(R.id.videoView);
        progressBar = findViewById(R.id.videoViewProgressBar);
        urlBackground = findViewById(R.id.urlContainerLayout);
        fileBackground = findViewById(R.id.fileContainerLayout);
        gestureOverlayView = findViewById(R.id.gesturesOverlay);

        // Initialize variables
        mediaController = new MediaController(MainActivity.this);
    }

    /**
     * Initializes the custom gestures library and listeners.
     */
    private void initializeCustomGestures() {
        gestureOverlayView.addOnGesturePerformedListener(this);
        gestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
        if (!gestureLib.load()) {
            finish();
        }
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
                mediaController.setAnchorView(videoView); // anchor the media controls
                progressBar.setVisibility(View.GONE); // hide progress bar
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
     * TODO
     * Saves the current state(video uri and timestamp, etc) and starts new activity.
     */
    public void onClickFullscreen(View view) {
        Intent intent = new Intent(this, FullscreenActivity.class);


        startActivity(intent);
    }

    // endregion

    /**
     * Sets the background of the video source that is currently loaded.
     */
    private void setSourceTextViewColors() {
        if (fileSource == FileSource.LOCAL) {
            urlBackground.setBackgroundColor(Color.TRANSPARENT);
            fileBackground.setBackgroundColor(getResources().getColor(R.color.colorHighligh));
        } else {
            fileBackground.setBackgroundColor(Color.TRANSPARENT);
            urlBackground.setBackgroundColor(getResources().getColor(R.color.colorHighligh));
        }
    }
}
