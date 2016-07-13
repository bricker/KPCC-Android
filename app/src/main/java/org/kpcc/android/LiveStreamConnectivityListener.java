package org.kpcc.android;

import android.content.Context;

/**
 * Created by rickb014 on 4/4/16.
 */
class LiveStreamConnectivityListener implements AppConnectivityManager.NetworkConnectivityListener {
    @Override
    public void onConnect(Context context) {
//        MainActivity activity = (MainActivity)context;
//        activity.bind
//        StreamService service = (StreamService)context;
//
//        if (service == null) {
//            // Nothing we can do
//            return;
//        }
//
//        Stream stream = service.getCurrentStream();
//
//        if (stream != null) {
//            stream.playIfTemporarilyPaused();
//        }
    }

    @Override
    public void onDisconnect(Context context) {
//        StreamManager streamManager = StreamManager.ConnectivityManager.getInstance().getStreamManager();
//        if (streamManager == null) {
//            // Nothing we can do
//            return;
//        }
//
//        LivePlayer currentLivePlayer = streamManager.getCurrentLivePlayer();
//
//        if (currentLivePlayer != null) {
//            currentLivePlayer.pauseTemporary();
//        }
    }
}
