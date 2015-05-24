package org.kpcc.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Program;

public class ProgramsFragment extends Fragment
        implements AdapterView.OnItemClickListener, ProgramsManager.OnProgramsResponseListener {
    public final static String STACK_TAG = "ProgramsFragment";

    private ListAdapter mAdapter;
    private boolean mDidError = false;
    private View mView;
    private ListView mListView;
    private LinearLayout mProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        ProgramsManager.instance.loadPrograms(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.programs);

        mView = inflater.inflate(R.layout.fragment_programs, container, false);
        mListView = (ListView) mView.findViewById(R.id.list_programs);
        mProgressBar = (LinearLayout) mView.findViewById(R.id.progress_layout);

        if (mDidError) {
            showError();
            return mView;
        }

        setAdapter();

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return mView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Program program = ProgramsManager.instance.ALL_PROGRAMS.get(position);

        JSONObject params = new JSONObject();

        try {
            params.put(AnalyticsManager.PARAM_PROGRAM_TITLE, program.title);
        } catch (JSONException e) {
            // No params
        }

        AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_PROGRAM_SELECTED, params);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container,
                        EpisodesFragment.newInstance(program.slug),
                        EpisodesFragment.STACK_TAG)
                .addToBackStack(EpisodesFragment.STACK_TAG)
                .commit();
    }

    @Override
    public void onProgramsResponse() {
        mAdapter = (new ArrayAdapter<Program>(getActivity(),
                R.layout.list_item_program, ProgramsManager.instance.ALL_PROGRAMS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                MainActivity activity = (MainActivity) getActivity();
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.list_item_program, null);
                }

                Program program = ProgramsManager.instance.ALL_PROGRAMS.get(position);
                TextView title = (TextView) view.findViewById(R.id.program_title);
                ImageView avatar = (ImageView) view.findViewById(R.id.program_avatar);
                ImageView arrow = (ImageView) view.findViewById(R.id.arrow);
                ImageView audio_icon = (ImageView) view.findViewById(R.id.audio_icon);
                TextView letter = (TextView) view.findViewById(R.id.program_letter);

                if (activity.streamIsBound) {
                    StreamManager.EpisodeStream currentPlayer = activity.streamManager.currentEpisodePlayer;
                    if (currentPlayer != null && currentPlayer.programSlug.equals(program.slug)) {
                        arrow.setVisibility(View.GONE);
                        audio_icon.setVisibility(View.VISIBLE);
                    } else {
                        arrow.setVisibility(View.VISIBLE);
                        audio_icon.setVisibility(View.GONE);
                    }
                }

                title.setText(program.title);

                String underscoreSlug = program.slug.replace("-", "_");
                int resId = getResources().getIdentifier(
                        "program_avatar_" + underscoreSlug,
                        "drawable",
                        getActivity().getApplicationContext().getPackageName());

                if (resId == 0) {
                    avatar.setImageResource(R.drawable.avatar_placeholder_bg);
                    letter.setText(String.valueOf(program.normalizedTitle.charAt(0)));
                    letter.setVisibility(View.VISIBLE);
                } else {
                    letter.setVisibility(View.GONE);
                    avatar.setImageResource(resId);
                }
                return view;
            }
        });

        setAdapter();
    }

    @Override
    public void onProgramsError(VolleyError error) {
        showError();
    }

    private void showError() {
        mDidError = true;

        if (mView != null) {
            LinearLayout errorView = (LinearLayout) mView.findViewById(R.id.generic_load_error);
            mListView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
        }
    }

    private void setAdapter() {
        if (mView == null || mAdapter == null){
            // View hasn't been initialized.
            // This method will be tried again in onCreateView.
            return;
        }

        // Set the adapter
        mListView.setAdapter(mAdapter);
        mProgressBar.setVisibility(View.GONE);
    }
}
