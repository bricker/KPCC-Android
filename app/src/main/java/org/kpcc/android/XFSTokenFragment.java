package org.kpcc.android;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class XFSTokenFragment extends Fragment {
    public static final String STACK_TAG = "XFSTokenFragment";
    private EditText mTokenInput;
    private Button mSubmitButton;
    private View mSpinner;

    public XFSTokenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_xfstoken, container, false);
        mSubmitButton = (Button) view.findViewById(R.id.tokenSubmitBtn);
        mTokenInput = (EditText) view.findViewById(R.id.tokenInputField);
        mSpinner = view.findViewById(R.id.spinner);

        mTokenInput.setText("57afdkQS2O5");
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                disableButton();

                // TODO: [bcr] Network connectivity handling
                final String token = mTokenInput.getText().toString();

                ParseQuery<ParseObject> query = ParseQuery.getQuery("PfsUser");
                query.whereEqualTo("pledgeToken", token);

                query.findInBackground(new FindCallback<ParseObject>() {
                    public void done(List<ParseObject> userList, ParseException e) {
                        if (e != null || userList.isEmpty()) {
                            AlertDialog.Builder tokenBuilder = new AlertDialog.Builder(getActivity());
                            tokenBuilder.setMessage(R.string.kpccplus_failure_dialog_message);

                            tokenBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });

                            tokenBuilder.setNegativeButton(R.string.contact_us, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String email = AppConfiguration.getInstance().getConfig("membershipEmail");
                                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                                    emailIntent.setData(Uri.parse("mailto:"));
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.kpccplus_failure_email_subject));

                                    String body = String.format(Locale.ENGLISH, getString(R.string.kpccplus_failure_email_body), token);
                                    emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                                    startActivity(emailIntent);
                                }
                            });

                            tokenBuilder.show();
                            enableButton();
                        } else {
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .replace(R.id.container,
                                            XFSTokenSuccessFragment.newInstance(),
                                            XFSTokenSuccessFragment.STACK_TAG)
                                    .addToBackStack(XFSTokenFragment.STACK_TAG)
                                    .commit();
                        }
                    }
                });
            }
        });

        return view;
    }

    private void disableButton() {
        mSubmitButton.setEnabled(false);
        mSubmitButton.setClickable(false);
        mSubmitButton.setVisibility(View.GONE);
        mSpinner.setVisibility(View.VISIBLE);
    }

    private void enableButton() {
        mSpinner.setVisibility(View.GONE);
        mSubmitButton.setEnabled(true);
        mSubmitButton.setClickable(true);
        mSubmitButton.setVisibility(View.VISIBLE);
    }
}
