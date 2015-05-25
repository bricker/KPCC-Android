package org.kpcc.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mAdView;
    private String mScheduleTitle;
    private StreamManager.LiveStream mPlayer;
    private AudioButtonManager mAudioButtonManager;
    private ProgressBar mPrerollProgressBar;
    private View mPrerollView;
    private ScheduleUpdater mScheduleUpdater;
    private Thread mUpdaterThread;
    private ImageView mBackground;
    private Request mRequest;
    private SleepFragment.ProgressObserver mTimerProgressObserver;
    private Thread mTimerProgressThread;
    private TextView mTimerRemaining;

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

        mAudioButtonManager = new AudioButtonManager(view);

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();

                if (!AppConnectivityManager.instance.streamIsBound || !AppConnectivityManager.instance.isConnectedToNetwork()) {
                    // The Error message should already be showing for connectivity problems.
                    // Just do nothing.
                    return;
                }

                if (AppConnectivityManager.instance.streamManager.currentPrerollPlayer != null) {
                    // Preroll was Paused - start it again.
                    AppConnectivityManager.instance.streamManager.currentPrerollPlayer.start();
                } else {
                    // We should always set a new mPlayer here. If the play button was clicked,
                    // either it was handled by the preroll case above or the previous
                    // stream was destroyed.
                    mPlayer = new StreamManager.LiveStream(activity);
                    setupAudioStateHandlers();

                    // Preroll will be handled normally
                    mPlayer.playWithPrerollAttempt();
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

                if (AppConnectivityManager.instance.streamManager.currentPrerollPlayer != null) {
                    // In this view, only Preroll can be paused.
                    AppConnectivityManager.instance.streamManager.currentPrerollPlayer.pause();
                }
            }
        });

        mAudioButtonManager.getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppConnectivityManager.instance.streamIsBound) {
                    return;
                }

                if (AppConnectivityManager.instance.streamManager.currentLivePlayer != null) {
                    // In this view, only the Stream can be stopped.
                    // We want to release it immediately too.
                    AppConnectivityManager.instance.streamManager.currentLivePlayer.stop();
                    logLiveStreamEvent(AnalyticsManager.EVENT_LIVE_STREAM_PAUSE);
                }
            }
        });

        if (DataManager.instance.getPlayNow()) {
            DataManager.instance.setPlayNow(false);

            // Play right away
            mAudioButtonManager.clickPlay();
        }

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
                initAudioButtonState();
            }
        });

        mScheduleUpdater = new ScheduleUpdater(new ScheduleUpdateCallback() {
            @Override
            public void onUpdate() {
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
        });

        if (BaseAlarmManager.SleepManager.instance.isRunning()) {
            mTimerRemaining.setVisibility(View.VISIBLE);

            mTimerProgressObserver = new SleepFragment.ProgressObserver(new SleepFragment.SleepTimerUpdater() {
                @Override
                public void onTimerUpdate(int hours, int mins, int secs) {
                    String mStrHr = getActivity().getResources().getString(R.string.timer_hr);
                    String mStrMin = getActivity().getResources().getString(R.string.timer_min);
                    String mStrSec = getActivity().getResources().getString(R.string.timer_sec);

                    mTimerRemaining.setText(
                            String.valueOf(hours) + mStrHr + " " +
                                    String.valueOf(mins) + mStrMin + " " +
                                    String.valueOf(secs) + mStrSec
                    );
                }

                @Override
                public void onTimerComplete() {
                    mTimerRemaining.setVisibility(View.GONE);
                }
            });

            startTimerUpdater();
        } else {
            mTimerRemaining.setVisibility(View.GONE);
        }

        // The callbacks below will get run right away whatever the state is. We want to start it
        // no matter what to setup the default state, and then let the callbacks handle the
        // connected state. Calling start() again should be safe and not start a second thread.
        AppConnectivityManager.instance.addOnNetworkConnectivityListener(LiveFragment.STACK_TAG, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                // We want this here so the schedule will get updated immediately when connectivity
                // is back, so we'll just restart it.
                startScheduleUpdater();
                mAudioButtonManager.hideError();
            }

            @Override
            public void onDisconnect() {
                stopScheduleUpdater();
                setDefaultValues();
                mAudioButtonManager.showError(R.string.network_error);
            }
        }, true);
    }

    private void startScheduleUpdater() {
        stopScheduleUpdater();

        if (mScheduleUpdater != null && !mScheduleUpdater.mIsObserving.get()) {
            mScheduleUpdater.start();

            if (mUpdaterThread != null && !mUpdaterThread.isInterrupted()) {
                mUpdaterThread.interrupt();
                // GC will take care of the dead thread hanging around.
            }

            mUpdaterThread = new Thread(mScheduleUpdater);
            mUpdaterThread.start();
        }
    }

    private void stopScheduleUpdater() {
        if (mScheduleUpdater != null) {
            mScheduleUpdater.stop();
        }

        if (mUpdaterThread != null) {
            if (!mUpdaterThread.isInterrupted()) {
                mUpdaterThread.interrupt();
            }

            mUpdaterThread = null;
        }
    }

    private void startTimerUpdater() {
        stopTimerUpdater();

        if (mTimerProgressObserver != null && !mTimerProgressObserver.isObserving()) {
            mTimerProgressObserver.start();

            if (mTimerProgressThread != null && !mTimerProgressThread.isInterrupted()) {
                mTimerProgressThread.interrupt();
                // GC will take care of the dead thread hanging around.
            }

            mTimerProgressThread = new Thread(mTimerProgressObserver);
            mTimerProgressThread.start();
        }
    }

    private void stopTimerUpdater() {
        if (mTimerProgressObserver != null) {
            mTimerProgressObserver.stop();
        }

        if (mTimerProgressThread != null) {
            if (!mTimerProgressThread.isInterrupted()) {
                mTimerProgressThread.interrupt();
            }

            mTimerProgressThread = null;
        }
    }

    @Override
    public void onPause() {
        if (mRequest != null) {
            mRequest.cancel();
        }

        stopScheduleUpdater();
        stopTimerUpdater();
        AppConnectivityManager.instance.removeOnNetworkConnectivityListener(LiveFragment.STACK_TAG);

        super.onPause();
    }

    private void setDefaultValues() {
        mStatus.setText(R.string.live);
        NetworkImageManager.instance.setDefaultBitmap(mBackground);
        mTitle.setText(null);
    }

    private void setupAudioStateHandlers() {
        if (mPlayer == null) {
            return;
        }

        mPlayer.setOnAudioEventListener(new StreamManager.AudioEventListener() {
            @Override
            public void onLoading() {
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onPlay() {
                mAudioButtonManager.togglePlayingForStop();
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
            }

            @Override
            public void onCompletion() {
                // MBC has burned down please call 911
            }

            @Override
            public void onError() {
                mAudioButtonManager.toggleStopped();
                mAudioButtonManager.showError(R.string.audio_error);
            }
        });

        mPlayer.setPrerollAudioEventListener(new StreamManager.AudioEventListener() {
            public void onPrerollData(final PrerollManager.PrerollData prerollData) {
                mPrerollProgressBar.setMax(prerollData.audioDurationSeconds * 1000);
                mPrerollView.setVisibility(View.VISIBLE);
                mStatus.setVisibility(View.INVISIBLE);
                mTitle.setVisibility(View.INVISIBLE);

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
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
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
                // Toggle loading so we get a loading icon while live stream is loading.
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onProgress(int progress) {
                mPrerollProgressBar.setProgress(progress);
            }

            @Override
            public void onError() {
                ((MainActivity) getActivity()).getNavigationDrawerFragment().enableDrawer();
            }
        });

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

    private void initAudioButtonState() {
        MainActivity activity = (MainActivity) getActivity();

        if (activity == null) {
            // FIXME: What should we do here?
            return;
        }

        if (AppConnectivityManager.instance.streamIsBound) {
            StreamManager.LiveStream currentPlayer = AppConnectivityManager.instance.streamManager.currentLivePlayer;
            if (currentPlayer != null && currentPlayer.isPlaying()) {
                mPlayer = currentPlayer;
                setupAudioStateHandlers();
                mAudioButtonManager.togglePlayingForStop();
                return;
            }
        }

        // Default State.
        // We put this here to reset any previous error message.
        mAudioButtonManager.toggleStopped();
    }

    private abstract class ScheduleUpdateCallback {
        public abstract void onUpdate();
    }

    private class ScheduleUpdater implements Runnable {
        private final AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private final Handler mHandler = new Handler();
        private final ScheduleUpdateCallback mCallbacks;

        public ScheduleUpdater(ScheduleUpdateCallback callbacks) {
            mCallbacks = callbacks;
        }

        public void start() {
            mIsObserving.set(true);
        }

        public void stop() {
            mIsObserving.set(false);
        }

        @Override
        public void run() {
            while (mIsObserving.get()) {
                // This is a neat feature but a little problematic:
                // On the one hand, we want to update as frequently and accurately as possible,
                // to future-proof the feature (what if there's a 15-minute program?),
                // and also allow for special broadcast events to show up as quickly as possible.
                // On the other hand, we don't want to hit the Schedule API too aggressively.
                // We could do something on a */5 cron-type thing (that is: 0, 5, 10, 15th minute
                // of the hour), but that would be potentially thousands of requests to the
                // servers all at the exact same time.
                // So the compromise is to stagger the requests and hit it every 5 minutes from
                // the first time the loop runs. So screens may be a few minutes off from the
                // live broadcast.
                try {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallbacks.onUpdate();
                        }
                    });

                    Thread.sleep(1000 * 60 * 5); // Run the thread every 5 minutes
                } catch (InterruptedException e) {
                    // If the progress observer fails, just stop the observer.
                    mIsObserving.set(false);
                }
            }
        }
    }
}
