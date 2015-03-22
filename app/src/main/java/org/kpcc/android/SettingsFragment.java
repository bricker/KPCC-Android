package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragment {
    public static final String PREF_KEY_PUSH_NOTIFICATIONS = "push_notifications";

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        Activity activity = getActivity();
        activity.setTitle(R.string.settings);

        return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
    }
}
