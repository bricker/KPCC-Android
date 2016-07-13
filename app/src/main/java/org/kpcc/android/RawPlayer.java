package org.kpcc.android;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

/**
 * Created by rickb014 on 4/3/16.
 */
class RawPlayer extends Stream {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    RawPlayer(Context context, final String assetPath) {
        super(context);

        AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, USER_AGENT, Uri.parse(assetPath));

        AudioPlayer player = new AudioPlayer(builder);
        player.setPlayWhenReady(false);
        player.addListener(this);

        setAudioPlayer(player);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    boolean requestAudioFocus() {
        int result = getAudioManager().requestAudioFocus(this, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
}
