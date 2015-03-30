package org.kpcc.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class SettingsFragment extends PreferenceFragment {
    public static final String PREF_KEY_PUSH_NOTIFICATIONS = "push_notifications";

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
