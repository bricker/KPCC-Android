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
import android.widget.ListAdapter;
import android.widget.TextView;

import org.kpcc.api.Program;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 */
public class ProgramsFragment extends Fragment
        implements AdapterView.OnItemClickListener {

    public final static String TAG = "ProgramsFragment";

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProgramsFragment() {
    }

    public static ProgramsFragment newInstance() {
        ProgramsFragment fragment = new ProgramsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = (new ArrayAdapter<Program>(getActivity(),
                R.layout.list_item_program, ProgramsManager.ALL_PROGRAMS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.list_item_program, null);
                }

                Program program = ProgramsManager.ALL_PROGRAMS.get(position);
                TextView title = (TextView) view.findViewById(R.id.program_title);
                ImageView avatar = (ImageView) view.findViewById(R.id.program_avatar);
                ImageView arrow = (ImageView) view.findViewById(R.id.arrow);

                title.setText(program.getTitle());

                String underscoreSlug = program.getSlug().replace("-", "_");
                int resId = getResources().getIdentifier(
                        "program_avatar_" + underscoreSlug,
                        "drawable",
                        getActivity().getApplicationContext().getPackageName());

                if (resId == 0) {
                    // TODO: Build placeholder avatar.
                } else {
                    avatar.setImageResource(resId);
                }
                return view;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Activity activity = getActivity();
        activity.setTitle(R.string.programs);

        View view = inflater.inflate(R.layout.fragment_programs, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(R.id.content_wrapper);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Program program = ProgramsManager.ALL_PROGRAMS.get(position);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, EpisodesFragment.newInstance(program.getSlug()))
                .addToBackStack(null)
                .commit();
    }
}
