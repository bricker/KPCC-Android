package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class FeedbackFragment extends Fragment {
    public static final String TAG = "kpcc.FeedbackFragment";

    private boolean mLock = false;
    private Button mSubmitButton;
    private RadioButton mFeedbackTypeBug;
    private RadioButton mFeedbackTypeSuggestion;
    private RadioButton mFeedbackTypeFeedback;
    private EditText mInputComments;
    private EditText mInputName;
    private EditText mInputEmail;
    private TextView mValidationMessage;

    public FeedbackFragment() {
        // Required empty public constructor
    }

    public static FeedbackFragment newInstance() {
        FeedbackFragment fragment = new FeedbackFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_feedback, container, false);

        TextView appVersion = (TextView) v.findViewById(R.id.appVersion);
        appVersion.setText(BuildConfig.VERSION_NAME);

        mSubmitButton = (Button) v.findViewById(R.id.submitButton);
        mFeedbackTypeBug = (RadioButton) v.findViewById(R.id.feedbackTypeBug);
        mFeedbackTypeSuggestion = (RadioButton) v.findViewById(R.id.feedbackTypeSuggestion);
        mFeedbackTypeFeedback = (RadioButton) v.findViewById(R.id.feedbackTypeFeedback);
        mInputComments = (EditText) v.findViewById(R.id.inputComments);
        mInputName = (EditText) v.findViewById(R.id.inputName);
        mInputEmail = (EditText) v.findViewById(R.id.inputEmail);
        mValidationMessage = (TextView) v.findViewById(R.id.validationMessage);

        TextWatcher validator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                FeedbackFragment.this.setButtonState();
            }
        };

        mInputComments.addTextChangedListener(validator);
        mInputName.addTextChangedListener(validator);
        mInputEmail.addTextChangedListener(validator);
        setButtonState();

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLock) {
                    return;
                }
                lockForm();

                mValidationMessage.setVisibility(View.GONE);

                String type = FeedbackManager.TYPE_FEEDBACK; // Default
                if (mFeedbackTypeBug.isChecked()) {
                    type = FeedbackManager.TYPE_BUG;
                }
                if (mFeedbackTypeFeedback.isChecked()) {
                    type = FeedbackManager.TYPE_FEEDBACK;
                }
                if (mFeedbackTypeSuggestion.isChecked()) {
                    type = FeedbackManager.TYPE_SUGGESTION;
                }

                String name = mInputName.getText().toString();
                String email = mInputEmail.getText().toString();
                String comments = mInputComments.getText().toString();

                if (!validateInput()) {
                    mValidationMessage.setVisibility(View.VISIBLE);
                    unlockForm();
                    return;
                }

                FeedbackManager.getInstance().sendFeedback(type, comments, name, email,
                        new FeedbackManager.FeedbackCallback() {
                            @Override
                            public void onSuccess() {
                                unlockForm();
                                // TODO: Display success message.
                                Log.d(TAG, "Success");
                            }

                            @Override
                            public void onFailure() {
                                unlockForm();
                                // TODO: Handle failures
                                Log.d(TAG, "Failure");
                            }
                        }
                );
            }
        });

        return v;
    }

    private void lockForm() {
        mLock = true;

        if (mSubmitButton != null) {
            mSubmitButton.setEnabled(false);
        }
    }

    private void unlockForm() {
        mLock = false;
        if (mSubmitButton != null) {
            mSubmitButton.setEnabled(true);
        }
    }

    private void setButtonState() {
        if (mSubmitButton == null) {
            return;
        }

        if (validateInput()) {
            mSubmitButton.setEnabled(true);
        } else {
            mSubmitButton.setEnabled(false);
        }
    }

    private boolean validateInput() {
        // Better safe than crashy.
        if (mInputName == null || mInputEmail == null || mInputComments == null) {
            return false;
        }

        String name = mInputName.getText().toString();
        String email = mInputEmail.getText().toString();
        String comments = mInputComments.getText().toString();

        return !name.isEmpty() && !email.isEmpty() && !comments.isEmpty();
    }
}
