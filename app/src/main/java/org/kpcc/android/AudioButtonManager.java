package org.kpcc.android;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

class AudioButtonManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final ImageButton mPlayButton;
    private final ImageButton mPauseButton;
    private final ProgressBar mLoadingInd;
    private final TextView mErrorMsg;
    private final AtomicBoolean mIsLocked = new AtomicBoolean(false);

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
    void lock() {
        mIsLocked.set(true);
    }

    void unlock() {
        mIsLocked.set(false);
    }
    void reset() {
        toggleStopped();
        hideError();
        unlock();
    }

    void toggleLoading() {
        if (mIsLocked.get()) return;
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.GONE);
    }

    void togglePlayingForPause() {
        if (mIsLocked.get()) return;
        mPauseButton.setVisibility(View.VISIBLE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
    }

    void togglePaused() {
        if (mIsLocked.get()) return;
        mPauseButton.setVisibility(View.GONE);
        mLoadingInd.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
    }

    void toggleStopped() {
        if (mIsLocked.get()) return;
        togglePaused();
    }

    // These are a way to just manage the error message, because sometimes there are
    // other things managing the button and we just want to deal with the error message
    // so we don't get conflicting buttons... you know?
    void showError(int stringId) {
        if (mIsLocked.get()) return;
        mErrorMsg.setText(stringId);
        mErrorMsg.setVisibility(View.VISIBLE);
    }

    void hideError() {
        if (mIsLocked.get()) return;
        mErrorMsg.setVisibility(View.GONE);
    }

    void clickPlay() {
        if (mIsLocked.get()) return;
       if (mPlayButton.getVisibility() == View.VISIBLE) {
           mPlayButton.callOnClick();
       }
    }
}
