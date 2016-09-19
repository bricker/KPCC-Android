package org.kpcc.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

public class AppConnectivityManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static AppConnectivityManager instance;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final ConnectivityManager mConnectivityManager;
    private final Map<String, NetworkConnectivityListener> mNetworkConnectivityListeners = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static void setupInstance(Context context) {
        instance = new AppConnectivityManager(context);
    }

    static AppConnectivityManager getInstance() {
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private AppConnectivityManager(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    boolean isConnectedToNetwork() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    void addOnNetworkConnectivityListener(Context context, String tag, boolean doNow, StreamService streamService, NetworkConnectivityListener listener) {
        mNetworkConnectivityListeners.put(tag, listener);

        if (!doNow) {
            return;
        }

        if (AppConnectivityManager.getInstance().isConnectedToNetwork()) {
            listener.onConnect(context, streamService);
        } else {
            listener.onDisconnect(context, streamService);
        }
    }

    void removeOnNetworkConnectivityListener(String tag) {
        mNetworkConnectivityListeners.remove(tag);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) return;

            Bundle extras = intent.getExtras();
            boolean isFailover = extras.getBoolean(ConnectivityManager.EXTRA_IS_FAILOVER);
            boolean noConnection = extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);

            StreamService.LocalBinder streamBinder = (StreamService.LocalBinder)peekService(context, new Intent(context, StreamService.class));
            StreamService streamService = null;
            if (streamBinder != null) {
                streamService = streamBinder.getService();
            }

            for (NetworkConnectivityListener listener : AppConnectivityManager.getInstance().mNetworkConnectivityListeners.values()) {
                if (isFailover) {
                    // TODO Do something?
                } else if (noConnection || !AppConnectivityManager.getInstance().isConnectedToNetwork()) {
                    listener.onDisconnect(context, streamService);
                } else {
                    listener.onConnect(context, streamService);
                }
            }
        }
    }

    interface NetworkConnectivityListener {
        void onConnect(Context context, StreamService streamService);
        void onDisconnect(Context context, StreamService streamService);
    }
}
