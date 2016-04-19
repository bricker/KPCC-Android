package org.kpcc.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamManager extends Service {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final IBinder mBinder = new LocalBinder();
    private OnDemandPlayer mCurrentOnDemandPlayer;
    private LivePlayer mCurrentLivePlayer;
    private PrerollPlayer mCurrentPrerollPlayer;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override// Service
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    synchronized LivePlayer getCurrentLivePlayer() {
        return mCurrentLivePlayer;
    }

    synchronized void setCurrentLivePlayer(final LivePlayer livePlayer) {
        mCurrentLivePlayer = livePlayer;
    }

    synchronized OnDemandPlayer getCurrentOnDemandPlayer() {
        return mCurrentOnDemandPlayer;
    }

    synchronized void setCurrentOnDemandPlayer(final OnDemandPlayer onDemandPlayer) {
        mCurrentOnDemandPlayer = onDemandPlayer;
    }

    synchronized PrerollPlayer getCurrentPrerollPlayer() {
        return mCurrentPrerollPlayer;
    }

    synchronized void setCurrentPrerollPlayer(final PrerollPlayer prerollPlayer) {
        mCurrentPrerollPlayer = prerollPlayer;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // The nuclear option - this isn't guaranteed to update button states. It's a cleanup task.
    void releaseAllActiveStreams() {
        if (getCurrentOnDemandPlayer() != null) getCurrentOnDemandPlayer().release();
        if (getCurrentLivePlayer() != null) getCurrentLivePlayer().release();
        if (getCurrentPrerollPlayer() != null) getCurrentPrerollPlayer().release();
    }

    void pauseAllActiveStreams() {
        if (getCurrentOnDemandPlayer() != null) getCurrentOnDemandPlayer().pause();
        if (getCurrentLivePlayer() != null) getCurrentLivePlayer().release();
        if (getCurrentPrerollPlayer() != null) getCurrentPrerollPlayer().pause();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    class LocalBinder extends Binder {
        StreamManager getService() {
            return StreamManager.this;
        }
    }

    static class ConnectivityManager implements ServiceConnection {
        private static ConnectivityManager instance;

        private StreamManager mStreamManager;
        private final Context mContext;
        private final AtomicBoolean mStreamIsBound = new AtomicBoolean(false);
        private final Map<String, OnStreamBindListener> mStreamBindListeners = new HashMap<>();

        public static void setupInstance(Context context) {
            instance = new ConnectivityManager(context);
        }

        public static ConnectivityManager getInstance() {
            return instance;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        // Constructors
        ////////////////////////////////////////////////////////////////////////////////////////////////
        private ConnectivityManager(Context context) {
            mContext = context;
        }

        synchronized StreamManager getStreamManager() {
            return mStreamManager;
        }

        private synchronized void setStreamManager(StreamManager streamManager) {
            mStreamManager = streamManager;
        }

        boolean getStreamIsBound() {
            return mStreamIsBound.get();
        }

        @Override // ServiceConnection
        public void onServiceConnected(ComponentName className, IBinder service) {
            StreamManager.LocalBinder binder = (StreamManager.LocalBinder) service;
            setStreamManager(binder.getService());
            mStreamIsBound.set(true);

            for (OnStreamBindListener listener : mStreamBindListeners.values()) {
                listener.onBind();
            }

            mStreamBindListeners.clear();
        }

        @Override // ServiceConnection
        public void onServiceDisconnected(ComponentName arg0) {
            if (mStreamIsBound.get()) {
                mStreamManager.releaseAllActiveStreams();
            }

            setStreamManager(null);
            mStreamIsBound.set(false);
        }

        void addOnStreamBindListener(String tag, OnStreamBindListener listener) {
            if (mStreamIsBound.get()) {
                listener.onBind();
            } else {
                mStreamBindListeners.put(tag, listener);
            }
        }

        void removeOnStreamBindListener(String tag) {
            mStreamBindListeners.remove(tag);
        }

        void bindStreamService() {
            Intent intent = new Intent(mContext, StreamManager.class);
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
            mContext.startService(intent);
        }

        void unbindStreamService() {
            if (mStreamIsBound.get()) {
                mContext.unbindService(this);
                mStreamIsBound.set(false);
            }
        }

        interface OnStreamBindListener {
            void onBind();
        }

    }
}
