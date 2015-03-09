package org.kpcc.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

/**
 * Created by rickb014 on 2/16/15.
 */
public class StreamManager extends Service {

    public static final String TAG = "kpcc.StreamManager";
    public final static String LIVESTREAM_URL = "http://live.scpr.org/kpcclive?preskio=true";

    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mAudioPlayer = null;
    private String mCurrentAudioUrl = null;

    public void playEpisode(String audioUrl,
                            Context context,
                            final AudioButtonManager audioButtonManager) {

        MainActivity activity = (MainActivity) context;
        if (!activity.streamIsBound()) return;

        // Check if we're trying to play the same audio as before.
        // For live stream, this will just re-start the stream because we should have stopped it
        // before. For episodes, it should start the audio from where it left off because it should
        // have been paused.
        if (mCurrentAudioUrl != null && mCurrentAudioUrl.equals(audioUrl)) {
            // Same audio was paused; continue playing.
            startForPause(audioButtonManager);
            return;
        }

        audioButtonManager.toggleLoading();
        setupAudioPlayer();

        mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                startForPause(audioButtonManager);
            }
        });

        // At this point we know we're going to be setting the data source so we can safely
        // reset no matter what.
        mAudioPlayer.reset();

        try {
            mAudioPlayer.setDataSource(audioUrl);
            mAudioPlayer.prepareAsync();
        } catch (IOException e) {
            // TODO: Handle errors
            e.printStackTrace();
        }

        mCurrentAudioUrl = audioUrl;
    }

    public void playLiveStream(Context context, final AudioButtonManager audioButtonManager) {
        MainActivity activity = (MainActivity) context;
        if (!activity.streamIsBound()) return;

        audioButtonManager.toggleLoading();
        setupAudioPlayer();

        mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // TODO: Preroll
                startForStop(audioButtonManager);
            }
        });

        mAudioPlayer.reset();

        try {
            mAudioPlayer.setDataSource(LIVESTREAM_URL);
            mAudioPlayer.prepareAsync();
        } catch (IOException e) {
            // TODO: Handle errors
            e.printStackTrace();
        }

        mCurrentAudioUrl = LIVESTREAM_URL;
    }

    public boolean isPlaying(String audioUrl) {
        return (mAudioPlayer != null) &&
                (mCurrentAudioUrl != null) &&
                (mCurrentAudioUrl.equals(audioUrl));
    }

    public void pause(AudioButtonManager audioButtonManager) {
        audioButtonManager.togglePaused();

        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
    }

    public void stop(AudioButtonManager audioButtonManager) {
        mCurrentAudioUrl = null;
        audioButtonManager.toggleStopped();

        if (mAudioPlayer != null) {
            mAudioPlayer.stop();
        }
    }

    public void release() {
        mCurrentAudioUrl = null;

        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
    }


    private void startForPause(AudioButtonManager audioButtonManager) {
        audioButtonManager.togglePlayingForPause();
        mAudioPlayer.start();
    }

    private void startForStop(AudioButtonManager audioButtonManager) {
        audioButtonManager.togglePlayingForStop();
        mAudioPlayer.start();
    }

    private void setupAudioPlayer() {
        if (mAudioPlayer == null) {
            mAudioPlayer = new MediaPlayer();
            mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mAudioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.d(TAG, "Got an error: " + what);
                    return false;
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }


    public class LocalBinder extends Binder {
        public StreamManager getService() {
            return StreamManager.this;
        }
    }
}
