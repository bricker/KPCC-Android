package org.kpcc.android;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

/**
 * StreamService is a service which plays streams. There should only be one instance of
 * this class running, but it can play multiple streams.
 * It keeps track of the current stream, the stream that just stopped playing and the upcoming stream.
 */
public class StreamService extends Service {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Stream previousStream;
    private Stream currentStream;
    private Stream nextStream;

    private final LocalBinder mBinder = new LocalBinder(){
        @Override
        StreamService getService() {
            return StreamService.this;
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // Service
    public IBinder onBind(Intent intent) {
        return getBinder();
    }

    @Override // Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (getCurrentStream() != null) {
            getCurrentStream().prepareAndStart();
        }

        return START_STICKY;
    }

    @Override // Service
    public void onDestroy() {
        releaseAllActiveStreams();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    LocalBinder getBinder() {
        return mBinder;
    }

    Stream getCurrentStream() {
        return currentStream;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void shift(Stream stream) {
        this.previousStream = currentStream;
        this.currentStream = nextStream;
        this.nextStream = stream;
    }

    void setCurrentStream(Stream stream) {
        this.previousStream = currentStream;
        this.currentStream = stream;
    }

    void setNextStream(Stream stream) {
        this.nextStream = stream;
    }

    void releaseAllActiveStreams() {
        if (previousStream != null) previousStream.release();
        if (currentStream != null) currentStream.release();
        if (nextStream != null) nextStream.release();
    }

    void pauseAllActiveStreams() {
        if (previousStream != null) previousStream.pause();
        if (currentStream != null) currentStream.pause();
        if (nextStream != null) nextStream.pause();
    }

    void prepareAndStart() {
        currentStream.prepareAndStart();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    abstract class LocalBinder extends Binder {
        abstract StreamService getService();
    }
}
