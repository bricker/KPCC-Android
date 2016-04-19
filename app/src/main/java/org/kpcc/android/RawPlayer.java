package org.kpcc.android;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

/**
 * Created by rickb014 on 4/3/16.
 */
class RawPlayer extends Stream implements AudioPlayer.Listener {
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

    @Override
    boolean requestAudioFocus() {
        int result = getAudioManager().requestAudioFocus(this, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
}
