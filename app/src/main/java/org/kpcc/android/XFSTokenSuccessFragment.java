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
public class XFSTokenSuccessFragment extends Fragment {
    public static String STACK_TAG = "XFSTokenSuccessFragment";
    private Button mCloseButton;

    public XFSTokenSuccessFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_xfstoken_success, container, false);
        mCloseButton = (Button) view.findViewById(R.id.tokenSuccessCloseBtn);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityIntent = new Intent(getActivity(), MainActivity.class);
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                DataManager.getInstance().setPlayNow(true);
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
