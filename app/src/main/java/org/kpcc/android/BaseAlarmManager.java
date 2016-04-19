package org.kpcc.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class BaseAlarmManager {
    final Context mContext;
    final AlarmManager mAlarmManager;

    public static void setupInstance(Context context) {
        WakeManager.setupInstance(context);
        SleepManager.setupInstance(context);
    }

    BaseAlarmManager(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
    }




    public static class WakeManager extends BaseAlarmManager {
        private static WakeManager instance;
        public static final String INTENT_ACTION = "org.kpcc.android.LIVESTREAM_ALARM";
        private static final int INTENT_ID = 0;

        public static void setupInstance(Context context) {
            instance = new WakeManager(context);
        }

        WakeManager(Context context) {
            super(context);
        }

        static WakeManager getInstance() {
            return instance;
        }

        public void set(int hourOfDay, int minute) {
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

            DataManager.getInstance().setAlarmDate(alarmMillis);
        }

        public void cancel() {
            reset();
        }

        public void reset() {
            Intent intent = buildIntent();
            PendingIntent pendingIntent = buildPendingIntent(intent);
            mAlarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            DataManager.getInstance().clearAlarmDate();
        }

        public boolean isRunning() {
            Intent intent = buildIntent();
            return PendingIntent.getBroadcast(mContext, INTENT_ID, intent, PendingIntent.FLAG_NO_CREATE) != null;
        }


        private Intent buildIntent() {
            Intent intent = new Intent(mContext, AlarmReceiver.AlarmClock.class);
            intent.setAction(INTENT_ACTION);
            return intent;
        }

        private PendingIntent buildPendingIntent(Intent intent) {
            return PendingIntent.getBroadcast(mContext, INTENT_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

    }




    public static class SleepManager extends BaseAlarmManager {
        private static SleepManager instance;
        public static final String INTENT_ACTION = "org.kpcc.android.SLEEP_TIMER";
        private static final int INTENT_ID = 1;

        public static void setupInstance(Context context) {
            instance = new SleepManager(context);
        }

        SleepManager(Context context) {
            super(context);
        }

        static SleepManager getInstance() {
            return instance;
        }


        public void set(long relativeMillis) {
            long endMillis = SystemClock.elapsedRealtime() + relativeMillis;

            Intent intent = buildIntent();
            PendingIntent pendingIntent = buildPendingIntent(intent);

            // This one doesn't need to be precise so we can let it run in the batch alarms from the os.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endMillis, pendingIntent);
            } else {
                mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, endMillis, pendingIntent);
            }

            DataManager.getInstance().setTimerMillis(endMillis);
        }


        public void cancel() {
            reset();
        }

        public void reset() {
            Intent intent = buildIntent();
            PendingIntent pendingIntent = buildPendingIntent(intent);
            mAlarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            DataManager.getInstance().clearTimerMillis();
        }

        public boolean isRunning() {
            Intent intent = buildIntent();
            return PendingIntent.getBroadcast(mContext, INTENT_ID, intent, PendingIntent.FLAG_NO_CREATE) != null;
        }


        private Intent buildIntent() {
            Intent intent = new Intent(mContext, AlarmReceiver.SleepTimer.class);
            intent.setAction(INTENT_ACTION);
            return intent;
        }

        private PendingIntent buildPendingIntent(Intent intent) {
            return PendingIntent.getBroadcast(mContext, INTENT_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

    }
}
