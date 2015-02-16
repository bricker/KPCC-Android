package org.kpcc.android;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

/**
 * Created by rickb014 on 2/15/15.
 */
public class Analytics {
    private static Analytics instance = null;
    public static final String TAG = "Analytics";
    private static final String TOKEN = AppConfiguration.getInstance().getConfig("mixpanel.token");

    private MixpanelAPI mMixpanelAPI;

    public static Analytics getInstance() {
        return instance;
    }

    public static void setupInstance(Context context) {
        if (instance == null) {
            instance = new Analytics(context);
        }
    }


    protected Analytics(Context context) {
        mMixpanelAPI = MixpanelAPI.getInstance(context, TOKEN);
    }


    public void logEvent(String name, JSONObject parameters) {
        mMixpanelAPI.track(name, parameters);
    }

    public void logEvent(String name) {
        // Send empty parameters
        JSONObject parameters = new JSONObject();
        mMixpanelAPI.track(name, parameters);
    }

    public void flush() {
        mMixpanelAPI.flush();
    }
}
