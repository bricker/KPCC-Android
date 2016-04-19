package org.kpcc.android;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.MimeTypes;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rickb014 on 4/3/16.
 */
abstract class Stream implements AudioManager.OnAudioFocusChangeListener, AudioPlayer.Listener {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Enums
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected enum AudioFocusState {
        OWNED,
        DUCKING,
        PAUSED,
        LOST
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final String USER_AGENT = "KPCCAndroid";
    private static final float DUCK_VOLUME = 0.25f;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private AudioPlayer mAudioPlayer;
    private final Context mContext;
    private AudioEventListener mAudioEventListener;
    private final StreamManager mStreamManager;
    private final AudioManager mAudioManager;
    private AudioFocusState mAudioFocusState;
    private boolean mIsTemporarilyPaused;
    private final AtomicBoolean mIsPaused = new AtomicBoolean(false); // ExoPlayer doesn't have a proper "paused" state so we keep track of it here.

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static String getTimeFormat(int seconds) {
        return String.format(Locale.ENGLISH, "%01d:%02d:%02d",
                TimeUnit.SECONDS.toHours(seconds),
                TimeUnit.SECONDS.toMinutes(seconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.SECONDS.toSeconds(seconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    Stream(final Context context) {
        mContext = context;
        mStreamManager = StreamManager.ConnectivityManager.getInstance().getStreamManager();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Interface
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates to ExoPlayer
    void prepareAndStart() {
        if (getAudioPlayer() == null) return;

        if (isPaused()) {
            play();
            return;
        }

        if (!requestAudioFocus()) {
            getAudioEventListener().onError();
            return;
        }

        prepare();
        play();
    }

    void play() {
        if (getAudioPlayer() == null) return;

        try {
            mIsPaused.set(false);
            getAudioPlayer().getPlayerControl().start();
        } catch (IllegalStateException e) {
            // Call release() to stop progress observer, update button state, etc.
            release();
        }
    }

    void pause() {
        if (getAudioPlayer() == null) return;

        try {
            mIsPaused.set(true);
            getAudioPlayer().getPlayerControl().pause();
        } catch (IllegalStateException e) {
            // Call release() to stop progress observer, update button state, etc.
            release();
        }
    }

    void stop() {
        getAudioManager().abandonAudioFocus(this);
        mIsPaused.set(false);
        if (getAudioPlayer() == null) return;
        release();
    }

    void release() {
        getAudioManager().abandonAudioFocus(this);
        mIsPaused.set(false);
        if (getAudioPlayer() == null) return;
        getAudioPlayer().release();
    }

    void seekTo(final long pos) {
        try {
            getAudioPlayer().seekTo(pos);
        } catch (IllegalStateException e) {
            release();
        }
    }

    // State listeners
    void onPlayerIdle() {
        getAudioEventListener().onStop();
    }
    void onPlayerPreparing() {
        getAudioEventListener().onPreparing();
    }
    void onPlayerBuffering() {
        getAudioEventListener().onBuffering();
    }
    void onPlayerPlaying() {
        getAudioEventListener().onPlay();
    }
    void onPlayerPaused() {
        getAudioEventListener().onPause();
    }
    void onPlayerEnded() {
        getAudioEventListener().onCompletion();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected Context getContext() {
        return mContext;
    }

    protected AudioPlayer getAudioPlayer() {
        return mAudioPlayer;
    }

    protected void setAudioPlayer(final AudioPlayer audioPlayer) {
        mAudioPlayer = audioPlayer;
    }

    protected AudioEventListener getAudioEventListener() {
        return mAudioEventListener;
    }

    void setAudioEventListener(final AudioEventListener audioEventListener) {
        mAudioEventListener = audioEventListener;
    }

    protected StreamManager getStreamManager() {
        return mStreamManager;
    }

    protected AudioManager getAudioManager() {
        return mAudioManager;
    }

    private synchronized void setAudioFocusState(final AudioFocusState state) {
        mAudioFocusState = state;
    }

    private synchronized AudioFocusState getAudioFocusState() {
        return mAudioFocusState;
    }

    protected void setIsTemporarilyPaused(boolean isTemporarilyPaused) { mIsTemporarilyPaused = isTemporarilyPaused; }

    protected boolean isTemporarilyPaused() { return mIsTemporarilyPaused; }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onAudioFocusChange(final int focusChange) { // must be public for override
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

    @Override // AudioPlayer.Listener
    public void onStateChanged(final boolean playWhenReady, final int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_IDLE:
                Log.d("ExoPlayer State", "IDLE");
                onPlayerIdle();
                break;

            case ExoPlayer.STATE_PREPARING:
                Log.d("ExoPlayer State", "PREPARING");
                onPlayerPreparing();
                break;

            case ExoPlayer.STATE_BUFFERING:
                Log.d("ExoPlayer State", "BUFFERING");
                onPlayerBuffering();
                break;

            case ExoPlayer.STATE_READY:
                Log.d("ExoPlayer State", "READY");

                if (isPlaying()) {
                    Log.d("ExoPlayer State", "READY (playing)");
                    onPlayerPlaying();
                    break;
                }

                if (isPaused()) {
                    Log.d("ExoPlayer State", "READY (paused)");
                    onPlayerPaused();
                    break;
                }

                break;

            case ExoPlayer.STATE_ENDED:
                Log.d("ExoPlayer State", "ENDED");
                onPlayerEnded();
                break;
        }
    }


    @Override // AudioPlayer.Listener
    public void onError(final Exception e) {
        getAudioEventListener().onError();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    boolean requestAudioFocus() {
        int result = getAudioManager().requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    long getDuration() {
        return getAudioPlayer() == null ? -1 : getAudioPlayer().getPlayerControl().getDuration();
    }

    boolean isIdle() {
        return getAudioPlayer() == null ||
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_IDLE;
    }

    boolean isPreparing() {
        return getAudioPlayer() != null &&
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_PREPARING;
    }

    boolean isBuffering() {
        return getAudioPlayer() != null &&
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_BUFFERING;
    }

    boolean isReady() {
        return getAudioPlayer() != null &&
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_READY;
    }

    boolean isPlaying() {
        return getAudioPlayer() != null &&
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_READY &&
                getAudioPlayer().getPlayerControl().isPlaying();
    }

    boolean isPaused() {
        return mIsPaused.get() &&
                getAudioPlayer() != null &&
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_READY &&
                !getAudioPlayer().getPlayerControl().isPlaying();
    }

    boolean isEnded() {
        return getAudioPlayer() != null &&
                getAudioPlayer().getPlaybackState() == ExoPlayer.STATE_ENDED;
    }

    long getCurrentPosition() {
        return getAudioPlayer() == null ? -1 : getAudioPlayer().getCurrentPosition();
    }

    protected void prepare() {
        if (getAudioPlayer() == null) return;
        getAudioPlayer().prepare();
    }

    boolean canSeekBackward() {
        if (getAudioPlayer() == null) return false;
        return getAudioPlayer().getPlayerControl().canSeekBackward();
    }

    boolean canSeekForward() {
        if (getAudioPlayer() == null) return false;
        return getAudioPlayer().getPlayerControl().canSeekForward();
    }

    private void audioFocusGain() {
        switch (getAudioFocusState()) {
            case DUCKING:
                unduckStream();
                break;
            case PAUSED:
                play();
                break;
            default:
                // Already playing, nothing to do.
        }

        setAudioFocusState(AudioFocusState.OWNED);
    }

    private void audioFocusLossTransientCanDuck() {
        duckStream();
        setAudioFocusState(AudioFocusState.DUCKING);
    }

    private void audioFocusLossTransient() {
        pause();
        setAudioFocusState(AudioFocusState.PAUSED);
    }

    private void audioFocusLoss() {
        stop();
        setAudioFocusState(AudioFocusState.LOST);
    }

    private void duckStream() {
        if (getAudioPlayer() == null) return;
        getAudioPlayer().setVolume(DUCK_VOLUME);
    }

    private void unduckStream() {
        if (getAudioPlayer() == null) return;
        getAudioPlayer().setVolume(1.0f);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    abstract static class AudioEventListener {
        void onPreparing() {}
        void onPlay() {}
        void onPause() {}
        void onStop() {}
        void onCompletion() {}
        void onProgress(int progress) {}
        void onError() {}
        void onBuffering() {}
        void onBufferingUpdate(int percent) {}
        void onPrerollData(final PrerollManager.PrerollData prerollData) {}
    }
}
