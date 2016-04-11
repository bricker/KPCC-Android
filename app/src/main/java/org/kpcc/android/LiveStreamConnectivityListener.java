package org.kpcc.android;

/**
 * Created by rickb014 on 4/4/16.
 */
class LiveStreamConnectivityListener implements AppConnectivityManager.NetworkConnectivityListener {
    @Override
    public void onConnect() {
        StreamManager streamManager = StreamManager.ConnectivityManager.getInstance().getStreamManager();

        if (streamManager == null) {
            // Nothing we can do but wait.
            return;
        }

        LivePlayer currentLivePlayer = streamManager.getCurrentLivePlayer();

        if (currentLivePlayer != null) {
            currentLivePlayer.playIfTemporarilyPaused();
        }
    }

    @Override
    public void onDisconnect() {
        StreamManager streamManager = StreamManager.ConnectivityManager.getInstance().getStreamManager();
        if (streamManager == null) {
            // Nothing we can do
            return;
        }

        LivePlayer currentLivePlayer = streamManager.getCurrentLivePlayer();

        if (currentLivePlayer != null) {
            currentLivePlayer.pauseTemporary();
        }
    }
}
