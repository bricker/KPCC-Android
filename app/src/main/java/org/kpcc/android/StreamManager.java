package org.kpcc.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rickb014 on 2/16/15.
 */
public class StreamManager extends Service {

    public static final String TAG = "kpcc.StreamManager";
    // We get preroll directly from Triton so we always use the skip-preroll url.
    public final static String LIVESTREAM_URL = "http://live.scpr.org/kpcclive?preskip=true";
    private final static String PREF_USER_PLAYED_LIVESTREAM = "live_stream_played";

    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mAudioPlayer = null;
    private MediaPlayer mPrerollPlayer = null;
    private String mCurrentAudioUrl = null;
    private boolean mIsPlayingPreroll = false;

    public static String getTimeFormat(int seconds) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.SECONDS.toHours(seconds),
                TimeUnit.SECONDS.toMinutes(seconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.SECONDS.toSeconds(seconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }

    public void playEpisode(String audioUrl,
                            Context context,
                            final AudioButtonManager audioButtonManager,
                            ProgressBar progressBar,
                            TextView currentTime) {

        MainActivity activity = (MainActivity) context;
        if (!activity.streamIsBound()) return;

        // Check if we're trying to play the same audio as before.
        // For live stream, this will just re-start the stream because we should have stopped it
        // before. For episodes, it should start the audio from where it left off because it should
        // have been paused.
        if (mCurrentAudioUrl != null && mCurrentAudioUrl.equals(audioUrl)) {
            // Same audio was paused; continue playing.
            audioButtonManager.togglePlayingForPause();
            mAudioPlayer.start();
            return;
        }

        audioButtonManager.toggleLoading();
        setupAudioPlayer();

        final ProgressObserver progressObserver = new ProgressObserver(progressBar, currentTime);

        mAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                progressObserver.stop();
            }
        });

        mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                audioButtonManager.togglePlayingForPause();
                mAudioPlayer.start();
                progressObserver.start();
                new Thread(progressObserver).start();
            }
        });

        // At this point we know we're going to be setting the data source so we can safely
        // reset no matter what.
        // This will also stop any other currently playing streams.
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

    public void playLiveStream(final Context context,
                               final AudioButtonManager audioButtonManager,
                               final NetworkImageView adView) {
        final MainActivity activity = (MainActivity) context;
        if (!activity.streamIsBound()) return;

        audioButtonManager.toggleLoading();
        setupAudioPlayer();

        mAudioPlayer.reset();
        mPrerollPlayer.reset();

        try {
            mAudioPlayer.setDataSource(LIVESTREAM_URL);
        } catch (IOException e) {
            // TODO: Handle errors
            e.printStackTrace();
        }

        mCurrentAudioUrl = LIVESTREAM_URL;
        mAudioPlayer.prepareAsync();

        // If they just installed the app (less than 10 minutes ago), and have never played the live
        // stream, don't play preroll.
        // Otherwise do the normal preroll flow.
        final long now = System.currentTimeMillis();
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

        boolean hasPlayedLiveStream = sharedPref.getBoolean(PREF_USER_PLAYED_LIVESTREAM, false);
        boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
        boolean heardPrerollRecently = PrerollManager.LAST_PREROLL_PLAY > (now - PrerollManager.PREROLL_THRESHOLD);

        if ((!hasPlayedLiveStream && installedRecently) || heardPrerollRecently) {
            // Skipping Preroll
            setPreparedListenerForLiveStream(audioButtonManager);
        } else {
            // Normal preroll flow (preroll still may not play, based on Triton response)
            PrerollManager.getInstance().getPrerollData(context, new PrerollManager.PrerollCallbackListener() {
                @Override
                public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                    if (prerollData == null || prerollData.getAudioUrl() == null) {
                        setPreparedListenerForLiveStream(audioButtonManager);
                    } else {
                        if (prerollData.getAssetUrl() != null) {
                            PrerollManager.getInstance().showPrerollAsset(context, adView, prerollData);
                        }

                        try {
                            mPrerollPlayer.setDataSource(prerollData.getAudioUrl());
                            mPrerollPlayer.prepareAsync();
                        } catch (IOException e) {
                            // TODO: Handle errors
                        }

                        mPrerollPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                PrerollManager.LAST_PREROLL_PLAY = now;
                                activity.getNavigationDrawerFragment().disableDrawer();
                                mIsPlayingPreroll = true;
                                audioButtonManager.togglePlayingForPause();
                                mPrerollPlayer.start();
                                mPrerollPlayer.setNextMediaPlayer(mAudioPlayer);
                            }
                        });

                        mPrerollPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                activity.getNavigationDrawerFragment().enableDrawer();
                                mIsPlayingPreroll = false;
                                adView.setVisibility(View.GONE);
                                audioButtonManager.togglePlayingForStop();
                            }
                        });
                    }
                }
            });
        }

        if (!hasPlayedLiveStream) {
            sharedPref.edit().putBoolean(PREF_USER_PLAYED_LIVESTREAM, true).apply();
        }
    }

    private void setPreparedListenerForLiveStream(final AudioButtonManager audioButtonManager) {
        mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                audioButtonManager.togglePlayingForStop();
                mAudioPlayer.start();
            }
        });
    }

    public boolean isPlayingPreroll() {
        // We don't want to check the state of mPrerollPlayer here, because if it's paused
        // it will be false. We want to check, rather, if the user is in middle of listening to
        // preroll, whether or not they have paused it.
        return mIsPlayingPreroll;
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

    public void resumePreroll(AudioButtonManager audioButtonManager) {
        audioButtonManager.togglePlayingForPause();

        if (mPrerollPlayer != null) {
            mPrerollPlayer.start();
        }
    }

    public void pausePreroll(AudioButtonManager audioButtonManager) {
        audioButtonManager.togglePaused();

        if (mPrerollPlayer != null) {
            mPrerollPlayer.pause();
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

    private void setupAudioPlayer() {
        if (mAudioPlayer == null) {
            mAudioPlayer = new MediaPlayer();
            mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mAudioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // TOOO: Handle audio errors
                    return false;
                }
            });
        }

        if (mPrerollPlayer == null) {
            mPrerollPlayer = new MediaPlayer();
            mPrerollPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mPrerollPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // No preroll
                    return false;
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class LocalBinder extends Binder {
        public StreamManager getService() {
            return StreamManager.this;
        }
    }

    public class ProgressObserver implements Runnable {
        private AtomicBoolean mIsObserving = new AtomicBoolean(false);
        private Handler mHandler = new Handler();
        private ProgressBar mProgressBar;
        private TextView mCurrentTime;

        public ProgressObserver(ProgressBar progressBar, TextView currentTime) {
            mProgressBar = progressBar;
            mCurrentTime = currentTime;
        }

        public void start() {
            mIsObserving.set(true);
        }

        public void stop() {
            mIsObserving.set(false);
        }

        @Override
        public void run() {
            while (mIsObserving.get()) {
                try {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int millis = mAudioPlayer.getCurrentPosition();
                            mProgressBar.setProgress(millis / 1000);
                            mCurrentTime.setText(StreamManager.getTimeFormat(millis / 1000));
                        }
                    });

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO: Handle Errors
                }
            }
        }
    }
}
