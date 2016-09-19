package org.kpcc.android;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.kpcc.api.Episode;
import org.kpcc.api.Program;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;


public class EpisodeFragment extends StreamBindFragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String STACK_TAG = "EpisodeFragment";
    private static final int SHARE_TYPE_REQUEST = 1;
    private static final String ARG_PROGRAM_SLUG = "programSlug";
    private static final String ARG_EPISODE = "episode";
    private static final String SHARE_TEXT = "%s - %s - %s";

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public final AtomicBoolean pagerVisible = new AtomicBoolean(false);
    public Episode episode;
    public Program program;
    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private AudioButtonManager mAudioButtonManager;
    // Unrecoverable error. Just show an error message if this is true.
    private boolean mDidError = false;
    private PeriodicBackgroundUpdater mPeriodicBackgroundUpdater;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param programSlug
     * @param episodeJson
     * @return
     */
    public static EpisodeFragment newInstance(String programSlug, String episodeJson) {
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_SLUG, programSlug);
        args.putString(ARG_EPISODE, episodeJson);

        EpisodeFragment fragment = new EpisodeFragment();
        fragment.setArguments(args);
        return fragment;
    }


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

        Bundle args = getArguments();
        if (args != null) {
            program = ProgramsManager.instance.find(args.getString(ARG_PROGRAM_SLUG));

            try {
                episode = Episode.buildFromJson(args.getString(ARG_EPISODE));
            } catch (JSONException e) {
                mDidError = true;
            }
        }
    }

    /**
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_episode, container, false);
        ScrollView episodePage = (ScrollView) view.findViewById(R.id.episode_page);
        episodePage.setFillViewport(true);

        FrameLayout contentWrapper = (FrameLayout) view.findViewById(R.id.content_wrapper);

        if (mDidError) {
            LinearLayout errorView = (LinearLayout) view.findViewById(R.id.generic_load_error);
            errorView.setVisibility(View.VISIBLE);
            contentWrapper.setVisibility(View.GONE);
            return view;
        }

        ImageView mShareButton = (ImageView) view.findViewById(R.id.share_btn);
        final TextView programTitle = (TextView) view.findViewById(R.id.program_title);
        final TextView episodeTitle = (TextView) view.findViewById(R.id.episode_title);
        final TextView date = (TextView) view.findViewById(R.id.air_date);
        final TextView totalTime = (TextView) view.findViewById(R.id.audio_total_time);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        String.format(SHARE_TEXT, episode.getTitle(), program.getTitle(), episode.getPublicUrl()));
                sendIntent.setType("text/plain");

                MainActivity activity = (MainActivity)getActivity();
                if (activity != null) {
                    activity.startActivityForResult(
                            Intent.createChooser(sendIntent, getString(R.string.share_episode)),
                            SHARE_TYPE_REQUEST);
                }
            }
        });

        mSeekBar = (SeekBar) view.findViewById(R.id.progress_bar);
        mCurrentTime = (TextView) view.findViewById(R.id.audio_current_time);
        mSeekBar.setMax(episode.getAudio().getDurationSeconds() * 1000);
        mSeekBar.setEnabled(false);

        programTitle.setText(program.getTitle());
        date.setText(episode.getFormattedAirDate());
        totalTime.setText(Stream.getTimeFormat(episode.getAudio().getDurationSeconds()));

        String title = episode.getTitle();

        float length = (float) title.length();
        if (length < 30) {
            episodeTitle.setTextSize(30);
        } else if (length >= 30 && length < 55) {
            episodeTitle.setTextSize(25);
        } else {
            episodeTitle.setTextSize(20);
        }

        episodeTitle.setText(title);

        mAudioButtonManager = new AudioButtonManager(view);

        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(getActivity(), episode.getPublicUrl(), new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect(Context context) {
                if (mAudioButtonManager == null) {
                    return;
                }

                // We want this here so the schedule will get updated immediately when connectivity
                // is back, so we'll just restart it.
                mAudioButtonManager.hideError();
            }

            @Override
            public void onDisconnect(Context context) {
                if (mAudioButtonManager == null) {
                    return;
                }

                mAudioButtonManager.showError(R.string.network_error);
            }
        }, true);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                OnDemandPlayer stream = getOnDemandPlayer();
                if (stream != null) {
                    stream.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StreamService service = getStreamService();
                if (service != null) {
                    OnDemandPlayer stream = getOnDemandPlayer();
                    if (stream != null) {
                        service.startForeground(Stream.NOTIFICATION_ID, getNotificationBuilder().build());
                        stream.prepareAndStart();
                    }
                }
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnDemandPlayer stream = getOnDemandPlayer();
                if (stream != null) { stream.pause(); }
                cancelNotification();
            }
        });

        buildNotification();
        return view;
    }

    /**
     *
     */
    @Override // Fragment
    public void onResume() {
        super.onResume();

        if (mDidError) {
            // The UI is handled in onCreateView; just don't play the audio right now.
            return;
        }

        // We have to load all of this stuff here because the pager doesn't invoke onCreateView
        // when a view becomes visible, but we need to make new audio players and reset the
        // listeners.
        boolean alreadyPlaying = false;


        OnDemandPlayer stream = getOnDemandPlayer();
        if (stream != null && stream.getAudioUrl().equals(episode.getAudio().getUrl())) {
            mSeekBar.setEnabled(true);

            if (stream.isPlaying()) {
                alreadyPlaying = true;
                mAudioButtonManager.togglePlayingForPause();
            }

            // Audio should be released when next one starts, based on Audio Focus rules.
        } else {
            stream = new OnDemandPlayer(getActivity(), episode.getAudio().getUrl(), program.getSlug());
            setCurrentStream(stream, STACK_TAG);
        }


        stream.setAudioEventListener(new Stream.AudioEventListener() {
            @Override
            public void onPreparing() {
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onPlay() {
                mSeekBar.setEnabled(true);
                mAudioButtonManager.togglePlayingForPause();

                if (mPeriodicBackgroundUpdater != null) {
                    mPeriodicBackgroundUpdater.start();
                }

                sendNotification();
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                if (mPeriodicBackgroundUpdater != null) {
                    mPeriodicBackgroundUpdater.release();
                }

                cancelNotification();
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
                if (mPeriodicBackgroundUpdater != null) {
                    mPeriodicBackgroundUpdater.release();
                }

                cancelNotification();
            }

            @Override
            public void onCompletion() {
                OnDemandPlayer stream = getOnDemandPlayer();
                if (stream == null || !isVisible()) {
                    return;
                }

                if (mPeriodicBackgroundUpdater != null) {
                    mPeriodicBackgroundUpdater.release();
                }

                FragmentManager fm = getActivity().getSupportFragmentManager();
                EpisodesPagerFragment fragment = (EpisodesPagerFragment) fm.findFragmentByTag(EpisodesPagerFragment.STACK_TAG);

                if (fragment != null && fragment.pager != null) {
                    if (fragment.pager.canScrollHorizontally(1)) {
                        fragment.pager.setCurrentItem(fragment.pager.getCurrentItem() + 1);
                    } else {
                        try {
                            // Go back to the episodes list.
                            fm.popBackStackImmediate();
                        } catch(java.lang.IllegalStateException e) {
                            // Do nothing - fragment was lost (app was backgrounded) so can't do anything.
                        }
                    }
                }
            }

            @Override
            public void onProgress(int progress) {
                mSeekBar.setProgress(progress);
                mCurrentTime.setText(Stream.getTimeFormat(progress / 1000));
            }

            @Override
            public void onBufferingUpdate(int percent) {
                float currentBuffer = mSeekBar.getMax() * (percent / 100f);
                mSeekBar.setSecondaryProgress((int) currentBuffer);
            }

            @Override
            public void onError() {
                mAudioButtonManager.toggleStopped();
                mAudioButtonManager.showError(R.string.audio_error);
            }
        });


        if (!pagerVisible.get()) {
            return;
        }

        mPeriodicBackgroundUpdater = new PeriodicBackgroundUpdater(new PeriodicBackgroundUpdater.ProgressBarRunner(stream), 100);
        if (stream.isPlaying()) { mPeriodicBackgroundUpdater.start(); }

        // No audio will play if:
        // 1. Stream isn't ready
        // 2. There is no audio (this shouldn't happen, but just in case...)
        // 3. The view isn't visible.
        // ViewPager calls the 'onCreateView' method for surrounding views, so we can't
        // play the audio automatically in this method.
        if (episode.getAudio() == null) {
            mAudioButtonManager.toggleStopped();
            mAudioButtonManager.showError(R.string.audio_error);
            return;
        }

        if (!alreadyPlaying) {
            mAudioButtonManager.toggleStopped();

            // Start the episode right away unless it's already playing.
            getStreamConnection().addOnStreamBindListener(EpisodeFragment.STACK_TAG, new StreamServiceConnection.OnStreamBindListener() {
                @Override
                public void onBind() {
                    mAudioButtonManager.clickPlay();
                }
            });
        }

        bindStreamService();
    }

    /**
     *
     */
    @Override // Fragment
    public void onPause() {
        super.onPause();
        pagerVisible.set(false);

        // If we caught an error in the episode page and episode URL was never set, then an error
        // will be raised when the user tries to back out.
        if (episode.getPublicUrl() != null) {
            AppConnectivityManager.getInstance().removeOnNetworkConnectivityListener(episode.getPublicUrl());
        }

        if (mPeriodicBackgroundUpdater != null) { mPeriodicBackgroundUpdater.release(); }
    }


    @Override // StreamBindFragment
    protected void buildNotification() {
        initNotificationBuilder(R.drawable.menu_antenna, getString(R.string.now_playing));
        updateNotificationWithCurrentEpisodeData();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void updateNotificationWithCurrentEpisodeData() {
        if (getNotificationBuilder() == null) buildNotification();

        String programName;
        if (program == null) {
            programName = getString(R.string.kpcc_live);
        } else {
            programName = program.getTitle();
        }

        getNotificationBuilder().
                setTicker(String.format(Locale.ENGLISH, getString(R.string.now_playing_program), programName)).
                setContentText(programName);
    }

    boolean episodeWasSkipped() {
        OnDemandPlayer stream = getOnDemandPlayer();
        if (stream == null) return false;

        try {
            long current = getCurrentPlayerPositionSeconds();
            return current < (stream.getDuration() / 4);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    long getCurrentPlayerPositionSeconds() throws IllegalStateException {
        OnDemandPlayer stream = getOnDemandPlayer();
        if (stream == null) return 0;
        return stream.getCurrentPosition() / 1000;
    }
}
