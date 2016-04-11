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
            DataManager.getInstance().setPlayNow(true);
            context.startActivity(activityIntent);

            BaseAlarmManager.WakeManager.getInstance().reset();
        }
    }

    public static class SleepTimer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StreamManager.ConnectivityManager.getInstance().getStreamIsBound()) {
                StreamManager.ConnectivityManager.getInstance().getStreamManager().pauseAllActiveStreams();
            }

            // TODO: What is the above fails? Should we retry?
            BaseAlarmManager.SleepManager.getInstance().reset();
        }
    }
}
