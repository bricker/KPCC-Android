package org.kpcc.android;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;

public class ExtractorRendererBuilder implements AudioPlayer.RendererBuilder {

    private static final int BUFFER_SIZE = 10 * 1024 * 1024;

    private final Context context;
    private final String userAgent;
    private final Uri uri;
    private final Extractor extractor;

    public ExtractorRendererBuilder(Context context, String userAgent, Uri uri, Extractor extractor) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
        this.extractor = extractor;
    }

    @Override
    public void buildRenderers(AudioPlayer player, AudioPlayer.RendererBuilderCallback callback) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, extractor,
                BUFFER_SIZE);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                null, true, player.getMainHandler(), null);

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[1];
        renderers[AudioPlayer.TYPE_AUDIO] = audioRenderer;
        callback.onRenderers(renderers);
    }

}
