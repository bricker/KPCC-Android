package org.kpcc.android;

import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rickb014 on 4/3/16.
 */
public class LivePlayer extends Stream {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    final static private String HLS_URL =
            String.format(AppConfiguration.getInstance().getConfig("livestream.url"),
                    BuildConfig.VERSION_NAME,
                    String.valueOf(BuildConfig.VERSION_CODE)
            );

    final static int JUMP_INTERVAL_SEC = 30;
    final static int JUMP_INTERVAL_MS = JUMP_INTERVAL_SEC*1000;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    final private PrerollCompleteCallback mPrerollCompleteCallback;
    private long mPausedAt = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    LivePlayer(final Context context) {
        super(context);

        AudioPlayer.RendererBuilder builder = new HlsRendererBuilder(context, USER_AGENT, HLS_URL);
        setAudioPlayer(new AudioPlayer(builder));
        getAudioPlayer().setPlayWhenReady(false);

        getAudioPlayer().addListener(this);
        mPrerollCompleteCallback = new PrerollCompleteCallback();

        if (getStreamManager() != null) {
            getStreamManager().setCurrentLivePlayer(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    PrerollCompleteCallback getPrerollCompleteCallback() {
        return mPrerollCompleteCallback;
    }

    synchronized long getPausedAt() {
        return mPausedAt;
    }

    synchronized void setPausedAt(long pausedAt) {
        mPausedAt = pausedAt;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    void pause() {
        super.pause();
        setPausedAt(System.currentTimeMillis());
    }

    @Override
    void stop() {
        super.stop();
        setPausedAt(System.currentTimeMillis());
    }

    @Override // Stream
    void release() {
        if (getStreamManager() != null && getStreamManager().getCurrentLivePlayer() == this) {
            getStreamManager().setCurrentLivePlayer(null);
        }

        super.release();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void seekToLive() {
        seekTo(getDuration());
    }

    void pauseTemporary() {
        pause();
        setIsTemporarilyPaused(true);
    }

    void playIfTemporarilyPaused() {
        if (isPaused() && isTemporarilyPaused()) {
            try {
                play();
            } catch (IllegalStateException e) {
                prepareAndStart();
            }
        }

        setIsTemporarilyPaused(false);
    }

    void skipBackward() {
        if (canSeekBackward()) {
            seekTo(getCurrentPosition() - JUMP_INTERVAL_MS);
        }
    }

    void skipForward() {
        if (canSeekForward()) {
            seekTo(getCurrentPosition() + JUMP_INTERVAL_MS);
        }
    }

    long getAudioTimestamp() {
        if (getAudioPlayer() == null) return 0;
        return getAudioPlayer().getProgramDateTimeUTS();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected class PrerollCompleteCallback {
        protected void onPrerollComplete() {
            prepareAndStart();
        }
    }

}
