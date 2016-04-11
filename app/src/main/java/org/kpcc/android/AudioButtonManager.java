package org.kpcc.android;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

class AudioButtonManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final ImageButton mPlayButton;
    private final ImageButton mPauseButton;
    private final ProgressBar mLoadingInd;
    private final TextView mErrorMsg;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    AudioButtonManager(@NonNull View view) {
        mPlayButton = (ImageButton) view.findViewById(R.id.play_button);
        mPauseButton = (ImageButton) view.findViewById(R.id.pause_button);
        mLoadingInd = (ProgressBar) view.findViewById(R.id.progress_circular);
        mErrorMsg = (TextView) view.findViewById(R.id.error_message);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ImageButton getPlayButton() {
        return mPlayButton;
    }

    ImageButton getPauseButton() {
        return mPauseButton;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void reset() {
        toggleStopped();
        hideError();
    }

    void toggleLoading() {
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.GONE);
    }

    void togglePlayingForPause() {
        mPauseButton.setVisibility(View.VISIBLE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
    }

    void togglePaused() {
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
    }

    void toggleStopped() {
        togglePaused();
    }

    // These are a way to just manage the error message, because sometimes there are
    // other things managing the button and we just want to deal with the error message
    // so we don't get conflicting buttons... you know?
    void showError(int stringId) {
        mErrorMsg.setText(stringId);
        mErrorMsg.setVisibility(View.VISIBLE);
    }

    void hideError() {
        mErrorMsg.setVisibility(View.GONE);
    }

    void clickPlay() {
       if (mPlayButton.getVisibility() == View.VISIBLE) {
           mPlayButton.callOnClick();
       }
    }
}
