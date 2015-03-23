package org.kpcc.android;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

public class AnalyticsManager {
    public static final String TAG = "AnalyticsManager";
    public final static String EVENT_PROGRAM_SELECTED = "programSelected";
    public final static String EVENT_MENU_SELECTION_LIVE_STREAM = "menuSelectionLiveStream";
    public final static String EVENT_MENU_SELECTION_PROGRAMS = "menuSelectionPrograms";
    public final static String EVENT_MENU_SELECTION_HEADLINES = "menuSelectionHeadlines";
    public final static String EVENT_MENU_SELECTION_DONATE = "menuSelectionDonate";
    public final static String EVENT_MENU_SELECTION_FEEDBACK = "menuSelectionFeedback";
    public final static String EVENT_MENU_SELECTION_SETTINGS = "menuSelectionSettings";
    public final static String EVENT_LIVE_STREAM_PLAY = "liveStreamPlay";
    public final static String EVENT_LIVE_STREAM_PAUSE = "liveStreamPause";
    public final static String EVENT_ON_DEMAND_COMPLETED = "onDemandEpisodeCompleted";
    public final static String EVENT_ON_DEMAND_BEGAN = "onDemandEpisodeBegan";
    public final static String EVENT_ON_DEMAND_PAUSED = "onDemandAudioPaused";
    public final static String EVENT_CLOSED_HEADLINES = "userClosedHeadlines";
    private static final String MIXPANEL_TOKEN = AppConfiguration.instance.getConfig("mixpanel.token");
    public static AnalyticsManager instance = null;
    private MixpanelAPI mMixpanelAPI;

    protected AnalyticsManager(Context context) {
        mMixpanelAPI = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
    }

    public static void setupInstance(Context context) {
        if (instance == null) {
            instance = new AnalyticsManager(context);
        }
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
