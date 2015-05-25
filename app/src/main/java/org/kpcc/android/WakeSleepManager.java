package org.kpcc.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WakeSleepManager {
    public static WakeSleepManager instance;
    public static final String INTENT_ACTION = "org.kpcc.android.LIVESTREAM_ALARM";
    private Context mContext;
    private AlarmManager mAlarmManager;

    public static void setupInstance(Context context) {
        instance = new WakeSleepManager(context);
    }

    protected WakeSleepManager(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
    }

    public void setAlarm(int hourOfDay, int minute) {
        long nowMillis = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long alarmMillis = calendar.getTimeInMillis();

        // If this alarm was set for the past, add 24 hours to it.
        if (alarmMillis < nowMillis) {
            alarmMillis += TimeUnit.HOURS.toMillis(24);
        }

        Intent intent = buildIntent();
        PendingIntent pendingIntent = buildPendingIntent(intent);

        // For API <19, set() is exact, so we can use it for this alarm.
        // For API 19+, set() is inexact, so we need to use setExact()
        // setExact() doesn't exist in API <19.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
        } else {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
        }

        DataManager.instance.setAlarmDate(alarmMillis);
        AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_ALARM_ARMED);
    }

    public void cancelAlarm() {
        Intent intent = buildIntent();
        PendingIntent pendingIntent = buildPendingIntent(intent);
        mAlarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        reset();
        AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_ALARM_CANCELED);
    }

    public void reset() {
        DataManager.instance.clearAlarmDate();
    }

    public boolean alarmIsRunning() {
        Intent intent = buildIntent();
        return PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    private Intent buildIntent() {
        Intent intent = new Intent(mContext, AlarmReceiver.class);
        intent.setAction(INTENT_ACTION);
        return intent;
    }

    private PendingIntent buildPendingIntent(Intent intent) {
        return PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    }
}
