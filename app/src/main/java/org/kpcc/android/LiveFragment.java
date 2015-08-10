package org.kpcc.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.ScheduleOccurrence;

import java.util.Date;
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
    private ProgressManager mProgressManager;
    private ProgressManager mPrerollProgressManager;
    private View mPrerollView;
    private ProgressManager mScheduleUpdater;
    private ProgressManager mTimerProgressObserver;
    private ImageView mBackground;
    private Request mRequest;
    private TextView mTimerRemaining;
    private LinearLayout mTimerRemainingWrapper;
    private Button mRewind;
    private final AtomicBoolean didInitAudio = new AtomicBoolean(false);

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
        mRewind = (Button)view.findViewById(R.id.rewind);

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
                    currentLivePlayer.prepareAndStart();
                    currentLivePlayer.didStopOnConnectivityLoss = false;
                }
            }

            @Override
            public void onDisconnect() {
                StreamManager.LiveStream currentLivePlayer = AppConnectivityManager.instance.streamManager.currentLivePlayer;

                if (currentLivePlayer != null) {
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
                } else if (streamBundle.liveStream.isPaused) {
                    // Live stream was paused; start it again.
                    streamBundle.liveStream.start();
                } else {
                    // Preroll will be handled normally
                    streamBundle.playWithPrerollAttempt();
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

        mRewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (streamBundle != null) {
                    long currentPosition = streamBundle.liveStream.getCurrentPosition();
                    streamBundle.liveStream.seekTo(currentPosition - 1000*30);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

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

        mScheduleUpdater = new ProgressManager(new Runnable() {
            @Override
            public void run() {
                mRequest = ScheduleOccurrence.Client.getCurrent(new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject jsonSchedule = response.getJSONObject(ScheduleOccurrence.SINGULAR_KEY);
                            ScheduleOccurrence schedule = ScheduleOccurrence.buildFromJson(jsonSchedule);

                            // It may be null, if nothing is on right now according to the API.
                            if (schedule != null) {
                                mScheduleTitle = schedule.title;

                                float length = (float) mScheduleTitle.length();
                                if (length < 12) {
                                    mTitle.setTextSize(50);
                                } else if (length >= 12 && length < 18) {
                                    mTitle.setTextSize(40);
                                } else {
                                    mTitle.setTextSize(30);
                                }

                                mTitle.setText(mScheduleTitle);

                                // If we're before this occurrence's 'soft start', then say "up next".
                                // Otherwise, set "On Now".
                                Date softStartsAt = schedule.softStartsAt;
                                if (softStartsAt != null && new Date().getTime() < softStartsAt.getTime()) {
                                    mStatus.setText(R.string.up_next);
                                } else {
                                    mStatus.setText(R.string.live);
                                }

                                NetworkImageManager.instance.setBitmap(mBackground, schedule.programSlug, getActivity());
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
        }, 1000 * 60 * 5); // Run the thread every 5 minutes)

        if (BaseAlarmManager.SleepManager.instance.isRunning()) {
            mTimerRemainingWrapper.setVisibility(View.VISIBLE);

            mTimerProgressObserver = new ProgressManager(new ProgressManager.TimerRunner() {
                @Override
                public void onTimerUpdate(int hours, int mins, int secs) {
                    mTimerRemaining.setText(
                            String.valueOf(hours) + ":" +
                                    String.format("%02d", mins) + ":" +
                                    String.format("%02d", secs)
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

        // The callbacks below will get run right away whatever the state is. We want to start it
        // no matter what to setup the default state, and then let the callbacks handle the
        // connected state. Calling start() again should be safe and not start a second thread.
        AppConnectivityManager.instance.addOnNetworkConnectivityListener(LiveFragment.STACK_TAG, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                // We want this here so the schedule will get updated immediately when connectivity
                // is back, so we'll just restart it.
                if (mScheduleUpdater != null) { mScheduleUpdater.start(); }

                mAudioButtonManager.hideError();
            }

            @Override
            public void onDisconnect() {
                if (mScheduleUpdater != null) { mScheduleUpdater.release(); }
                setDefaultValues();
                mAudioButtonManager.showError(R.string.network_error);
            }
        }, true);
    }

    @Override
    public void onPause() {
        if (mRequest != null) { mRequest.cancel(); }
        if (mScheduleUpdater != null) { mScheduleUpdater.release(); }
        if (mTimerProgressObserver != null) { mTimerProgressObserver.release(); }
        if (mProgressManager != null) { mProgressManager.release(); }
        if (mPrerollProgressManager != null) { mPrerollProgressManager.release(); }

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
                mAudioButtonManager.togglePlayingForPause();
                if (mProgressManager != null) { mProgressManager.start(); }
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                if (mProgressManager != null) { mProgressManager.release(); }
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
                if (mProgressManager != null) { mProgressManager.release(); }
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

            if (currentPlayer != null) {
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
            } else if (currentPreroll != null && currentPreroll.isPlaying()) {
                mAudioButtonManager.togglePlayingForPause();
            } else {
                // Default State.
                // We put this here to reset any previous error message.
                mAudioButtonManager.toggleStopped();
            }

            didInitAudio.set(true);
        } else {
            mAudioButtonManager.toggleStopped();
        }
    }
}
