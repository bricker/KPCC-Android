package org.kpcc.android;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

/**
 * Created by rickb014 on 4/3/16.
 */
public class LivePlayer extends Stream {
    public static class StreamOption {
        private String key;
        private int id;

        public StreamOption(String key, int id) {
            this.key = key;
            this.id = id;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static final StreamOption STREAM_STANDARD = new StreamOption("standard", R.string.kpcc_live);
    public static final StreamOption STREAM_XFS = new StreamOption("xfs", R.string.kpcc_pfs);
    public static final StreamOption[] STREAM_OPTIONS = {STREAM_STANDARD, STREAM_XFS};

    private final static int JUMP_INTERVAL_SEC = 30;
    final static int JUMP_INTERVAL_MS = JUMP_INTERVAL_SEC*1000;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static String getStreamUrl() {
        String baseUrl = "";
        if (isXfs()) {
            baseUrl = XFSManager.getInstance().getPfsUrl();
        }

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = AppConfiguration.getInstance().getConfig("livestreamUrl");
        }

        return String.format(baseUrl + "?ua=KPCCAndroid-%s-%s",
                BuildConfig.VERSION_NAME,
                String.valueOf(BuildConfig.VERSION_CODE)
        );
    }

    public static boolean isXfs() {
        XFSManager xfsManager = XFSManager.getInstance();
        String streamPref = DataManager.getInstance().getStreamPreference();
        return xfsManager.isDriveActive() && streamPref.equals(LivePlayer.STREAM_XFS.getKey());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    LivePlayer(Context context) {
        super(context);

        Handler mainHandler = new Handler();
        TrackSelection.Factory trackSelectionFactory = new FixedTrackSelection.Factory();
        TrackSelector trackSelector = new DefaultTrackSelector(mainHandler, trackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);

//        AudioPlayer.RendererBuilder builder = new HlsRendererBuilder(context, Stream.USER_AGENT, );
//
//        AudioPlayer player = new AudioPlayer(builder);
        player.setPlayWhenReady(false);
        player.addListener(this);
        setAudioPlayer(player);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    boolean canSeekBackward() {
        return getCurrentPosition() > getLowerBoundMs() + JUMP_INTERVAL_MS;
    }

    @Override
    boolean canSeekForward() {
        return getAudioPlayer() != null &&
                getCurrentPosition() < getUpperBoundMs() - LivePlayer.JUMP_INTERVAL_MS;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void seekToLive() {
        seekTo(0);
    }

    void skipBackward() {
        if (canSeekBackward()) {
            seekTo(getCurrentPosition() - JUMP_INTERVAL_MS);
        }
    }

    void skipForward() {
        if (canSeekForward()) {
            seekTo(getCurrentPosition() + JUMP_INTERVAL_MS);
        }
    }

    private long getLowerBoundMs() {
        TimeRange range = getAvailableRange();
        if (range == null) return -1;
        return range.getCurrentBoundsMs(null)[0];
    }

    long getUpperBoundMs() {
        TimeRange range = getAvailableRange();
        if (range == null) return -1;
        return range.getCurrentBoundsMs(null)[1];
    }

    long relativeMsBehindLive() {
        return getUpperBoundMs() - getCurrentPosition();
    }

    long getPlaybackTimestamp() {
        return System.currentTimeMillis() - relativeMsBehindLive();
    }

    private TimeRange getAvailableRange() {
        return getAudioPlayer() == null ? null : getAudioPlayer().getPlayerControl().getAvailableRange();
    }
}
