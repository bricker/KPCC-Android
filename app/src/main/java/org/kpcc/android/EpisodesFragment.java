package org.kpcc.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

import com.android.volley.Request;
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

public class EpisodesFragment extends StreamBindFragment
        implements AdapterView.OnItemClickListener {

    public final static String STACK_TAG = "EpisodesFragment";
    private static final String ARG_PROGRAM_SLUG = "program_slug";
    private static final String PARAM_PROGRAM = "program";
    private static final String PARAM_LIMIT = "limit";
    private static final String EPISODE_LIMIT = "8";
    private final ArrayList<Episode> mEpisodes = new ArrayList<>();
    private AbsListView mListView;
    private LinearLayout mProgressBar;
    private ListAdapter mAdapter;
    private Program mProgram;
    private Request mRequest;
    private View mView;
    private LinearLayout mErrorView;
    private TextView mErrorText;
    private int mErrorMessage;
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

        mDidError = false;

        // This must be getArguments(), not savedInstanceState
        Bundle args = getArguments();
        if (args != null) {
            String programSlug = args.getString(ARG_PROGRAM_SLUG);
            mProgram = ProgramsManager.instance.find(programSlug);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_episodes, container, false);
        getActivity().setTitle(mProgram.getTitle());
        ImageView background = (ImageView) mView.findViewById(R.id.background);
        NetworkImageManager.getInstance().setBitmap(getActivity(), background, mProgram.getSlug());
        mListView = (AbsListView) mView.findViewById(android.R.id.list);
        mProgressBar = (LinearLayout) mView.findViewById(R.id.progress_layout);
        mErrorView = (LinearLayout)mView.findViewById(R.id.generic_load_error);
        mErrorText = (TextView)mErrorView.findViewById(R.id.error_text);

        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(getActivity(), EpisodesFragment.STACK_TAG, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect(Context context) {
                if (mProgressBar != null) {
                    mErrorView.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);
                }

                loadEpisodes();
            }

            @Override
            public void onDisconnect(Context context) {
                showError(R.string.network_error);
            }
        }, true);

        if (mDidError) {
            showError(mErrorMessage);
            return mView;
        }

        // In case onCreate was not invoked.
        setAdapter();
        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRequest != null) { mRequest.cancel(); }
        AppConnectivityManager.getInstance().removeOnNetworkConnectivityListener(EpisodesFragment.STACK_TAG);
    }

    private void loadEpisodes() {
        if (!mEpisodes.isEmpty()) {
            setupAdapter();
            return;
        }

        HashMap<String, String> params = new HashMap<>();

        params.put(PARAM_PROGRAM, mProgram.getSlug());
        params.put(PARAM_LIMIT, EPISODE_LIMIT);

        mRequest = Episode.Client.getCollection(params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonEpisodes = response.getJSONArray(Episode.PLURAL_KEY);

                    for (int i = 0; i < jsonEpisodes.length(); i++) {
                        try {
                            Episode episode = Episode.buildFromJson(jsonEpisodes.getJSONObject(i));

                            // Don't show the episode if there is no audio.
                            if (episode.getAudio() != null) {
                                mEpisodes.add(episode);
                            } else {
                                // If there was no episode but there are segments, use those as episodes.
                                if (episode.getSegments().size() > 0) {
                                    for (Segment segment : episode.getSegments()) {
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
                    // This will happen if the API returns malformed JSON.
                    showError(R.string.load_error);
                }

                Collections.sort(mEpisodes);
                setupAdapter();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showError(R.string.load_error);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getActivity().setTitle(R.string.programs);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.container,
                        EpisodesPagerFragment.newInstance(mEpisodes, position, mProgram.getSlug()),
                        EpisodesPagerFragment.STACK_TAG)
                .addToBackStack(EpisodesPagerFragment.STACK_TAG)
                .commit();
    }

    private void showError(int stringId) {
        mDidError = true;
        mErrorMessage = stringId;

        if (mView == null) {
            return;
        }

        mListView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mErrorText.setText(stringId);
        mErrorView.setVisibility(View.VISIBLE);
    }

    private void setupAdapter() {
        mAdapter = (new ArrayAdapter<Episode>(getActivity(), R.layout.list_item_episode, mEpisodes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.list_item_episode, null);
                }

                final Episode episode = mEpisodes.get(position);
                TextView title = (TextView) convertView.findViewById(R.id.episode_title);
                TextView date = (TextView) convertView.findViewById(R.id.air_date);
                final ImageView audio_icon = (ImageView) convertView.findViewById(R.id.audio_icon);

                title.setText(episode.getTitle());
                date.setText(episode.getFormattedAirDate());

                OnDemandPlayer stream = getOnDemandPlayer();
                if (stream != null) {
                    if (stream.getAudioUrl().equals(episode.getAudio().getUrl())) {
                        audio_icon.setVisibility(View.VISIBLE);
                    } else {
                        audio_icon.setVisibility(View.GONE);
                    }
                }

                return convertView;
            }
        });

        setAdapter();
    }

    private void setAdapter() {
        if (mView == null || mAdapter == null){
            // View hasn't been initialized.
            // This method will be tried again in onCreateView.
            return;
        }

        // Set the adapter
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mProgressBar.setVisibility(View.GONE);
        mErrorView.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
    }
}
