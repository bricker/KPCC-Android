package org.kpcc.android;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class WakeFragment extends Fragment {
    public final static String STACK_TAG = "WakeFragment";

    public WakeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wake, container, false);
        Button button = (Button)view.findViewById(R.id.set_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment fragment = new TimePickerFragment();
                fragment.show(getChildFragmentManager(), TimePickerFragment.STACK_TAG);
            }
        });

        return view;
    }


}
