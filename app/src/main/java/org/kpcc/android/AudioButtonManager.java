package org.kpcc.android;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

/**
 * Created by rickb014 on 3/1/15.
 */
public class AudioButtonManager {
    private MainActivity mActivity;
    private Button mPlayButton;
    private Button mPauseButton;
    private Button mStopButton;
    private ProgressBar mLoadingInd;

    public AudioButtonManager(Context context, View view) {
        mActivity = (MainActivity) context;
        mPlayButton = (Button) view.findViewById(R.id.play_button);
        mPauseButton = (Button) view.findViewById(R.id.pause_button);
        mStopButton = (Button) view.findViewById(R.id.stop_button);
        mLoadingInd = (ProgressBar) view.findViewById(R.id.progress_circular);
    }

    public void toggleLoading() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.GONE);
    }

    public void togglePlayingForPause() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.VISIBLE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
    }

    public void togglePlayingForStop() {
        mStopButton.setVisibility(View.VISIBLE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
    }

    public void togglePaused() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
    }

    public void toggleStopped() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
    }
}
