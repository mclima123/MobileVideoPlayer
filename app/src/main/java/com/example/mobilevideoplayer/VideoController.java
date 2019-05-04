package com.example.mobilevideoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;


@SuppressLint("ViewConstructor")
public class VideoController extends MediaController {

    private Context context;
    private ImageButton fullscreenButton;
    private ImageButton stopButton;
    private MainActivity activity;
    private boolean isFullscreen = false;

    public VideoController(Context context, MainActivity activity) {
        super(context);
        this.context = context;
        this.activity = activity;
    }

    @Override
    public void setAnchorView(View view) {
        super.setAnchorView(view);

        fullscreenButton = new ImageButton(context);
        fullscreenButton.setBackgroundResource(R.drawable.fullscreen_icon);
        FrameLayout.LayoutParams params1 = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params1.gravity = Gravity.END;
        params1.height = 100;
        params1.width = 100;
        params1.topMargin = 25;
        addView(fullscreenButton, params1);

        stopButton = new ImageButton(context);
        stopButton.setBackgroundResource(R.drawable.stop_icon);
        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params2.gravity = Gravity.START;
        params2.height = 100;
        params2.width = 100;
        params2.topMargin = 25;
        addView(stopButton, params2);

        setNewButtonListeners();
    }

    private void setNewButtonListeners() {
        fullscreenButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoController.super.show();

                if (!isFullscreen) {
                    fullscreenButton.setBackgroundResource(R.drawable.fullscreen_exit_icon);
                    isFullscreen = true;
                } else {
                    fullscreenButton.setBackgroundResource(R.drawable.fullscreen_icon);
                    isFullscreen = false;
                }

                activity.onClickFullscreen();
            }
        });

        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoController.super.show();
                activity.onClickStop();
            }
        });
    }
}
