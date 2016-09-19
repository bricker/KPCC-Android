package org.kpcc.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class XFSTokenSuccessFragment extends StreamBindFragment {
    public static String STACK_TAG = "XFSTokenSuccessFragment";
    public static XFSTokenSuccessFragment newInstance() {
        return new XFSTokenSuccessFragment();
    }

    public XFSTokenSuccessFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_xfstoken_success, container, false);
        Button closeButton = (Button) view.findViewById(R.id.tokenSuccessCloseBtn);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityIntent = new Intent(getActivity(), MainActivity.class);
                DataManager.getInstance().setStreamPreference(LivePlayer.STREAM_XFS.getKey());
                DataManager.getInstance().setIsXfsValidated(true);
                DataManager.getInstance().setPlayNow(true);

                // Release the currently playing stream, so we can restart it with the new URL.
                LivePlayer stream = getLivePlayer();
                if (stream != null) {
                    stream.release();
                }

                startActivity(activityIntent);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
