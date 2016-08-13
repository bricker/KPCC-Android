package org.kpcc.android;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Locale;

/**
 * Created by rickb014 on 7/27/16.
 */
public class AnalyticsManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static class Action {
        private String mCategory;
        private String mAction;

        Action(String category, String action) {
            mCategory = category;
            mAction = action;
        }

        public String getCategory() {
            return mCategory;
        }

        public String getAction() {
            return mAction;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static AnalyticsManager instance;

    public static final String CATEGORY_LIVE_STREAM = "Live Stream";
    public static final String CATEGORY_ON_DEMAND = "On Demand";
    public static final String CATEGORY_USER_INTERACTION = "User Interaction";
    public static final String CATEGORY_ALARM = "Alarm / Sleep Timer";
    public static final String CATEGORY_GENERAL = "General";
    public static final String CATEGORY_SSO = "SSO";

    public static final String ACTION_LIVE_STREAM_PLAY = "liveStreamPlay";
    public static final String ACTION_LIVE_STREAM_MOVED_TO_BACKGROUND = "liveStreamMovedToBackground";
    public static final String ACTION_LIVE_STREAM_PAUSE = "liveStreamPause";
    public static final String ACTION_LIVE_STREAM_TIME_SHIFTED = "liveStreamTimeShifted";
    public static final String ACTION_LIVE_STREAM_RETURNED_TO_FOREGROUND = "liveStreamReturnedToForeground";
    public static final String ACTION_LIVE_STREAM_SESSION_EXPIRED = "liveStreamSessionExpired";
    public static final String ACTION_LIVE_STREAM_STALLED = "liveStreamStalled";
    public static final String ACTION_ON_DEMAND_AUDIO_TIME_SHIFTED = "episodeAudioTimeShifted";
    public static final String ACTION_ON_DEMAND_COMPLETED = "episodeCompleted";
    public static final String ACTION_ON_DEMAND_MOVED_TO_BACKGROUND = "episodeAudioMovedToBackground";
    public static final String ACTION_ON_DEMAND_PLAY = "episodePlay";
    public static final String ACTION_ON_DEMAND_PAUSE = "episodePaused";
    public static final String ACTION_ON_DEMAND_25_PERCENT = "episodeReached25percent";
    public static final String ACTION_ON_DEMAND_RETURNED_TO_FOREGROUND = "episodeAudioReturnedToForeground";
    public static final String ACTION_ON_DEMAND_MIDWAY = "episodeReachedMidwayPoint";
    public static final String ACTION_ON_DEMAND_75_PERCENT = "episodeReached75percent";
    public static final String ACTION_ON_DEMAND_SKIPPED = "episodeSkipped";
    public static final String ACTION_ON_DEMAND_SHARED_MESSAGE = "episodeSharedMessage";
    public static final String ACTION_ON_DEMAND_SHARED_MAIL = "episodeSharedMail";
    public static final String ACTION_ON_DEMAND_COPIED = "episodeSharedCopytopasteboard";
    public static final String ACTION_ON_DEMAND_FACEBOOK = "episodeSharedPosttofacebook";
    public static final String ACTION_ON_DEMAND_ACTION_EXTENSION = "episodeSharedActionextension";
    public static final String ACTION_ON_DEMAND_SHARING_EXTENSION = "episodeSharedSharingextension";
    public static final String ACTION_ON_DEMAND_SHARE_EXTENSION = "episodeSharedShareextension";
    public static final String ACTION_ON_DEMAND_COMPOSE_SHARE_EXTENSION = "episodeSharedCompose-Shareextension";
    public static final String ACTION_ON_DEMAND_SHARED = "episodeSharedShare";
    public static final String ACTION_USER_VIEWING_UP_NEXT = "userViewingUpNext";
    public static final String ACTION_USER_VIEWING_FULL_SCHEDULE = "userViewingFullSchedule";
    public static final String ACTION_USER_VIEWING_HEADLINES = "userViewingHeadlines";
    public static final String ACTION_USER_DONATE = "userSelectedDonate";
    public static final String ACTION_SLEEP_TIMER_ARMED = "sleepTimerArmed";
    public static final String ACTION_SLEEP_TIMER_FIRED = "sleepTimerFired";
    public static final String ACTION_SLEEP_TIMER_CANCELED = "sleepTimerCanceled";
    public static final String ACTION_ALARM_ARMED = "alarmClockArmed";
    public static final String ACTION_ALARM_FIRED = "alarmClockFired";
    public static final String ACTION_KPCC_PLUS_SELECTED = "kpcc-plus-selected";
    public static final String ACTION_TICKET_AD_TAPPED = "ticketTuesdayAdTapped";
    public static final String ACTION_LOGGED_IN = "loggedIn";
    public static final String ACTION_SIGNED_UP = "signedUp";

    public static final String LABEL_LIVE_STREAM_PLAY = "behindLiveSeconds:%s |-| behindLiveStatus:%s |-| programTitle: %s |-| streamId: %s";
    public static final String LABEL_LIVE_STREAM_MOVED_TO_BACKGROUND = "secondsSinceSessionBegan: %s";
    public static final String LABEL_LIVE_STREAM_PAUSE = "programTitle: %s |-| sessionLength: %s |-| sessionLengthInSeconds: %s";
    public static final String LABEL_LIVE_STREAM_TIME_SHIFTED = "amount: %s |-| method: %s";
    public static final String LABEL_LIVE_STREAM_RETURNED_TO_FOREGROUND = "secondsSinceAppWasBackgrounded: %s";
    public static final String LABEL_LIVE_STREAM_SESSION_EXPIRED = "idle_seconds: %s";
    public static final String LABEL_LIVE_STREAM_STALLED = "elapsedTime: %s |-| program: %s";
    public static final String LABEL_ON_DEMAND_AUDIO_TIME_SHIFTED = "amount: %s |-| method: %s";
    public static final String LABEL_ON_DEMAND_COMPLETED = "currentProgress: %s |-| duration: %s |-| episodeTitle: %s |-| programTitle %s";
    public static final String LABEL_ON_DEMAND_MOVED_TO_BACKGROUND = "secondsSinceSessionBegan: %s";
    public static final String LABEL_ON_DEMAND_PLAY = "episodeOrSegmentTitle: %s |-| programTitle: %s";
    public static final String LABEL_ON_DEMAND_PAUSE = "currentProgress: %s |-| duration: %s |-| episodeTitle: %s |-| programTitle %s";
    public static final String LABEL_ON_DEMAND_25_PERCENT = "currentProgress: %s |-| duration: %s |-| episodeTitle: %s |-| programTitle %s";
    public static final String LABEL_ON_DEMAND_RETURNED_TO_FOREGROUND = "secondsSinceAppWasBackgrounded: %s";
    public static final String LABEL_ON_DEMAND_MIDWAY = "currentProgress: %s |-| duration: %s |-| episodeTitle: %s |-| programTitle %s";
    public static final String LABEL_ON_DEMAND_75_PERCENT = "currentProgress: %s |-| duration: %s |-| episodeTitle: %s |-| programTitle %s";
    public static final String LABEL_ON_DEMAND_SKIPPED = "currentProgress: %s |-| duration: %s |-| episodeTitle: %s |-| programTitle %s";
    public static final String LABEL_ON_DEMAND_SHARED_MESSAGE = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_SHARED_MAIL = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_COPIED = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_FACEBOOK = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_ACTION_EXTENSION = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_SHARING_EXTENSION = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_SHARE_EXTENSION = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_COMPOSE_SHARE_EXTENSION = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_ON_DEMAND_SHARED = "episodeTitle: %s |-| episodeUrl: %s |-| program-title: %s";
    public static final String LABEL_USER_VIEWING_UP_NEXT = "";
    public static final String LABEL_USER_VIEWING_FULL_SCHEDULE = "";
    public static final String LABEL_USER_VIEWING_HEADLINES = "";
    public static final String LABEL_USER_DONATE = "";
    public static final String LABEL_SLEEP_TIMER_ARMED = "";
    public static final String LABEL_SLEEP_TIMER_FIRED = "";
    public static final String LABEL_SLEEP_TIMER_CANCELED = "";
    public static final String LABEL_ALARM_ARMED = "";
    public static final String LABEL_ALARM_FIRED = "";
    public static final String LABEL_KPCC_PLUS_SELECTED = "token-status: %s";
    public static final String LABEL_TICKET_AD_TAPPED = "elqCampaignId: %s |-| elqFormName: %s |-| elqSiteId: %s";
    public static final String LABEL_LOGGED_IN = "method: %s |-| origin: %s";
    public static final String LABEL_SIGNED_UP = "method: %s |-| origin: %s";

    private static final String TRACKING_ID = AppConfiguration.getInstance().getConfig("analytics.trackingId");


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Tracker mTracker;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static void setupInstance(Context context) {
        instance = new AnalyticsManager(context);
    }

    static String buildLabel(String label, String... values) {
        return String.format(Locale.ENGLISH, label, values);
    }

    static AnalyticsManager getInstance() {
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private AnalyticsManager(Context context) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
        mTracker = analytics.newTracker(TRACKING_ID);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void sendAction(String category, String action, String label) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }
}
