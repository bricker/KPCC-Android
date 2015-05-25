package org.kpcc.android;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

public class AnalyticsManager {
    // Any key discrepancy is for parity with the iOS app.
    public final static String EVENT_PROGRAM_SELECTED = "programSelected";
    public final static String EVENT_MENU_SELECTION_LIVE_STREAM = "menuSelectionLiveStream";
    public final static String EVENT_MENU_SELECTION_PROGRAMS = "menuSelectionPrograms";
    public final static String EVENT_MENU_SELECTION_HEADLINES = "menuSelectionHeadlines";
    public final static String EVENT_MENU_SELECTION_WAKE_SLEEP = "menuSelectionWakeSleep";
    public final static String EVENT_MENU_SELECTION_DONATE = "menuSelectionDonate";
    public final static String EVENT_MENU_SELECTION_FEEDBACK = "menuSelectionFeedback";
    public final static String EVENT_MENU_SELECTION_SETTINGS = "menuSelectionSettings";
    public final static String EVENT_LIVE_STREAM_PLAY = "liveStreamPlay";
    public final static String EVENT_LIVE_STREAM_PAUSE = "liveStreamPause";
    public final static String EVENT_ON_DEMAND_COMPLETED = "onDemandEpisodeCompleted";
    public final static String EVENT_ON_DEMAND_BEGAN = "onDemandEpisodeBegan";
    public final static String EVENT_ON_DEMAND_PAUSED = "onDemandAudioPaused";
    public final static String EVENT_ON_DEMAND_SKIPPED = "onDemandAudioSkipped";
    public final static String EVENT_CLOSED_HEADLINES = "userClosedHeadlines";
    public final static String EVENT_MENU_OPENED = "menuOpened";
    public final static String EVENT_MENU_CLOSED = "menuClosed";
    public final static String EVENT_EPISODE_SHARED = "programEpisodeShared";
    public final static String EVENT_SLEEP_TIMER_ARMED = "sleepTimerArmed";
    public final static String EVENT_SLEEP_TIMER_CANCELED = "sleepTimerCanceled";
    public final static String EVENT_SLEEP_TIMER_FIRED = "sleepTimerFired";
    public final static String EVENT_ALARM_ARMED = "alarmArmed";
    public final static String EVENT_ALARM_CANCELED = "alarmCanceled";
    public final static String EVENT_ALARM_FIRED = "alarmFired";

    public final static String PARAM_PROGRAM_PUBLISHED_AT = "programPublishedAt";
    public final static String PARAM_PROGRAM_TITLE = "programTitle";
    public final static String PARAM_EPISODE_TITLE = "episodeTitle";
    public final static String PARAM_PROGRAM_LENGTH = "programLengthInSeconds";
    public final static String PARAM_PLAYED_DURATION = "playedDurationInSeconds";
    public final static String PARAM_SESSION_LENGTH = "sessionLengthInSeconds";
    public final static String PARAM_ACTIVITY_TYPE = "activityType";

    private static final String MIXPANEL_TOKEN = AppConfiguration.instance.getConfig("mixpanel.token");
    public static AnalyticsManager instance = null;
    private final MixpanelAPI mMixpanelAPI;

    private AnalyticsManager(Context context) {
        mMixpanelAPI = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
    }

    public static void setupInstance(Context context) {
        if (instance == null) {
            instance = new AnalyticsManager(context);
        }
    }

    public void logEvent(String name, JSONObject parameters) {
        try {
            mMixpanelAPI.track(name, parameters);
        } catch (Exception e) {
            // No event will be sent.
        }
    }

    public void logEvent(String name) {
        try {
            // Send empty parameters
            JSONObject parameters = new JSONObject();
            mMixpanelAPI.track(name, parameters);
        } catch (Exception e) {
            // No event will be sent.
        }
    }

    public void flush() {
        try {
            mMixpanelAPI.flush();
        } catch (Exception e) {
            // Events are not flushed.
        }
    }
}
