package org.kpcc.android;

import android.app.Application;
import android.preference.PreferenceManager;

/**
 * Created by rickb014 on 2/15/15.
 */
public class KPCCApplication extends Application {
    @Override
    public void onCreate() {
        // The order of these is important.
        AppConfiguration.setupInstance(this);
        ParseManager.setupInstance(this);
        AnalyticsManager.setupInstance(this);
        NotificationManager.setupInstance(this);

        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }
}
