package org.kpcc.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;

class DataManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final static String PREF_FALLBACK_AD_ID = "fallback_ad_id";
    private final static String PREF_USER_PLAYED_LIVESTREAM = "live_stream_played";
    private final static String PREF_LIVESTREAM_PLAY_NOW = "live_stream_play_now";
    private final static String PREF_ALARM_DATE = "alarm_date";
    private final static String PREF_TIMER_MILLIS = "timer_date";
    private final static String PREF_STREAM_PREFIX = "stream";
    private final static String PREF_XFS_VALIDATED_PREFIX = "xfs_validated";

    private static DataManager instance;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final SharedPreferences mPrefs;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static void setupInstance(Context context) {
        instance = new DataManager(context);
    }
    static DataManager getInstance() {
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private DataManager(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public String getAdId() {
        return mPrefs.getString(PREF_FALLBACK_AD_ID, "");
    }

    public void setAdId(String id) {
        mPrefs.edit().putString(PREF_FALLBACK_AD_ID, id).apply();
    }

    public boolean getHasPlayedLiveStream() {
        return mPrefs.getBoolean(PREF_USER_PLAYED_LIVESTREAM, false);
    }

    public void setHasPlayedLiveStream(boolean hasPlayedLiveStream) {
        mPrefs.edit().putBoolean(PREF_USER_PLAYED_LIVESTREAM, hasPlayedLiveStream).apply();
    }

    public boolean getPlayNow() {
        return mPrefs.getBoolean(PREF_LIVESTREAM_PLAY_NOW, false);
    }

    public void setPlayNow(boolean playNow) {
        mPrefs.edit().putBoolean(PREF_LIVESTREAM_PLAY_NOW, playNow).apply();
    }

    public String getStreamPreference() {
        String keyHash = XFSManager.getInstance().getDriveHash();
        return mPrefs.getString(PREF_STREAM_PREFIX + keyHash, LivePlayer.STREAM_STANDARD.getKey());
    }

    public void setStreamPreference(String stream) {
        String keyHash = XFSManager.getInstance().getDriveHash();
        mPrefs.edit().putString(PREF_STREAM_PREFIX + keyHash, stream).apply();
    }

    public boolean getIsXfsValidated() {
        String keyHash = XFSManager.getInstance().getDriveHash();
        return mPrefs.getBoolean(PREF_XFS_VALIDATED_PREFIX + keyHash, false);
    }

    public void setIsXfsValidated(boolean validated) {
        String keyHash = XFSManager.getInstance().getDriveHash();
        mPrefs.edit().putBoolean(PREF_XFS_VALIDATED_PREFIX + keyHash, validated).apply();
    }

    public Date getAlarmDate() {
        long millis = mPrefs.getLong(PREF_ALARM_DATE, 0);
        return new Date(millis);
    }

    public void setAlarmDate(long millis) {
        mPrefs.edit().putLong(PREF_ALARM_DATE, millis).apply();
    }

    public void clearAlarmDate() {
        mPrefs.edit().remove(PREF_ALARM_DATE).apply();
    }


    public long getTimerMillis() {
        return mPrefs.getLong(PREF_TIMER_MILLIS, 0);
    }

    public void setTimerMillis(long millis) {
        mPrefs.edit().putLong(PREF_TIMER_MILLIS, millis).apply();
    }

    public void clearTimerMillis() {
        mPrefs.edit().remove(PREF_TIMER_MILLIS).apply();
    }
}
