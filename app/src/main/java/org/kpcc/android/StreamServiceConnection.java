package org.kpcc.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rickb014 on 5/20/16.
 */
public class StreamServiceConnection implements ServiceConnection {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private StreamService mStreamService;
    private final AtomicBoolean mStreamIsBound = new AtomicBoolean(false);
    private final List<OnStreamBindListener> mStreamBindListeners = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    StreamServiceConnection() {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    synchronized StreamService getStreamService() {
        return mStreamService;
    }

    private synchronized void setStreamService(StreamService streamService) {
        mStreamService = streamService;
    }

    boolean getStreamIsBound() {
        return mStreamIsBound.get();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // ServiceConnection
    public void onServiceConnected(ComponentName className, IBinder binder) {
        StreamService service = ((StreamService.LocalBinder) binder).getService();
        setStreamService(service);
        mStreamIsBound.set(true);

        for (OnStreamBindListener listener : mStreamBindListeners) {
            listener.onBind();
        }

        mStreamBindListeners.clear();
    }

    @Override // ServiceConnection
    public void onServiceDisconnected(ComponentName arg0) {
        mStreamService.stopForeground(true);
        mStreamIsBound.set(false);
        setStreamService(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void addOnStreamBindListener(String tag, OnStreamBindListener listener) {
        if (mStreamIsBound.get()) {
            listener.onBind();
        } else {
            mStreamBindListeners.add(listener);
        }
    }

    void bindStreamService(Context context, Class<?> clazz) {
        Intent intent = new Intent(context, clazz);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    void unbindStreamService(Context context) {
        if (mStreamIsBound.get()) {
            context.unbindService(this);
            mStreamIsBound.set(false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    interface OnStreamBindListener {
        void onBind();
    }
}
