package org.kpcc.android;

import android.app.Application;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParsePush;

/**
 * Created by rickb014 on 2/15/15.
 */
public class PushManager {
    private static PushManager instance = null;
    public static final String TAG = "PushManager";
    private static final String APP_ID = AppConfiguration.getInstance().getConfig("parse.applicationId");
    private static final String CLIENT_KEY = AppConfiguration.getInstance().getConfig("parse.clientKey");

    public static PushManager getInstance() {
        return instance;
    }

    public static void setupInstance(Application application) {
        if (instance == null) {
            instance = new PushManager(application);
        }
    }


    protected PushManager(Application application) {
        Parse.initialize(application, APP_ID, CLIENT_KEY);
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParsePush.subscribeInBackground("breakingNews");
    }
}
