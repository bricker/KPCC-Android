package org.kpcc.android;

import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. It can be prepared
 * with one of a number of {@link RendererBuilder} classes to suit different use cases (e.g. DASH,
 * SmoothStreaming and so on).
 */
public class AudioPlayer implements
        ExoPlayer.Listener,
        HlsSampleSource.EventListener,
        MediaCodecAudioTrackRenderer.EventListener {

    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Constructs the necessary components for playback.
         *
         * @param player The parent player.
         * @param callback The callback to invoke with the constructed components.
         */
        void buildRenderers(AudioPlayer player, RendererBuilderCallback callback);
    }

    /**
     * A callback invoked by a {@link RendererBuilder}.
     */
    public interface RendererBuilderCallback {
        /**
         * Invoked with the results from a {@link RendererBuilder}.
         * @param renderers Renderers indexed by {@link AudioPlayer} TYPE_* constants. An individual
         *     element may be null if there do not exist tracks of the corresponding type.
         */
        void onRenderers(TrackRenderer[] renderers);
        /**
         * Invoked if a {@link RendererBuilder} encounters an error.
         *
         * @param e Describes the error.
         */
        void onRenderersError(Exception e);
    }

    public static final int RENDERER_COUNT = 1;
    public static final int TYPE_AUDIO = 0;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private final RendererBuilder rendererBuilder;
    private final ExoPlayer player;
    private final Handler mainHandler;
    private final PlayerControl playerControl;

    private int rendererBuildingState;

    private InternalRendererBuilderCallback builderCallback;

    private TrackRenderer[] trackRenderers;

    public AudioPlayer(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        this.playerControl = new PlayerControl(player);
        mainHandler = new Handler();
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        if (builderCallback != null) {
            builderCallback.cancel();
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        builderCallback = new InternalRendererBuilderCallback();
        rendererBuilder.buildRenderers(this, builderCallback);
    }

    /* package */ void onRenderers(TrackRenderer[] renderers) {
        builderCallback = null;
        this.trackRenderers = renderers;

        player.prepare(renderers);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void addListener(ExoPlayer.Listener listener) {
        player.addListener(listener);
    }

    public boolean isPlaying() {
        return playerControl.isPlaying();
    }

    public void setVolume(float volume) {
        try {
            trackRenderers[TYPE_AUDIO].handleMessage(MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
        } catch (ExoPlaybackException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        playerControl.start();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void pause() {
        playerControl.pause();
    }

    public void stop() {
        player.stop();
    }

    public void release() {
        if (builderCallback != null) {
            builderCallback.cancel();
            builderCallback = null;
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        player.release();
    }

    /* package */ Handler getMainHandler() {
        return mainHandler;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        Log.d("AudioPlayer", "called onPlayerStateChanged");
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        Log.d("AudioPlayer", "called onPlayerError");
    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger, int mediaTimeMs) {
        Log.d("AudioPlayer", "called onDownstreamFormatChanged");
    }

    @Override
    public void onLoadError(int sourceId, IOException e) {
        Log.d("AudioPlayer", "called onLoadError");
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        Log.d("AudioPlayer", "called onPlayWhenReadyCommitted");
    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                              int mediaStartTimeMs, int mediaEndTimeMs) {
        Log.d("AudioPlayer", "called onLoadStarted");
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                                int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        Log.d("AudioPlayer", "called onLoadCompleted");
    }

    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
        Log.d("AudioPlayer", "called onLoadCanceled");
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, int mediaStartTimeMs, int mediaEndTimeMs) {
        Log.d("AudioPlayer", "called onUpstreamDiscarded");
    }

    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
        Log.d("AudioPlayer", "called onAudioTrackInitializationError");
    }

    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {
        Log.d("AudioPlayer", "called onAudioTrackWriteError");
    }

    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
        Log.d("AudioPlayer", "called onDecoderInitializationError");
    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        Log.d("AudioPlayer", "called onCryptoError");
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                     long initializationDurationMs) {
        Log.d("AudioPlayer", "called onDecoderInitialized");
    }

    private class InternalRendererBuilderCallback implements RendererBuilderCallback {
        private boolean canceled;

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRenderers(TrackRenderer[] renderers) {
            if (!canceled) {
                AudioPlayer.this.onRenderers(renderers);
            }
        }

        @Override
        public void onRenderersError(Exception e) {
        }

    }

}
