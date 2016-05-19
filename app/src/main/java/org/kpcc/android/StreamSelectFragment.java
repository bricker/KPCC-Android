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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class StreamSelectFragment extends Fragment implements AdapterView.OnItemClickListener {
    public final static String STACK_TAG = "StreamSelectFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_stream_select, container, false);
        ListView listView = (ListView) view.findViewById(R.id.list_streams);

        ListAdapter adapter = (new ArrayAdapter<LivePlayer.StreamOption>(getActivity(),
                R.layout.list_item_stream, LivePlayer.STREAM_OPTIONS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                MainActivity activity = (MainActivity) getActivity();
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.list_item_stream, null);
                }

                LivePlayer.StreamOption stream = LivePlayer.STREAM_OPTIONS[position];
                TextView title = (TextView) view.findViewById(R.id.stream_title);
                title.setText(stream.getId());

                String streamPref = DataManager.getInstance().getStreamPreference();
                toggleStreamPreferenceUI(streamPref, stream, view);

                return view;
            }
        });

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LivePlayer.StreamOption stream = LivePlayer.STREAM_OPTIONS[position];
        String streamPref = DataManager.getInstance().getStreamPreference();

        if (stream.equals(LivePlayer.STREAM_XFS) && !DataManager.getInstance().getIsXfsValidated()) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.container,
                            new XFSTokenFragment(),
                            XFSTokenFragment.STACK_TAG)
                    .addToBackStack(StreamSelectFragment.STACK_TAG)
                    .commit();
        } else {
            Intent activityIntent = new Intent(getActivity(), MainActivity.class);
            if (!stream.getKey().equals(streamPref)) {
                // Release the currently playing stream, so we can restart it with the new URL.
                LivePlayer livePlayer = StreamManager.ConnectivityManager.getInstance().getStreamManager().getCurrentLivePlayer();
                if (livePlayer != null) {
                    StreamManager.ConnectivityManager.getInstance().getStreamManager().getCurrentLivePlayer().release();
                }
            }

            DataManager.getInstance().setStreamPreference(stream.getKey());
            DataManager.getInstance().setPlayNow(true);
            startActivity(activityIntent);
        }
    }

    private void toggleStreamPreferenceUI(String streamPref, LivePlayer.StreamOption stream, View view) {
        ImageView icon = (ImageView) view.findViewById(R.id.live_icon);

        if (streamPref.equals(stream.getKey())) {
            icon.setVisibility(View.VISIBLE);
            view.setAlpha(1.0f);
        } else {
            icon.setVisibility(View.GONE);
            view.setAlpha(0.4f);
        }
    }
}
