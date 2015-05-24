package org.kpcc.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.parse.Parse;
import com.parse.ParseInstallation;

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
        HttpRequest.Manager.setupInstance(this);
        AnalyticsManager.setupInstance(this);

        Parse.initialize(this,
                AppConfiguration.instance.getConfig("parse.applicationId"),
                AppConfiguration.instance.getConfig("parse.clientKey")
        );
        ParseInstallation.getCurrentInstallation().saveInBackground();

        // Parse must be initialized first.
        NotificationManager.setupInstance(this);

        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/FreigSanProLig.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }
}
