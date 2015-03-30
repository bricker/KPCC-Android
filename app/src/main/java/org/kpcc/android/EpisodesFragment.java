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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Episode;
import org.kpcc.api.Program;
import org.kpcc.api.Segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EpisodesFragment extends Fragment implements AbsListView.OnItemClickListener {
    public final static String STACK_TAG = "episodesList";
    private static final String ARG_PROGRAM_SLUG = "program_slug";
    private static final String PARAM_PROGRAM = "program";
    private static final String PARAM_LIMIT = "limit";
    private static final String EPISODE_LIMIT = "8";
    private final ArrayList<Episode> mEpisodes = new ArrayList<>();
    private AbsListView mListView;
    private LinearLayout mProgressBar;
    private ListAdapter mAdapter;
    private Program mProgram;
    // Unrecoverable error. Just show an error message if this is true.
    private boolean mDidError = false;

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
        setRetainInstance(true);

        // This must be getArguments(), not savedInstanceState
        Bundle args = getArguments();
        if (args != null) {
            String programSlug = args.getString(ARG_PROGRAM_SLUG);
            mProgram = ProgramsManager.instance.find(programSlug);
        }

        HashMap<String, String> params = new HashMap<>();

        params.put(PARAM_PROGRAM, mProgram.slug);
        params.put(PARAM_LIMIT, EPISODE_LIMIT);

        Episode.Client.getCollection(params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonEpisodes = response.getJSONArray(Episode.PLURAL_KEY);

                    for (int i = 0; i < jsonEpisodes.length(); i++) {
                        try {
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
                        } catch (JSONException e) {
                            // implicit continue
                            // This episode will not show.
                        }
                    }
                } catch (JSONException e) {
                    // The list will be empty. This will happen if the API returns malformed JSON.
                    // I don't expect that to happen so it's not worth the time to write a proper
                    // UI for a failure here.
                }

                Collections.sort(mEpisodes);

                mAdapter = (new ArrayAdapter<Episode>(getActivity(), R.layout.list_item_episode, mEpisodes) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        MainActivity activity = (MainActivity) getActivity();
                        View view = convertView;

                        if (view == null) {
                            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            view = inflater.inflate(R.layout.list_item_episode, null);
                        }

                        Episode episode = mEpisodes.get(position);
                        TextView title = (TextView) view.findViewById(R.id.episode_title);
                        TextView date = (TextView) view.findViewById(R.id.air_date);
                        ImageView audio_icon = (ImageView) view.findViewById(R.id.audio_icon);

                        title.setText(episode.title);
                        date.setText(episode.formattedAirDate);

                        if (activity.streamIsBound()) {
                            StreamManager.EpisodeStream currentPlayer = activity.streamManager.currentEpisodePlayer;
                            if (currentPlayer != null && currentPlayer.audioUrl.equals(episode.audio.url)) {
                                audio_icon.setVisibility(View.VISIBLE);
                            } else {
                                audio_icon.setVisibility(View.GONE);
                            }
                        }

                        return view;
                    }
                });

                setAdapter();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // This boolean will be checked in onCreateView() and handled.
                mDidError = true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(mProgram.title);

        View view = inflater.inflate(R.layout.fragment_episodes, container, false);

        ImageView background = (ImageView) view.findViewById(R.id.background);
        NetworkImageManager.instance.setBitmap(background, mProgram.slug, getActivity());

        mProgressBar = (LinearLayout) view.findViewById(R.id.progress_layout);

        if (mDidError) {
            LinearLayout errorView = (LinearLayout) view.findViewById(R.id.generic_load_error);
            mProgressBar.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
        } else {
            // Set the adapter
            mListView = (AbsListView) view.findViewById(android.R.id.list);

            // Set OnItemClickListener so we can be notified on item clicks
            mListView.setOnItemClickListener(this);

            // This should be called here in case onCreate() was bypassed.
            setAdapter();
        }

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getActivity().setTitle(R.string.programs);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container,
                        EpisodesPagerFragment.newInstance(mEpisodes, position, mProgram.slug),
                        EpisodesPagerFragment.STACK_TAG)
                .addToBackStack(STACK_TAG)
                .commit();
    }

    void setAdapter() {
        if (mAdapter != null) {
            mListView.setAdapter(mAdapter);
            mProgressBar.setVisibility(View.GONE);
        }
    }
}
