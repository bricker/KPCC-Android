package org.kpcc.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Episode;
import org.kpcc.api.Program;
import org.kpcc.api.Segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 */
public class EpisodesFragment extends Fragment implements AbsListView.OnItemClickListener {
    public final static String TAG = "EpisodesFragment";
    public final static String STACK_TAG = "episodesList";
    private static final String ARG_PROGRAM_SLUG = "program_slug";

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    private LinearLayout mProgressBar;
    private NetworkImageView mBackground;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    private Program mProgram;
    private ArrayList<Episode> mEpisodes = new ArrayList<>();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EpisodesFragment() {
    }

    public static EpisodesFragment newInstance(String programSlug) {
        EpisodesFragment fragment = new EpisodesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_SLUG, programSlug);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            String programSlug = getArguments().getString(ARG_PROGRAM_SLUG);
            mProgram = ProgramsManager.instance.find(programSlug);
        }

        HashMap<String, String> params = new HashMap<>();

        params.put("program", mProgram.slug);
        params.put("limit", "8");

        Episode.Client.getCollection(params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonEpisodes = response.getJSONArray(Episode.PLURAL_KEY);

                    for (int i = 0; i < jsonEpisodes.length(); i++) {
                        Episode episode = Episode.buildFromJson(jsonEpisodes.getJSONObject(i));

                        // Don't show the episode if there is no audio.
                        if (episode.audio != null) {
                            mEpisodes.add(episode);
                        } else {
                            // If there was no episode but there are segments, use those as episodes.
                            if (episode.segments.size() > 0) {
                                for (Segment segment : episode.segments) {
                                    if (segment.audio != null) {
                                        mEpisodes.add(Episode.buildFromSegment(segment));
                                    }
                                }
                            }
                        }
                    }

                    Collections.sort(mEpisodes);

                    mAdapter = (new ArrayAdapter<Episode>(getActivity(), R.layout.list_item_episode, mEpisodes) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = convertView;

                            if (view == null) {
                                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                view = inflater.inflate(R.layout.list_item_episode, null);
                            }

                            Episode episode = mEpisodes.get(position);
                            TextView title = (TextView) view.findViewById(R.id.episode_title);
                            TextView date = (TextView) view.findViewById(R.id.air_date);

                            title.setText(episode.title);
                            date.setText(episode.formattedAirDate);

                            return view;
                        }
                    });

                    setAdapter();

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(mProgram.title);

        View view = inflater.inflate(R.layout.fragment_episodes, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mProgressBar = (LinearLayout) view.findViewById(R.id.progress_layout);
        mBackground = (NetworkImageView) view.findViewById(R.id.background);
        NetworkImageManager.instance.setBackgroundImage(mBackground, mProgram.slug);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        setAdapter();

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Episode episode = mEpisodes.get(position);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, EpisodeFragment.newInstance(
                                mProgram.slug, episode.title, episode.formattedAirDate,
                                episode.audio.url, episode.audio.durationSeconds),
                        STACK_TAG)
                .addToBackStack(null)
                .commit();
    }

    public void setAdapter() {
        if (mAdapter != null) {
            mListView.setAdapter(mAdapter);
            mProgressBar.setVisibility(View.GONE);
        }
    }
}
