package org.kpcc.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.parse.Parse;
import com.parse.ParseInstallation;

import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class KPCCApplication extends Application {
    public static long INSTALLATION_TIME = 0;

    @Override
    public void onCreate() {
        try {
            INSTALLATION_TIME = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            // Default value is already set.
        }

        // The order of these is important.
        AppConnectivityManager.setupInstance(this);
        AppConfiguration.setupInstance(this);
        DataManager.setupInstance(this);
        HttpRequest.Manager.setupInstance(this);
        AnalyticsManager.setupInstance(this);
        BaseAlarmManager.setupInstance(this);

        Parse.initialize(this,
                AppConfiguration.instance.getSecret("parse.applicationId"),
                AppConfiguration.instance.getSecret("parse.clientKey")
        );
        ParseInstallation.getCurrentInstallation().saveInBackground();

        // Parse must be initialized first.
        AppNotificationManager.setupInstance(this);

        super.onCreate();
        Fabric.with(this, new Crashlytics());

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/FreigSanProLig.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }
}
