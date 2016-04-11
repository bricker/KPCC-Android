package org.kpcc.android;

import android.content.Context;

/**
 * Created by rickb014 on 4/4/16.
 */
// This class is responsible for orchestrating the interaction between livestream and preroll.
class LiveStreamBundle {
    private final LivePlayer mLivePlayer;
    private final PrerollPlayer mPrerollPlayer;
    private final Context mContext;

    LiveStreamBundle(final Context context) {
        mContext = context;
        mLivePlayer = new LivePlayer(context);
        mPrerollPlayer = new PrerollPlayer(context, mLivePlayer.getPrerollCompleteCallback());
    }

    LiveStreamBundle(final Context context, final LivePlayer livePlayer) {
        mContext = context;
        mLivePlayer = livePlayer;
        mPrerollPlayer = new PrerollPlayer(context, mLivePlayer.getPrerollCompleteCallback());
    }

    LiveStreamBundle(final Context context, final LivePlayer livePlayer, final PrerollPlayer prerollPlayer) {
        mContext = context;
        mLivePlayer = livePlayer;
        prerollPlayer.setOnPrerollCompleteCallback(mLivePlayer.getPrerollCompleteCallback());
        mPrerollPlayer = prerollPlayer;
    }

    public LivePlayer getLivePlayer() {
        return mLivePlayer;
    }

    public PrerollPlayer getPrerollPlayer() {
        return mPrerollPlayer;
    }

    public void playWithPrerollAttempt() {
        mLivePlayer.getAudioEventListener().onPreparing();

        // If they just installed the app (less than 10 minutes ago), and have never played the live
        // stream, don't play preroll.
        // Otherwise do the normal preroll flow.
        final long now = System.currentTimeMillis();

        boolean hasPlayedLiveStream = DataManager.getInstance().getHasPlayedLiveStream();
        boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
        boolean heardPrerollRecently = PrerollManager.getInstance().getLastPlay() > (now - PrerollManager.PREROLL_THRESHOLD);

        if (!AppConfiguration.getInstance().isDebug && ((!hasPlayedLiveStream && installedRecently) || heardPrerollRecently)) {
            // Skipping Preroll
            mLivePlayer.prepareAndStart();
        } else {
            // Normal preroll flow (preroll still may not play, based on preroll response)
            PrerollManager.getInstance().getPrerollData(mContext, new PrerollManager.PrerollCallbackListener() {
                @Override
                public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                    if (prerollData == null || prerollData.getAudioUrl() == null) {
                        mLivePlayer.prepareAndStart();
                    } else {
                        mPrerollPlayer.setupPreroll(prerollData.getAudioUrl());
                        mPrerollPlayer.getAudioEventListener().onPrerollData(prerollData);
                        mPrerollPlayer.prepareAndStart();
                    }
                }
            });
        }

        if (!hasPlayedLiveStream) {
            DataManager.getInstance().setHasPlayedLiveStream(true);
        }
    }
}
