package org.kpcc.android;

import android.content.Context;
import android.net.Uri;

/**
 * Created by rickb014 on 4/3/16.
 */
class OnDemandPlayer extends Stream implements AudioPlayer.Listener {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private String mAudioUrl;
    private String mProgramSlug;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    OnDemandPlayer(Context context, final String audioUrl, final String programSlug) {
        super(context);

        AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, Stream.USER_AGENT,
                Uri.parse(audioUrl));

        AudioPlayer player = new AudioPlayer(builder);
        // Don't play right away - this initialization occurs while other audio is still playing.
        player.setPlayWhenReady(false);
        player.addListener(this);

        setAudioPlayer(player);
        setAudioUrl(audioUrl);
        setProgramSlug(programSlug);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void setAudioUrl(String audioUrl) {
        mAudioUrl = audioUrl;
    }

    public String getAudioUrl() {
        return mAudioUrl;
    }

    private void setProgramSlug(String programSlug) {
        mProgramSlug = programSlug;
    }

    public String getProgramSlug() {
        return mProgramSlug;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // Stream
    public void release() {
        super.release();
    }

    @Override // Stream
    public void prepareAndStart() {
        super.prepareAndStart();
    }
}
