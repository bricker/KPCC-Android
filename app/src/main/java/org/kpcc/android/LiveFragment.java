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
    private static final String ARG_PLAY_NOW = "args_play_now";
    public static final String STACK_TAG = "LiveFragment";
    private static long PLAY_START = 0;

    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mAdView;
    private String mScheduleTitle;
    private StreamManager.LiveStream mPlayer;
    private boolean mPlayNow;
    private AudioButtonManager mAudioButtonManager;
    private ProgressBar mPrerollProgressBar;
    private View mPrerollView;
    private ScheduleUpdater mScheduleUpdater;
    private ImageView mBackground;
    private Request mRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (savedInstanceState != null) {
            mPlayNow = savedInstanceState.getBoolean(ARG_PLAY_NOW);
        }
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

        mAudioButtonManager = new AudioButtonManager(view);

        if (activity.streamIsBound) {
            initAudioButtonState();
        } else {
            activity.addOnStreamBindListener(new MainActivity.OnStreamBindListener() {
                @Override
                public void onBind() {
                    initAudioButtonState();
                }
            });
        }

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();

                if (!activity.streamIsBound || !activity.isConnectedToNetwork()) {
                    return;
                }

                if (activity.streamManager.currentPrerollPlayer != null) {
                    // Preroll was Paused - start it again.
                    activity.streamManager.currentPrerollPlayer.start();
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
                MainActivity activity = (MainActivity) getActivity();

                if (!activity.streamIsBound) {
                    return;
                }

                if (activity.streamManager.currentPrerollPlayer != null) {
                    // In this view, only Preroll can be paused.
                    activity.streamManager.currentPrerollPlayer.pause();
                }
            }
        });

        mAudioButtonManager.getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();

                if (!activity.streamIsBound) {
                    return;
                }

                if (activity.streamManager.currentLivePlayer != null) {
                    // In this view, only the Stream can be stopped.
                    // We want to release it immediately too.
                    activity.streamManager.currentLivePlayer.stop();
                    logLiveStreamEvent(AnalyticsManager.EVENT_LIVE_STREAM_PAUSE);
                }
            }
        });

        if (mPlayNow) {
            // Play right away
            mAudioButtonManager.getPlayButton().callOnClick();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

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
                                setDefaultValues(true, true);
                            }


                        } catch (JSONException e) {
                            setDefaultValues(true, true);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setDefaultValues(true, true);
                    }
                });
            }
        });

        mScheduleUpdater.start();
        new Thread(mScheduleUpdater).start();
    }

    @Override
    public void onPause() {
        if (mRequest != null) {
            mRequest.cancel();
        }

        if (mScheduleUpdater != null) {
            mScheduleUpdater.stop();
        }

        super.onPause();
    }

    private void setDefaultValues(boolean doStatus, boolean doImage) {
        if (doStatus) {
            mStatus.setText(R.string.live);
        }
        if (doImage) {
            NetworkImageManager.instance.setDefaultBitmap(mBackground);
        }
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
                mAudioButtonManager.toggleError(R.string.audio_error);
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
                if (getActivity() == null || !((MainActivity) getActivity()).streamIsBound || !isVisible()) {
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

        if (!activity.isConnectedToNetwork()) {
            mAudioButtonManager.toggleError(R.string.network_error);
            return;
        }

        if (activity.streamIsBound) {
            StreamManager.LiveStream currentPlayer = activity.streamManager.currentLivePlayer;
            if (currentPlayer != null && currentPlayer.isPlaying()) {
                mPlayer = currentPlayer;
                setupAudioStateHandlers();
                mAudioButtonManager.togglePlayingForStop();
            }
        }
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
