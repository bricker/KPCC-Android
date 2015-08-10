package org.kpcc.android;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

public class SleepFragment extends Fragment {

    private FrameLayout mSetButtonWrapper;
    private Button mCancelButton;
    private TextView mCurrentTimerHeader;
    private SeekBar mSeekbar;
    private ProgressManager mProgressObserver;
    private TextView mSelectionHr;
    private TextView mSelectionMin;
    private TextView mSelectionSec;
    private String mStrHr;
    private String mStrMin;
    private String mStrSec;

    public SleepFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sleep, container, false);
        mSeekbar = (SeekBar) view.findViewById(R.id.seek_bar);
        Button setButton = (Button) view.findViewById(R.id.set_button);
        mSetButtonWrapper = (FrameLayout) view.findViewById(R.id.set_button_wrapper);
        mCancelButton = (Button) view.findViewById(R.id.cancel_button);
        mCurrentTimerHeader = (TextView) view.findViewById(R.id.current_timer_header);

        mSelectionHr = (TextView) view.findViewById(R.id.selection_time_hr);
        mSelectionMin = (TextView) view.findViewById(R.id.selection_time_min);
        mSelectionSec = (TextView) view.findViewById(R.id.selection_time_sec);

        mSeekbar.setMax(96); // 8 hours in 5m intervals.

        mStrHr = getActivity().getResources().getString(R.string.timer_hr);
        mStrMin = getActivity().getResources().getString(R.string.timer_min);
        mStrSec = getActivity().getResources().getString(R.string.timer_sec);

        mProgressObserver = new ProgressManager(new ProgressManager.TimerRunner() {
            @Override
            public void onTimerComplete() {
                showSeekPrompt();
            }

            @Override
            public void onTimerUpdate(int hours, int mins, int secs) {
                mSelectionHr.setText(String.valueOf(hours) + " " + mStrHr);
                mSelectionMin.setText(String.valueOf(mins) + " " + mStrMin);
                mSelectionSec.setText(String.valueOf(secs) + " " + mStrSec);
            }
        }, 1000);

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int totalSeconds = progress * 5 * 60;
                int totalMinutes = totalSeconds / 60;

                int mins = totalMinutes % 60;
                int hours = totalMinutes / 60;

                mSelectionHr.setText(String.valueOf(hours) + " " + mStrHr);
                mSelectionMin.setText(String.valueOf(mins) + " " + mStrMin);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int interval = mSeekbar.getProgress();
                long relativeMillis = interval * 5 * 60 * 1000;
                BaseAlarmManager.SleepManager.instance.set(relativeMillis);
                showCurrentTimerData();

                // Go to the live stream if an episode isn't already playing.
                if (AppConnectivityManager.instance.streamManager != null) {
                    StreamManager.EpisodeStream currentEpisodePlayer = AppConnectivityManager.instance.streamManager.currentEpisodePlayer;
                    if (currentEpisodePlayer == null || !currentEpisodePlayer.isPlaying()) {
                        Intent activityIntent = new Intent(getActivity(), MainActivity.class);
                        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        DataManager.instance.setPlayNow(true);
                        startActivity(activityIntent);
                    }
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseAlarmManager.SleepManager.instance.cancel();
                showSeekPrompt();
            }
        });

        if (BaseAlarmManager.SleepManager.instance.isRunning()) {
            showCurrentTimerData();
        } else {
            showSeekPrompt();
        }

        return view;
    }


    private void showCurrentTimerData() {
        mSetButtonWrapper.setVisibility(View.GONE);
        mSeekbar.setVisibility(View.GONE);

        mCancelButton.setVisibility(View.VISIBLE);
        mCurrentTimerHeader.setVisibility(View.VISIBLE);
        mSelectionSec.setVisibility(View.VISIBLE);

        mProgressObserver.start();
    }

    private void showSeekPrompt() {
        mProgressObserver.release();
        mSeekbar.setProgress(1);
        mSetButtonWrapper.setVisibility(View.VISIBLE);
        mSeekbar.setVisibility(View.VISIBLE);

        mCancelButton.setVisibility(View.GONE);
        mCurrentTimerHeader.setVisibility(View.GONE);
        mSelectionSec.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mProgressObserver.release();
    }
}
