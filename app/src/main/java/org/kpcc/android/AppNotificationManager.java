package org.kpcc.android;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.parse.GcmBroadcastReceiver;
import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;

class AppNotificationManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String CHANNEL_LISTEN_LIVE = "listenLive";
    private static AppNotificationManager instance = null;

    private final Application application;

    private AppNotificationManager(Application application) {
        this.application = application;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (prefs.getBoolean(SettingsFragment.PREF_KEY_PUSH_NOTIFICATIONS, true)) {
            subscribe(CHANNEL_LISTEN_LIVE);
        } else {
            disableReceivers();
        }
    }

    public static void setupInstance(Application application) {
        if (instance == null) {
            instance = new AppNotificationManager(application);
        }
    }

    void enableReceivers() {
        PackageManager pm = application.getPackageManager();

        ComponentName receiver1 = new ComponentName(application, BroadcastReceiver.class);
        pm.setComponentEnabledSetting(receiver1,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        ComponentName receiver2 = new ComponentName(application, GcmBroadcastReceiver.class);
        pm.setComponentEnabledSetting(receiver2,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    void disableReceivers() {
        PackageManager pm = application.getPackageManager();

        ComponentName receiver1 = new ComponentName(application, BroadcastReceiver.class);
        pm.setComponentEnabledSetting(receiver1,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        ComponentName receiver2 = new ComponentName(application, GcmBroadcastReceiver.class);
        pm.setComponentEnabledSetting(receiver2,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        switch (key) {
            case SettingsFragment.PREF_KEY_PUSH_NOTIFICATIONS:
                boolean pushEnabled = sharedPreferences.getBoolean(key, true);

                if (pushEnabled) {
                    subscribe(CHANNEL_LISTEN_LIVE);
                } else {
                    unsubscribe(CHANNEL_LISTEN_LIVE);
                }

                break;
        }
    }

    void subscribe(String channel) {
        ParsePush.subscribeInBackground(channel);
        enableReceivers();
    }

    void unsubscribe(String channel) {
        ParsePush.unsubscribeInBackground(channel);
        disableReceivers();
    }

    public static class BroadcastReceiver extends ParsePushBroadcastReceiver {

        @Override
        protected void onPushOpen(Context context, Intent intent) {
            // Parse automatically opens the MainActivity, which fortunately goes directly
            // to the live fragment so we don't have to do that ourselves. All we have to do is
            // tell the live stream to start playing right away.
            DataManager.instance.setPlayNow(true);
            super.onPushOpen(context, intent);
        }

    }
}
