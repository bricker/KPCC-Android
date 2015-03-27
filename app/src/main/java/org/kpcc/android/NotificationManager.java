package org.kpcc.android;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;

public class NotificationManager
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "NotificationManager";
    public static final String CHANNEL_LISTEN_LIVE = "sandbox_listenLive";
    private static NotificationManager instance = null;

    protected NotificationManager(Application application) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (prefs.getBoolean(SettingsFragment.PREF_KEY_PUSH_NOTIFICATIONS, true)) {
            subscribe(CHANNEL_LISTEN_LIVE);
        }
    }

    public static void setupInstance(Application application) {
        if (instance == null) {
            instance = new NotificationManager(application);
        }
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

    public void subscribe(String channel) {
        ParsePush.subscribeInBackground(channel);
    }

    public void unsubscribe(String channel) {
        ParsePush.unsubscribeInBackground(channel);
    }

    public static class BroadcastReceiver extends ParsePushBroadcastReceiver {
        @Override
        protected void onPushReceive(Context context, Intent intent) {
            super.onPushReceive(context, intent);
        }

        @Override
        protected void onPushOpen(Context context, Intent intent) {
            super.onPushOpen(context, intent);
        }

        @Override
        protected void onPushDismiss(Context context, Intent intent) {
            super.onPushDismiss(context, intent);
        }
    }
}
