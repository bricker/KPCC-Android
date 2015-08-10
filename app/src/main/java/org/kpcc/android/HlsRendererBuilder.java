package org.kpcc.android;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

import java.io.IOException;

public class HlsRendererBuilder implements AudioPlayer.RendererBuilder, ManifestCallback<HlsPlaylist> {

    private static final int BUFFER_SEGMENT_SIZE = 256 * 1024;
    private static final int BUFFER_SEGMENTS = 64;

    private final Context context;
    private final String userAgent;
    private final String url;

    private AudioPlayer player;
    private AudioPlayer.RendererBuilderCallback callback;

    public HlsRendererBuilder(Context context, String userAgent, String url) {
        this.context = context;
        this.userAgent = userAgent;
        this.url = url;
    }

    @Override
    public void buildRenderers(AudioPlayer player, AudioPlayer.RendererBuilderCallback callback) {
        this.player = player;
        this.callback = callback;
        HlsPlaylistParser parser = new HlsPlaylistParser();
        ManifestFetcher<HlsPlaylist> playlistFetcher = new ManifestFetcher<>(url,
                new DefaultUriDataSource(context, userAgent), parser);
        playlistFetcher.singleLoad(player.getMainHandler().getLooper(), this);
    }

    @Override
    public void onSingleManifestError(IOException e) {
        callback.onRenderersError(e);
    }

    @Override
    public void onSingleManifest(HlsPlaylist manifest) {
        Handler mainHandler = player.getMainHandler();
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        HlsChunkSource chunkSource = new HlsChunkSource(dataSource, url, manifest, bandwidthMeter,
                null, HlsChunkSource.ADAPTIVE_MODE_SPLICE, null);
        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl,
                BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, null, AudioPlayer.TYPE_AUDIO);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        TrackRenderer[] renderers = new TrackRenderer[1];
        renderers[AudioPlayer.TYPE_AUDIO] = audioRenderer;
        callback.onRenderers(renderers, bandwidthMeter);
    }
}
