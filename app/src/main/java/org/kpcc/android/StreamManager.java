package org.kpcc.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer.util.MimeTypes;

import org.kpcc.api.ScheduleOccurrence;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamManager extends Service {
    public static final String USER_AGENT = "KPCCAndroid";
    private final IBinder mBinder = new LocalBinder();

    public EpisodeStream currentEpisodePlayer;
    public LiveStream currentLivePlayer;
    public PrerollStream currentPrerollPlayer;

    public static String getTimeFormat(int seconds) {
        return String.format("%01d:%02d:%02d",
                TimeUnit.SECONDS.toHours(seconds),
                TimeUnit.SECONDS.toMinutes(seconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.SECONDS.toSeconds(seconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }

    public static String getMimeTypeFromAudioUrl(String audioUrl) {
        String mimeType = MimeTypes.AUDIO_MPEG; // Default mime type.

        String ext = MimeTypeMap.getFileExtensionFromUrl(audioUrl);
        if (ext != null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        } else {
            // TODO: Extension couldn't be determined. We should notify the user that the
            // audio probably won't play, for now we'll just assume it's mp3 (the default mime).
        }

        return mimeType;
    }

    public static Extractor getExtractorFromContentType(String contentType) {
        switch (contentType) {
            case MimeTypes.AUDIO_MPEG:
                return new Mp3Extractor();
            case MimeTypes.AUDIO_MP4:
                return new Mp4Extractor();
            default:
                return null;
        }
    }

    // The nuclear option - this isn't guaranteed to update button states. It's a cleanup task.
    public void releaseAllActiveStreams() {
        if (currentEpisodePlayer != null) currentEpisodePlayer.release();
        if (currentLivePlayer != null) currentLivePlayer.release();
        if (currentPrerollPlayer != null) currentPrerollPlayer.release();
    }

    public void pauseAllActiveStreams() {
        if (currentEpisodePlayer != null) currentEpisodePlayer.pause();
        if (currentLivePlayer != null) currentLivePlayer.pause();
        if (currentPrerollPlayer != null) currentPrerollPlayer.pause();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private static class AudioFocusNotGrantedException extends Exception {
    }

    public abstract static class AudioEventListener {
        public abstract void onLoading();

        public void onPrepared() {
        }

        public abstract void onPlay();

        public abstract void onPause();

        public abstract void onStop();

        public abstract void onCompletion();

        public void onProgress(int progress) {
        }

        public abstract void onError();

        public void onBufferingUpdate(int percent) {
        }

        public void onPrerollData(PrerollManager.PrerollData prerollData) {
        }
    }

    public abstract static class BaseStream {
        public AudioPlayer audioPlayer;
        final Context mContext;
        final AudioManager mAudioManager;
        final AtomicBoolean mIsDucking = new AtomicBoolean(false);
        final AudioManager.OnAudioFocusChangeListener mAfChangeListener;
        final StreamManager mStreamManager;
        public AudioEventListener audioEventListener;
        final AtomicBoolean mDidPauseForAudioLoss = new AtomicBoolean(false);
        boolean isPaused = false;
        boolean didStopOnConnectivityLoss = false;

        public BaseStream(Context context) {
            mContext = context;
            mStreamManager = AppConnectivityManager.instance.streamManager;
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            mAfChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            audioFocusLossTransientCanDuck();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            audioFocusLossTransient();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            audioFocusGain();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            audioFocusLoss();
                            break;
                    }
                }
            };
        }

        public void setOnAudioEventListener(AudioEventListener eventListener) {
            audioEventListener = eventListener;
        }

        public void start() {
            try {
                audioEventListener.onPlay();
                isPaused = false;
                audioPlayer.start();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        public void stop() {
            try {
                audioEventListener.onStop();
                audioPlayer.stop();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        public void pause() {
            try {
                audioEventListener.onPause();
                isPaused = true;
                audioPlayer.pause();
            } catch (IllegalStateException e) {
                // Call release() to stop progress observer, update button state, etc.
                release();
            }
        }

        public void release() {
            audioEventListener.onStop();
            isPaused = false;
            audioPlayer.release();
        }

        public void seekTo(long pos) {
            try {
                if (isPlaying() || isPaused) {
                    audioPlayer.seekTo(pos);
                }
            } catch (IllegalStateException e) {
                release();
            }
        }

        public long getDuration() {
            try {
                return audioPlayer.getDuration();
            } catch (IllegalStateException e) {
                return -1;
            }
        }

        public void prepare() {
            audioPlayer.prepare();
        }

        public boolean isPlaying() {
            return audioPlayer.isPlaying();
        }

        void requestAudioFocus() throws AudioFocusNotGrantedException {
            int result = mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                throw new AudioFocusNotGrantedException();
            }
        }

        public long getCurrentPosition() {
            return audioPlayer.getCurrentPosition();
        }

        public void prepareAndStart() {
            if (isPaused) {
                start();
                return;
            }

            audioEventListener.onLoading();

            try {
                requestAudioFocus();
                prepare();
                start();
            } catch (AudioFocusNotGrantedException e) {
                audioEventListener.onError();
            }
        }

        void audioFocusLossTransientCanDuck() {
            duckStream();
        }

        void audioFocusLossTransient() {
            if (isPlaying()) {
                mDidPauseForAudioLoss.set(true);
                pause();
            }
        }

        void audioFocusGain() {
            // NOTE: This is overridden in LiveStream
            if (mIsDucking.get() && isPlaying()) {
                unduckStream();
            } else {
                if (mDidPauseForAudioLoss.get()) {
                    start();
                } // Otherwise, just keep it paused.
            }

            // Always do this just to be safe.
            mDidPauseForAudioLoss.set(false);
        }

        void audioFocusLoss() {
            mDidPauseForAudioLoss.set(false);
            release();
            mAudioManager.abandonAudioFocus(mAfChangeListener);
        }

        void duckStream() {
            if (!mIsDucking.get() && isPlaying()) {
                mIsDucking.set(true);

                if (isPlaying()) {
                    audioPlayer.setVolume(0.25f);
                }
            }
        }

        void unduckStream() {
            if (mIsDucking.get() && isPlaying()) {
                mIsDucking.set(false);

                if (isPlaying()) {
                    audioPlayer.setVolume(1.0f);
                }
            }
        }
    }

    public static class RawStream extends BaseStream {
        public final String assetPath;

        public RawStream(Context context, String assetPath) {
            super(context);
            this.assetPath = assetPath;

            String mimeType = StreamManager.getMimeTypeFromAudioUrl(assetPath);
            AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, USER_AGENT,
                    Uri.parse(assetPath), StreamManager.getExtractorFromContentType(mimeType));

            audioPlayer = new AudioPlayer(builder);
            audioPlayer.setPlayWhenReady(false);

            audioPlayer.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int i) {
                    switch (i) {
                        case ExoPlayer.STATE_IDLE:
                            break;
                        case ExoPlayer.STATE_PREPARING:
                            audioEventListener.onLoading();
                            break;
                        case ExoPlayer.STATE_BUFFERING:
                            audioEventListener.onLoading();
                            break;
                        case ExoPlayer.STATE_READY:
                            audioEventListener.onPrepared();
                            break;
                        case ExoPlayer.STATE_ENDED:
                            audioEventListener.onCompletion();
                            release();
                            break;
                    }
                }

                @Override
                public void onPlayWhenReadyCommitted() {
                }

                @Override
                public void onPlayerError(ExoPlaybackException e) {
                    audioEventListener.onError();
                }
            });
        }

        public void prepareAndStart() {
            audioEventListener.onLoading();
            prepare();
            start();
        }

    }

    public static class EpisodeStream extends BaseStream {
        public final String audioUrl;
        public final String programSlug;
        public final int durationSeconds;

        public EpisodeStream(String audioUrl, String programSlug, int durationSeconds, Context context) {
            super(context);

            String mimeType = StreamManager.getMimeTypeFromAudioUrl(audioUrl);
            AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(context, USER_AGENT,
                    Uri.parse(audioUrl), StreamManager.getExtractorFromContentType(mimeType));

            audioPlayer = new AudioPlayer(builder);
            audioPlayer.setPlayWhenReady(false);

            audioPlayer.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int i) {
                    switch (i) {
                        case ExoPlayer.STATE_IDLE:
                            break;
                        case ExoPlayer.STATE_PREPARING:
                            break;
                        case ExoPlayer.STATE_BUFFERING:
                            break;
                        case ExoPlayer.STATE_READY:
                            audioEventListener.onPrepared();
                            break;
                        case ExoPlayer.STATE_ENDED:
                            audioEventListener.onCompletion();
                            release();
                            break;
                    }
                }

                @Override
                public void onPlayWhenReadyCommitted() {
                }

                @Override
                public void onPlayerError(ExoPlaybackException e) {
                    audioEventListener.onError();
                }
            });

            this.audioUrl = audioUrl;
            this.durationSeconds = durationSeconds;
            this.programSlug = programSlug;
        }

        @Override
        public void release() {
            if (mStreamManager != null) {
                if (mStreamManager.currentEpisodePlayer == this) {
                    mStreamManager.currentEpisodePlayer = null;
                }
            }

            super.release();
        }

        @Override
        public void prepareAndStart() {
            if (mStreamManager != null) {
                mStreamManager.currentEpisodePlayer = this;
            }

            super.prepareAndStart();
        }
    }

    public static class LiveStream extends BaseStream {
        // We get preroll directly from Triton so we always use the skip-preroll url.
        // Also send the UA for logs.
        // Using Uri.parse because there seems to be a bug in Lollipop that causes
        // the stream to take 20 seconds to start playing. Uri.parse is a workaround.
        public final static String HLS_URL =
                String.format(AppConfiguration.instance.getConfig("livestream.url"),
                        BuildConfig.VERSION_NAME,
                        String.valueOf(BuildConfig.VERSION_CODE)
                );

        public final static int EDGE_OFFSET_MS = 60 * 1000;
        public final static int JUMP_INTERVAL_SEC = 30;

        public final PrerollCompleteCallback prerollCompleteCallback;
        public ScheduleOccurrence currentSchedule = null;
        public long pausedAt = 0;
        public boolean startLive = true;

        public LiveStream(Context context) {
            super(context);

            AudioPlayer.RendererBuilder builder = new HlsRendererBuilder(context, USER_AGENT, HLS_URL);
            audioPlayer = new AudioPlayer(builder);
            audioPlayer.setPlayWhenReady(false);

            audioPlayer.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int i) {
                    switch (i) {
                        case ExoPlayer.STATE_IDLE:
                            break;
                        case ExoPlayer.STATE_PREPARING:
                            break;
                        case ExoPlayer.STATE_BUFFERING:
                            break;
                        case ExoPlayer.STATE_READY:
                            boolean sl = startLive;
                            boolean p = isPaused;
                            boolean pl = isPlaying();
                            long d = getDuration();

                            if (startLive && getDuration() > 0) {
                                startLive = false;
                                seekToLive();
                            }

                            break;
                        case ExoPlayer.STATE_ENDED:
                            break;
                    }
                }

                @Override
                public void onPlayWhenReadyCommitted() {
                }

                @Override
                public void onPlayerError(ExoPlaybackException e) {
                    audioEventListener.onError();
                    release();
                }
            });

            this.prerollCompleteCallback = new PrerollCompleteCallback();

            if (mStreamManager != null) {
                mStreamManager.currentLivePlayer = this;
            }
        }

        @Override
        public void release() {
            if (mStreamManager != null) {
                // We don't want to call stop here, because in the case where the live stream was
                // stopped and started again in the same screen, the audio focus is lost at the same
                // time that the new player is being setup, and the button states collide.
                if (mStreamManager.currentLivePlayer == this) {
                    // The currentLivePlayer may have already been taken over.
                    // If not, clear this one out.
                    mStreamManager.currentLivePlayer = null;
                }
            }

            super.release();
        }

        public void prepareAndStartFromLive() {
            startLive = true;
            super.prepareAndStart();
        }

        public void seekToLive() {
            // Stay a few seconds behind edge to avoid stalling.
            seekTo(getDuration() - EDGE_OFFSET_MS);
        }

        public long windowStartUTS() {
            return System.currentTimeMillis() - getDuration();
        }

        private class PrerollCompleteCallback {
            public void onPrerollComplete() {
                prepareAndStartFromLive();
            }
        }
    }

    public static class PrerollStream extends BaseStream {
        final Context context;
        LiveStream.PrerollCompleteCallback prerollCompleteCallback;

        public PrerollStream(Context context) {
            super(context);
            this.context = context;
        }

        // We want to be able to rely on the preroll player existing, but we don't know if it will
        // actually be playing anything, so this method acts as sort of a "deferred constructor".
        public void setupPreroll(PrerollManager.PrerollData prerollData) {
            AudioPlayer.RendererBuilder builder = new ExtractorRendererBuilder(this.context, USER_AGENT,
                    Uri.parse(prerollData.audioUrl), StreamManager.getExtractorFromContentType(prerollData.audioType));

            audioPlayer = new AudioPlayer(builder);
            audioPlayer.setPlayWhenReady(false);

            audioPlayer.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int i) {
                    switch (i) {
                        case ExoPlayer.STATE_IDLE:
                            break;
                        case ExoPlayer.STATE_PREPARING:
                            break;
                        case ExoPlayer.STATE_BUFFERING:
                            break;
                        case ExoPlayer.STATE_READY:
                            audioEventListener.onPrepared();
                            break;
                        case ExoPlayer.STATE_ENDED:
                            audioEventListener.onCompletion();
                            prerollCompleteCallback.onPrerollComplete();
                            release();
                            break;
                    }
                }

                @Override
                public void onPlayWhenReadyCommitted() {
                }

                @Override
                public void onPlayerError(ExoPlaybackException e) {
                    audioEventListener.onError();
                    prerollCompleteCallback.onPrerollComplete();
                    release();
                }
            });

            if (mStreamManager != null) {
                mStreamManager.currentPrerollPlayer = this;
            }
        }

        @Override
        public void seekTo(long pos) {
            // Can't seek preroll.
        }

        @Override
        public void start() {
            super.start();
            PrerollManager.LAST_PREROLL_PLAY = System.currentTimeMillis();
        }

        @Override
        public void release() {
            if (mStreamManager != null) {
                if (mStreamManager.currentPrerollPlayer == this) {
                    mStreamManager.currentPrerollPlayer = null;
                }
            }

            super.release();
        }

        // We're overriding this because we don't want to show an error if the preroll can't
        // play. Instead we should just continue with the stream.
        @Override
        public void prepareAndStart() {
            audioEventListener.onLoading();

            try {
                requestAudioFocus();
                prepare();
                start();
            } catch (AudioFocusNotGrantedException e) {
                // No preroll, do nothing.
            }
        }

        public void setOnPrerollCompleteCallback(LiveStream.PrerollCompleteCallback prerollCompleteCallback) {
            this.prerollCompleteCallback = prerollCompleteCallback;
        }
    }

    // This class is responsible for orchestrating the interaction between livestream and preroll.
    public static class LiveStreamBundle {
        public final LiveStream liveStream;
        public final PrerollStream prerollStream;
        private final Context context;

        public LiveStreamBundle(Context context) {
            this.context = context;
            this.liveStream = new LiveStream(context);
            this.prerollStream = new PrerollStream(context);
        }

        public LiveStreamBundle(Context context, LiveStream liveStream) {
            this.context = context;
            this.liveStream = liveStream;
            this.prerollStream = new PrerollStream(context);
        }

        public LiveStreamBundle(Context context, LiveStream liveStream, PrerollStream prerollStream) {
            this.context = context;
            this.liveStream = liveStream;
            this.prerollStream = prerollStream;
        }

        public void playWithPrerollAttempt() {
            liveStream.audioEventListener.onLoading();

            // If they just installed the app (less than 10 minutes ago), and have never played the live
            // stream, don't play preroll.
            // Otherwise do the normal preroll flow.
            final long now = System.currentTimeMillis();

            boolean hasPlayedLiveStream = DataManager.instance.getHasPlayedLiveStream();
            boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
            boolean heardPrerollRecently = PrerollManager.LAST_PREROLL_PLAY > (now - PrerollManager.PREROLL_THRESHOLD);

            if (!AppConfiguration.instance.isDebug && ((!hasPlayedLiveStream && installedRecently) || heardPrerollRecently)) {
                // Skipping Preroll
                liveStream.prepareAndStartFromLive();
            } else {
                // Normal preroll flow (preroll still may not play, based on preroll response)
                PrerollManager.instance.getPrerollData(context, new PrerollManager.PrerollCallbackListener() {
                    @Override
                    public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                        if (prerollData == null || prerollData.audioUrl == null) {
                            liveStream.prepareAndStartFromLive();
                        } else {
                            prerollStream.setupPreroll(prerollData);
                            prerollStream.audioEventListener.onPrerollData(prerollData);
                            prerollStream.setOnPrerollCompleteCallback(liveStream.prerollCompleteCallback);
                            prerollStream.prepareAndStart();
                        }
                    }
                });
            }

            if (!hasPlayedLiveStream) {
                DataManager.instance.setHasPlayedLiveStream(true);
            }
        }
    }
    public class LocalBinder extends Binder {
        public StreamManager getService() {
            return StreamManager.this;
        }
    }
}
