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
    public final static String LIVESTREAM_URL = "http://live.scpr.org/kpcclive";
    public final static String LIVESTREAM_NOPREROLL_URL = LIVESTREAM_URL + "?preskip=true";
    private final static long PREROLL_THRESHOLD = 600L;
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

    public void startForPause(AudioButtonManager audioButtonManager) {
        if (mAudioPlayer == null) {
            return;
        }
        audioButtonManager.togglePlayingForPause();
        mAudioPlayer.start();
    }

    public void startForStop(AudioButtonManager audioButtonManager) {
        if (mAudioPlayer == null) {
            return;
        }
        audioButtonManager.togglePlayingForStop();
        mAudioPlayer.start();
    }

    public boolean isPlaying(String audioUrl) {
        return (mAudioPlayer != null) &&
                (mCurrentAudioUrl != null) &&
                (mCurrentAudioUrl.equals(audioUrl));
    }

    public void pause(AudioButtonManager audioButtonManager) {
        if (mAudioPlayer == null) {
            return;
        }
        mAudioPlayer.pause();

        if (audioButtonManager != null) {
            audioButtonManager.togglePaused();
        }
    }

    public void stop(AudioButtonManager audioButtonManager) {
        if (mAudioPlayer == null) {
            return;
        }
        mAudioPlayer.stop();
        mCurrentAudioUrl = null;

        if (audioButtonManager != null) {
            audioButtonManager.toggleStopped();
        }
    }

    public void release() {
        if (mAudioPlayer == null) {
            return;
        }
        mAudioPlayer.release();
        mAudioPlayer = null;
        mCurrentAudioUrl = null;
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
