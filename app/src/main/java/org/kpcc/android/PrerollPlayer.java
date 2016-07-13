package org.kpcc.android;

import android.content.Context;
import android.net.Uri;

/**
 * Created by rickb014 on 4/3/16.
 */
class PrerollPlayer extends Stream implements AudioPlayer.Listener {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private PrerollCompleteCallback mPrerollCompleteCallback;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    PrerollPlayer(Context context) {
        super(context);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // Stream
    public void seekTo(final long pos) {
        // Can't seek preroll.
    }

    @Override // Stream
    public void release() {
        super.release();
    }

    // We're overriding this because we don't want to show an error if the preroll can't
    // play. Instead we should just continue with the stream.
    @Override // Stream
    public void prepareAndStart() {
        if (getAudioPlayer() == null) return;

        if (isPaused()) {
            play();
            return;
        }

        getAudioEventListener().onPreparing();

        if (!requestAudioFocus()) {
            return;
        }

        prepare();
        play();
    }

    @Override // AudioPlayer.Listener
    public void onError(final Exception e) {
        super.onError(e);
        getPrerollCompleteCallback().onPrerollComplete();
    }

    @Override
    void onPlayerEnded() {
        super.onPlayerEnded();
        getPrerollCompleteCallback().onPrerollComplete();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // We want to be able to rely on the preroll player existing, but we don't know if it will
    // actually be playing anything, so this method acts as sort of a "deferred constructor".
    void setupPreroll(Context context, final String audioUrl) {
        AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, USER_AGENT,
                Uri.parse(audioUrl));

        setAudioPlayer(new AudioPlayer(builder));
        getAudioPlayer().setPlayWhenReady(false);

        getAudioPlayer().addListener(this);
    }

    private PrerollCompleteCallback getPrerollCompleteCallback() {
        return mPrerollCompleteCallback;
    }

    void setOnPrerollCompleteCallback(final PrerollCompleteCallback prerollCompleteCallback) {
        mPrerollCompleteCallback = prerollCompleteCallback;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static abstract class PrerollCompleteCallback {
        abstract void onPrerollComplete();
    }
}
