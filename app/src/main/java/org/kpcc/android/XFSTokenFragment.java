package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class XFSTokenFragment extends Fragment {
    public static final String STACK_TAG = "XFSTokenFragment";
    private EditText mTokenInput;
    private Button mSubmitButton;

    public XFSTokenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_xfstoken, container, false);
        mSubmitButton = (Button) view.findViewById(R.id.tokenSubmitBtn);
        mTokenInput = (EditText) view.findViewById(R.id.tokenInputField);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: [bcr] Network connectivity handling
                v.setEnabled(false);
                String token = mTokenInput.getText().toString();

                // TODO: validate token with Parse
                if (true /*token.equals("valid")*/) {
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .replace(R.id.container,
                                    new XFSTokenSuccessFragment(),
                                    XFSTokenSuccessFragment.STACK_TAG)
                            .addToBackStack(STACK_TAG)
                            .commit();

                } else {
                    // TODO: Error
                }
            }
        });

        return view;
    }
}
