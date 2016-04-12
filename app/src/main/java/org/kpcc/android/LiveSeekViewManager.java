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
public class LiveSeekViewManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private SeekBar mSeekBar;
    private ImageButton mRewindBtn;
    private ImageButton mForwardBtn;
    private Button mGoLiveBtn;
    private Button mProgramStartBtn;
    private FrameLayout mLiveJumpBtns;
    private FrameLayout mGoLiveBtnWrapper;
    private FrameLayout mProgStartBtnWrapper;
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

    ScheduleOccurrence getSchedule() {
        return mScheduleOccurrence;
    }

    synchronized void setSchedule(ScheduleOccurrence scheduleOccurrence) {
        mScheduleOccurrence = scheduleOccurrence;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void reset() {
        mLiveJumpBtns.setVisibility(View.GONE);
        hideSeekBar();
        hideSeekButtons();
    }

    void showProgramStartBtn() {
        mProgStartBtnWrapper.setVisibility(View.VISIBLE);
        mGoLiveBtnWrapper.setVisibility(View.GONE);
    }

    void showGoLiveBtn() {
        mProgStartBtnWrapper.setVisibility(View.GONE);
        mGoLiveBtnWrapper.setVisibility(View.VISIBLE);
    }

    void skipBackward(boolean canSkipBackward) {
        mSeekBar.setProgress(mSeekBar.getProgress() - LivePlayer.JUMP_INTERVAL_MS);
        mForwardBtn.setAlpha(1.0f);
        mForwardBtn.setEnabled(true);
        showGoLiveBtn();
        toggleBackwardBtn(canSkipBackward);
    }

    void skipForward(boolean canSkipForward) {
        mSeekBar.setProgress(mSeekBar.getProgress() + LivePlayer.JUMP_INTERVAL_MS);
        mRewindBtn.setAlpha(1.0f);
        mRewindBtn.setEnabled(true);
        toggleForwardBtn(canSkipForward);
    }

    void toggleBackwardBtn(boolean canSkipBackward) {
        if (!canSkipBackward) {
            mRewindBtn.setAlpha(0.4f);
            mRewindBtn.setEnabled(false);
        }
    }

    void toggleForwardBtn(boolean canSkipForward) {
        if (!canSkipForward) {
            mForwardBtn.setAlpha(0.4f);
            mForwardBtn.setEnabled(false);
        }
    }

    void enableSeekBar() {
        mSeekBar.setVisibility(View.VISIBLE);
        mSeekBar.setEnabled(true);
    }

    void disableSeekBar() {
        mSeekBar.setEnabled(false);
    }

    void hideSeekBar() {
        mSeekBar.setVisibility(View.GONE);
        disableSeekBar();
    }

    void showSeekButtons() {
        mRewindBtn.setVisibility(View.VISIBLE);
        mForwardBtn.setVisibility(View.VISIBLE);
    }

    void hideSeekButtons() {
        mRewindBtn.setVisibility(View.INVISIBLE);
        mForwardBtn.setVisibility(View.INVISIBLE);
    }

    void showAfterPreroll() {
        mLiveJumpBtns.setVisibility(View.VISIBLE);
        showSeekButtons();
    }

    void hideForPreroll() {
        mLiveJumpBtns.setVisibility(View.INVISIBLE);
        hideSeekButtons();
    }


    //
    // Progress management
    //

    void incrementProgress() {
        mSeekBar.setProgress(mSeekBar.getProgress() + LiveFragment.LIVE_SEEKBAR_REFRESH_INTERVAL);
    }

    void setSeekProgress(int progress) {
        mSeekBar.setProgress(progress);
    }

    void setSeekBarMaxFromSchedule() {
        mSeekBar.setMax(getProgramDuration());
    }

    void setSecondaryProgressFromSchedule() {
        long now = System.currentTimeMillis();
        long programStartUTS = getProgramStartUTS();
        mSeekBar.setSecondaryProgress((int) (now - programStartUTS));
    }

    void setSeekProgressToLiveHead() {
        mSeekBar.setProgress(mSeekBar.getSecondaryProgress());
    }

    void setSeekProgressFromOffset(long offset, long duration) {
        long now = System.currentTimeMillis();
        long currentPositionInWindow = duration - offset;
        long programStartWindowPosition = getProgramStartWindowPosition(duration);
        setSeekProgress((int)(currentPositionInWindow - programStartWindowPosition));

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

    long getProgramStartUTS() {
        long now = System.currentTimeMillis();
        ScheduleOccurrence schedule = getSchedule();
        if (schedule == null) {
            return now - (now % 1000*60*60);
        } else {
            return schedule.getSoftStartsAtMs();
        }
    }

    int getProgramDuration() {
        ScheduleOccurrence schedule = getSchedule();

        if (schedule == null) {
            // If there's no schedule then we'll just go in hour-long intervals.
            return 1000*60*60;
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
     * @param duration
     * @return
     */
    long getProgramStartWindowPosition(long duration) {
        // If we go back to the beginning of the program, how far behind live does that put us?
        // We have to go back to the beginning of the program because that's the closest known
        // absolute timestamp. This gets us the number of milliseconds behind live.
        long timeSinceProgramStart = System.currentTimeMillis() - getProgramStartUTS();

        // Now we subtract that number from the HLS duration to convert it to number of milliseconds
        // INTO the live window we are.
        return duration - timeSinceProgramStart;
    }
}
