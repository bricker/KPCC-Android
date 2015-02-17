package org.kpcc.android;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Episode;
import org.kpcc.api.Program;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 */
public class EpisodesFragment extends Fragment implements AbsListView.OnItemClickListener {
    public final static String TAG = "EpisodesFragment";
    private static final String ARG_PROGRAM_SLUG = "program_slug";

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    private Program mProgram;
    private ArrayList<Episode> mEpisodes = new ArrayList<>();

    public static EpisodesFragment newInstance(String programSlug) {
        EpisodesFragment fragment = new EpisodesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_SLUG, programSlug);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    protected EpisodesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            String programSlug = getArguments().getString(ARG_PROGRAM_SLUG);
            mProgram = ProgramsManager.getInstance().find(programSlug);
        }

        RequestParams params = new RequestParams();
        params.add("program", mProgram.getSlug());
        params.add("limit", "8");

        Episode.Client.getCollection(params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                // TODO: Loading indicator
            }

            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "Got Episodes.");

                try {
                    JSONArray jsonEpisodes = response.getJSONArray(Episode.PLURAL_KEY);

                    for (int i=0; i < jsonEpisodes.length(); i++) {
                        Episode episode = Episode.buildFromJson(jsonEpisodes.getJSONObject(i));
                        mEpisodes.add(episode);
                    }

                    Collections.sort(mEpisodes);
                    Log.d(TAG, "Episodes: " + String.valueOf(jsonEpisodes.length()));

                    mAdapter = (new ArrayAdapter<Episode>(getActivity(), R.layout.list_item_episode, mEpisodes) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            Log.d(TAG, "getView called");

                            View v = convertView;

                            if (v == null) {
                                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                v = inflater.inflate(R.layout.list_item_program, null);
                            }
                            Episode episode = mEpisodes.get(position);
                            TextView title = (TextView) v.findViewById(android.R.id.text1);
                            title.setText(episode.getTitle());

                            return v;
                        }
                    });

                    mListView.setAdapter(mAdapter);

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
        View view = inflater.inflate(R.layout.fragment_episodes, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Loading episode.");
    }
}
