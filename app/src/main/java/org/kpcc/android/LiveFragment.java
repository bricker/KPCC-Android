package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.ScheduleOccurrence;

import java.util.Date;
import java.util.concurrent.TimeUnit;


public class LiveFragment extends Fragment {
    public final static String TAG = "LiveFragment";
    private static long PLAY_START = 0;

    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mBackground;
    private NetworkImageView mAdView;
    private String mScheduleTitle;
    private StreamManager.LiveStream mPlayer;

    public LiveFragment() {
        // Required empty public constructor
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
                            mStatus.setText(R.string.on_now);
                        }

                        NetworkImageManager.instance.setBackgroundImage(mBackground, schedule.programSlug);
                    } else {
                        mStatus.setText(R.string.live);
                        NetworkImageManager.instance.setDefaultBackgroundImage(mBackground);
                    }


                } catch (JSONException e) {
                    // TODO: Handle failures
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle failures
            }
        });

        final AudioButtonManager audioButtonManager = new AudioButtonManager(view);
        final StreamManager sm = activity.streamManager;
        boolean alreadyPlaying = false;

        if (sm != null) {
            StreamManager.LiveStream currentPlayer = sm.currentLivePlayer;

            if (currentPlayer != null && currentPlayer.isPlaying()) {
                mPlayer = currentPlayer;

                alreadyPlaying = true;
                audioButtonManager.togglePlayingForStop();
            }
        }

        if (!alreadyPlaying) {
            audioButtonManager.toggleStopped();
        }

        audioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sm != null && sm.currentPrerollPlayer != null) {
                    // Preroll was Paused - start it again.
                    activity.streamManager.currentPrerollPlayer.start();
                } else {
                    // We should always set a new mPlayer here. If the play button was clicked,
                    // either it was handled by the preroll case above or the previous
                    // stream was destroyed.
                    mPlayer = new StreamManager.LiveStream(activity, audioButtonManager, mAdView);

                    // Preroll will be handled normally
                    mPlayer.playWithPrerollAttempt();
                    PLAY_START = System.currentTimeMillis();
                    logLiveStreamEvent(AnalyticsManager.EVENT_LIVE_STREAM_PLAY);
                }
            }
        });

        audioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.streamManager.currentPrerollPlayer != null) {
                    // In this view, only Preroll can be paused.
                    activity.streamManager.currentPrerollPlayer.pause();
                }
            }
        });

        audioButtonManager.getStopButton().setOnClickListener(new View.OnClickListener() {
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

        return view;
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
}
