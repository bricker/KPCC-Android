package org.kpcc.android;

import android.app.Service;
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

    public static class EpisodeStream {
        public String audioUrl;
        private MediaPlayer mAudioPlayer = new MediaPlayer();
        private MainActivity mActivity;
        private EpisodeFragment.EpisodeAudioCompletionCallback mCompletionCallback;
        private AudioManager mAudioManager;
        private AtomicBoolean mIsDucking = new AtomicBoolean(false);
        private float mPreviousVolume = 0;
        private AudioManager.OnAudioFocusChangeListener mAfChangeListener;
        private ProgressObserver mProgressObserver;
        private boolean isPaused = false;
        private AudioEventListener mAudioEventListener;

        public EpisodeStream(String audioUrl,
                             Context context,
                             EpisodeFragment.EpisodeAudioCompletionCallback completionCallback) {

            mAudioPlayer = new MediaPlayer();
            mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            this.audioUrl = audioUrl;
            mCompletionCallback = completionCallback;
            mActivity = (MainActivity) context;
            mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);

            mAfChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            duckStream();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (mIsDucking.get() && mAudioPlayer.isPlaying()) {
                                unduckStream();
                            } else {
                                start();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            release();
                            mAudioManager.abandonAudioFocus(this);
                            break;
                    }
                }
            };

            if (mActivity.streamManager != null) {
                mActivity.streamManager.currentEpisodePlayer = this;
            }
        }

        public void setOnAudioEventListener(AudioEventListener eventListener) {
            mAudioEventListener = eventListener;

            stopProgressObserver();
            mProgressObserver = new StreamManager.ProgressObserver(mAudioPlayer, mAudioEventListener);
            startProgressObserver();
        }

        public void play() {
            if (!mActivity.streamIsBound()) return;

            if (isPaused) {
                start();
                return;
            }

            mAudioEventListener.onLoading();

            int result = mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // TODO: Show an error
                return;
            }

            try {
                mAudioPlayer.setDataSource(audioUrl);
            } catch (IOException e) {
                // TODO: Handle errors
                return;
            }

            mAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mCompletionCallback.onCompletion();
                    release();
                }
            });

            mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    start();
                    startProgressObserver();
                }
            });

            mAudioPlayer.prepareAsync();
        }

        public boolean isPlaying() {
            return mAudioPlayer.isPlaying();
        }

        public long getCurrentPosition() {
            return mAudioPlayer.getCurrentPosition();
        }

        public void start() {
            mAudioEventListener.onPlay();
            startProgressObserver();
            isPaused = false;
            mAudioPlayer.start();
        }

        public void pause() {
            mAudioEventListener.onPause();
            stopProgressObserver();
            isPaused = true;
            mAudioPlayer.pause();
        }

        public void stop() {
            mAudioEventListener.onStop();
            stopProgressObserver();
            mAudioPlayer.stop();
        }

        public void release() {
            if (mActivity.streamManager.currentEpisodePlayer == this) {
                mActivity.streamManager.currentEpisodePlayer = null;
            }

            stop();
            mAudioPlayer.release();
        }

        private void startProgressObserver() {
            mProgressObserver.start();
            new Thread(mProgressObserver).start();
        }

        private void stopProgressObserver() {
            if (mProgressObserver != null) {
                mProgressObserver.stop();
            }
        }

        private void duckStream() {
            if (!mIsDucking.get()) {
                mIsDucking.set(true);
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                if (mAudioPlayer.isPlaying()) {
                    mAudioPlayer.setVolume(mPreviousVolume / 2, mPreviousVolume / 2);
                }
            }
        }

        private void unduckStream() {
            if (mIsDucking.get()) {
                mIsDucking.set(false);

                if (mAudioPlayer.isPlaying()) {
                    mAudioPlayer.setVolume(mPreviousVolume, mPreviousVolume);
                    mPreviousVolume = 0;
                }
            }
        }

        public abstract static class AudioEventListener {
            public abstract void onLoading();

            public abstract void onPlay();

            public abstract void onPause();

            public abstract void onStop();

            public abstract void onProgress(int progress);
        }
    }

    public static class LiveStream {
        // We get preroll directly from Triton so we always use the skip-preroll url.
        public final static String LIVESTREAM_URL = "http://live.scpr.org/kpcclive?preskip=true";
        private final static String PREF_USER_PLAYED_LIVESTREAM = "live_stream_played";

        public MediaPlayer audioPlayer = new MediaPlayer();
        private MainActivity mActivity;
        private AudioButtonManager mAudioButtonManager;
        private NetworkImageView mAdView;
        private AudioManager mAudioManager;
        private AtomicBoolean mIsDucking = new AtomicBoolean(false);
        private float mPreviousVolume = 0;
        private AudioManager.OnAudioFocusChangeListener mAfChangeListener;

        public LiveStream(Context context,
                          AudioButtonManager audioButtonManager,
                          NetworkImageView adView) {

            audioPlayer = new MediaPlayer();
            audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mActivity = (MainActivity) context;
            mAudioButtonManager = audioButtonManager;
            mAdView = adView;
            mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);

            mAfChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            duckStream();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            stop();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (mIsDucking.get() && audioPlayer.isPlaying()) {
                                unduckStream();
                            } else {
                                start();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            release();
                            mAudioManager.abandonAudioFocus(this);
                            break;
                    }
                }
            };

            if (mActivity.streamManager != null) {
                mActivity.streamManager.currentLivePlayer = this;
            }
        }

        public void playWithPrerollAttempt() {
            if (!mActivity.streamIsBound()) return;
            mAudioButtonManager.toggleLoading();

            int result = mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // TODO: Show an error
                return;
            }

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
                            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    // Don't start manually, nextMediaPlayer() will do that for us.
                                    mAudioButtonManager.togglePlayingForStop();
                                }
                            });

                            prepare();

                            PrerollStream preroll = new PrerollStream(mActivity,
                                    mAudioButtonManager, mAdView, prerollData, LiveStream.this);

                            preroll.playAndShowAsset();
                        }
                    }
                });
            }

            if (!hasPlayedLiveStream) {
                sharedPref.edit().putBoolean(PREF_USER_PLAYED_LIVESTREAM, true).apply();
            }
        }

        public boolean isPlaying() {
            return audioPlayer.isPlaying();
        }

        public void stop() {
            mAudioButtonManager.toggleStopped();
            audioPlayer.stop();
        }

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

        public void start() {
            mAudioButtonManager.togglePlayingForStop();
            audioPlayer.start();
        }

        private void prepareAndStart() {
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
                // TODO: Handle errors
                return;
            }
        }

        private void duckStream() {
            if (!mIsDucking.get()) {
                mIsDucking.set(true);
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                if (audioPlayer.isPlaying()) {
                    audioPlayer.setVolume(mPreviousVolume / 2, mPreviousVolume / 2);
                }
            }
        }

        private void unduckStream() {
            if (mIsDucking.get()) {
                mIsDucking.set(false);

                if (audioPlayer.isPlaying()) {
                    audioPlayer.setVolume(mPreviousVolume, mPreviousVolume);
                    mPreviousVolume = 0;
                }
            }
        }
    }

    public static class PrerollStream {
        private MediaPlayer mAudioPlayer = new MediaPlayer();
        private MainActivity mActivity;
        private NetworkImageView mAdView;
        private AudioButtonManager mAudioButtonManager;
        private AudioManager mAudioManager;
        private LiveStream mParentStream;
        private PrerollManager.PrerollData mPrerollData;
        private AtomicBoolean mIsDucking = new AtomicBoolean(false);
        private float mPreviousVolume = 0;
        private AudioManager.OnAudioFocusChangeListener mAfChangeListener;

        public PrerollStream(Context context,
                             AudioButtonManager audioButtonManager,
                             NetworkImageView adView,
                             PrerollManager.PrerollData prerollData,
                             LiveStream parentStream) {

            mAudioPlayer = new MediaPlayer();
            mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mActivity = (MainActivity) context;
            mParentStream = parentStream;
            mPrerollData = prerollData;
            mAdView = adView;
            mAudioButtonManager = audioButtonManager;
            mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);

            mAfChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            duckStream();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (mIsDucking.get() && mAudioPlayer.isPlaying()) {
                                unduckStream();
                            } else {
                                start();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            release();
                            mAudioManager.abandonAudioFocus(this);
                            break;
                    }
                }
            };

            if (mActivity.streamManager != null) {
                mActivity.streamManager.currentPrerollPlayer = this;
            }
        }

        public void playAndShowAsset() {
            if (mPrerollData.assetUrl != null) {
                NetworkImageManager.instance.setPrerollImage(mAdView, mPrerollData.assetUrl);

                mAdView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(mPrerollData.assetClickUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                        if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                            mActivity.startActivity(intent);
                        }
                    }
                });
            }

            int result = mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // TODO: Show an error
                return;
            }

            try {
                mAudioPlayer.setDataSource(mPrerollData.audioUrl);
            } catch (IOException e) {
                // TODO: Handle errors
                return;
            }

            mAudioPlayer.prepareAsync();

            mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    PrerollManager.LAST_PREROLL_PLAY = System.currentTimeMillis();
                    mActivity.getNavigationDrawerFragment().disableDrawer();
                    start();
                    mAudioPlayer.setNextMediaPlayer(mParentStream.audioPlayer);
                }
            });

            mAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mActivity.getNavigationDrawerFragment().enableDrawer();
                    mAdView.setVisibility(View.GONE);
                    // Even though we're technically changing the state for the Live Stream,
                    // and not Preroll, it's the same set of buttons so we can do this here.
                    release();
                    mAudioButtonManager.togglePlayingForStop();
                }
            });
        }

        public void start() {
            mAudioButtonManager.togglePlayingForStop();
            mAudioPlayer.start();
        }

        public void pause() {
            mAudioButtonManager.togglePaused();
            mAudioPlayer.pause();
        }

        public void stop() {
            mAudioButtonManager.toggleStopped();
            mAudioPlayer.stop();
        }

        public void release() {
            stop();

            if (mActivity.streamManager.currentPrerollPlayer == this) {
                mActivity.streamManager.currentPrerollPlayer = null;
            }

            mAudioPlayer.release();
        }

        private void duckStream() {
            if (!mIsDucking.get()) {
                mIsDucking.set(true);
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                if (mAudioPlayer.isPlaying()) {
                    mAudioPlayer.setVolume(mPreviousVolume / 2, mPreviousVolume / 2);
                }
            }
        }

        private void unduckStream() {
            if (mIsDucking.get()) {
                mIsDucking.set(false);

                if (mAudioPlayer.isPlaying()) {
                    mAudioPlayer.setVolume(mPreviousVolume, mPreviousVolume);
                    mPreviousVolume = 0;
                }
            }
        }

    }

    public static class ProgressObserver implements Runnable {
        private AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private Handler mHandler = new Handler();
        private MediaPlayer mMediaPlayer;
        private EpisodeStream.AudioEventListener mAudioEventListener;

        public ProgressObserver(MediaPlayer mediaPlayer, EpisodeStream.AudioEventListener eventListener) {
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
                    // TODO: Handle Errors
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
