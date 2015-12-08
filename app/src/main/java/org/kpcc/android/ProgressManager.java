package org.kpcc.android;

import android.os.Handler;
import android.os.SystemClock;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rickb014 on 8/9/15.
 */
public class ProgressManager {
    private final ProgressObserver observer;
    private Thread progressThread;

    public ProgressManager(Runnable runner, int interval) {
        this.observer = new ProgressObserver(runner, interval);
    }

    boolean isObserving() {
        return observer.isObserving();
    }

    void start() {
        // If the thread is already running, we want to avoid IllegalThreadStateException.
        if (progressThread != null && progressThread.isAlive()) {
            return;
        }

        if (observer.isObserving()) {
            return;
        }

        observer.start();

        if (progressThread == null || progressThread.isInterrupted()) {
            progressThread = new Thread(observer);
        }

        progressThread.start();
    }

    void release() {
        observer.stop();

        if (progressThread != null && !progressThread.isInterrupted()) {
            progressThread.interrupt();
        }

        progressThread = null;
    }

    public class ProgressObserver implements Runnable {
        private final AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private final Handler mHandler = new Handler();
        private final Runnable runner;
        private final int interval;

        public ProgressObserver(Runnable runner, int interval) {
            this.runner = runner;
            this.interval = interval;
        }

        public boolean isObserving() {
            return mIsObserving.get();
        }

        public void start() {
            mIsObserving.set(true);
        }

        public void stop() {
            mIsObserving.set(false);
        }

        @Override
        public void run() {
            while (mIsObserving.get()) {
                try {
                    mHandler.post(this.runner);
                    Thread.sleep(this.interval);
                } catch (InterruptedException | IllegalStateException e) {
                    // If the progress observer fails, just stop the observer.
                    // We're rescuing IllegalStateException incase the audio is stopped
                    // before this observer is shut down.
                    // We'll just refuse to observe anymore.
                    stop();
                }
            }
        }
    }

    public static class ProgressBarRunner implements Runnable {
        protected final StreamManager.BaseStream stream;

        public ProgressBarRunner(StreamManager.BaseStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            long millis = stream.getCurrentPosition();
            stream.audioEventListener.onProgress((int)millis);
        }
    }

    public static abstract class TimerRunner implements Runnable {
        @Override
        public void run() {
            final long sleepUntilMillis = DataManager.instance.getTimerMillis();
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
