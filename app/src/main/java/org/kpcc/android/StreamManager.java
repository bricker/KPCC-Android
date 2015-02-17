package org.kpcc.android;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by rickb014 on 2/16/15.
 */
public class StreamManager extends Service {

    public static final String TAG = "kpcc.StreamManager";
    private final static long PREROLL_THRESHOLD = 600L;
    public final static String LIVESTREAM_URL = "http://live.scpr.org/kpcclive";
    public final static String LIVESTREAM_NOPREROLL_URL = LIVESTREAM_URL + "?preskip=true";

    private final MediaPlayer mMediaPlayer = new MediaPlayer();
    private final IBinder mBinder = new LocalBinder();
    private boolean mIsInitialized = false;

    public void startLiveStream() {
        Toast.makeText(this, "Starting Stream", Toast.LENGTH_LONG).show();
        setupMediaPlayer();

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "Got an error: " + what);
                return false;
            }
        });

        try {
            mMediaPlayer.setDataSource(LIVESTREAM_URL);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            // TODO: Handle errors
            e.printStackTrace();
        }

        mIsInitialized = true;
    }

    public void startEpisode() {
        Toast.makeText(this, "Starting Episode", Toast.LENGTH_LONG).show();
    }


    public void start() {
        if (!mIsInitialized) return;
        mMediaPlayer.start();
    }

    public boolean isPlaying() {
        return mIsInitialized && mMediaPlayer.isPlaying();
    }

    public void pause() {
        if (!mIsInitialized) return;
        mMediaPlayer.pause();
    }

    public void stop() {
        if (!mIsInitialized) return;
        mMediaPlayer.stop();
    }

    public void release() {
        if (!mIsInitialized) return;
        mMediaPlayer.release();
    }


    private void setupMediaPlayer() {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
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
