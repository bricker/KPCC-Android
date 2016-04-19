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
    private LivePlayer.PrerollCompleteCallback mPrerollCompleteCallback;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    PrerollPlayer(final Context context, final LivePlayer.PrerollCompleteCallback prerollCompleteCallback) {
        super(context);
        mPrerollCompleteCallback = prerollCompleteCallback;
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
        if (getStreamManager() != null && getStreamManager().getCurrentPrerollPlayer() == this) {
            getStreamManager().setCurrentPrerollPlayer(null);
        }

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
    void setupPreroll(final String audioUrl) {
        AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(getContext(), USER_AGENT,
                Uri.parse(audioUrl));

        setAudioPlayer(new AudioPlayer(builder));
        getAudioPlayer().setPlayWhenReady(false);

        getAudioPlayer().addListener(this);

        if (getStreamManager() != null) {
            getStreamManager().setCurrentPrerollPlayer(this);
        }
    }

    private LivePlayer.PrerollCompleteCallback getPrerollCompleteCallback() {
        return mPrerollCompleteCallback;
    }

    void setOnPrerollCompleteCallback(final LivePlayer.PrerollCompleteCallback prerollCompleteCallback) {
        mPrerollCompleteCallback = prerollCompleteCallback;
    }
}
