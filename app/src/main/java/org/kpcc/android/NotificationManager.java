package org.kpcc.android;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;

/**
 * Created by rickb014 on 2/15/15.
 */
public class NotificationManager
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static NotificationManager INSTANCE = null;
    public static final String TAG = "NotificationManager";
    public static final String CHANNEL_BREAKING_NEWS = "breakingNews";

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    public static void setupInstance(Application application) {
        if (INSTANCE == null) {
            INSTANCE = new NotificationManager(application);
        }
    }


    protected NotificationManager(Application application) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (prefs.getBoolean(SettingsFragment.PREF_KEY_PUSH_NOTIFICATIONS, true)) {
            subscribe(CHANNEL_BREAKING_NEWS);
        }
    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        switch (key) {
            case SettingsFragment.PREF_KEY_PUSH_NOTIFICATIONS:
                boolean pushEnabled = sharedPreferences.getBoolean(key, true);

                if (pushEnabled) {
                    subscribe(CHANNEL_BREAKING_NEWS);
                } else {
                    unsubscribe(CHANNEL_BREAKING_NEWS);
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
            // Determine which type of push notification this was and
            // do something about it.
            super.onPushOpen(context, intent);
            Toast toast = Toast.makeText(context, "Push Opened!", Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        protected void onPushDismiss(Context context, Intent intent) {
            super.onPushDismiss(context, intent);
        }
    }
}
