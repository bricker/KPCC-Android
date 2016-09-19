package org.kpcc.android;

import android.content.Context;

/**
 * Created by rickb014 on 4/4/16.
 */
class LiveStreamConnectivityListener implements AppConnectivityManager.NetworkConnectivityListener {
    @Override
    public void onConnect(Context context, StreamService streamServiceUnsafe) {
        if (streamServiceUnsafe == null) {
            // Nothing we can do
            return;
        }

        try {
            Stream stream = streamServiceUnsafe.getCurrentStream();

            if (stream != null) {
                stream.playIfTemporarilyPaused();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) { throw e; }
            // Otherwise swallow this error.
        }
    }

    @Override
    public void onDisconnect(Context context, StreamService streamServiceUnsafe) {
        if (streamServiceUnsafe == null) {
            // Nothing we can do
            return;
        }

        try {
            LivePlayer currentLivePlayer = (LivePlayer) streamServiceUnsafe.getCurrentStream();

            if (currentLivePlayer != null) {
                currentLivePlayer.pauseTemporary();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) { throw e; }
            // Otherwise swallow this error.
        }
    }
}
