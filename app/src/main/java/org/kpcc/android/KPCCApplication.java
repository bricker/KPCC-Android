package org.kpcc.android;

import android.app.Application;
import android.preference.PreferenceManager;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by rickb014 on 2/15/15.
 */
public class KPCCApplication extends Application {
    @Override
    public void onCreate() {
        // The order of these is important.
        // Can we move these to static initializers? Probably not, because the order
        // of these honors dependencies and needs to stay intact.
        AppConfiguration.setupInstance(this);
        HttpRequest.Manager.setupInstance(this);
        BackgroundImageManager.setupInstance();
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
