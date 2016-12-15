package org.kpcc.android;

import android.app.Application;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

class AppNotificationManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String CHANNEL_LISTEN_LIVE = AppConfiguration.getInstance().getConfig("channel.listenLive");
    private static AppNotificationManager instance = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final Application application;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static void setupInstance(Application application) {
        if (getInstance() == null) {
            instance = new AppNotificationManager(application);
        }
    }

    private static AppNotificationManager getInstance() {
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void enableReceivers() {
        PackageManager pm = application.getPackageManager();

        ComponentName receiver1 = new ComponentName(application, BroadcastReceiver.class);
        pm.setComponentEnabledSetting(receiver1,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void disableReceivers() {
        PackageManager pm = application.getPackageManager();

        ComponentName receiver1 = new ComponentName(application, BroadcastReceiver.class);
        pm.setComponentEnabledSetting(receiver1,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void subscribe(String channel) {
        OneSignal.setSubscription(true);
        enableReceivers();
    }

    private void unsubscribe(String channel) {
        OneSignal.setSubscription(false);
        disableReceivers();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // SharedPreferences.OnSharedPreferenceChangeListener
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static class BroadcastReceiver implements OneSignal.NotificationOpenedHandler {
        @Override
        public void notificationOpened(OSNotificationOpenResult result) {
            DataManager.getInstance().setPlayNow(true);
        }
    }
}
