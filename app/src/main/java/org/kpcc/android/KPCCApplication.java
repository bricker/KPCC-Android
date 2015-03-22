package org.kpcc.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by rickb014 on 2/15/15.
 */
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
        // Can we move these to static initializers? Probably not, because the order
        // of these honors dependencies and needs to stay intact.
        AppConfiguration.setupInstance(this);
        HttpRequest.Manager.setupInstance(this);
        NetworkImageManager.setupInstance();
        ParseManager.setupInstance(this);
        AnalyticsManager.setupInstance(this);
        NotificationManager.setupInstance(this);
        FeedbackManager.setupInstance();
        ProgramsManager.setupInstance();

        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/FreigSanProMed.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }
}
