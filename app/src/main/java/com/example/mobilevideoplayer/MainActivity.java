package com.example.mobilevideoplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 96;
    private TextView filePathTextView;
    private TextView urlPathTextView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private MediaController mediaController;
    private ConstraintLayout urlBackground;
    private ConstraintLayout fileBackground;
    private GestureOverlayView gestureOverlayView;
    private FrameLayout frameLayout;
    private ConstraintLayout constraintLayout;
    private ConstraintSet constraintSet;
    private Intent serviceIntent;
    private SensorBroadcastReceiver receiver;
    private IntentFilter filter;
    private Uri fileStorageURI;

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
        registerReceiver(receiver, filter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // On result from the file chooser activity
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                fileStorageURI = resultData.getData();
                if (fileStorageURI != null) {
                    filePathTextView.setText(getFileName(fileStorageURI));
                    filePathTextView.setTextColor(Color.WHITE);
                }
            }
        }
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_info);
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileChooser();
                }

                break;
            }
        }
    }

    /**
     * Checks if any dangerous permission hasn't been granted and requests it.
     */
    private void checkPermissions() {
        // Check if read external storage permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "No permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            openFileChooser();
        }
    }

    /**
     * Finds the UI views and initializes variables.
     */
    private void initializeVariables() {
        // Initialize custom toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Find views
        filePathTextView = findViewById(R.id.file_text_view);
        urlPathTextView = findViewById(R.id.url_text_view);
        videoView = findViewById(R.id.video_view);
        progressBar = findViewById(R.id.video_view_progress_bar);
        urlBackground = findViewById(R.id.url_container_layout);
        fileBackground = findViewById(R.id.file_container_layout);
        gestureOverlayView = findViewById(R.id.gestures_overlay);
        frameLayout = findViewById(R.id.video_container_layout);
        constraintLayout = findViewById(R.id.main_constraint_layout);

        // Initialize variables
        mediaController = new VideoMediaController(this, this);
        constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout); // cache portrait layout constraints

        // Sensors
        filter = new IntentFilter();
        filter.addAction("GET_PROXIMITY_GRAVITY_ACTION");
        receiver = new SensorBroadcastReceiver(videoView);
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
                videoView.pause(); // sometimes video plays instantly without this

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

    public void onClickFileSearch(View view) {
        checkPermissions();
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select a video.
     * https://developer.android.com/guide/topics/providers/document-provider
     */
    private void openFileChooser() {
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
        videoView.setVideoURI(Uri.parse(fileStorageURI.toString()));
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
    public void onClickFullscreen() {
        if (!isFullscreen && isVideoReady) {
            float ratio = (float) videoView.getWidth() / videoView.getHeight();
            enterFullscreen(ratio);
        } else if (isFullscreen) {
            exitFullscreen();
        }
    }

    /**
     * Stops the video, if it's ready. (Pause and back to start)
     */
    public void onClickStop() {
        videoView.pause();
        videoView.seekTo(0);
    }

    // endregion

    /**
     * Enters fullscreen.
     * Rotates screen to landscape if video is wider, stays in portrait otherwise.
     * https://stackoverflow.com/questions/18268218/change-screen-orientation-programmatically-using-a-button
     */
    private void enterFullscreen(float ratio) {
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

    /**
     * Gets file name from URI.
     * https://stackoverflow.com/questions/5568874/how-to-extract-the-file-name-from-uri-returned-from-intent-action-get-content
     */
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : 0;
            if (cut != -1) {
                result = result != null ? result.substring(cut + 1) : null;
            }
        }
        return result;
    }
}
