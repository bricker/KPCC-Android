package org.kpcc.android;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.ExoPlayer;

/**
 * Created by rickb014 on 4/3/16.
 */
class OnDemandPlayer extends Stream implements AudioPlayer.Listener {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final String mAudioUrl;
    private final String mProgramSlug;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    OnDemandPlayer(final Context context, final String audioUrl, final String programSlug, final int durationSeconds) {
        super(context);

        AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, USER_AGENT,
                Uri.parse(audioUrl));

        setAudioPlayer(new AudioPlayer(builder));
        // Don't play right away - this initialization occurs while other audio is still playing.
        getAudioPlayer().setPlayWhenReady(false);
        getAudioPlayer().addListener(this);

        mAudioUrl = audioUrl;
        mProgramSlug = programSlug;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public String getAudioUrl() {
        return mAudioUrl;
    }

    public int getDurationSeconds() {
        return (int)(getDuration() / 1000);
    }

    public String getProgramSlug() {
        return mProgramSlug;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // Stream
    public void release() {
        if (getStreamManager() != null && getStreamManager().getCurrentOnDemandPlayer() == this) {
            getStreamManager().setCurrentOnDemandPlayer(null);
        }

        super.release();
    }

    @Override // Stream
    public void prepareAndStart() {
        // We have to set this here, not on initialization, because these players get
        // initialized while the previous episode is still playing.
        if (getStreamManager() != null) {
            getStreamManager().setCurrentOnDemandPlayer(this);
        }

        super.prepareAndStart();
    }
}
