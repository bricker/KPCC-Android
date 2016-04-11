package org.kpcc.android;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rickb014 on 8/9/15.
 */
public class PeriodicBackgroundUpdater implements Runnable {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Thread mBackgroundThread;
    private final AtomicBoolean mIsObserving = new AtomicBoolean(false);
    private final Handler mHandler = new Handler();
    private final Runnable mRunner;
    private final int mInterval;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    PeriodicBackgroundUpdater(final Runnable runner, final int interval) {
        mRunner = runner;
        mInterval = interval;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // Runnable
    public void run() {
        while (mIsObserving.get()) {
            try {
                mHandler.post(mRunner);
                Thread.sleep(mInterval);
            } catch (InterruptedException | IllegalStateException e) {
                // If the progress observer fails, just stop the observer.
                // We're rescuing IllegalStateException incase the audio is stopped
                // before this observer is shut down.
                // We'll just refuse to observe anymore.
                release();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    boolean isObserving() {
        return mIsObserving.get();
    }

    /**
     * Start this updater.
     * This starts the background thread, i.e. begins running this class's run function.
     */
    synchronized void start() {
        // If the thread is already running, we want to avoid IllegalThreadStateException.
        if (mBackgroundThread != null && mBackgroundThread.isAlive()) return;
        if (isObserving()) return;

        mIsObserving.set(true);

        if (mBackgroundThread == null || !mBackgroundThread.isAlive() || mBackgroundThread.isInterrupted()) {
            mBackgroundThread = new Thread(this);
        }

        mBackgroundThread.start();
    }

    /**
     * Stop the updater and interrupt the background thread.
     */
    synchronized void release() {
        mIsObserving.set(false);

        if (mBackgroundThread != null && !mBackgroundThread.isInterrupted()) {
            try {
                mBackgroundThread.interrupt();
            } catch(Exception e) {
                // This would occur if two threads tried to release this at the same time.
                // Several sources can release this thread and they might happen at the same time.
                // I added synchronization but this is a really lame reason for the app to
                // crash so better safe than sorry.
            }
        }

        mBackgroundThread = null;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Runnable Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * A Runnable class to use for static progress bars (eg OnDemand, Preroll).
     */
    static class ProgressBarRunner implements Runnable {
        private final Stream mStream;

        ProgressBarRunner(final Stream stream) {
            mStream = stream;
        }

        @Override
        public void run() {
            long millis = mStream.getCurrentPosition();
            mStream.getAudioEventListener().onProgress((int)millis);
        }
    }


    /**
     * A Runnable class to use for the sleep timer.
     * It calculates the current hours, minutes, and seconds remaining until sleep and passes it
     * to the handler.
     */
    public static abstract class TimerRunner implements Runnable {
        @Override
        public void run() {
            final long sleepUntilMillis = DataManager.getInstance().getTimerMillis();
            long now = SystemClock.elapsedRealtime();
            long diff = sleepUntilMillis - now;

            if (diff <= 0) {
                onTimerComplete();
                return;
            }

            int totalSeconds = (int) diff/1000;
            int totalMinutes = totalSeconds / 60;

            int secs = totalSeconds % 60;
            int mins = totalMinutes % 60;
            int hours = totalMinutes / 60;

            // onUpdate
            onTimerUpdate(hours, mins, secs);
        }

        public abstract void onTimerUpdate(int hours, int mins, int secs);

        public abstract void onTimerComplete();
    }
}
