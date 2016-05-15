package org.kpcc.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.kpcc.api.Program;

public class StreamSelectFragment extends Fragment implements AdapterView.OnItemClickListener {
    public final static String STACK_TAG = "StreamSelectFragment";

    private ListAdapter mAdapter;
    private View mView;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.programs);

        mView = inflater.inflate(R.layout.fragment_programs, container, false);
        mListView = (ListView) mView.findViewById(R.id.list_programs);

        mAdapter = (new ArrayAdapter<Program>(getActivity(),
                R.layout.list_item_program, ProgramsManager.instance.ALL_PROGRAMS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                MainActivity activity = (MainActivity) getActivity();
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.list_item_program, null);
                }

                Program program = ProgramsManager.instance.ALL_PROGRAMS.get(position);
                TextView title = (TextView) view.findViewById(R.id.program_title);
                ImageView avatar = (ImageView) view.findViewById(R.id.program_avatar);
                ImageView arrow = (ImageView) view.findViewById(R.id.arrow);
                ImageView audio_icon = (ImageView) view.findViewById(R.id.audio_icon);
                TextView letter = (TextView) view.findViewById(R.id.program_letter);

                if (StreamManager.ConnectivityManager.getInstance().getStreamIsBound()) {
                    OnDemandPlayer currentPlayer = StreamManager.ConnectivityManager.getInstance().getStreamManager().getCurrentOnDemandPlayer();
                    if (currentPlayer != null && currentPlayer.getProgramSlug().equals(program.slug)) {
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
                        activity.getApplicationContext().getPackageName());

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

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Program program = ProgramsManager.instance.ALL_PROGRAMS.get(position);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.container,
                        EpisodesFragment.newInstance(program.slug),
                        EpisodesFragment.STACK_TAG)
                .addToBackStack(EpisodesFragment.STACK_TAG)
                .commit();
    }
}
