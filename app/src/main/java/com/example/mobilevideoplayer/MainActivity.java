package com.example.mobilevideoplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    /**
     * Software Architecture for User Interfaces project
     *
     * Get test video Url's from this site:
     * https://www.sample-videos.com/
     */

    private static final int READ_REQUEST_CODE = 42;
    private TextView filePathTextView;
    private TextView urlPathTextView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find UI views
        filePathTextView = findViewById(R.id.mainActivityFileTextView);
        urlPathTextView = findViewById(R.id.mainActivityUrlTextView);
        videoView = findViewById(R.id.mainActivityVideoView);
        progressBar = findViewById(R.id.videoViewProgressBar);

        // Initialize variables
        mediaController = new MediaController(MainActivity.this);

        setVideoViewListeners();
        setUrlTextViewListener();
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
                videoView.pause(); // video starts instantly playing without this
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
    public void performFileSearch(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("video/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void onClickLoadFile(View view) {
        progressBar.setVisibility(View.VISIBLE);
        videoView.setVideoURI(Uri.parse(filePathTextView.getText().toString()));
    }

    public void onClickLoadUrl(View view) {
        progressBar.setVisibility(View.VISIBLE);
        String urlInput = urlPathTextView.getText().toString();
        videoView.setVideoURI(Uri.parse(urlInput));
    }

    // endregion
}
