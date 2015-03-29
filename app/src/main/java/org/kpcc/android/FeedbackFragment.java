package org.kpcc.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class FeedbackFragment extends Fragment {
    private boolean mLock = false;
    private Button mSubmitButton;
    private RadioButton mFeedbackTypeBug;
    private RadioButton mFeedbackTypeSuggestion;
    private RadioButton mFeedbackTypeFeedback;
    private EditText mInputComments;
    private EditText mInputName;
    private EditText mInputEmail;
    private TextView mValidationMessage;
    private TextView mSuccessMessage;
    private TextView mErrorMessage;

    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Activity activity = getActivity();
        activity.setTitle(R.string.feedback);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        TextView appVersion = (TextView) view.findViewById(R.id.appVersion);
        appVersion.setText(BuildConfig.VERSION_NAME);

        mSubmitButton = (Button) view.findViewById(R.id.submitButton);
        mFeedbackTypeBug = (RadioButton) view.findViewById(R.id.feedbackTypeBug);
        mFeedbackTypeSuggestion = (RadioButton) view.findViewById(R.id.feedbackTypeSuggestion);
        mFeedbackTypeFeedback = (RadioButton) view.findViewById(R.id.feedbackTypeFeedback);
        mInputComments = (EditText) view.findViewById(R.id.inputComments);
        mInputName = (EditText) view.findViewById(R.id.inputName);
        mInputEmail = (EditText) view.findViewById(R.id.inputEmail);
        mValidationMessage = (TextView) view.findViewById(R.id.validationMessage);
        mSuccessMessage = (TextView) view.findViewById(R.id.successMessage);
        mErrorMessage = (TextView) view.findViewById(R.id.errorMessage);

        // Override the button and put it on the right.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFeedbackTypeBug.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.color.transparent, 0);
                mFeedbackTypeSuggestion.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.color.transparent, 0);
                mFeedbackTypeFeedback.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.color.transparent, 0);
                ((RadioButton) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
            }
        };

        mFeedbackTypeBug.setOnClickListener(listener);
        mFeedbackTypeSuggestion.setOnClickListener(listener);
        mFeedbackTypeFeedback.setOnClickListener(listener);

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
                mSuccessMessage.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);

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

                FeedbackManager.instance.sendFeedback(type, comments, name, email,
                        new FeedbackManager.FeedbackCallback() {
                            @Override
                            public void onSuccess() {
                                unlockForm();
                                mSubmitButton.setVisibility(View.GONE);
                                mSuccessMessage.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onFailure() {
                                unlockForm();
                                mErrorMessage.setVisibility(View.VISIBLE);
                            }
                        }
                );
            }
        });

        return view;
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
