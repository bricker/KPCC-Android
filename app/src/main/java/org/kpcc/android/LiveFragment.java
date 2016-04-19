package org.kpcc.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
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


public class LiveFragment extends Fragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static final String STACK_TAG = "LiveFragment";
    private static final String ASSET_AUDIO_REWIND_MP3 = "asset:///audio/rewind.mp3";
    public static final int LIVE_SEEKBAR_REFRESH_INTERVAL = 1000;
    private static final String PLAY_START_KEY = "liveStreamPlayStart";
    private static final String LIVESTREAM_LISTENER_KEY = "livestream";
    public static final int NOTIFICATION_ID = 1;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView mTitle;
    private TextView mStatus;
    private NetworkImageView mAdView;
    private LiveStreamBundle streamBundle = null;
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
    private NotificationCompat.Builder mNotificationBuilder;

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
        activity.setTitle(R.string.kpcc_live);

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
        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(LIVESTREAM_LISTENER_KEY, new LiveStreamConnectivityListener(), false);

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!StreamManager.ConnectivityManager.getInstance().getStreamIsBound() || !AppConnectivityManager.getInstance().isConnectedToNetwork()) {
                    // The Error message should already be showing for connectivity problems.
                    // Just do nothing.
                    return;
                }

                initAudio();

                if (streamBundle == null) {
                    return; // streamBundle initialization failed for some reason.
                }

                PrerollPlayer prerollPlayer = getPrerollPlayer();
                LivePlayer livePlayer = getLivePlayer();

                if (prerollPlayer != null && prerollPlayer.isPaused()) {
                    // Preroll was Paused; start it again.
                    streamBundle.getPrerollPlayer().play();
                } else if (livePlayer != null && livePlayer.isPaused() && livePlayer.getPausedAt() > (System.currentTimeMillis() - 1000 * 60 * 60 * 8)) {
                    // Live stream was paused; start it again.
                    streamBundle.getLivePlayer().play();
                } else {
                    // Preroll will be handled normally
                    streamBundle.playWithPrerollAttempt();
                    AppConfiguration.getInstance().setDynamicProperty(PLAY_START_KEY, String.valueOf(System.currentTimeMillis()));
                }
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!StreamManager.ConnectivityManager.getInstance().getStreamIsBound()) {
                    return;
                }

                StreamManager sm = StreamManager.ConnectivityManager.getInstance().getStreamManager();
                if (sm == null) {
                    return;
                }

                PrerollPlayer prerollPlayer = sm.getCurrentPrerollPlayer();
                LivePlayer livePlayer = sm.getCurrentLivePlayer();

                if (prerollPlayer != null && prerollPlayer.isPlaying()) {
                    prerollPlayer.pause();
                } else if (livePlayer != null && livePlayer.isPlaying()) {
                    livePlayer.pause();
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
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // The jump buttons COULD use this function, but since we know exactly how much they are
                // jumping it's more comfortable to just update the seekbar and livestream manually in
                // those cases.
                if (!fromUser) {
                    return;
                }

                LivePlayer livePlayer = getLivePlayer();
                if (livePlayer == null) return;

                if (progress >= mLiveSeekViewManager.getLiveHeadProgress()) {
                    // Prevent from seeking past live head.
                    mLiveSeekViewManager.setSeekProgressToLiveHead();
                    livePlayer.seekToLive();
                    return;
                }

                long programStartPositionInHLSWindow = mLiveSeekViewManager.getProgramStartWindowPositionMs(livePlayer.getUpperBoundMs());
                livePlayer.seekTo(programStartPositionInHLSWindow + progress);
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
                LivePlayer livePlayer = getLivePlayer();
                if (livePlayer == null) return;

                livePlayer.skipBackward();
                mLiveSeekViewManager.skipBackward(livePlayer.canSeekBackward());

                // This prevents a schedule update unless we pass the seek boundaries for this program.
                if (mLiveSeekViewManager.hasExceededLowerSeekBound()) {
                    updateSchedule();
                }
            }
        });

        mLiveSeekViewManager.getForwardBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePlayer livePlayer = getLivePlayer();
                if (livePlayer == null) return;

                livePlayer.skipForward();
                mLiveSeekViewManager.skipForward(livePlayer.canSeekForward());

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
                        LivePlayer livePlayer = getLivePlayer();
                        if (livePlayer == null) return;

                        // These must happen in this order so the states don't override each other.
                        mLiveStatusUpdater.pause();
                        livePlayer.pause();
                        mAudioButtonManager.toggleLoading();
                        mAudioButtonManager.lock();
                        livePlayer.seekToLive();
                    }

                    @Override
                    public void onCompletion() {
                        LivePlayer livePlayer = getLivePlayer();
                        if (livePlayer == null) return;

                        mAudioButtonManager.unlock();
                        livePlayer.play();
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
                        LivePlayer livePlayer = getLivePlayer();
                        if (livePlayer == null) return;
                        // These must happen in this order so the states don't override each other.
                        mLiveStatusUpdater.pause();
                        livePlayer.pause();
                        mAudioButtonManager.toggleLoading();
                        mAudioButtonManager.lock();

                        long programStartPositionInHLSWindow = mLiveSeekViewManager.getProgramStartWindowPositionMs(livePlayer.getUpperBoundMs());
                        livePlayer.seekTo(programStartPositionInHLSWindow);
                    }

                    @Override
                    public void onCompletion() {
                        LivePlayer livePlayer = getLivePlayer();
                        if (livePlayer == null) return;

                        mAudioButtonManager.unlock();
                        livePlayer.play();
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
        StreamManager.ConnectivityManager.getInstance().addOnStreamBindListener(LiveFragment.STACK_TAG, new StreamManager.ConnectivityManager.OnStreamBindListener() {
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
        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(LiveFragment.STACK_TAG, new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                // We want this here so the schedule will get updated immediately when connectivity
                // is back, so we'll just restart it.
                if (mScheduleUpdater != null) mScheduleUpdater.start();
                mAudioButtonManager.hideError();
            }

            @Override
            public void onDisconnect() {
                if (mScheduleUpdater != null) mScheduleUpdater.release();
                setDefaultScheduleValues();
                mAudioButtonManager.showError(R.string.network_error);
            }
        }, true);
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

        super.onPause();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
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

        long uts;
        LivePlayer livePlayer = getLivePlayer();
        if (livePlayer == null) {
            uts = System.currentTimeMillis();
        } else {
            uts = livePlayer.getPlaybackTimestamp();
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
        if (!StreamManager.ConnectivityManager.getInstance().getStreamIsBound()) {
            resetLiveState();
            return;
        }

        StreamManager streamManager = StreamManager.ConnectivityManager.getInstance().getStreamManager();

        if (streamManager == null) return; // We have to wait until the stream is available

        LivePlayer currentPlayer = streamManager.getCurrentLivePlayer();
        PrerollPlayer currentPreroll = streamManager.getCurrentPrerollPlayer();

        Context context = getActivity();

        if (streamBundle == null) {
            if (currentPlayer != null && !currentPlayer.isIdle()) {
                if (currentPreroll != null) {
                    streamBundle = new LiveStreamBundle(context, currentPlayer, currentPreroll);
                } else {
                    streamBundle = new LiveStreamBundle(context, currentPlayer);
                }
            } else {
                // Nothing is playing, build a brand new bundle.
                streamBundle = new LiveStreamBundle(context);
            }
        }

        LivePlayer livePlayer = getLivePlayer();
        if (livePlayer != null) {
            LivePlayerAudioEventListener ls = new LivePlayerAudioEventListener();
            livePlayer.setAudioEventListener(ls);
            if (livePlayer.isPlaying()) ls.onPlay();
        }

        PrerollPlayer prerollPlayer = getPrerollPlayer();
        if (prerollPlayer != null) {
            PrerollPlayerAudioEventListener ls = new PrerollPlayerAudioEventListener();
            prerollPlayer.setAudioEventListener(ls);
            if (prerollPlayer.isPlaying()) {
                ls.onPlay();
            }
        }
    }

    private void resetLiveState() {
        mAudioButtonManager.reset();
        mLiveSeekViewManager.reset();
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
     * @return
     */
    private LivePlayer getLivePlayer() {
        if (streamBundle == null) return null;
        return streamBundle.getLivePlayer();
    }

    /**
     *
     * @return
     */
    private PrerollPlayer getPrerollPlayer() {
        if (streamBundle == null) return null;
        return streamBundle.getPrerollPlayer();
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

    private void buildNotification() {
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            PendingIntent pi = PendingIntent.getActivity(activity.getApplicationContext(), 0,
                    new Intent(activity.getApplicationContext(), MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mNotificationBuilder = new NotificationCompat.Builder(activity.getApplicationContext())
                    .setSmallIcon(R.drawable.menu_antenna)
                    .setContentTitle(getString(R.string.kpcc_live))
                    .setContentIntent(pi)
                    .setOngoing(true);

            updateNotificationWithCurrentScheduleData();
        }
    }

    private void updateNotificationWithCurrentScheduleData() {
        if (mNotificationBuilder == null) return;

        String programName;
        ScheduleOccurrence schedule = getCurrentSchedule();

        if (schedule == null) {
            programName = getString(R.string.kpcc_live);
        } else {
            programName = schedule.getTitle();
        }

        mNotificationBuilder.
                setTicker(String.format(Locale.ENGLISH, getString(R.string.now_playing_program), programName)).
                setContentText(programName);
    }

    private void cancelNotification() {
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Service.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void sendNotification() {
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            mNotificationBuilder.setWhen(System.currentTimeMillis());
            NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Service.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
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

            LivePlayer livePlayer = getLivePlayer();
            if (livePlayer == null) return;

            mLiveSeekViewManager.setSecondaryProgressFromSchedule();

            long relativeMsBehindLive = livePlayer.relativeMsBehindLive();
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

            mLiveSeekViewManager.toggleBackwardBtn(livePlayer.canSeekBackward());
            mLiveSeekViewManager.toggleForwardBtn(livePlayer.canSeekForward());
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

                    if (mNotificationBuilder != null) {
                        updateNotificationWithCurrentScheduleData();
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
            PrerollPlayer prerollPlayer = getPrerollPlayer();
            if (prerollPlayer == null || prerollPlayer.isIdle()) {
                mAudioButtonManager.toggleLoading();
            }
        }

        @Override
        public void onPlay() {
            mAudioButtonManager.togglePlayingForPause();
            mLiveSeekViewManager.showSeekButtons();
            mLiveSeekViewManager.enableSeekBar();

            LivePlayer livePlayer = getLivePlayer();
            if (livePlayer != null && mLiveSeekBarUpdater == null) {
                mLiveSeekBarUpdater = new PeriodicBackgroundUpdater(
                        new PeriodicBackgroundUpdater.ProgressBarRunner(livePlayer), LIVE_SEEKBAR_REFRESH_INTERVAL);
            }

            mLiveSeekBarUpdater.start();
            if (mLiveStatusUpdater != null) mLiveStatusUpdater.start();

            buildNotification();
            sendNotification();
        }

        @Override
        public void onProgress(int currentPosition) {
            mLiveSeekViewManager.incrementProgress();
            LivePlayer livePlayer = getLivePlayer();
            if (livePlayer == null) return;

            mLiveSeekViewManager.setSeekProgressFromPlayerPosition(livePlayer.getUpperBoundMs(), currentPosition);
        }

        @Override
        public void onPause() {
            mAudioButtonManager.togglePaused();

            LivePlayer livePlayer = getLivePlayer();
            if (livePlayer != null) livePlayer.setPausedAt(System.currentTimeMillis());

            if (mLiveSeekBarUpdater != null) mLiveSeekBarUpdater.release();
            cancelNotification();
        }

        @Override
        public void onStop() {
            mAudioButtonManager.toggleStopped();

            LivePlayer livePlayer = getLivePlayer();
            if (livePlayer != null) livePlayer.setPausedAt(System.currentTimeMillis());

            if (mLiveSeekBarUpdater != null) mLiveSeekBarUpdater.release();
            cancelNotification();
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
            cancelNotification();
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
            disableNavigationDrawer();
        }

        @Override
        public void onPlay() {
            mAudioButtonManager.togglePlayingForPause();

            PrerollPlayer prerollPlayer = getPrerollPlayer();
            if (prerollPlayer != null && mPrerollSeekBarUpdater == null) {
                mPrerollSeekBarUpdater = new PeriodicBackgroundUpdater(new PeriodicBackgroundUpdater.ProgressBarRunner(prerollPlayer), 100);
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
