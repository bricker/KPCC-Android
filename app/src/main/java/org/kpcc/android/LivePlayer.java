package org.kpcc.android;

import android.content.Context;

import com.google.android.exoplayer.TimeRange;

/**
 * Created by rickb014 on 4/3/16.
 */
public class LivePlayer extends Stream {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
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

    public static final StreamOption STREAM_STANDARD = new StreamOption("standard", R.string.kpcc_live);
    public static final StreamOption STREAM_XFS = new StreamOption("xfs", R.string.kpcc_pfs);
    public static final StreamOption[] STREAM_OPTIONS = {STREAM_STANDARD, STREAM_XFS};

    private final static int JUMP_INTERVAL_SEC = 30;
    final static int JUMP_INTERVAL_MS = JUMP_INTERVAL_SEC*1000;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String getStreamUrl() {
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
    final private PrerollCompleteCallback mPrerollCompleteCallback;
    private long mPausedAt = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    LivePlayer(final Context context) {
        super(context);

        AudioPlayer.RendererBuilder builder = new HlsRendererBuilder(context, USER_AGENT, LivePlayer.getStreamUrl());
        setAudioPlayer(new AudioPlayer(builder));
        getAudioPlayer().setPlayWhenReady(false);

        getAudioPlayer().addListener(this);
        mPrerollCompleteCallback = new PrerollCompleteCallback();

        if (getStreamManager() != null) {
            getStreamManager().setCurrentLivePlayer(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    PrerollCompleteCallback getPrerollCompleteCallback() {
        return mPrerollCompleteCallback;
    }

    synchronized long getPausedAt() {
        return mPausedAt;
    }

    synchronized void setPausedAt(long pausedAt) {
        mPausedAt = pausedAt;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    void pause() {
        super.pause();
        setPausedAt(System.currentTimeMillis());
    }

    @Override
    void stop() {
        super.stop();
        setPausedAt(System.currentTimeMillis());
    }

    @Override // Stream
    void release() {
        if (getStreamManager() != null && getStreamManager().getCurrentLivePlayer() == this) {
            getStreamManager().setCurrentLivePlayer(null);
        }

        super.release();
    }

    @Override
    void seekTo(long pos) {
        super.seekTo(pos);
    }

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

    void pauseTemporary() {
        pause();
        setIsTemporarilyPaused(true);
    }

    void playIfTemporarilyPaused() {
        if (isPaused() && isTemporarilyPaused()) {
            try {
                play();
            } catch (IllegalStateException e) {
                prepareAndStart();
            }
        }

        setIsTemporarilyPaused(false);
    }

    long getPlaybackTimestamp() {
        return System.currentTimeMillis() - relativeMsBehindLive();
    }

    private TimeRange getAvailableRange() {
        return getAudioPlayer() == null ? null : getAudioPlayer().getPlayerControl().getAvailableRange();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected class PrerollCompleteCallback {
        protected void onPrerollComplete() {
            prepareAndStart();
        }
    }

}
