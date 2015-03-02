package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

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

    public LiveFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScheduleOccurrence.Client.getCurrent(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONObject jsonSchedule = response.getJSONObject(ScheduleOccurrence.SINGULAR_KEY);
                    ScheduleOccurrence schedule = ScheduleOccurrence.buildFromJson(jsonSchedule);

                    mTitle.setText(schedule.getTitle());

                    // If we're before this occurrence's 'soft start', then say "up next".
                    // Otherwise, set "On Now".
                    if (new Date().getTime() < schedule.getSoftStartsAt().getTime()) {
                        mStatus.setText(R.string.up_next);
                    } else {
                        mStatus.setText(R.string.on_now);
                    }

                } catch (JSONException e) {
                    Log.d(TAG, "JSON Error");
                    // TODO: Handle failures
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String responseBody, Throwable error) {
                Log.d(TAG, "API Error");
                // TODO: Handle failures
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final MainActivity activity = (MainActivity) getActivity();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        mTitle = (TextView) view.findViewById(R.id.live_title);
        mStatus = (TextView) view.findViewById(R.id.live_status);
        mPlayButton = (Button) view.findViewById(R.id.play_button);
        mStopButton = (Button) view.findViewById(R.id.stop_button);

        final AudioButtonManager audioButtonManager = new AudioButtonManager(activity, view);

        StreamManager streamManager = activity.getStreamManager();
        if (streamManager != null && streamManager.isPlaying(StreamManager.LIVESTREAM_URL)) {
            audioButtonManager.togglePlayingForStop();
        } else {
            audioButtonManager.toggleStopped();
        }

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getStreamManager().playLiveStream(activity, audioButtonManager);
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getStreamManager().stop(audioButtonManager);
            }
        });

        return view;
    }
}
