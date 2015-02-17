package org.kpcc.android;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Episode;
import org.kpcc.api.ScheduleOccurrence;

import java.util.Collections;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LiveFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveFragment extends Fragment {
    public final static String TAG = "LiveFragment";

    private final static long PREROLL_THRESHOLD = 600L;
    public final static String LIVESTREAM_URL = "http://live.scpr.org/aac";
    public final static String LIVESTREAM_NOPREROLL_URL = LIVESTREAM_URL + "?preskip=true";

    public TextView mTitle;
    public TextView mStatus;

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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        mTitle = (TextView) view.findViewById(R.id.live_title);
        mStatus = (TextView) view.findViewById(R.id.live_status);

        return view;
    }
}
