package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.ScheduleOccurrence;

import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LiveFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveFragment extends Fragment {
    public final static String TAG = "LiveFragment";

    private TextView mTitle;
    private TextView mStatus;
    private Button mPlayButton;
    private Button mStopButton;
    private NetworkImageView mBackground;

    public LiveFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment LiveFragment.
     */
    public static LiveFragment newInstance() {
        LiveFragment fragment = new LiveFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final MainActivity activity = (MainActivity) getActivity();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        mTitle = (TextView) view.findViewById(R.id.live_title);
        mStatus = (TextView) view.findViewById(R.id.live_status);
        mBackground = (NetworkImageView) view.findViewById(R.id.background);

        ScheduleOccurrence.Client.getCurrent(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject jsonSchedule = response.getJSONObject(ScheduleOccurrence.SINGULAR_KEY);
                    ScheduleOccurrence schedule = ScheduleOccurrence.buildFromJson(jsonSchedule);

                    // It may be null, if nothing is on right now according to the API.
                    if (schedule != null) {
                        mTitle.setText(schedule.getTitle());

                        // If we're before this occurrence's 'soft start', then say "up next".
                        // Otherwise, set "On Now".
                        Date softStartsAt = schedule.getSoftStartsAt();
                        if (softStartsAt != null && new Date().getTime() < softStartsAt.getTime()) {
                            mStatus.setText(R.string.up_next);
                        } else {
                            mStatus.setText(R.string.on_now);
                        }

                        BackgroundImageManager.getInstance().setBackgroundImage(mBackground, schedule.getProgramSlug());
                    } else {
                        mStatus.setText(R.string.live);
                        BackgroundImageManager.getInstance().setDefaultBackgroundImage(mBackground);
                    }


                } catch (JSONException e) {
                    Log.d(TAG, "JSON Error");
                    // TODO: Handle failures
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "API Error");
                // TODO: Handle failures
            }
        });

        final AudioButtonManager audioButtonManager = new AudioButtonManager(activity, view);

        StreamManager streamManager = activity.getStreamManager();
        if (streamManager != null && streamManager.isPlaying(StreamManager.LIVESTREAM_URL)) {
            audioButtonManager.togglePlayingForStop();
        } else {
            audioButtonManager.toggleStopped();
        }

        audioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getStreamManager().playLiveStream(activity, audioButtonManager);
            }
        });

        audioButtonManager.getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getStreamManager().stop(audioButtonManager);
            }
        });

        return view;
    }
}
