package org.kpcc.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;

import java.util.Locale;

/**
 * Created by rickb014 on 7/13/16.
 */
public class StreamBindFragment extends Fragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private StreamServiceConnection mStreamConnection = new StreamServiceConnection();
    private NotificationCompat.Builder mNotificationBuilder;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected OnDemandPlayer getOnDemandPlayer() {
        Stream stream = getCurrentStream();
        if (stream != null && stream instanceof OnDemandPlayer) {
            return (OnDemandPlayer)stream;
        } else {
            return null;
        }
    }

    protected LivePlayer getLivePlayer() {
        Stream stream = getCurrentStream();
        if (stream != null && stream instanceof LivePlayer) {
            return (LivePlayer)stream;
        } else {
            return null;
        }
    }

    protected PrerollPlayer getPrerollPlayer() {
        Stream stream = getCurrentStream();
        if (stream != null && stream instanceof PrerollPlayer) {
            return (PrerollPlayer)stream;
        } else {
            return null;
        }
    }

    protected Stream getCurrentStream() {
        StreamService service = getStreamService();
        if (service == null) return null;

        Stream stream = service.getCurrentStream();
        if (stream != null) {
            return stream;
        } else {
            return null;
        }
    }

    protected void setCurrentStream(final Stream stream, String tag) {
        if (getStreamConnection() == null) return;

        getStreamConnection().addOnStreamBindListener(tag, new StreamServiceConnection.OnStreamBindListener() {
            @Override
            public void onBind() {
                StreamService service = getStreamService();
                if (service == null) return;
                service.setCurrentStream(stream);
            }
        });
    }

    protected void bindStreamService() {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            mStreamConnection.bindStreamService(activity, StreamService.class);
        }
    }

    protected StreamServiceConnection getStreamConnection() {
        return mStreamConnection;
    }

    protected StreamService getStreamService() {
        if (mStreamConnection == null || !mStreamConnection.getStreamIsBound()) return null;
        return mStreamConnection.getStreamService();
    }


    protected void initNotificationBuilder(int icon, String title) {
        if (mNotificationBuilder != null) return;

        MainActivity activity = (MainActivity)getActivity();
        PendingIntent pi = PendingIntent.getActivity(activity.getApplicationContext(), 0,
                new Intent(activity.getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder = new NotificationCompat.Builder(activity.getApplicationContext())
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentIntent(pi)
                .setOngoing(true);
    }

    protected NotificationCompat.Builder getNotificationBuilder() {
        return mNotificationBuilder;
    }

    // Override this for your own class.
    protected void buildNotification() {
    }

    protected void cancelNotification() {
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Service.NOTIFICATION_SERVICE);
            notificationManager.cancel(Stream.NOTIFICATION_ID);
        }
    }

    protected void sendNotification() {
        if (mNotificationBuilder == null) buildNotification();

        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            mNotificationBuilder.setWhen(System.currentTimeMillis());
            NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Service.NOTIFICATION_SERVICE);
            notificationManager.notify(Stream.NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

}
