package org.kpcc.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver {
    public static class AlarmClock extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            DataManager.instance.setPlayNow(true);
            context.startActivity(activityIntent);

            BaseAlarmManager.WakeManager.instance.reset();
            AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_ALARM_FIRED);
        }
    }

    public static class SleepTimer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AppConnectivityManager.instance.streamIsBound) {
                AppConnectivityManager.instance.streamManager.pauseAllActiveStreams();
            }

            // TODO: What is the above fails? Should we retry?
            BaseAlarmManager.SleepManager.instance.reset();
            AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_SLEEP_TIMER_FIRED);
        }
    }
}
