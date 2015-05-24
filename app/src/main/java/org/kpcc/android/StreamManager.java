package org.kpcc.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamManager extends Service {
    private final IBinder mBinder = new LocalBinder();

    public EpisodeStream currentEpisodePlayer;
    public LiveStream currentLivePlayer;
    public PrerollStream currentPrerollPlayer;

    public static String getTimeFormat(int seconds) {
        return String.format("%01d:%02d:%02d",
                TimeUnit.SECONDS.toHours(seconds),
                TimeUnit.SECONDS.toMinutes(seconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.SECONDS.toSeconds(seconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }

    public void releaseAllActiveStreams() {
        if (currentEpisodePlayer != null) currentEpisodePlayer.release();
        if (currentLivePlayer != null) currentLivePlayer.release();
        if (currentPrerollPlayer != null) currentPrerollPlayer.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private static class AudioFocusNotGrantedException extends Exception {
    }

    public abstract static class AudioEventListener {
        public abstract void onLoading();

        public void onPrepared() {
        }

        public abstract void onPlay();

        public abstract void onPause();

        public abstract void onStop();

        public abstract void onCompletion();

        public void onProgress(int progress) {
        }

        public abstract void onError();

        public void onBufferingUpdate(int percent) {
        }

        public void onPrerollData(PrerollManager.PrerollData prerollData) {
        }
    }

    public abstract static class BaseStream {
        public final MediaPlayer audioPlayer;
        final Context mContext;
        final AudioManager mAudioManager;
        final AtomicBoolean mIsDucking = new AtomicBoolean(false);
        final AudioManager.OnAudioFocusChangeListener mAfChangeListener;
        final StreamManager mStreamManager;
        AudioEventListener mAudioEventListener;
        ProgressObserver mProgressObserver;
        Thread mProgressThread;
        final AtomicBoolean isPrepared = new AtomicBoolean(false);
        final AtomicBoolean mDidPauseForAudioLoss = new AtomicBoolean(false);
        final AtomicBoolean mPlayingOnDisconnect = new AtomicBoolean(false);

        public BaseStream(Context context) {
            audioPlayer = new MediaPlayer();
            audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mContext = context;
            mStreamManager = AppConnectivityManager.instance.streamManager;
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            mAfChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            audioFocusLossTransientCanDuck();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            audioFocusLossTransient();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            audioFocusGain();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            audioFocusLoss();
                            break;
                    }
                }
            };
        }

        public void setOnAudioEventListener(AudioEventListener eventListener) {
            mAudioEventListener = eventListener;
        }

        public void onPrerollComplete() {
        }

        public abstract void start();

        public abstract void stop();

        public abstract void pause();

        public abstract void release();

        public boolean isPlaying() {
            return audioPlayer.isPlaying();
        }

        public void reset() { audioPlayer.reset(); }

        void requestAudioFocus() throws AudioFocusNotGrantedException {
            int result = mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                throw new AudioFocusNotGrantedException();
            }
        }

        void audioFocusLossTransientCanDuck() {
            duckStream();
        }

        void audioFocusLossTransient() {
            if (isPlaying()) {
                mDidPauseForAudioLoss.set(true);
                pause();
            }
        }

        void audioFocusGain() {
            // NOTE: This is overridden in LiveStream
            if (mIsDucking.get() && isPlaying()) {
                unduckStream();
            } else {
                if (mDidPauseForAudioLoss.get()) {
                    start();
                } // Otherwise, just keep it paused.
            }

            // Always do this just to be safe.
            mDidPauseForAudioLoss.set(false);
        }

        void audioFocusLoss() {
            mDidPauseForAudioLoss.set(false);
            release();
            mAudioManager.abandonAudioFocus(mAfChangeListener);
        }

        void duckStream() {
            if (!mIsDucking.get() && isPlaying()) {
                mIsDucking.set(true);

                if (isPlaying()) {
                    audioPlayer.setVolume(0.25f, 0.25f);
                }
            }
        }

        void unduckStream() {
            if (mIsDucking.get() && isPlaying()) {
                mIsDucking.set(false);

                if (isPlaying()) {
                    audioPlayer.setVolume(1.0f, 1.0f);
                }
            }
        }

        void startProgressObserver() {
            stopProgressObserver();
            mProgressObserver.start();

            mProgressThread = new Thread(mProgressObserver);
            mProgressThread.start();
        }

        void stopProgressObserver() {
            if (mProgressObserver != null) {
                mProgressObserver.stop();
            }

            if (mProgressThread != null && !mProgressThread.isInterrupted()) {
                mProgressThread.interrupt();
            }

            mProgressThread = null;
        }

        void resetProgressObserver() {
            stopProgressObserver();
            mProgressObserver = new StreamManager.ProgressObserver(audioPlayer, mAudioEventListener);

            if (isPlaying()) {
                startProgressObserver();
            }
        }
    }

    public static class EpisodeStream extends BaseStream {
        public final String audioUrl;
        public final String programSlug;
        public final int durationSeconds;
        boolean isPaused = false;

        public EpisodeStream(String audioUrl, String programSlug, int durationSeconds, Context context) {
            super(context);
            this.audioUrl = audioUrl;
            this.durationSeconds = durationSeconds;
            this.programSlug = programSlug;
        }

        @Override
        public void setOnAudioEventListener(AudioEventListener eventListener) {
            mAudioEventListener = eventListener;
            resetProgressObserver();
        }

        public void play() {
            if (mStreamManager != null) {
                mStreamManager.currentEpisodePlayer = this;
            }

            if (isPaused) {
                start();
                return;
            }

            mAudioEventListener.onLoading();

            try {
                requestAudioFocus();
            } catch (AudioFocusNotGrantedException e) {
                mAudioEventListener.onError();
                return;
            }

            try {
                audioPlayer.setDataSource(audioUrl);
            } catch (IOException | IllegalStateException e) {
                mAudioEventListener.onError();
                return;
            }

            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mAudioEventListener.onCompletion();
                    release();
                }
            });

            audioPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mAudioEventListener.onBufferingUpdate(percent);
                }
            });

            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isPrepared.set(true);
                    start();
                }
            });

            audioPlayer.prepareAsync();
        }

        public long getCurrentPosition() throws IllegalStateException {
            return audioPlayer.getCurrentPosition();
        }

        public void seekTo(int pos) {
            try {
                // If it's not prepared, it'll transfer to the Error state, call onCompletion(),
                // and mess everything up. Since seeking is a UI action that can be interacted with
                // before the player is ready, we need to just not do anything if it's not ready.
                if (isPrepared.get()) {
                    audioPlayer.seekTo(pos);
                }
            } catch (IllegalStateException e) {
                release();
            }
        }

        @Override
        public void start() {
            try {
                mAudioEventListener.onPlay();
                startProgressObserver();
                isPaused = false;
                audioPlayer.start();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        @Override
        public void pause() {
            try {
                mAudioEventListener.onPause();
                stopProgressObserver();
                isPaused = true;
                audioPlayer.pause();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        @Override
        public void stop() {
            try {
                mAudioEventListener.onStop();
                stopProgressObserver();
                audioPlayer.stop();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        @Override
        public void release() {
            if (mStreamManager != null) {
                if (mStreamManager.currentEpisodePlayer == this) {
                    mStreamManager.currentEpisodePlayer = null;
                }
            }

            mAudioEventListener.onStop();
            stopProgressObserver();

            isPaused = false;
            audioPlayer.release();
        }
    }

    public static class LiveStream extends BaseStream {
        // We get preroll directly from Triton so we always use the skip-preroll url.
        // Also send the UA for logs.
        // Using Uri.parse because there seems to be a bug in Lollipop that causes
        // the stream to take 20 seconds to start playing. Uri.parse is a workaround.
        public final static String LIVESTREAM_URL =
                "http://live.scpr.org/kpcclive?preskip=true&ua=KPCCAndroid-" +
                BuildConfig.VERSION_NAME + "-" +
                String.valueOf(BuildConfig.VERSION_CODE);

        private final static String PREF_USER_PLAYED_LIVESTREAM = "live_stream_played";
        private AudioEventListener mPrerollAudioEventListener;

        public LiveStream(Context context) {
            super(context);

            if (mStreamManager != null) {
                mStreamManager.currentLivePlayer = this;
            }
        }

        public void setPrerollAudioEventListener(AudioEventListener eventListener) {
            mPrerollAudioEventListener = eventListener;
            resetProgressObserver();
        }

        public void playWithPrerollAttempt() {
            mAudioEventListener.onLoading();

            // If they just installed the app (less than 10 minutes ago), and have never played the live
            // stream, don't play preroll.
            // Otherwise do the normal preroll flow.
            final long now = System.currentTimeMillis();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());

            boolean hasPlayedLiveStream = prefs.getBoolean(PREF_USER_PLAYED_LIVESTREAM, false);
            boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
            boolean heardPrerollRecently = PrerollManager.LAST_PREROLL_PLAY > (now - PrerollManager.PREROLL_THRESHOLD);

            if ((!hasPlayedLiveStream && installedRecently) || heardPrerollRecently) {
                // Skipping Preroll
                prepareAndStart();
            } else {
                // Normal preroll flow (preroll still may not play, based on Triton response)
                PrerollManager.instance.getPrerollData(mContext, new PrerollManager.PrerollCallbackListener() {
                    @Override
                    public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                        if (prerollData == null || prerollData.audioUrl == null) {
                            prepareAndStart();
                        } else {
                            mPrerollAudioEventListener.onPrerollData(prerollData);

                            PrerollStream preroll = new PrerollStream(mContext,
                                    prerollData, LiveStream.this);

                            preroll.setOnAudioEventListener(mPrerollAudioEventListener);
                            preroll.play();
                        }
                    }
                });
            }

            if (!hasPlayedLiveStream) {
                prefs.edit().putBoolean(PREF_USER_PLAYED_LIVESTREAM, true).apply();
            }
        }

        @Override
        public void onPrerollComplete() {
            prepareAndStart();
        }

        @Override
        void audioFocusGain() {
            // Since we STOP the stream, we can't just start() it again. We need to prepare it
            // first. So we're overriding this method.
            if (mIsDucking.get() && isPlaying()) {
                unduckStream();
            } else {
                if (mDidPauseForAudioLoss.get()) {
                    prepareAndStart();
                }
            }

            // Always do this just to be safe.
            mDidPauseForAudioLoss.set(false);
        }

        @Override
        public void start() {
            try {
                mAudioEventListener.onPlay();
                audioPlayer.start();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        @Override
        public void pause() {
            stop();
        }

        @Override
        public void stop() {
            try {
                mAudioEventListener.onStop();
                audioPlayer.stop();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        @Override
        public void release() {
            if (mStreamManager != null) {
                // We don't want to call stop here, because in the case where the live stream was
                // stopped and started again in the same screen, the audio focus is lost at the same
                // time that the new player is being setup, and the button states collide.
                if (mStreamManager.currentLivePlayer == this) {
                    // The currentLivePlayer may have already been taken over.
                    // If not, clear this one out.
                    mStreamManager.currentLivePlayer = null;
                }
            }

            audioPlayer.release();
        }

        public void prepareAndStart() {
            // Since this method can be called directly, we need to trigger loading state here too.
            mAudioEventListener.onLoading();

            try {
                audioPlayer.setDataSource(LIVESTREAM_URL);
                requestAudioFocus();
            } catch (IOException | IllegalStateException | AudioFocusNotGrantedException e) {
                mAudioEventListener.onError();
                return;
            }

            // When the network changes, the stream will eventually "complete" because of the
            // gap in data. So we just reboot the stream. It's not ideal but at least it restarts
            // by itself instead of just stopping all together.
            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stop();
                    reset();
                    prepareAndStart();
                }
            });

            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isPrepared.set(true);
                    start();
                }
            });

            prepare();
        }

        private void prepare() {
            try {
                audioPlayer.prepareAsync();
            } catch (IllegalStateException e) {
                mAudioEventListener.onError();
            }
        }
    }

    public static class PrerollStream extends BaseStream {
        private final BaseStream mParentStream;
        private final PrerollManager.PrerollData mPrerollData;

        public PrerollStream(Context context,
                             PrerollManager.PrerollData prerollData,
                             BaseStream parentStream) {
            super(context);

            mParentStream = parentStream;
            mPrerollData = prerollData;

            if (mStreamManager != null) {
                mStreamManager.currentPrerollPlayer = this;
            }
        }

        @Override
        public void setOnAudioEventListener(AudioEventListener eventListener) {
            mAudioEventListener = eventListener;
            resetProgressObserver();
        }

        public void play() {
            mAudioEventListener.onLoading();

            try {
                requestAudioFocus();
            } catch (AudioFocusNotGrantedException e) {
                // No preroll.
                return;
            }

            try {
                audioPlayer.setDataSource(mPrerollData.audioUrl);
            } catch (IOException | IllegalStateException e) {
                // No preroll.
                return;
            }

            audioPlayer.prepareAsync();

            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isPrepared.set(true);
                    mAudioEventListener.onPrepared();
                    PrerollManager.LAST_PREROLL_PLAY = System.currentTimeMillis();
                    start();
                }
            });

            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mAudioEventListener.onCompletion();
                    mParentStream.onPrerollComplete();
                }
            });
        }

        @Override
        public void start() {
            try {
                audioPlayer.start();
                mAudioEventListener.onPlay();
                startProgressObserver();
            } catch (IllegalStateException e) {
                // Nothing to do, player has been released.
            }
        }

        @Override
        public void pause() {
            try {
                audioPlayer.pause();
                mAudioEventListener.onPause();
                stopProgressObserver();
            } catch (IllegalStateException e) {
                // Nothing to do, player has been released.
            }
        }

        @Override
        public void stop() {
            try {
                audioPlayer.stop();
                mAudioEventListener.onStop();
                stopProgressObserver();
            } catch (IllegalStateException e) {
                // Nothing to do, player has been released.
            }
        }

        @Override
        public void release() {
            if (mStreamManager != null) {
                if (mStreamManager.currentPrerollPlayer == this) {
                    mStreamManager.currentPrerollPlayer = null;
                }
            }

            mAudioEventListener.onStop();
            stopProgressObserver();
            audioPlayer.release();
        }
    }


    public static class ProgressObserver implements Runnable {
        private final AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private final Handler mHandler = new Handler();
        private final MediaPlayer mMediaPlayer;
        private final AudioEventListener mAudioEventListener;

        public ProgressObserver(MediaPlayer mediaPlayer, AudioEventListener eventListener) {
            mMediaPlayer = mediaPlayer;
            mAudioEventListener = eventListener;
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
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int millis = mMediaPlayer.getCurrentPosition();
                                mAudioEventListener.onProgress(millis);
                            } catch (IllegalStateException e) {
                                // We're rescuing IllegalStateException incase the audio is stopped
                                // before this observer is shut down.
                                // We'll just refuse to observe anymore.
                                mIsObserving.set(false);
                            }
                        }
                    });

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // If the progress observer fails, just stop the observer.
                    mIsObserving.set(false);
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public StreamManager getService() {
            return StreamManager.this;
        }
    }
}
