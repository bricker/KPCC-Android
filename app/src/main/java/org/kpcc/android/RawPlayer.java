package org.kpcc.android;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.ExoPlayer;

/**
 * Created by rickb014 on 4/3/16.
 */
public class RawPlayer extends Stream implements AudioPlayer.Listener {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    RawPlayer(final Context context, final String assetPath) {
        super(context);

        AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, USER_AGENT,
                Uri.parse(assetPath));

        setAudioPlayer(new AudioPlayer(builder));
        getAudioPlayer().setPlayWhenReady(false);

        getAudioPlayer().addListener(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // AudioPlayer.Listener
    public void onStateChanged(final boolean playWhenReady, final int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_IDLE:
                break;
            case ExoPlayer.STATE_PREPARING:
                break;
            case ExoPlayer.STATE_BUFFERING:
                break;
            case ExoPlayer.STATE_READY:
                break;
            case ExoPlayer.STATE_ENDED:
                getAudioEventListener().onCompletion();
                release();
                break;
        }
    }

    @Override // AudioPlayer.Listener
    public void onError(final Exception e) {
        getAudioEventListener().onError();
    }
}
