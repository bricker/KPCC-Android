package org.kpcc.android;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.ScheduleOccurrence;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class LiveFragment extends Fragment {

    public static final String STACK_TAG = "LiveFragment";
    private static long PLAY_START = 0;
    private static final String LIVESTREAM_LISTENER_KEY = "livestream";

    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mAdView;
    private String mScheduleTitle;
    private StreamManager.LiveStreamBundle streamBundle;
    private AudioButtonManager mAudioButtonManager;
    private ProgressBar mPrerollProgressBar;
    private ProgressManager mProgressManager; // Updates seek bar
    private ProgressManager mPersistentProgressObserver; // Updates live status
    private ProgressManager mPrerollProgressManager; // Updates preroll seek bar
    private ProgressManager mScheduleUpdater; // Updates program info
    private ProgressManager mTimerProgressObserver; // Updates sleep timer
    private View mPrerollView;
    private ImageView mBackground;
    private Request mRequest;
    private TextView mTimerRemaining;
    private LinearLayout mTimerRemainingWrapper;
    private ImageButton mRewind;
    private ImageButton mForward;
    private Button mGoLive;
    private Button mProgStart;
    private SeekBar mSeekBar;
    private FrameLayout mGoLiveWrapper;
    private FrameLayout mProgStartWrapper;
    private FrameLayout mLiveJumpBtns;
    private final AtomicBoolean didInitAudio = new AtomicBoolean(false);
    private ScheduleOccurrence currentSchedule = null;
    private AtomicBoolean isCloseToLive = new AtomicBoolean(true);
    private int positionInWindow = 0;
    private boolean isTimeTraveling = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.kpcc_live);

        View view = inflater.inflate(R.layout.fragment_live, container, false);

        mBackground = (ImageView) view.findViewById(R.id.background);
        mTitle = (TextView) view.findViewById(R.id.live_title);
        mStatus = (TextView) view.findViewById(R.id.live_status);
        mAdView = (NetworkImageView) view.findViewById(R.id.preroll_ad);
        mPrerollProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mPrerollView = view.findViewById(R.id.preroll);
        mTimerRemaining = (TextView)view.findViewById(R.id.timer_remaining);
        mTimerRemainingWrapper = (LinearLayout)view.findViewById(R.id.timer_remaining_wrapper);
        mRewind = (ImageButton)view.findViewById(R.id.rewind);
        mForward = (ImageButton)view.findViewById(R.id.forward);
        mGoLive = (Button)view.findViewById(R.id.golive_button);
        mProgStart = (Button)view.findViewById(R.id.progstart_button);
        mGoLiveWrapper = (FrameLayout)view.findViewById(R.id.golive_wrapper);
        mProgStartWrapper = (FrameLayout)view.findViewById(R.id.progstart_wrapper);
        mLiveJumpBtns = (FrameLayout)view.findViewById(R.id.live_jump_buttons);
        mSeekBar = (SeekBar)view.findViewById(R.id.live_seek_bar);

        mAudioButtonManager = new AudioButtonManager(view);

        // Register a listener to start/stop the stream immediately based on connectivity status.
        // We had a process in place to handle this lazily but there was a certain device which
        // wasn't releasing the stream, and ended up creating a new stream every time a network
        // was available and simultaneously streaming dozens (or more) streams at once. I think.
        // It's hard to tell for sure but this is what I guess was happening.
        // So, we're being more proactive about managing streams on network connectivity changes.
        AppConnectivityManager.instance.addOnNetworkConnectivityListener(LIVESTREAM_LISTENER_KEY, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                StreamManager.LiveStream currentLivePlayer = AppConnectivityManager.instance.streamManager.currentLivePlayer;

                if (currentLivePlayer != null && currentLivePlayer.didStopOnConnectivityLoss) {
                    currentLivePlayer.stop();
                    currentLivePlayer.prepareAndStartFromLive();
                    currentLivePlayer.didStopOnConnectivityLoss = false;
                }
            }

            @Override
            public void onDisconnect() {
                StreamManager.LiveStream currentLivePlayer = AppConnectivityManager.instance.streamManager.currentLivePlayer;

                if (currentLivePlayer != null && currentLivePlayer.isPlaying()) {
                    currentLivePlayer.didStopOnConnectivityLoss = true;
                    currentLivePlayer.stop();
                }
            }
        }, false);

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppConnectivityManager.instance.streamIsBound || !AppConnectivityManager.instance.isConnectedToNetwork()) {
                    // The Error message should already be showing for connectivity problems.
                    // Just do nothing.
                    return;
                }

                initAudio();

                if (streamBundle.prerollStream.isPaused) {
                    // Preroll was Paused; start it again.
                    streamBundle.prerollStream.start();
                } else if (streamBundle.liveStream.isPaused && streamBundle.liveStream.pausedAt > (System.currentTimeMillis() - 1000*60*60*8)) {
                    // Live stream was paused; start it again.
                    streamBundle.liveStream.start();
                } else {
                    // Preroll will be handled normally
                    streamBundle.playWithPrerollAttempt();
                    if (currentSchedule != null) {
                        mSeekBar.setProgress((int) currentSchedule.timeSinceSoftStartMs());
                    }

                    toggleBtnProgStart();
                    PLAY_START = System.currentTimeMillis();
                    logLiveStreamEvent(AnalyticsManager.EVENT_LIVE_STREAM_PLAY);
                }
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppConnectivityManager.instance.streamIsBound) {
                    return;
                }

                StreamManager.PrerollStream prerollPlayer = AppConnectivityManager.instance.streamManager.currentPrerollPlayer;
                StreamManager.LiveStream livePlayer = AppConnectivityManager.instance.streamManager.currentLivePlayer;

                if (prerollPlayer != null && prerollPlayer.isPlaying()) {
                    AppConnectivityManager.instance.streamManager.currentPrerollPlayer.pause();
                } else if (livePlayer != null && livePlayer.isPlaying()) {
                    AppConnectivityManager.instance.streamManager.currentLivePlayer.pause();
                }
            }
        });

        mTimerRemainingWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer index = Navigation.NavigationItem.getIndexByStackTag(AlarmFragment.STACK_TAG);

                if (index != null) {
                    ((MainActivity) getActivity()).getNavigationDrawerFragment().selectItem(index);
                }
            }
        });

        mSeekBar.setEnabled(false);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                if (streamBundle != null) {
                    if (progress >= mSeekBar.getSecondaryProgress()) {
                        mSeekBar.setProgress((int)currentSchedule.timeSinceSoftStartMs());
                        streamBundle.liveStream.seekToLive();
                        toggleBtnProgStart();
                        return;
                    }

                    long _now = System.currentTimeMillis();
                    long _duration = streamBundle.liveStream.getDuration();
                    long _start = currentSchedule.softStartsAtMs;
                    long _behind = _now - _start;
                    positionInWindow = (int)(_duration - _behind + progress);

                    streamBundle.liveStream.seekTo(positionInWindow);
                }

                toggleBtnGoLive();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mRewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamBundle != null) {
                    int jumpMs = 1000 * StreamManager.LiveStream.JUMP_INTERVAL_SEC;
                    mSeekBar.setProgress(mSeekBar.getProgress() - jumpMs);
                    positionInWindow -= jumpMs;

                    if (positionInWindow > jumpMs) {
                        streamBundle.liveStream.seekTo(positionInWindow);
                    }
                    toggleBtnGoLive();

                    if (mSeekBar.getProgress() <= 0) {
                        mRewind.setEnabled(false);
                        // don't judge me
                        isTimeTraveling = true;
                        updateSchedule(currentSchedule.startsAtMs - 1000*60);
                    }
                }
            }
        });
        mForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamBundle != null) {
                    int jumpMs = 1000 * StreamManager.LiveStream.JUMP_INTERVAL_SEC;
                    mSeekBar.setProgress(mSeekBar.getProgress() + jumpMs);
                    positionInWindow += jumpMs;

                    if (streamBundle.liveStream.getDuration() - jumpMs > positionInWindow) {
                        streamBundle.liveStream.seekTo(positionInWindow);
                    }

                    if (mSeekBar.getProgress() >= mSeekBar.getMax()) {
                        mForward.setEnabled(false);
                        isTimeTraveling = true;
                        updateSchedule(currentSchedule.endsAtMs + 1000*60);
                    }
                }
            }
        });

        mGoLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isTimeTraveling = false;
                updateSchedule(System.currentTimeMillis());

                if (streamBundle != null) {
                    positionInWindow = (int)streamBundle.liveStream.getDuration();
                    streamBundle.liveStream.pause();
                    streamBundle.liveStream.seekToLive();

                    playRewindBeat(new StreamManager.AudioEventListener() {
                        @Override
                        public void onLoading() {
                            mAudioButtonManager.toggleLoading();
                        }

                        @Override
                        public void onPlay() {
                        }

                        @Override
                        public void onPause() {
                        }

                        @Override
                        public void onStop() {
                        }

                        @Override
                        public void onCompletion() {
                            mSeekBar.setProgress((int) currentSchedule.timeSinceSoftStartMs());
                            toggleBtnProgStart();
                            streamBundle.liveStream.start();
                        }

                        @Override
                        public void onError() {
                        }
                    });
                }
            }
        });

        mProgStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamBundle != null && currentSchedule != null) {
                    streamBundle.liveStream.pause();
                    long _now = System.currentTimeMillis();
                    long _duration = streamBundle.liveStream.getDuration();
                    long _start = currentSchedule.softStartsAtMs;
                    long _behind = _now - _start;
                    positionInWindow = (int)(_duration - _behind);

                    streamBundle.liveStream.seekTo(positionInWindow);

                    playRewindBeat(new StreamManager.AudioEventListener() {
                        @Override
                        public void onLoading() {
                            mAudioButtonManager.toggleLoading();
                        }

                        @Override
                        public void onPlay() {
                        }

                        @Override
                        public void onPause() {
                        }

                        @Override
                        public void onStop() {
                        }

                        @Override
                        public void onCompletion() {
                            toggleBtnGoLive();
                            mSeekBar.setProgress(0);
                            streamBundle.liveStream.start();
                        }

                        @Override
                        public void onError() {
                        }
                    });
                }
            }
        });

        return view;
    }

    private void playRewindBeat(StreamManager.AudioEventListener eventListener) {
        StreamManager.RawStream rewindAudio = new StreamManager.RawStream(getActivity(), "asset:///audio/rewind.mp3");
        rewindAudio.setOnAudioEventListener(eventListener);
        rewindAudio.prepareAndStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        mPersistentProgressObserver = new ProgressManager(new Runnable() {
            @Override
            public void run() {
                // Do this stuff every second. This should never be stopped by any action except
                // leaving the fragment.
                int relativeLiveHead = mSeekBar.getSecondaryProgress();

                if (currentSchedule != null) {
                    relativeLiveHead = (int) currentSchedule.timeSinceSoftStartMs();
                }
                long currentPositionMs = mSeekBar.getProgress();
                mSeekBar.setSecondaryProgress(relativeLiveHead);

                if (streamBundle != null && streamBundle.liveStream != null) {
                    if (relativeLiveHead > (currentPositionMs + (1000 * 60 * 2))) {
                        isCloseToLive.set(false);

                        int minBehindLive = (int) (relativeLiveHead - currentPositionMs) / 1000 / 60;

                        String val;
                        if (minBehindLive <= 1) {
                            val = getString(R.string.less_than_a_minute);
                        } else {
                            val = String.valueOf(minBehindLive) + " " + getString(R.string.minutes);
                        }

                        val = val + " " + getString(R.string.behind_live);
                        mStatus.setText(val);

                        toggleBtnGoLive();
                    } else {
                        // If we're before this occurrence's 'soft start', then say "up next".
                        // Otherwise, set "On Now".
                        if (currentSchedule != null && System.currentTimeMillis() < currentSchedule.softStartsAtMs) {
                            mStatus.setText(R.string.up_next);
                        } else {
                            mStatus.setText(R.string.live);
                        }

                        toggleBtnProgStart();
                    }

                    if (relativeLiveHead < (currentPositionMs + (1000 * StreamManager.LiveStream.JUMP_INTERVAL_SEC))) {
                        mForward.setAlpha((float)0.4);
                        mForward.setEnabled(false);
                    } else {
                        mForward.setAlpha((float)1.0);
                        mForward.setEnabled(true);
                    }

                    if (positionInWindow < 1000*StreamManager.LiveStream.JUMP_INTERVAL_SEC) {
                        mRewind.setAlpha((float)0.4);
                        mRewind.setEnabled(false);
                    } else {
                        mRewind.setAlpha((float)1.0);
                        mRewind.setEnabled(true);
                    }
                }
            }
        }, 1000);

        mScheduleUpdater = new ProgressManager(new Runnable() {
            @Override
            public void run() {
                if (!isTimeTraveling) {
                    updateSchedule(System.currentTimeMillis());
                }
            }
        }, 1000 * 60 * 5); // Run the thread every 5 minutes

        if (BaseAlarmManager.SleepManager.instance.isRunning()) {
            mTimerRemainingWrapper.setVisibility(View.VISIBLE);

            mTimerProgressObserver = new ProgressManager(new ProgressManager.TimerRunner() {
                @Override
                public void onTimerUpdate(int hours, int mins, int secs) {
                    mTimerRemaining.setText(
                            String.format("%s:%s:%s",
                                    String.valueOf(hours),
                                    String.format("%02d", mins),
                                    String.format("%02d", secs)
                            )
                    );
                }

                @Override
                public void onTimerComplete() {
                    mTimerRemainingWrapper.setVisibility(View.GONE);
                }
            }, 1000);

            mTimerProgressObserver.start();
        } else {
            mTimerRemainingWrapper.setVisibility(View.GONE);
        }

        // This will init state immediately if stream is already bound.
        // We're putting it here (in onResume) so it can change for network updates.
        AppConnectivityManager.instance.addOnStreamBindListener(new AppConnectivityManager.OnStreamBindListener() {
            @Override
            public void onBind() {
                initAudio();

                // This must come after calling initAudio(), otherwise the play button could be
                // clicked before we have the chance to hide it if necessary.
                if (DataManager.instance.getPlayNow()) {
                    DataManager.instance.setPlayNow(false);

                    // Play right away
                    mAudioButtonManager.clickPlay();
                }
            }
        });

        // The callbacks below will get run right away whatever the state is. We want to start it
        // no matter what to setup the default state, and then let the callbacks handle the
        // connected state. Calling start() again should be safe and not start a second thread.
        AppConnectivityManager.instance.addOnNetworkConnectivityListener(LiveFragment.STACK_TAG, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                // We want this here so the schedule will get updated immediately when connectivity
                // is back, so we'll just restart it.
                if (mScheduleUpdater != null) {
                    mScheduleUpdater.start();
                }

                mAudioButtonManager.hideError();
            }

            @Override
            public void onDisconnect() {
                if (mScheduleUpdater != null) {
                    mScheduleUpdater.release();
                }
                setDefaultValues();
                mAudioButtonManager.showError(R.string.network_error);
            }
        }, true);
    }

    private void updateSchedule(long uts) {
        mRequest = ScheduleOccurrence.Client.getAtTimestamp(uts / 1000,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject jsonSchedule = response.getJSONObject(ScheduleOccurrence.SINGULAR_KEY);
                    ScheduleOccurrence schedule = ScheduleOccurrence.buildFromJson(jsonSchedule);

                    // It may be null, if nothing is on right now according to the API.
                    if (schedule != null) {
                        // Don't make a network request for the image if it's the same program.
                        if (currentSchedule == null || !currentSchedule.programSlug.equals(schedule.programSlug)) {
                            NetworkImageManager.instance.setBitmap(mBackground, schedule.programSlug, getActivity());
                        }

                        currentSchedule = schedule;
                        mScheduleTitle = currentSchedule.title;
                        if (streamBundle != null) {
                            streamBundle.liveStream.currentSchedule = schedule;
                        }

                        float length = (float) mScheduleTitle.length();
                        if (length < 12) {
                            mTitle.setTextSize(50);
                        } else if (length >= 12 && length < 18) {
                            mTitle.setTextSize(40);
                        } else {
                            mTitle.setTextSize(30);
                        }

                        mSeekBar.setMax(currentSchedule.length());

                        if (isTimeTraveling) {
                            mSeekBar.setProgress(mSeekBar.getMax());
                        }

                        mRewind.setEnabled(true);

                        if (positionInWindow < streamBundle.liveStream.getDuration() - StreamManager.LiveStream.JUMP_INTERVAL_SEC*1000) {
                            mForward.setEnabled(true);
                        }

                        mTitle.setText(mScheduleTitle);
                    } else {
                        setDefaultValues();
                    }


                } catch (JSONException e) {
                    setDefaultValues();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                setDefaultValues();
            }
        });
    }

    @Override
    public void onPause() {
        if (mRequest != null) { mRequest.cancel(); }
        if (mScheduleUpdater != null) { mScheduleUpdater.release(); }
        if (mTimerProgressObserver != null) { mTimerProgressObserver.release(); }
        if (mProgressManager != null) { mProgressManager.release(); }
        if (mPrerollProgressManager != null) { mPrerollProgressManager.release(); }
        if (mPersistentProgressObserver != null) {mPersistentProgressObserver.release(); }

        AppConnectivityManager.instance.removeOnNetworkConnectivityListener(LiveFragment.STACK_TAG);

        super.onPause();
    }

    private void setDefaultValues() {
        mStatus.setText(R.string.live);
        NetworkImageManager.instance.setDefaultBitmap(mBackground);
        mTitle.setText(null);
    }

    private void setupAudioStateHandlers() {
        if (streamBundle == null) {
            return;
        }

        streamBundle.liveStream.setOnAudioEventListener(new StreamManager.AudioEventListener() {
            @Override
            public void onLoading() {
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onPlay() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAudioButtonManager.togglePlayingForPause();
                        showSeekButtons();
                        mSeekBar.setVisibility(View.VISIBLE);
                        mSeekBar.setEnabled(true);
                    }
                });

                if (mProgressManager != null) { mProgressManager.start(); }
                if (mScheduleUpdater != null) { mScheduleUpdater.start(); }
                if (!mPersistentProgressObserver.isObserving()) { mPersistentProgressObserver.start(); }
            }

            @Override
            public void onProgress(int progress) {
                mSeekBar.setProgress(mSeekBar.getProgress() + 100);
                if (positionInWindow <= 0) {
                    positionInWindow = (int)streamBundle.liveStream.getDuration();
                }
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                streamBundle.liveStream.pausedAt = System.currentTimeMillis();
                isTimeTraveling = true;
                if (mProgressManager != null) { mProgressManager.release(); }
                if (mScheduleUpdater != null) { mScheduleUpdater.release(); }
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
                streamBundle.liveStream.pausedAt = System.currentTimeMillis();
                if (mProgressManager != null) { mProgressManager.release(); }
                if (mScheduleUpdater != null) { mScheduleUpdater.release(); }
            }

            @Override
            public void onCompletion() {
                // MBC has burned down please call 911
            }

            @Override
            public void onError() {
                mAudioButtonManager.toggleStopped();
                mAudioButtonManager.showError(R.string.audio_error);
                if (mProgressManager != null) { mProgressManager.release(); }
            }
        });

        streamBundle.prerollStream.setOnAudioEventListener(new StreamManager.AudioEventListener() {
            public void onPrerollData(final PrerollManager.PrerollData prerollData) {
                mPrerollProgressBar.setMax(prerollData.audioDurationSeconds * 1000);
                mPrerollView.setVisibility(View.VISIBLE);
                mStatus.setVisibility(View.INVISIBLE);
                mTitle.setVisibility(View.INVISIBLE);
                mTimerRemainingWrapper.setVisibility(View.GONE);
                mLiveJumpBtns.setVisibility(View.INVISIBLE);
                mRewind.setVisibility(View.INVISIBLE);
                mForward.setVisibility(View.INVISIBLE);

                if (prerollData.assetUrl != null) {
                    NetworkImageManager.instance.setPrerollImage(mAdView, prerollData.assetUrl);

                    if (prerollData.assetClickUrl != null) {
                        mAdView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Uri uri = Uri.parse(prerollData.assetClickUrl);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    getActivity().startActivity(intent);
                                }

                                if (prerollData.trackingUrl != null) {
                                    HttpRequest.ImpressionRequest.get(prerollData.trackingUrl);
                                }
                            }
                        });
                    }

                    if (prerollData.impressionUrl != null) {
                        HttpRequest.ImpressionRequest.get(prerollData.impressionUrl);
                    }
                }
            }

            @Override
            public void onLoading() {
                mAudioButtonManager.toggleLoading();
                ((MainActivity) getActivity()).getNavigationDrawerFragment().disableDrawer();
            }

            @Override
            public void onPlay() {
                mAudioButtonManager.togglePlayingForPause();
                mPrerollProgressManager = new ProgressManager(new ProgressManager.ProgressBarRunner(streamBundle.prerollStream), 100);
                mPrerollProgressManager.start();
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                if (mPrerollProgressManager != null) { mPrerollProgressManager.release(); }
            }

            @Override
            public void onStop() {
                MainActivity activity = ((MainActivity) getActivity());

                if (activity != null) {
                    NavigationDrawerFragment frag = activity.getNavigationDrawerFragment();
                    if (frag != null) {
                        frag.enableDrawer();
                    }
                }

                // This gets called when the preroll player is released, which happens
                // when it's completed. Don't toggle any button state here because we're going to
                // immediately switch to livestream, which will handle button state updates.

                if (mPrerollProgressManager != null) { mPrerollProgressManager.release(); }
            }

            @Override
            public void onCompletion() {
                if (!isVisible()) {
                    return;
                }

                ((MainActivity) getActivity()).getNavigationDrawerFragment().enableDrawer();
                mAdView.setVisibility(View.GONE);
                mPrerollView.setVisibility(View.GONE);
                mStatus.setVisibility(View.VISIBLE);
                mTitle.setVisibility(View.VISIBLE);
                mLiveJumpBtns.setVisibility(View.VISIBLE);
                mRewind.setVisibility(View.VISIBLE);
                mForward.setVisibility(View.VISIBLE);

                if (BaseAlarmManager.SleepManager.instance.isRunning()) {
                    mTimerRemainingWrapper.setVisibility(View.VISIBLE);
                }

                // Toggle loading so we get a loading icon while live stream is loading.
                mAudioButtonManager.toggleLoading();
                if (mPrerollProgressManager != null) { mPrerollProgressManager.release(); }
            }

            @Override
            public void onProgress(int progress) {
                mPrerollProgressBar.setProgress(progress);
            }

            @Override
            public void onError() {
                ((MainActivity) getActivity()).getNavigationDrawerFragment().enableDrawer();
                if (mPrerollProgressManager != null) { mPrerollProgressManager.release(); }
            }
        });

        mProgressManager = new ProgressManager(new ProgressManager.ProgressBarRunner(streamBundle.liveStream), 100);
        if (streamBundle.liveStream.isPlaying()) { mProgressManager.start(); }
        // There is a possible bug here, where if you start preroll and leave, then come back,
        // the observer won't refresh. I'm not super worried about it.
    }

    private void toggleBtnProgStart() {
        mGoLiveWrapper.setVisibility(View.GONE);
        mProgStartWrapper.setVisibility(View.VISIBLE);
    }

    private void toggleBtnGoLive() {
        if (streamBundle != null) {
            mGoLiveWrapper.setVisibility(View.VISIBLE);
            mProgStartWrapper.setVisibility(View.GONE);
        }
    }

    private void logLiveStreamEvent(String key) {
        JSONObject params = new JSONObject();

        try {
            params.put(AnalyticsManager.PARAM_PROGRAM_TITLE, mScheduleTitle);

            if (key.equals(AnalyticsManager.EVENT_LIVE_STREAM_PAUSE)) {
                if (PLAY_START != 0) {
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - PLAY_START);
                    params.put(AnalyticsManager.PARAM_SESSION_LENGTH, seconds);
                }
            }
        } catch (JSONException e) {
            // No program title will be sent.
        }

        AnalyticsManager.instance.logEvent(key, params);
    }

    private void initAudio() {
        if (didInitAudio.get()) {
            return;
        }

        if (AppConnectivityManager.instance.streamIsBound) {
            StreamManager.LiveStream currentPlayer = AppConnectivityManager.instance.streamManager.currentLivePlayer;
            StreamManager.PrerollStream currentPreroll = AppConnectivityManager.instance.streamManager.currentPrerollPlayer;
            Context context = getActivity();

            if (currentPlayer != null && currentPlayer.state != StreamManager.State.RELEASED) {
                if (currentPreroll != null) {
                    streamBundle = new StreamManager.LiveStreamBundle(context, currentPlayer, currentPreroll);
                } else {
                    streamBundle = new StreamManager.LiveStreamBundle(context, currentPlayer);
                }
            } else {
                // Nothing is playing, build a brand new bundle.
                streamBundle = new StreamManager.LiveStreamBundle(context);
            }

            setupAudioStateHandlers();

            if (currentPlayer != null && currentPlayer.isPlaying()) {
                mAudioButtonManager.togglePlayingForPause();
                mSeekBar.setVisibility(View.VISIBLE);
                mSeekBar.setEnabled(true);
            } else if (currentPreroll != null && currentPreroll.isPlaying()) {
                mAudioButtonManager.togglePlayingForPause();
            } else {
                // Default State.
                // We put this here to reset any previous error message.
                mAudioButtonManager.toggleStopped();
                hideSeekButtons();
            }

            didInitAudio.set(true);
        } else {
            mAudioButtonManager.toggleStopped();
        }
    }

    private void hideSeekButtons() {
        mRewind.setVisibility(View.INVISIBLE);
        mForward.setVisibility(View.INVISIBLE);
    }

    private void showSeekButtons() {
        mRewind.setVisibility(View.VISIBLE);
        mForward.setVisibility(View.VISIBLE);
    }
}
