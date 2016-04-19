package org.kpcc.android;

import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;

import org.kpcc.api.ScheduleOccurrence;

/**
 * Created by rickb014 on 4/4/16.
 */
class LiveSeekViewManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int DEFAULT_BLOCK_MS = 1000*60*60;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final SeekBar mSeekBar;
    private final ImageButton mRewindBtn;
    private final ImageButton mForwardBtn;
    private final Button mGoLiveBtn;
    private final Button mProgramStartBtn;
    private final FrameLayout mLiveJumpBtns;
    private final FrameLayout mGoLiveBtnWrapper;
    private final FrameLayout mProgStartBtnWrapper;
    private ScheduleOccurrence mScheduleOccurrence;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    LiveSeekViewManager(View view) {
        mRewindBtn = (ImageButton)view.findViewById(R.id.rewind);
        mForwardBtn = (ImageButton)view.findViewById(R.id.forward);
        mGoLiveBtn = (Button)view.findViewById(R.id.golive_button);
        mProgramStartBtn = (Button)view.findViewById(R.id.progstart_button);
        mGoLiveBtnWrapper = (FrameLayout)view.findViewById(R.id.golive_wrapper);
        mProgStartBtnWrapper = (FrameLayout)view.findViewById(R.id.progstart_wrapper);
        mSeekBar = (SeekBar)view.findViewById(R.id.live_seek_bar);
        mLiveJumpBtns = (FrameLayout)view.findViewById(R.id.live_jump_buttons);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    SeekBar getSeekBar() {
        return mSeekBar;
    }

    ImageButton getRewindBtn() {
        return mRewindBtn;
    }

    ImageButton getForwardBtn() {
        return mForwardBtn;
    }

    Button getGoLiveBtn() {
        return mGoLiveBtn;
    }

    Button getProgramStartBtn() {
        return mProgramStartBtn;
    }

    private ScheduleOccurrence getSchedule() {
        return mScheduleOccurrence;
    }

    synchronized void setSchedule(ScheduleOccurrence scheduleOccurrence) {
        mScheduleOccurrence = scheduleOccurrence;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////

    //
    // View Management
    //
    synchronized void reset() {
        mLiveJumpBtns.setVisibility(View.GONE);
        hideSeekBar();
        hideSeekButtons();
    }

    synchronized void showProgramStartBtn() {
        mProgStartBtnWrapper.setVisibility(View.VISIBLE);
        mGoLiveBtnWrapper.setVisibility(View.GONE);
    }

    synchronized void showGoLiveBtn() {
        mProgStartBtnWrapper.setVisibility(View.GONE);
        mGoLiveBtnWrapper.setVisibility(View.VISIBLE);
    }

    synchronized void skipBackward(boolean canSkipBackward) {
        mSeekBar.setProgress(mSeekBar.getProgress() - LivePlayer.JUMP_INTERVAL_MS);
        showGoLiveBtn();
    }

    synchronized void skipForward(boolean canSkipForward) {
        mSeekBar.setProgress(mSeekBar.getProgress() + LivePlayer.JUMP_INTERVAL_MS);
    }

    void toggleBackwardBtn(boolean canSkipBackward) {
        if (canSkipBackward) {
            enableJumpBtn(mRewindBtn);
        } else {
            disableJumpBtn(mRewindBtn);
        }
    }

    void toggleForwardBtn(boolean canSkipForward) {
        if (canSkipForward) {
            enableJumpBtn(mForwardBtn);
        } else {
            disableJumpBtn(mForwardBtn);
        }
    }

    synchronized void enableJumpBtn(View btn) {
        btn.setAlpha(1.0f);
        btn.setEnabled(true);
    }

    synchronized void disableJumpBtn(ImageButton btn) {
        btn.setAlpha(0.4f);
        btn.setEnabled(false);
    }

    synchronized void enableSeekBar() {
        mSeekBar.setVisibility(View.VISIBLE);
        mSeekBar.setEnabled(true);
    }

    synchronized void disableSeekBar() {
        mSeekBar.setEnabled(false);
    }

    private synchronized void hideSeekBar() {
        mSeekBar.setVisibility(View.INVISIBLE);
        disableSeekBar();
    }

    private synchronized void showSeekBar() {
        mSeekBar.setVisibility(View.VISIBLE);
        enableSeekBar();
    }

    synchronized void showSeekButtons() {
        mRewindBtn.setVisibility(View.VISIBLE);
        mForwardBtn.setVisibility(View.VISIBLE);
    }

    private synchronized void hideSeekButtons() {
        mRewindBtn.setVisibility(View.INVISIBLE);
        mForwardBtn.setVisibility(View.INVISIBLE);
    }

    synchronized void showAfterPreroll() {
        mLiveJumpBtns.setVisibility(View.VISIBLE);
        showSeekButtons();
        showSeekBar();
    }

    synchronized void hideForPreroll() {
        mLiveJumpBtns.setVisibility(View.INVISIBLE);
        hideSeekButtons();
        hideSeekBar();
    }


    //
    // Progress management
    //

    void incrementProgress() {
        setSeekProgress(mSeekBar.getProgress() + LiveFragment.LIVE_SEEKBAR_REFRESH_INTERVAL);
    }

    private synchronized void setSeekProgress(int progress) {
        mSeekBar.setProgress(progress);
    }

    void setSeekProgressFromPlayerPosition(long upperBoundMs, long currentPositionMs) {
        long programStartWindowPositionMs = getProgramStartWindowPositionMs(upperBoundMs);
        setSeekProgress((int)(currentPositionMs - programStartWindowPositionMs));
    }

    synchronized void setSeekBarMaxFromSchedule() {
        mSeekBar.setMax(getProgramDuration());
    }

    synchronized void setSecondaryProgressFromSchedule() {
        long now = System.currentTimeMillis();
        long programStartUTS = getProgramStartUTS();
        mSeekBar.setSecondaryProgress((int) (now - programStartUTS));
    }

    synchronized void setSeekProgressToLiveHead() {
        setSeekProgress(mSeekBar.getSecondaryProgress());
    }

    int getLiveHeadProgress() {
        return mSeekBar.getSecondaryProgress();
    }

    boolean hasExceededUpperSeekBound() {
        return mSeekBar.getProgress() >= mSeekBar.getMax();
    }

    boolean hasExceededLowerSeekBound() {
        return mSeekBar.getProgress() <= 0;
    }

    private long getProgramStartUTS() {
        long now = System.currentTimeMillis();
        ScheduleOccurrence schedule = getSchedule();
        if (schedule == null) {
            // One hour program blocks when there is no schedule available.
            return now - (now % DEFAULT_BLOCK_MS);
        } else {
            return schedule.getSoftStartsAtMs();
        }
    }

    private int getProgramDuration() {
        ScheduleOccurrence schedule = getSchedule();

        if (schedule == null) {
            // If there's no schedule then we'll just go in hour-long intervals.
            return DEFAULT_BLOCK_MS;
        } else {
            return (int)(schedule.getEndsAtMs() - schedule.getSoftStartsAtMs());
        }
    }

    /**
     * This is used for two things:
     * 1. Rewinding to start of program. This function returns the DVR position of the start of the program.
     * 2. To seek to requested position when manually seeking (via seekbar).
     * We get the program start DVR position, and then (outside of this function) we add the progress value
     * to determine
     * @param upperBoundMs
     * @return
     */
    long getProgramStartWindowPositionMs(long upperBoundMs) {
        // If we go back to the beginning of the program, how far behind live does that put us?
        // We have to go back to the beginning of the program because that's the closest known
        // absolute timestamp. This gets us the number of milliseconds behind live.
        long msSinceProgramStart = System.currentTimeMillis() - getProgramStartUTS();

        // Now we subtract that number from the HLS upper range.
        // This gives us the number of milliseconds INTO the live window we are.
        // In other words, we are calculating the "currentPosition" property before the player
        // knows it.
        return upperBoundMs - msSinceProgramStart;
    }
}
