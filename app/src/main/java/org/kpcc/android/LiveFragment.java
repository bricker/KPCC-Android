package org.kpcc.android;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.ScheduleOccurrence;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class LiveFragment extends StreamBindFragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static final String STACK_TAG = "LiveFragment";
    private static final String ASSET_AUDIO_REWIND_MP3 = "asset:///audio/rewind.mp3";
    public static final int LIVE_SEEKBAR_REFRESH_INTERVAL = 1000;
    private static final String PLAY_START_KEY = "liveStreamPlayStart";
    private static final String LIVESTREAM_LISTENER_KEY = "livestream";

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mAdView;
    private AudioButtonManager mAudioButtonManager;
    private LiveSeekViewManager mLiveSeekViewManager;
    private ProgressBar mPrerollProgressBar;
    private PeriodicBackgroundUpdater mLiveSeekBarUpdater; // Updates live stream seek bar
    private PeriodicBackgroundUpdater mPrerollSeekBarUpdater; // Updates preroll seek bar
    private PeriodicBackgroundUpdater mLiveStatusUpdater; // Updates live status
    private PeriodicBackgroundUpdater mScheduleUpdater; // Updates program info
    private PeriodicBackgroundUpdater mTimerUpdater; // Updates sleep timer
    private View mPrerollView;
    private ImageView mBackground;
    private Request mRequest;
    private TextView mTimerRemaining;
    private LinearLayout mTimerRemainingWrapper;
    private ScheduleOccurrence mCurrentSchedule = null;
    private final AtomicBoolean mScheduleUpdaterMutex = new AtomicBoolean(false);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     *
     * @param savedInstanceState
     */
    @Override // Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    /**
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override  // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            if (LivePlayer.isXfs()) {
                activity.setTitle(R.string.kpcc_pfs);
            } else {
                activity.setTitle(R.string.kpcc_live);
            }
        }

        View view = inflater.inflate(R.layout.fragment_live, container, false);

        mBackground = (ImageView) view.findViewById(R.id.background);
        mTitle = (TextView) view.findViewById(R.id.live_title);
        mStatus = (TextView) view.findViewById(R.id.live_status);
        mAdView = (NetworkImageView) view.findViewById(R.id.preroll_ad);
        mPrerollProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mPrerollView = view.findViewById(R.id.preroll);
        mTimerRemaining = (TextView)view.findViewById(R.id.timer_remaining);
        mTimerRemainingWrapper = (LinearLayout)view.findViewById(R.id.timer_remaining_wrapper);

        mAudioButtonManager = new AudioButtonManager(view);
        mLiveSeekViewManager = new LiveSeekViewManager(view);

        // Register a listener to start/stop the stream immediately based on connectivity status.
        // We had a process in place to handle this lazily but there was a certain device which
        // wasn't releasing the stream, and ended up creating a new stream every time a network
        // was available and simultaneously streaming dozens (or more) streams at once. I think.
        // It's hard to tell for sure but this is what I guess was happening.
        // So, we're being more proactive about managing streams on network connectivity changes.
        if (activity != null) {
            bindStreamService();
            AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(getActivity(), LIVESTREAM_LISTENER_KEY,
                    false, getStreamServiceUnsafe(), new LiveStreamConnectivityListener());
        }

        initNotificationBuilder(R.drawable.menu_antenna, getString(R.string.kpcc_live));

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppConnectivityManager.getInstance().isConnectedToNetwork()) {
                    // The Error message should already be showing for connectivity problems.
                    // Just do nothing.
                    return;
                }

                initAudio();

                StreamService service = getStreamService();
                if (service != null) {
                    NotificationCompat.Builder builder = getNotificationBuilder();
                    if (builder != null) {
                        service.startForeground(Stream.NOTIFICATION_ID, builder.build());
                    }


                }

                LivePlayer stream = getLivePlayer();
                PrerollPlayer preroll = getPrerollPlayer();

                if (preroll != null && preroll.isPaused()) {
                    preroll.play();
                    return;
                }

                if (stream != null && stream.isPaused() && stream.getPausedAt() > (System.currentTimeMillis() - 1000 * 60 * 60 * 8)) {
                    // Stream was paused; start it again.
                    stream.play();
                    return;
                }

                // Initialize the lives stream/preroll bundle.
                final LivePlayer livePlayer = new LivePlayer(getActivity());
                final PrerollPlayer prerollPlayer = new PrerollPlayer(getActivity());

                livePlayer.setAudioEventListener(new LivePlayerAudioEventListener());
                prerollPlayer.setAudioEventListener(new PrerollPlayerAudioEventListener());

                // If they just installed the app (less than 10 minutes ago), and have never played the live
                // stream, don't play preroll.
                // Otherwise do the normal preroll flow.
                final long now = System.currentTimeMillis();

                final PrerollManager prerollManager = new PrerollManager();
                boolean hasPlayedLiveStream = DataManager.getInstance().getHasPlayedLiveStream();
                boolean installedRecently = KPCCApplication.INSTALLATION_TIME > (now - PrerollManager.INSTALL_GRACE);
                boolean heardPrerollRecently = prerollManager.getLastPlay() > (now - PrerollManager.PREROLL_THRESHOLD);

                if (!AppConfiguration.getInstance().getConfigBool("preroll.enabled") ||
                        (!hasPlayedLiveStream && installedRecently) ||
                        heardPrerollRecently) {

                    // Skipping Preroll
                    setCurrentStream(livePlayer, STACK_TAG);
                    livePlayer.prepareAndStart();
                    analyticsLogPlayEvent(livePlayer);
                } else {
                    prerollPlayer.getAudioEventListener().onPreparing();

                    final MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        // Normal preroll flow (preroll still may not play, based on preroll response)
                        prerollManager.getPrerollData(activity, new PrerollManager.PrerollCallbackListener() {
                            @Override
                            public void onPrerollResponse(final PrerollManager.PrerollData prerollData) {
                                if (prerollData == null || prerollData.getAudioUrl() == null) {
                                    setCurrentStream(livePlayer, STACK_TAG);
                                    livePlayer.prepareAndStart();
                                    analyticsLogPlayEvent(livePlayer);
                                } else {
                                    setCurrentStream(prerollPlayer, STACK_TAG);
                                    StreamService service = getStreamService();
                                    if (service != null) {
                                        MainActivity activity = (MainActivity) getActivity();
                                        if (activity != null) {
                                            prerollPlayer.setupPreroll(activity, prerollData.getAudioUrl());
                                            prerollPlayer.getAudioEventListener().onPrerollData(prerollData);
                                            prerollPlayer.prepareAndStart();
                                            prerollManager.setLastPlayToNow();

                                            livePlayer.prepare();
                                            prerollPlayer.setOnPrerollCompleteCallback(new PrerollPlayer.PrerollCompleteCallback() {
                                                @Override
                                                void onPrerollComplete() {
                                                    setCurrentStream(livePlayer, STACK_TAG);
                                                    livePlayer.play();
                                                    analyticsLogPlayEvent(livePlayer);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        });
                    }
                }

                if (!hasPlayedLiveStream) {
                    DataManager.getInstance().setHasPlayedLiveStream(true);
                }

                AppConfiguration.getInstance().setDynamicProperty(PLAY_START_KEY, String.valueOf(System.currentTimeMillis()));
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LivePlayer livePlayer = getLivePlayer();
                PrerollPlayer prerollPlayer = getPrerollPlayer();
                if (livePlayer != null && livePlayer.isPlaying()) {
                    livePlayer.pause();
                    analyticsLogPauseEvent(livePlayer);
                }

                if (prerollPlayer != null && prerollPlayer.isPlaying()) {
                    prerollPlayer.pause();
                    // We don't log pauses for preroll.
                }
            }
        });

        mTimerRemainingWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer index = Navigation.NavigationItem.getIndexByStackTag(AlarmFragment.STACK_TAG);

                if (index != null) {
                    NavigationDrawerFragment navigationDrawerFragment = getNavigationDrawerFragment();
                    if (navigationDrawerFragment == null) return;

                    navigationDrawerFragment.selectItem(index);
                }
            }
        });

        mLiveSeekViewManager.disableSeekBar();
        mLiveSeekViewManager.getSeekBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                // The jump buttons COULD use this function, but since we know exactly how much they are
                // jumping it's more comfortable to just update the seekbar and livestream manually in
                // those cases.
                if (!fromUser) {
                    return;
                }

                LivePlayer stream = getLivePlayer();
                if (stream == null) return;

                if (progress >= mLiveSeekViewManager.getLiveHeadProgress()) {
                    // Prevent from seeking past live head.
                    mLiveSeekViewManager.setSeekProgressToLiveHead();
                    analyticsLogSeekEvent("scrubber", stream.relativeMsBehindLive());
                    stream.seekToLive();
                    return;
                }

                long programStartPositionInHLSWindow = mLiveSeekViewManager.getProgramStartWindowPositionMs(stream.getUpperBoundMs());
                long seekPos = programStartPositionInHLSWindow + progress;
                analyticsLogSeekEvent("scrubber", seekPos - stream.getCurrentPosition());
                stream.seekTo(seekPos);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mLiveSeekViewManager.getRewindBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePlayer stream = getLivePlayer();
                if (stream == null) return;

                analyticsLogSeekEvent("button", -LivePlayer.JUMP_INTERVAL_MS);
                stream.skipBackward();
                mLiveSeekViewManager.skipBackward(stream.canSeekBackward());

                // This prevents a schedule update unless we pass the seek boundaries for this program.
                if (mLiveSeekViewManager.hasExceededLowerSeekBound()) {
                    updateSchedule();
                }
            }
        });

        mLiveSeekViewManager.getForwardBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePlayer stream = getLivePlayer();
                if (stream == null) return;

                analyticsLogSeekEvent("button", LivePlayer.JUMP_INTERVAL_MS);
                stream.skipForward();
                mLiveSeekViewManager.skipForward(stream.canSeekForward());

                // This prevents a schedule update unless we pass the seek boundaries for this program.
                if (mLiveSeekViewManager.hasExceededUpperSeekBound()) {
                    updateSchedule();
                }
            }
        });

        mLiveSeekViewManager.getGoLiveBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playRewindBeat(new Stream.AudioEventListener() {
                    @Override
                    public void onPlay() {
                        LivePlayer stream = getLivePlayer();
                        if (stream == null) return;

                        // These must happen in this order so the states don't override each other.
                        mLiveStatusUpdater.pause();
                        stream.pause();
                        mAudioButtonManager.toggleLoading();
                        mAudioButtonManager.lock();

                        analyticsLogSeekEvent("back-to-live-button", stream.relativeMsBehindLive());

                        stream.seekToLive();
                    }

                    @Override
                    public void onCompletion() {
                        LivePlayer stream = getLivePlayer();
                        if (stream == null) return;

                        mAudioButtonManager.unlock();
                        stream.play();
                        mLiveStatusUpdater.resume();
                    }

                    @Override
                    void onError() {
                        mAudioButtonManager.unlock();
                        mLiveStatusUpdater.resume();
                    }
                });
            }
        });

        mLiveSeekViewManager.getProgramStartBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playRewindBeat(new Stream.AudioEventListener() {
                    @Override
                    public void onPlay() {
                        LivePlayer stream = getLivePlayer();
                        if (stream == null) return;

                        // These must happen in this order so the states don't override each other.
                        mLiveStatusUpdater.pause();
                        stream.pause();
                        mAudioButtonManager.toggleLoading();
                        mAudioButtonManager.lock();

                        long programStartPositionInHLSWindow = mLiveSeekViewManager.getProgramStartWindowPositionMs(stream.getUpperBoundMs());
                        analyticsLogSeekEvent("rewind-to-start-button", programStartPositionInHLSWindow - stream.getCurrentPosition());

                        stream.seekTo(programStartPositionInHLSWindow);
                    }

                    @Override
                    public void onCompletion() {
                        LivePlayer stream = getLivePlayer();
                        if (stream == null) return;

                        mAudioButtonManager.unlock();
                        stream.play();
                        mLiveStatusUpdater.resume();
                    }

                    @Override
                    void onError() {
                        mAudioButtonManager.unlock();
                    }
                });
            }
        });

        return view;
    }


    /**
     *
     */
    @Override  // Fragment
    public void onResume() {
        super.onResume();

        mScheduleUpdaterMutex.set(false);

        // Setup runners
        mLiveStatusUpdater = new PeriodicBackgroundUpdater(new LiveStatusUpdater(), 1000);
        mScheduleUpdater = new PeriodicBackgroundUpdater(new ScheduleUpdater(), 1000 * 60 * 5); // Run the thread every 5 minutes

        if (BaseAlarmManager.SleepManager.getInstance().isRunning()) {
            mTimerRemainingWrapper.setVisibility(View.VISIBLE);
            mTimerUpdater = new PeriodicBackgroundUpdater(new TimerUpdater(), 1000);
            mTimerUpdater.start();
        } else {
            mTimerRemainingWrapper.setVisibility(View.GONE);
        }

        // Setup connectivity handlers
        // This will init state immediately if stream is already bound.
        // We're putting it here (in onResume) so it can change for network updates.
        getStreamConnection().addOnStreamBindListener(LiveFragment.STACK_TAG, new StreamServiceConnection.OnStreamBindListener() {
            @Override
            public void onBind() {
                // When returning to this fragment, this will probably be run right away and setup the view state.
                initAudio();

                // This must come after calling initAudio(), otherwise the play button could be
                // clicked before we have the chance to hide it if necessary.
                if (DataManager.getInstance().getPlayNow()) {
                    DataManager.getInstance().setPlayNow(false);

                    // Play right away
                    mAudioButtonManager.clickPlay();
                }
            }
        });

        // Listener for starting/stopping the Schedule updater on connectivity loss.
        // The callbacks below will get run right away whatever the state is. We want to start it
        // no matter what to setup the default state, and then let the callbacks handle the
        // connected state. Calling start() again should be safe and not start a second thread.
        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(getActivity(), LiveFragment.STACK_TAG, true, getStreamService(),
                new AppConnectivityManager.NetworkConnectivityListener() {
                    @Override
                    public void onConnect(Context context, StreamService streamService) {
                        // We want this here so the schedule will get updated immediately when connectivity
                        // is back, so we'll just restart it.
                        if (mScheduleUpdater != null) mScheduleUpdater.start();
                        mAudioButtonManager.hideError();
                    }

                    @Override
                    public void onDisconnect(Context context, StreamService streamService) {
                        if (mScheduleUpdater != null) mScheduleUpdater.release();
                        setDefaultScheduleValues();
                        mAudioButtonManager.showError(R.string.network_error);
                    }
                }
        );

        LivePlayer livePlayer = getLivePlayer();
        if (livePlayer != null) {
            analyticsLogForegroundEvent(livePlayer);
            livePlayer.setBackgroundStart(-1);
        }
    }

    /**
     *
     */
    @Override  // Fragment
    public void onPause() {
        if (mRequest != null) { mRequest.cancel(); }
        mScheduleUpdaterMutex.set(false);

        // Release all the updater threads
        if (mScheduleUpdater != null) mScheduleUpdater.release();
        if (mTimerUpdater != null) mTimerUpdater.release();
        if (mLiveSeekBarUpdater != null) mLiveSeekBarUpdater.release();
        if (mPrerollSeekBarUpdater != null) mPrerollSeekBarUpdater.release();
        if (mLiveStatusUpdater != null) mLiveStatusUpdater.release();

        AppConnectivityManager.getInstance().removeOnNetworkConnectivityListener(LiveFragment.STACK_TAG);

        LivePlayer livePlayer = getLivePlayer();
        if (livePlayer != null) {
            analyticsLogBackgroundEvent(livePlayer);
            livePlayer.setBackgroundStart(System.currentTimeMillis());
        }

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_items, menu);

        XFSManager.getInstance().loadSettings(new XFSManager.XFSSettingsLoadCallback() {
            @Override
            void onSettingsLoaded() {
                MenuItem item = menu.findItem(R.id.action_streamselect);
                if (item != null) {
                    if (XFSManager.getInstance().isDriveActive()) {
                        item.setVisible(true);
                    } else {
                        item.setVisible(false);
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_streamselect:
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.container, new StreamSelectFragment(), StreamSelectFragment.STACK_TAG)
                        .addToBackStack(LiveFragment.STACK_TAG)
                        .commit();

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param stream
     */
    private void analyticsLogPlayEvent(LivePlayer stream) {
        String status;

        float msBehindLive = stream.relativeMsBehindLive();
        if (msBehindLive < LivePlayer.JUMP_INTERVAL_MS) {
            status = "LIVE";
        } else {
            status = String.format(Locale.ENGLISH, "%d HR %d MINS", (int)msBehindLive/1000/60/60, (int)msBehindLive/1000/60%60);
        }

        String title = mCurrentSchedule == null ? "UNKNOWN" : mCurrentSchedule.getTitle();

        AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_LIVE_STREAM, AnalyticsManager.ACTION_LIVE_STREAM_PLAY,
                String.format(Locale.ENGLISH, AnalyticsManager.LABEL_LIVE_STREAM_PLAY,
                        String.valueOf(stream.relativeMsBehindLive() / 1000), status, title, "KPCC Live"));
    }

    /**
     *
     * @param stream
     */
    private void analyticsLogPauseEvent(LivePlayer stream) {
        String title = mCurrentSchedule == null ? "UNKNOWN" : mCurrentSchedule.getTitle();
        long lengthMs = stream.getSessionDurationMs();
        int lengthSec = lengthMs == -1 ? -1 : (int)(lengthMs / 1000);
        int lengthMin = lengthSec == -1 ? -1 : lengthSec / 60;

        AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_LIVE_STREAM, AnalyticsManager.ACTION_LIVE_STREAM_PAUSE,
                String.format(Locale.ENGLISH, AnalyticsManager.LABEL_LIVE_STREAM_PAUSE,
                        title, String.valueOf(lengthMin), String.valueOf(lengthSec)));
    }

    /**
     *
     * @param method
     * @param amountMs
     */
    private void analyticsLogSeekEvent(String method, long amountMs) {
        String dir = amountMs < 0 ? "Backward" : "Forward";

        AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_LIVE_STREAM, AnalyticsManager.ACTION_LIVE_STREAM_TIME_SHIFTED,
                String.format(Locale.ENGLISH, AnalyticsManager.LABEL_LIVE_STREAM_TIME_SHIFTED,
                        String.format(Locale.ENGLISH, "%s %s", dir, String.valueOf(amountMs / 1000)), method));
    }

    /**
     *
     * @param stream
     */
    private void analyticsLogBackgroundEvent(LivePlayer stream) {
        AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_LIVE_STREAM, AnalyticsManager.ACTION_LIVE_STREAM_MOVED_TO_BACKGROUND,
                String.format(Locale.ENGLISH, AnalyticsManager.LABEL_LIVE_STREAM_MOVED_TO_BACKGROUND,
                        String.valueOf(stream.getSessionDurationMs() / 1000)));
    }

    /**
     *
     * @param stream
     */
    private void analyticsLogForegroundEvent(LivePlayer stream) {
        AnalyticsManager.getInstance().sendAction(AnalyticsManager.CATEGORY_LIVE_STREAM, AnalyticsManager.ACTION_LIVE_STREAM_RETURNED_TO_FOREGROUND,
                String.format(Locale.ENGLISH, AnalyticsManager.LABEL_LIVE_STREAM_RETURNED_TO_FOREGROUND,
                        String.valueOf(System.currentTimeMillis() - stream.getBackgroundStart())));
    }

    /**
     *
     */
    private void playRewindBeat(Stream.AudioEventListener eventListener) {
        RawPlayer rewindAudio = new RawPlayer(getActivity(), ASSET_AUDIO_REWIND_MP3);
        rewindAudio.setAudioEventListener(eventListener);
        rewindAudio.prepareAndStart();
    }

    /**
     *
     */
    private synchronized void updateSchedule() {
        if (mScheduleUpdaterMutex.get()) return;
        mScheduleUpdaterMutex.set(true);

        LivePlayer stream = getLivePlayer();
        long uts;

        if (stream == null) {
            uts = System.currentTimeMillis();
        } else {
            uts = stream.getPlaybackTimestamp();
        }

        mRequest = ScheduleOccurrence.Client.getAtTimestamp(uts / 1000, new ScheduleResponseHandler(), new ScheduleErrorHandler());
    }

    /**
     * This function handles setting all the variables and UI state for the live view.
     * It should get called in two scenarios:
     * 1. The play button is clicked
     * 2. The fragment is re-initialized when the player was already playing (eg. a user comes back to the app after previously leaving).
     */
    private synchronized void initAudio() {
        getStreamConnection().addOnStreamBindListener(LiveFragment.STACK_TAG, new StreamServiceConnection.OnStreamBindListener() {
            @Override
            public void onBind() {
                LivePlayer stream = getLivePlayer();
                if (stream != null) {
                    LivePlayerAudioEventListener ls = new LivePlayerAudioEventListener();
                    stream.setAudioEventListener(ls);
                    if (stream.isPlaying()) ls.onPlay();
                }

                PrerollPlayer preroll = getPrerollPlayer();
                if (preroll != null) {
                    PrerollPlayerAudioEventListener ls = new PrerollPlayerAudioEventListener();
                    preroll.setAudioEventListener(ls);
                    if (preroll.isPlaying()) ls.onPlay();
                }

            }
        });
    }

    /**
     *
     */
    private void setDefaultScheduleValues() {
        mStatus.setText(R.string.live);
        NetworkImageManager.getInstance().setDefaultBitmap(mBackground);
        mTitle.setText(null);
    }

    /**
     *
     */
    private void enableNavigationDrawer() {
        NavigationDrawerFragment frag = getNavigationDrawerFragment();
        if (frag == null) return;
        frag.enableDrawer();
    }

    private void disableNavigationDrawer() {
        NavigationDrawerFragment frag = getNavigationDrawerFragment();
        if (frag == null) return;
        frag.disableDrawer();
    }

    private NavigationDrawerFragment getNavigationDrawerFragment() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity == null) return null;

        return activity.getNavigationDrawerFragment();
    }

    private void updateNotificationWithCurrentScheduleData() {
        if (getNotificationBuilder() == null) return;

        String programName;
        ScheduleOccurrence schedule = getCurrentSchedule();

        if (schedule == null) {
            programName = getString(R.string.kpcc_live);
        } else {
            programName = schedule.getTitle();
        }

        try {
            getNotificationBuilder().
                    setTicker(String.format(Locale.ENGLISH, getString(R.string.now_playing_program), programName)).
                    setContentText(programName);
        } catch (IllegalStateException e) {
            // FIXME: Fragment not attached. What do?
        }
    }

    /**
     *
     * @param schedule
     */
    private synchronized void setCurrentSchedule(ScheduleOccurrence schedule) {
        mCurrentSchedule = schedule;
    }

    /**
     *
     * @return
     */
    private synchronized ScheduleOccurrence getCurrentSchedule() {
        return mCurrentSchedule;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private class LiveStatusUpdater implements Runnable {
        @Override
        public void run() {
            // Do this stuff every second. This should never be stopped by any action except
            // leaving the fragment.
            LivePlayer stream = getLivePlayer();

            if (stream == null) return;
            if (mLiveSeekViewManager == null) return;
            if (mStatus == null) return;

            mLiveSeekViewManager.setSecondaryProgressFromSchedule();

            long relativeMsBehindLive = stream.relativeMsBehindLive();
            long minBehindLive = TimeUnit.MILLISECONDS.toMinutes(relativeMsBehindLive);

            if (minBehindLive >= 2) {
                long hoursBehindLive = TimeUnit.MILLISECONDS.toHours(relativeMsBehindLive);

                String val;
                if (hoursBehindLive > 0) {
                    long modMin = minBehindLive % 60;
                    val = String.format("%s%s %s%s",
                            String.valueOf(hoursBehindLive),
                            getString(R.string.hourShort),
                            String.valueOf(modMin),
                            getString(R.string.minuteShort)
                    );
                } else {
                    val = String.format("%s %s",
                            String.valueOf(minBehindLive),
                            getString(minBehindLive == 1 ? R.string.minute : R.string.minutes)
                    );
                }

                val = String.format("%s %s", val, getString(R.string.behind_live));
                mStatus.setText(val);
                mLiveSeekViewManager.showGoLiveBtn();
            } else {
                long now = System.currentTimeMillis();
                ScheduleOccurrence schedule = getCurrentSchedule();
                // If we're before this occurrence's 'soft start', then say "up next".
                // Otherwise, set "On Now".
                if (schedule != null && now < schedule.getSoftStartsAtMs()) {
                    mStatus.setText(R.string.up_next);
                } else {
                    mStatus.setText(R.string.live);
                }

                mLiveSeekViewManager.showProgramStartBtn();
            }

            mLiveSeekViewManager.toggleBackwardBtn(stream.canSeekBackward());
            mLiveSeekViewManager.toggleForwardBtn(stream.canSeekForward());
        }
    }


    /**
     *
     */
    private class TimerUpdater extends PeriodicBackgroundUpdater.TimerRunner {
        @Override
        public void onTimerUpdate(int hours, int mins, int secs) {
            mTimerRemaining.setText(
                    String.format("%s:%s:%s",
                            String.valueOf(hours),
                            String.format(Locale.ENGLISH, "%02d", mins),
                            String.format(Locale.ENGLISH, "%02d", secs)
                    )
            );
        }

        @Override
        public void onTimerComplete() {
            mTimerRemainingWrapper.setVisibility(View.GONE);
        }
    }


    /**
     *
     */
    private class ScheduleUpdater implements Runnable {
        @Override
        public void run() {
            updateSchedule();
        }
    }

    /**
     *
     */
    private class ScheduleResponseHandler implements Response.Listener<JSONObject> {
        @Override // Response.Listener
        public void onResponse(JSONObject response) {
            try {
                JSONObject jsonSchedule = response.getJSONObject(ScheduleOccurrence.SINGULAR_KEY);
                ScheduleOccurrence schedule = ScheduleOccurrence.buildFromJson(jsonSchedule);

                // If this is null, we want to set the live seek schedule to null as well.
                mLiveSeekViewManager.setSchedule(schedule);
                mLiveSeekViewManager.setSeekBarMaxFromSchedule();

                // It may be null, if nothing is on right now according to the API.
                if (schedule == null) {
                    setDefaultScheduleValues();
                    return;
                }

                // Don't make a network request for the image if it's the same program.
                ScheduleOccurrence previousSchedule = getCurrentSchedule();
                boolean didChangeProgram = previousSchedule == null || !previousSchedule.getProgramSlug().equals(schedule.getProgramSlug());
                setCurrentSchedule(schedule);

                if (didChangeProgram) {
                    NetworkImageManager.getInstance().setBitmap(getActivity(), mBackground, schedule.getProgramSlug());

                    updateNotificationWithCurrentScheduleData();

                    LivePlayer stream = getLivePlayer();
                    if (stream != null && stream.isPlaying()) {
                        sendNotification();
                    }
                }

                String title = schedule.getTitle();
                mTitle.setTextSize(Math.min(Math.max(55 - title.length(), 20), 50));
                mTitle.setText(title);

            } catch (JSONException e) {
                setDefaultScheduleValues();
            } finally {
                mScheduleUpdaterMutex.set(false);
            }
        }
    }


    /**
     *
     */
    private class ScheduleErrorHandler implements Response.ErrorListener {
        @Override // Response.ErrorListener
        public void onErrorResponse(VolleyError error) {
            setDefaultScheduleValues();
            mScheduleUpdaterMutex.set(false);
        }
    }


    /**
     *
     */
    private class LivePlayerAudioEventListener extends Stream.AudioEventListener {
        @Override
        public void onPreparing() {
            // This may happen while preroll is playing so we shouldn't update audio button state in that case.
            PrerollPlayer stream = getPrerollPlayer();
            if (stream == null || stream.isIdle()) {
                mAudioButtonManager.toggleLoading();
            }
        }

        @Override
        public void onPlay() {
            mAudioButtonManager.togglePlayingForPause();
            mLiveSeekViewManager.showSeekButtons();
            mLiveSeekViewManager.enableSeekBar();

            LivePlayer stream = getLivePlayer();
            if (stream == null ) return;

            if (mLiveSeekBarUpdater == null) {
                mLiveSeekBarUpdater = new PeriodicBackgroundUpdater(
                        new PeriodicBackgroundUpdater.ProgressBarRunner(stream), LIVE_SEEKBAR_REFRESH_INTERVAL);
            }

            mLiveSeekBarUpdater.start();
            if (mLiveStatusUpdater != null) mLiveStatusUpdater.start();

            updateNotificationWithCurrentScheduleData();
            sendNotification();
        }

        @Override
        public void onProgress(final int currentPosition) {
            mLiveSeekViewManager.incrementProgress();

            LivePlayer stream = getLivePlayer();
            if (stream == null) return;
            mLiveSeekViewManager.setSeekProgressFromPlayerPosition(stream.getUpperBoundMs(), currentPosition);
        }

        @Override
        public void onPause() {
            mAudioButtonManager.togglePaused();

            LivePlayer stream = getLivePlayer();
            if (stream != null) {
                stream.setPausedAt(System.currentTimeMillis());
            }

            if (mLiveSeekBarUpdater != null) mLiveSeekBarUpdater.release();

            stopService();
        }

        @Override
        public void onStop() {
            mAudioButtonManager.toggleStopped();

            LivePlayer stream = getLivePlayer();
            if (stream != null) {
                stream.setPausedAt(System.currentTimeMillis());
            }

            if (mLiveSeekBarUpdater != null) mLiveSeekBarUpdater.release();
            stopService();
        }

        @Override
        public void onCompletion() {
            // MBC has burned down please call 911
            onStop();
        }

        @Override
        public void onError() {
            mAudioButtonManager.toggleStopped();
            mAudioButtonManager.showError(R.string.audio_error);
            if (mLiveSeekBarUpdater != null) mLiveSeekBarUpdater.release();
            stopService();
        }
    }


    /**
     *
     */
    private class PrerollPlayerAudioEventListener extends Stream.AudioEventListener {
        @Override
        public void onPrerollData(final PrerollManager.PrerollData prerollData) {
            mPrerollProgressBar.setMax(prerollData.getAudioDurationSeconds() * 1000);
            mPrerollView.setVisibility(View.VISIBLE);
            mStatus.setVisibility(View.INVISIBLE);
            mTitle.setVisibility(View.INVISIBLE);
            mTimerRemainingWrapper.setVisibility(View.GONE);
            mLiveSeekViewManager.hideForPreroll();
            disableNavigationDrawer();

            if (prerollData.getAssetUrl() == null) return;
            NetworkImageManager.getInstance().setPrerollImage(mAdView, prerollData.getAssetUrl());

            if (prerollData.getAssetClickUrl() != null) {
                mAdView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(prerollData.getAssetClickUrl());
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                            getActivity().startActivity(intent);
                        }

                        if (prerollData.getTrackingUrl() != null) {
                            HttpRequest.ImpressionRequest.get(prerollData.getTrackingUrl());
                        }
                    }
                });
            }

            if (prerollData.getImpressionUrl() != null) {
                HttpRequest.ImpressionRequest.get(prerollData.getImpressionUrl());
            }
        }

        @Override
        public void onPreparing() {
            mAudioButtonManager.toggleLoading();
        }

        @Override
        public void onPlay() {
            mAudioButtonManager.togglePlayingForPause();

            PrerollPlayer stream = getPrerollPlayer();
            if (stream == null) return;

            if (mPrerollSeekBarUpdater == null) {
                mPrerollSeekBarUpdater = new PeriodicBackgroundUpdater(new PeriodicBackgroundUpdater.ProgressBarRunner(stream), 100);
            }

            mPrerollSeekBarUpdater.start();
        }

        @Override
        public void onPause() {
            mAudioButtonManager.togglePaused();
            if (mPrerollSeekBarUpdater != null) mPrerollSeekBarUpdater.release();
        }

        @Override
        public void onStop() {
            enableNavigationDrawer();
            // This gets called when the preroll player is released, which happens
            // when it's completed. Don't toggle any button state here because we're going to
            // immediately switch to livestream, which will handle button state updates.
            if (mPrerollSeekBarUpdater != null) mPrerollSeekBarUpdater.release();
        }

        @Override
        public void onCompletion() {
            if (!isVisible()) return;

            onStop();
            mAdView.setVisibility(View.GONE);
            mPrerollView.setVisibility(View.GONE);
            mStatus.setVisibility(View.VISIBLE);
            mTitle.setVisibility(View.VISIBLE);
            mLiveSeekViewManager.showAfterPreroll();

            if (BaseAlarmManager.SleepManager.getInstance().isRunning()) {
                mTimerRemainingWrapper.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onProgress(int progress) {
            mPrerollProgressBar.setProgress(progress);
        }

        @Override
        public void onError() {
            // Preroll error, just pretend it completed and move on.
            onCompletion();
        }
    }
}
