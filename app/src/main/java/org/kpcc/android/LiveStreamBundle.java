package org.kpcc.android;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Locale;

/**
 * Created by rickb014 on 4/4/16.
 */
// This class is responsible for orchestrating the interaction between livestream and preroll.
public class LiveStreamBundle {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private LivePlayer mLivePlayer;
    private PrerollPlayer mPrerollPlayer;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    LiveStreamBundle(Context context) {
        setLivePlayer(new LivePlayer(context));
        setPrerollPlayer(new PrerollPlayer(context));
    }


    LiveStreamBundle(Context context, LivePlayer livePlayer) {
        setLivePlayer(livePlayer);
        setPrerollPlayer(new PrerollPlayer(context));
    }

    LiveStreamBundle(Context context, LivePlayer livePlayer, PrerollPlayer prerollPlayer) {
        setLivePlayer(livePlayer);
        setPrerollPlayer(prerollPlayer);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void setLivePlayer(LivePlayer livePlayer) {
        mLivePlayer = livePlayer;
    }

    public LivePlayer getLivePlayer() {
        return mLivePlayer;
    }

    private void setPrerollPlayer(PrerollPlayer prerollPlayer) {
        mPrerollPlayer = prerollPlayer;
    }

    public PrerollPlayer getPrerollPlayer() {
        return mPrerollPlayer;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void playWithPrerollAttempt(final Context context) {
        // If they just installed the app (less than 10 minutes ago), and have never played the live
        // stream, don't play preroll.
        // Otherwise do the normal preroll flow.
        final long now = System.currentTimeMillis();

        final PrerollManager prerollManager = new PrerollManager();
        boolean hasPlayedLiveStream = DataManager.getInstance().getHasPlayedLiveStream();
        boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
        boolean heardPrerollRecently = prerollManager.getLastPlay() > (now - PrerollManager.PREROLL_THRESHOLD);

        if ((AppConfiguration.getInstance().isDebug && !AppConfiguration.getInstance().getConfigBool("preroll.enabled")) ||
                (!hasPlayedLiveStream && installedRecently) ||
                heardPrerollRecently) {
            // Skipping Preroll
            mLivePlayer.prepareAndStart();
        } else {
            mPrerollPlayer.getAudioEventListener().onPreparing();

            // Normal preroll flow (preroll still may not play, based on preroll response)
            prerollManager.getPrerollData(context, new PrerollManager.PrerollCallbackListener() {
                @Override
                public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                    if (prerollData == null || prerollData.getAudioUrl() == null) {
                        mLivePlayer.prepareAndStart();
                    } else {
                        mPrerollPlayer.setupPreroll(context, prerollData.getAudioUrl());
                        mPrerollPlayer.getAudioEventListener().onPrerollData(prerollData);
                        mPrerollPlayer.prepareAndStart();
                        prerollManager.setLastPlayToNow();

                        mLivePlayer.prepare();
                        mPrerollPlayer.setOnPrerollCompleteCallback(new PrerollPlayer.PrerollCompleteCallback(){
                            @Override
                            void onPrerollComplete() {
                                mLivePlayer.prepareAndStart();
                            }
                        });
                    }
                }
            });
        }

        if (!hasPlayedLiveStream) {
            DataManager.getInstance().setHasPlayedLiveStream(true);
        }
    }
}
