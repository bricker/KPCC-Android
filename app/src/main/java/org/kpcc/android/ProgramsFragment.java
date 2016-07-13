package org.kpcc.android;

import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;

import org.kpcc.api.Program;

public class ProgramsFragment extends StreamBindFragment
        implements AdapterView.OnItemClickListener, ProgramsManager.OnProgramsResponseListener {
    public final static String STACK_TAG = "ProgramsFragment";

    private ListAdapter mAdapter;
    private boolean mDidError = false;
    private View mView;
    private ListView mListView;
    private LinearLayout mProgressBar;
    private LinearLayout mErrorView;
    private TextView mErrorText;
    private Request mRequest;
    private int mErrorMessage = R.string.load_error;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(getActivity(), ProgramsFragment.STACK_TAG, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect(Context context) {
                if (mProgressBar != null) {
                    mErrorView.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);
                }

                mRequest = ProgramsManager.instance.loadPrograms(ProgramsFragment.this);
            }

            @Override
            public void onDisconnect(Context context) {
                showError(R.string.network_error);
            }
        }, true);

        getActivity().bindService(new Intent(getActivity(), StreamService.class), mStreamConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.programs);

        mView = inflater.inflate(R.layout.fragment_programs, container, false);
        mListView = (ListView) mView.findViewById(R.id.list_programs);
        mProgressBar = (LinearLayout) mView.findViewById(R.id.progress_layout);
        mErrorView = (LinearLayout)mView.findViewById(R.id.generic_load_error);
        mErrorText = (TextView)mErrorView.findViewById(R.id.error_text);

        if (mDidError) {
            showError(mErrorMessage);
            return mView;
        }

        setAdapter();
        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mRequest != null) {
            mRequest.cancel();
        }

        AppConnectivityManager.getInstance().removeOnNetworkConnectivityListener(ProgramsFragment.STACK_TAG);
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

    @Override
    public void onProgramsResponse() {
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

                OnDemandPlayer stream = getOnDemandPlayer();
                if (stream != null && stream.getProgramSlug().equals(program.slug)) {
                    arrow.setVisibility(View.GONE);
                    audio_icon.setVisibility(View.VISIBLE);
                } else {
                    arrow.setVisibility(View.VISIBLE);
                    audio_icon.setVisibility(View.GONE);
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

        setAdapter();
    }

    @Override
    public void onProgramsError() {
        showError(R.string.load_error);
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
