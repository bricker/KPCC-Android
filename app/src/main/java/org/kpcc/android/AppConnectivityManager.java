package org.kpcc.android;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.HashMap;

public class AppConnectivityManager {
    public static AppConnectivityManager instance;
    private final ConnectivityManager mConnectivityManager;
    public boolean streamIsBound = false;
    private final ArrayList<OnStreamBindListener> mStreamBindListeners = new ArrayList<>();
    public final HashMap<String, NetworkConnectivityListener> networkConnectivityListeners = new HashMap<>();
    public StreamManager streamManager;
    private Context mContext;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            StreamManager.LocalBinder binder = (StreamManager.LocalBinder) service;
            streamManager = binder.getService();
            streamIsBound = true;

            for (OnStreamBindListener listener : mStreamBindListeners) {
                listener.onBind();
            }

            mStreamBindListeners.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (streamIsBound) {
                streamManager.releaseAllActiveStreams();
            }

            streamIsBound = false;
        }
    };

    public static void setupInstance(Context context) {
        instance = new AppConnectivityManager(context);
    }

    private AppConnectivityManager(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mContext = context;
    }

    boolean isConnectedToNetwork() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    public static class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            boolean isFailover = extras.getBoolean(ConnectivityManager.EXTRA_IS_FAILOVER);
            boolean noConnection = extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);

            for (NetworkConnectivityListener listener : AppConnectivityManager.instance.networkConnectivityListeners.values()) {
                if (isFailover) {
                    // TODO Do something?
                } else if (noConnection || !AppConnectivityManager.instance.isConnectedToNetwork()) {
                    listener.onDisconnect();
                } else {
                    listener.onConnect();
                }
            }
        }
    }


    void addOnStreamBindListener(OnStreamBindListener listener) {
        if (streamIsBound) {
            listener.onBind();
        } else {
            mStreamBindListeners.add(listener);
        }
    }

    void bindStreamService() {
        Intent intent = new Intent(mContext, StreamManager.class);
        mContext.startService(intent);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void unbindStreamService() {
        if (streamIsBound) {
            mContext.unbindService(mConnection);
            streamIsBound = false;
        }
    }

    abstract static interface OnStreamBindListener {
        public abstract void onBind();
    }

    void addOnNetworkConnectivityListener(String tag, NetworkConnectivityListener listener, boolean doNow) {
        networkConnectivityListeners.put(tag, listener);

        if (!doNow) {
            return;
        }

        if (AppConnectivityManager.instance.isConnectedToNetwork()) {
            listener.onConnect();
        } else {
            listener.onDisconnect();
        }
    }

    void removeOnNetworkConnectivityListener(String tag) {
        networkConnectivityListeners.remove(tag);
    }

    abstract static class NetworkConnectivityListener {
        public abstract void onConnect();
        public abstract void onDisconnect();
    }
}
