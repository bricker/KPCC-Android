package org.kpcc.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;

import com.android.volley.toolbox.NetworkImageView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamManager extends Service {
    public static final String TAG = "kpcc.StreamManager";
    private final IBinder mBinder = new LocalBinder();

    public EpisodeStream currentEpisodePlayer;
    public LiveStream currentLivePlayer;
    public PrerollStream currentPrerollPlayer;

    public static String getTimeFormat(int seconds) {
        return String.format("%02d:%02d:%02d",
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

    public static class AudioFocusNotGrantedException extends Exception {
    }

    public abstract static class AudioEventListener {
        public abstract void onLoading();

        public abstract void onPlay();

        public abstract void onPause();

        public abstract void onStop();

        public abstract void onProgress(int progress);

        public abstract void onError();

        public void onPrerollData(PrerollManager.PrerollData prerollData) {
        }
    }

    public abstract static class BaseStream {
        public MediaPlayer audioPlayer;
        protected MainActivity mActivity;
        protected AudioManager mAudioManager;
        protected AtomicBoolean mIsDucking = new AtomicBoolean(false);
        protected AudioManager.OnAudioFocusChangeListener mAfChangeListener;
        protected AudioEventListener mAudioEventListener;
        protected ProgressObserver mProgressObserver;

        public BaseStream(Context context) {
            audioPlayer = new MediaPlayer();
            audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mActivity = (MainActivity) context;
            mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);

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

        protected void requestAudioFocus() throws AudioFocusNotGrantedException {
            int result = mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                throw new AudioFocusNotGrantedException();
            }
        }

        protected void audioFocusLossTransientCanDuck() {
            duckStream();
        }

        protected void audioFocusLossTransient() {
            pause();
        }

        protected void audioFocusGain() {
            if (mIsDucking.get() && audioPlayer.isPlaying()) {
                unduckStream();
            } else {
                start();
            }
        }

        protected void audioFocusLoss() {
            release();
            mAudioManager.abandonAudioFocus(mAfChangeListener);
        }

        protected void duckStream() {
            if (!mIsDucking.get()) {
                mIsDucking.set(true);

                if (audioPlayer.isPlaying()) {
                    audioPlayer.setVolume(0.25f, 0.25f);
                }
            }
        }

        protected void unduckStream() {
            if (mIsDucking.get()) {
                mIsDucking.set(false);

                if (audioPlayer.isPlaying()) {
                    audioPlayer.setVolume(1.0f, 1.0f);
                }
            }
        }

        protected void startProgressObserver() {
            mProgressObserver.start();
            new Thread(mProgressObserver).start();
        }

        protected void stopProgressObserver() {
            if (mProgressObserver != null) {
                mProgressObserver.stop();
            }
        }

        protected void resetProgressObserver() {
            stopProgressObserver();
            mProgressObserver = new StreamManager.ProgressObserver(audioPlayer, mAudioEventListener);

            if (audioPlayer.isPlaying()) {
                startProgressObserver();
            }
        }
    }

    public static class EpisodeStream extends BaseStream {
        public String audioUrl;
        private EpisodeFragment.EpisodeAudioCompletionCallback mCompletionCallback;
        private boolean isPaused = false;

        public EpisodeStream(String audioUrl,
                             Context context,
                             EpisodeFragment.EpisodeAudioCompletionCallback completionCallback) {

            super(context);
            this.audioUrl = audioUrl;
            mCompletionCallback = completionCallback;

            if (mActivity.streamIsBound()) {
                mActivity.streamManager.currentEpisodePlayer = this;
            }
        }

        @Override
        public void setOnAudioEventListener(AudioEventListener eventListener) {
            mAudioEventListener = eventListener;
            resetProgressObserver();
        }

        public void play() {
            if (!mActivity.streamIsBound()) return;

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
            } catch (IOException e) {
                mAudioEventListener.onError();
                return;
            }

            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mCompletionCallback.onCompletion();
                    release();
                }
            });

            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    start();
                    startProgressObserver();
                }
            });

            audioPlayer.prepareAsync();
        }

        public long getCurrentPosition() {
            return audioPlayer.getCurrentPosition();
        }

        @Override
        public void start() {
            mAudioEventListener.onPlay();
            startProgressObserver();
            isPaused = false;
            audioPlayer.start();
        }

        @Override
        public void pause() {
            mAudioEventListener.onPause();
            stopProgressObserver();
            isPaused = true;
            audioPlayer.pause();
        }

        @Override
        public void stop() {
            mAudioEventListener.onStop();
            stopProgressObserver();
            audioPlayer.stop();
        }

        @Override
        public void release() {
            if (mActivity.streamManager.currentEpisodePlayer == this) {
                mActivity.streamManager.currentEpisodePlayer = null;
            }

            mAudioEventListener.onStop();
            stopProgressObserver();
            audioPlayer.release();
        }
    }

    public static class LiveStream extends BaseStream {
        // We get preroll directly from Triton so we always use the skip-preroll url.
        public final static String LIVESTREAM_URL = "http://live.scpr.org/kpcclive?preskip=true";
        private final static String PREF_USER_PLAYED_LIVESTREAM = "live_stream_played";
        private NetworkImageView mAdView;
        private AudioEventListener mPrerollAudioEventListener;

        public LiveStream(Context context,
                          NetworkImageView adView) {
            super(context);
            mAdView = adView;

            if (mActivity.streamIsBound()) {
                mActivity.streamManager.currentLivePlayer = this;
            }
        }

        public void setPrerollAudioEventListener(AudioEventListener eventListener) {
            mPrerollAudioEventListener = eventListener;
            resetProgressObserver();
        }

        public void playWithPrerollAttempt() {
            if (!mActivity.streamIsBound()) return;
            mAudioEventListener.onLoading();

            // If they just installed the app (less than 10 minutes ago), and have never played the live
            // stream, don't play preroll.
            // Otherwise do the normal preroll flow.
            final long now = System.currentTimeMillis();
            SharedPreferences sharedPref = mActivity.getPreferences(Context.MODE_PRIVATE);

            boolean hasPlayedLiveStream = sharedPref.getBoolean(PREF_USER_PLAYED_LIVESTREAM, false);
            boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
            boolean heardPrerollRecently = PrerollManager.LAST_PREROLL_PLAY > (now - PrerollManager.PREROLL_THRESHOLD);

            if ((!hasPlayedLiveStream && installedRecently) || heardPrerollRecently) {
                // Skipping Preroll
                prepareAndStart();
            } else {
                // Normal preroll flow (preroll still may not play, based on Triton response)
                PrerollManager.instance.getPrerollData(mActivity, new PrerollManager.PrerollCallbackListener() {
                    @Override
                    public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                        if (prerollData == null || prerollData.audioUrl == null) {
                            prepareAndStart();
                        } else {
                            mPrerollAudioEventListener.onPrerollData(prerollData);

                            PrerollStream preroll = new PrerollStream(mActivity,
                                    mAdView, prerollData, LiveStream.this);

                            preroll.setOnAudioEventListener(mPrerollAudioEventListener);
                            preroll.playAndShowAsset();
                        }
                    }
                });
            }

            if (!hasPlayedLiveStream) {
                sharedPref.edit().putBoolean(PREF_USER_PLAYED_LIVESTREAM, true).apply();
            }
        }

        @Override
        public void onPrerollComplete() {
            prepareAndStart();
        }

        @Override
        public void start() {
            mAudioEventListener.onPlay();
            audioPlayer.start();
        }

        @Override
        public void pause() {
            stop();
        }

        @Override
        public void stop() {
            mAudioEventListener.onStop();
            audioPlayer.stop();
        }

        @Override
        public void release() {
            // We don't want to call stop here, because in the case where the live stream was
            // stopped and started again in the same screen, the audio focus is lost at the same
            // time that the new player is being setup, and the button states collide.

            if (mActivity.streamManager.currentLivePlayer == this) {
                // The currentLivePlayer may have already been taken over.
                // If not, clear this one out.
                mActivity.streamManager.currentLivePlayer = null;
            }

            audioPlayer.release();
        }

        private void prepareAndStart() {
            try {
                requestAudioFocus();
            } catch (AudioFocusNotGrantedException e) {
                mAudioEventListener.onError();
                return;
            }

            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    start();
                }
            });
            prepare();
        }

        private void prepare() {
            try {
                audioPlayer.setDataSource(LIVESTREAM_URL);
                audioPlayer.prepareAsync();
            } catch (IOException e) {
                mAudioEventListener.onError();
            }
        }
    }

    public static class PrerollStream extends BaseStream {
        private NetworkImageView mAdView;
        private BaseStream mParentStream;
        private PrerollManager.PrerollData mPrerollData;

        public PrerollStream(Context context,
                             NetworkImageView adView,
                             PrerollManager.PrerollData prerollData,
                             BaseStream parentStream) {
            super(context);

            mParentStream = parentStream;
            mPrerollData = prerollData;
            mAdView = adView;

            if (mActivity.streamIsBound()) {
                mActivity.streamManager.currentPrerollPlayer = this;
            }
        }

        @Override
        public void setOnAudioEventListener(AudioEventListener eventListener) {
            mAudioEventListener = eventListener;
            resetProgressObserver();
        }

        public void playAndShowAsset() {
            if (mPrerollData.assetUrl != null) {
                NetworkImageManager.instance.setPrerollImage(mAdView, mPrerollData.assetUrl);

                if (mPrerollData.impressionUrl != null) {
                    HttpRequest.ImpressionRequest.get(mPrerollData.impressionUrl);
                }

                mAdView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(mPrerollData.assetClickUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                        if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                            mActivity.startActivity(intent);
                        }

                        if (mPrerollData.trackingUrl != null) {
                            HttpRequest.ImpressionRequest.get(mPrerollData.trackingUrl);
                        }
                    }
                });
            }

            try {
                requestAudioFocus();
            } catch (AudioFocusNotGrantedException e) {
                // No preroll.
                return;
            }

            try {
                audioPlayer.setDataSource(mPrerollData.audioUrl);
            } catch (IOException e) {
                // No preroll.
                return;
            }

            audioPlayer.prepareAsync();

            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    PrerollManager.LAST_PREROLL_PLAY = System.currentTimeMillis();
                    mActivity.getNavigationDrawerFragment().disableDrawer();
                    start();
                }
            });

            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mActivity.getNavigationDrawerFragment().enableDrawer();
                    mAdView.setVisibility(View.GONE);
                    mParentStream.onPrerollComplete();
                }
            });
        }

        @Override
        public void start() {
            mAudioEventListener.onPlay();
            startProgressObserver();
            audioPlayer.start();
        }

        @Override
        public void pause() {
            mAudioEventListener.onPause();
            stopProgressObserver();
            audioPlayer.pause();
        }

        @Override
        public void stop() {
            mAudioEventListener.onStop();
            stopProgressObserver();
            audioPlayer.stop();
        }

        @Override
        public void release() {
            if (mActivity.streamManager.currentPrerollPlayer == this) {
                mActivity.streamManager.currentPrerollPlayer = null;
            }

            mAudioEventListener.onStop();
            stopProgressObserver();
            audioPlayer.release();
        }
    }


    public static class ProgressObserver implements Runnable {
        private AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private Handler mHandler = new Handler();
        private MediaPlayer mMediaPlayer;
        private AudioEventListener mAudioEventListener;

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

    public static class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    public class LocalBinder extends Binder {
        public StreamManager getService() {
            return StreamManager.this;
        }
    }
}
