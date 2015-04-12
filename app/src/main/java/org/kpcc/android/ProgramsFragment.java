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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Program;

public class ProgramsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static String STACK_TAG = "programsList";

    private ListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.programs);

        View view = inflater.inflate(R.layout.fragment_programs, container, false);

        // Set the adapter
        ListView listView = (ListView) view.findViewById(R.id.content_wrapper);
        listView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        listView.setOnItemClickListener(this);

        return view;
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
                .addToBackStack(STACK_TAG)
                .commit();
    }
}
