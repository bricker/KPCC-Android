package org.kpcc.android;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    public final static String TAG = "LiveFragment";
    public static String ARG_PLAY_NOW = "args_play_now";
    private static long PLAY_START = 0;

    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mBackground;
    private NetworkImageView mAdView;
    private String mScheduleTitle;
    private StreamManager.LiveStream mPlayer;
    private boolean mPlayNow;
    private AudioButtonManager mAudioButtonManager;
    private ProgressBar mPrerollProgressBar;
    private View mPrerollView;
    private ScheduleUpdater mScheduleUpdater;

    public LiveFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mPlayNow = getArguments().getBoolean(ARG_PLAY_NOW);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final MainActivity activity = (MainActivity) getActivity();

        activity.setTitle(R.string.kpcc_live);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        mTitle = (TextView) view.findViewById(R.id.live_title);
        mStatus = (TextView) view.findViewById(R.id.live_status);
        mBackground = (NetworkImageView) view.findViewById(R.id.background);
        mAdView = (NetworkImageView) view.findViewById(R.id.preroll_ad);
        mPrerollProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mPrerollView = view.findViewById(R.id.preroll);

        mScheduleUpdater = new ScheduleUpdater(new ScheduleUpdateCallback() {
            @Override
            public void onUpdate() {
                ScheduleOccurrence.Client.getCurrent(new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject jsonSchedule = response.getJSONObject(ScheduleOccurrence.SINGULAR_KEY);
                            ScheduleOccurrence schedule = ScheduleOccurrence.buildFromJson(jsonSchedule);

                            // It may be null, if nothing is on right now according to the API.
                            if (schedule != null) {
                                mScheduleTitle = schedule.title;
                                mTitle.setText(schedule.title);

                                // If we're before this occurrence's 'soft start', then say "up next".
                                // Otherwise, set "On Now".
                                Date softStartsAt = schedule.softStartsAt;
                                if (softStartsAt != null && new Date().getTime() < softStartsAt.getTime()) {
                                    mStatus.setText(R.string.up_next);
                                } else {
                                    setDefaultValues(true, false);
                                }

                                NetworkImageManager.instance.setBackgroundImage(mBackground, schedule.programSlug);
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

        mAudioButtonManager = new AudioButtonManager(view);
        boolean alreadyPlaying = false;

        if (activity.streamManager != null) {
            StreamManager.LiveStream currentPlayer = activity.streamManager.currentLivePlayer;

            if (currentPlayer != null && currentPlayer.isPlaying()) {
                mPlayer = currentPlayer;
                setupAudioStateHandlers();

                alreadyPlaying = true;
                mAudioButtonManager.togglePlayingForStop();
            }
        }

        if (!alreadyPlaying) {
            mAudioButtonManager.toggleStopped();
        }

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.streamManager != null && activity.streamManager.currentPrerollPlayer != null) {
                    // Preroll was Paused - start it again.
                    activity.streamManager.currentPrerollPlayer.start();
                } else {
                    // We should always set a new mPlayer here. If the play button was clicked,
                    // either it was handled by the preroll case above or the previous
                    // stream was destroyed.
                    mPlayer = new StreamManager.LiveStream(activity, mAdView);
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
                if (activity.streamManager.currentPrerollPlayer != null) {
                    // In this view, only Preroll can be paused.
                    activity.streamManager.currentPrerollPlayer.pause();
                }
            }
        });

        mAudioButtonManager.getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
    public void onDestroyView() {
        mScheduleUpdater.stop();
        super.onDestroyView();
    }

    private void setDefaultValues(boolean doStatus, boolean doImage) {
        if (doStatus) {
            mStatus.setText(R.string.live);
        }
        if (doImage) {
            NetworkImageManager.instance.setDefaultBackgroundImage(mBackground);
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
            public void onProgress(int progress) {
                mPrerollProgressBar.setProgress(progress / 1000);
            }

            @Override
            public void onError() {
                mAudioButtonManager.toggleError(R.string.audio_error);
            }
        });

        mPlayer.setPrerollAudioEventListener(new StreamManager.AudioEventListener() {
            public void onPrerollData(PrerollManager.PrerollData prerollData) {
                mPrerollProgressBar.setMax(prerollData.audioDuration);
                ;
                mPrerollView.setVisibility(View.VISIBLE);
                mStatus.setVisibility(View.INVISIBLE);
                mTitle.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onLoading() {
                mAudioButtonManager.toggleLoading();
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
                mPrerollView.setVisibility(View.GONE);
                mStatus.setVisibility(View.VISIBLE);
                mTitle.setVisibility(View.VISIBLE);
                // Toggle loading so we get a loading icon while live stream is loading.
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onProgress(int progress) {
                mPrerollProgressBar.setProgress(progress / 1000);
            }

            @Override
            public void onError() {
                mAudioButtonManager.toggleError(R.string.audio_error);
            }
        });

    }

    private void logLiveStreamEvent(String key) {
        JSONObject params = new JSONObject();

        try {
            params.put("programTitle", mScheduleTitle);

            if (key.equals(AnalyticsManager.EVENT_LIVE_STREAM_PAUSE)) {
                if (PLAY_START != 0) {
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - PLAY_START);
                    params.put("sessionLengthInSeconds", seconds);
                }
            }
        } catch (JSONException e) {
            // No program title will be sent.
        }

        AnalyticsManager.instance.logEvent(key, params);
    }

    private abstract class ScheduleUpdateCallback {
        public abstract void onUpdate();
    }

    private class ScheduleUpdater implements Runnable {
        private AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private Handler mHandler = new Handler();
        private ScheduleUpdateCallback mCallbacks;

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
