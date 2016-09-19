package org.kpcc.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class AlarmReceiver {
    public static class AlarmClock extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            DataManager.getInstance().setPlayNow(true);
            context.startActivity(activityIntent);

            BaseAlarmManager.WakeManager.getInstance().reset();

            AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_ALARM, AnalyticsManager.ACTION_ALARM_FIRED);
        }
    }

    public static class SleepTimer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent streamIntent = new Intent(context, StreamService.class);
            IBinder binder = peekService(context, streamIntent);
            if (binder != null) {
                StreamService service = ((StreamService.LocalBinder) binder).getService();
                service.getCurrentStream().stop();
            }

            BaseAlarmManager.SleepManager.getInstance().reset();

            AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_ALARM, AnalyticsManager.ACTION_SLEEP_TIMER_FIRED);
        }
    }
}
