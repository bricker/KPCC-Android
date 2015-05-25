package org.kpcc.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;

public class DataManager {
    private final static String PREF_FALLBACK_AD_ID = "fallback_ad_id";
    private final static String PREF_USER_PLAYED_LIVESTREAM = "live_stream_played";
    private final static String PREF_LIVESTREAM_PLAY_NOW = "live_stream_play_now";
    private final static String PREF_ALARM_DATE= "alarm_date";

    public static DataManager instance;
    private final Context mContext;
    private final SharedPreferences mPrefs;

    public static void setupInstance(Context context) {
        instance = new DataManager(context);
    }

    protected DataManager(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
    }

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
}
