package org.kpcc.android;

import android.app.Application;

/**
 * Created by rickb014 on 2/15/15.
 */
public class KPCCApplication extends Application {
    @Override
    public void onCreate() {
        AppConfiguration.setupInstance(getApplicationContext());
        Analytics.setupInstance(getApplicationContext());
        PushManager.setupInstance(this);

        super.onCreate();
    }
}
