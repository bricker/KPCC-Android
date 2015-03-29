package org.kpcc.android;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AudioButtonManager {
    private ImageButton mPlayButton;
    private ImageButton mPauseButton;
    private ImageButton mStopButton;
    private ProgressBar mLoadingInd;
    private TextView mErrorMsg;

    public AudioButtonManager(@NonNull View view) {
        mPlayButton = (ImageButton) view.findViewById(R.id.play_button);
        mPauseButton = (ImageButton) view.findViewById(R.id.pause_button);
        mStopButton = (ImageButton) view.findViewById(R.id.stop_button);
        mLoadingInd = (ProgressBar) view.findViewById(R.id.progress_circular);
        mErrorMsg = (TextView) view.findViewById(R.id.error_message);
    }

    public void toggleLoading() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.GONE);
        mErrorMsg.setVisibility(View.GONE);
    }

    public void togglePlayingForPause() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.VISIBLE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
        mErrorMsg.setVisibility(View.GONE);
    }

    public void togglePlayingForStop() {
        mStopButton.setVisibility(View.VISIBLE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
        mErrorMsg.setVisibility(View.GONE);
    }

    public void togglePaused() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
        mErrorMsg.setVisibility(View.GONE);
    }

    public void toggleStopped() {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
        mErrorMsg.setVisibility(View.GONE);
    }

    public void toggleError(int stringId) {
        mStopButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);

        mErrorMsg.setText(stringId);
        mErrorMsg.setVisibility(View.VISIBLE);
    }

    public ImageButton getPlayButton() {
        return mPlayButton;
    }

    public ImageButton getPauseButton() {
        return mPauseButton;
    }

    public ImageButton getStopButton() {
        return mStopButton;
    }

    public ProgressBar getLoadingIndicator() {
        return mLoadingInd;
    }

    public void clickPlay() {
        mPlayButton.callOnClick();
    }
}
