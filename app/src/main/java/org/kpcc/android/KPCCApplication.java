package org.kpcc.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.flurry.android.FlurryAgent;
import com.onesignal.OneSignal;
import com.parse.Parse;

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
        BaseAlarmManager.setupInstance(this);
        AnalyticsManager.setupInstance(this);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(AppConfiguration.getInstance().getSecret("parse.newApplicationId"))
                .server("http://parse.scprdev.org/")
                .build()
        );

        FlurryAgent.setLogEnabled(AppConfiguration.getInstance().isDebug);
        FlurryAgent.init(this, AppConfiguration.getInstance().getSecret("flurry.apiKey"));

        // Parse must be initialized first.
        AppNotificationManager.setupInstance(this);
        XFSManager.setupInstance(this);

        super.onCreate();
        OneSignal.startInit(this).setNotificationOpenedHandler(new AppNotificationManager.BroadcastReceiver()).init();
        Fabric.with(this, new Crashlytics(), new Answers());

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/FreigSanProLig.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }
}
