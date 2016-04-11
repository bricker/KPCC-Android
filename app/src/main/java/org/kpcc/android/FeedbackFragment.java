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
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

public class FeedbackFragment extends Fragment {
    public final static String STACK_TAG = "FeedbackFragment";

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
    private ProgressBar mProgressBar;
    private FeedbackManager.Type mCurrentType = FeedbackManager.Type.BUG;
    private String mCurrentName = "";
    private String mCurrentEmail = "";
    private String mCurrentComments = "";
    private boolean mDidSucceed = false;
    private boolean mDidFail = false;
    private boolean mDidFailValidation = false;
    private boolean mIsLoading = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Activity activity = getActivity();
        activity.setTitle(R.string.feedback);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        TextView appVersion = (TextView) view.findViewById(R.id.appVersion);
        String appName = getString(R.string.app_name);
        appVersion.setText(appName + " " + BuildConfig.VERSION_NAME);

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
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_circular);

        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(FeedbackFragment.STACK_TAG,
                new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                if (mErrorMessage == null) {
                    return;
                }

                setButtonState();
                mErrorMessage.setVisibility(View.GONE);
            }

            @Override
            public void onDisconnect() {
                if (mErrorMessage == null) {
                    return;
                }

                disableButton();
                mErrorMessage.setText(R.string.network_error);
                mErrorMessage.setVisibility(View.VISIBLE);
            }
        }, true);

        mFeedbackTypeBug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideCheck(mFeedbackTypeSuggestion);
                hideCheck(mFeedbackTypeFeedback);
                showCheck(mFeedbackTypeBug);
                mCurrentType = FeedbackManager.Type.BUG;
            }
        });

        mFeedbackTypeSuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCheck(mFeedbackTypeSuggestion);
                hideCheck(mFeedbackTypeFeedback);
                hideCheck(mFeedbackTypeBug);
                mCurrentType = FeedbackManager.Type.SUGGESTION;
            }
        });

        mFeedbackTypeFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideCheck(mFeedbackTypeSuggestion);
                showCheck(mFeedbackTypeFeedback);
                hideCheck(mFeedbackTypeBug);
                mCurrentType = FeedbackManager.Type.FEEDBACK;
            }
        });

        mInputComments.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mCurrentComments = getInputVal(mInputComments);
                setButtonState();
            }
        });

        mInputName.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mCurrentName = getInputVal(mInputName);
                setButtonState();
            }
        });

        mInputEmail.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mCurrentEmail = getInputVal(mInputEmail);
                setButtonState();
            }
        });

        // Restore states
        if (mCurrentType == FeedbackManager.Type.BUG) {
            mFeedbackTypeBug.callOnClick();
        } else if (mCurrentType == FeedbackManager.Type.SUGGESTION) {
            mFeedbackTypeSuggestion.callOnClick();
        } else if (mCurrentType == FeedbackManager.Type.FEEDBACK) {
            mFeedbackTypeFeedback.callOnClick();
        }

        mInputName.setText(mCurrentName);
        mInputEmail.setText(mCurrentEmail);
        mInputComments.setText(mCurrentComments);

        if (mIsLoading) {
            showLoading();
        } else if (mDidSucceed) {
            showSuccessMessage();
        } else if (mDidFail) {
            showErrorMessage();
        } else if (mDidFailValidation) {
            showValidationMessage();
        }

        setButtonState();

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLock) {
                    return;
                }
                mLock = true;

                mDidFailValidation = false;
                mDidSucceed = false;
                mDidFail = false;

                mValidationMessage.setVisibility(View.GONE);
                mSuccessMessage.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);

                if (!validateInput()) {
                    mDidFailValidation = true;
                    showValidationMessage();
                    mLock = false;
                    return;
                }

                mIsLoading = true;
                showLoading();

                FeedbackManager.getInstance().sendFeedback(mCurrentType, mCurrentComments, mCurrentName, mCurrentEmail,
                        new FeedbackManager.FeedbackCallback() {
                            @Override
                            public void onSuccess() {
                                mLock = false;
                                mDidSucceed = true;
                                mIsLoading = false;
                                showSuccessMessage();
                            }

                            @Override
                            public void onFailure() {
                                mLock = false;
                                mDidFail = true;
                                mIsLoading = false;
                                showErrorMessage();
                            }
                        }
                );
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Cleanup our reference.
        AppConnectivityManager.getInstance().removeOnNetworkConnectivityListener(FeedbackFragment.STACK_TAG);
    }

    private void setButtonState() {
        if (mSubmitButton == null) {
            return;
        }

        if (validateInput()) {
            enableButton();
        } else {
            disableButton();
        }
    }

    private void enableButton() {
        if (AppConnectivityManager.getInstance().isConnectedToNetwork()) {
            mSubmitButton.setEnabled(true);
            mSubmitButton.setClickable(true);
            mSubmitButton.setAlpha(1.0f);
        }
    }

    private void disableButton() {
        mSubmitButton.setEnabled(false);
        mSubmitButton.setClickable(false);
        mSubmitButton.setAlpha(0.4f);
    }

    private boolean validateInput() {
        // Better safe than crashy.
        //noinspection SimplifiableIfStatement
        if (mCurrentName == null || mCurrentComments == null || mCurrentEmail == null) {
            return false;
        }

        return !mCurrentName.isEmpty() && !mCurrentEmail.isEmpty() && !mCurrentComments.isEmpty();
    }

    private void showCheck(RadioButton view) {
        view.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
    }

    private void hideCheck(RadioButton view) {
        view.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.color.transparent, 0);
    }

    private void showSuccessMessage() {
        mProgressBar.setVisibility(View.GONE);
        mSuccessMessage.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        mProgressBar.setVisibility(View.GONE);
        mSubmitButton.setVisibility(View.VISIBLE);
        mErrorMessage.setText(R.string.feedback_error_message);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    private void showValidationMessage() {
        mValidationMessage.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        mSubmitButton.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private String getInputVal(EditText view) {
        return view.getText().toString();
    }

    private class BaseTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
