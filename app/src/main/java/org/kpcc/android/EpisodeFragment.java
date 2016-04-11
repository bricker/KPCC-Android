package org.kpcc.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import java.util.concurrent.atomic.AtomicBoolean;


public class EpisodeFragment extends Fragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
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
    private OnDemandPlayer mPlayer;
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
        TextView programTitle = (TextView) view.findViewById(R.id.program_title);
        TextView episodeTitle = (TextView) view.findViewById(R.id.episode_title);
        TextView date = (TextView) view.findViewById(R.id.air_date);
        TextView totalTime = (TextView) view.findViewById(R.id.audio_total_time);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        String.format(SHARE_TEXT, episode.getTitle(), program.title, episode.getPublicUrl()));
                sendIntent.setType("text/plain");

                getActivity().startActivityForResult(
                        Intent.createChooser(sendIntent, getString(R.string.share_episode)),
                        SHARE_TYPE_REQUEST);
            }
        });

        mSeekBar = (SeekBar) view.findViewById(R.id.progress_bar);
        mCurrentTime = (TextView) view.findViewById(R.id.audio_current_time);
        mSeekBar.setMax(episode.getAudio().getDurationSeconds() * 1000);
        mSeekBar.setEnabled(false);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                if (mPlayer != null) {
                    mPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        programTitle.setText(program.title);
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

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.prepareAndStart();
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.pause();
            }
        });

        AppConnectivityManager.getInstance().addOnNetworkConnectivityListener(episode.getPublicUrl(), new AppConnectivityManager.NetworkConnectivityListener() {
            @Override
            public void onConnect() {
                if (mAudioButtonManager == null) {
                    return;
                }

                // We want this here so the schedule will get updated immediately when connectivity
                // is back, so we'll just restart it.
                mAudioButtonManager.hideError();
            }

            @Override
            public void onDisconnect() {
                if (mAudioButtonManager == null) {
                    return;
                }

                mAudioButtonManager.showError(R.string.network_error);
            }
        }, true);

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
        MainActivity activity = (MainActivity) getActivity();
        boolean alreadyPlaying = false;

        OnDemandPlayer currentPlayer = StreamManager.ConnectivityManager.getInstance().getStreamManager().getCurrentOnDemandPlayer();
        if (currentPlayer != null && currentPlayer.getAudioUrl().equals(episode.getAudio().getUrl())) {
            mPlayer = currentPlayer;
            mSeekBar.setEnabled(true);

            if (currentPlayer.isPlaying()) {
                alreadyPlaying = true;
                mAudioButtonManager.togglePlayingForPause();
            }

            // Audio should be released when next one starts, based on Audio Focus rules.
        } else {
            mPlayer = new OnDemandPlayer(activity, episode.getAudio().getUrl(),
                    program.slug, episode.getAudio().getDurationSeconds());
        }


        mPlayer.setAudioEventListener(new Stream.AudioEventListener() {
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
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                if (mPeriodicBackgroundUpdater != null) {
                    mPeriodicBackgroundUpdater.release();
                }
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
                if (mPeriodicBackgroundUpdater != null) {
                    mPeriodicBackgroundUpdater.release();
                }
            }

            @Override
            public void onCompletion() {
                if (!StreamManager.ConnectivityManager.getInstance().getStreamIsBound() || !isVisible()) {
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

        mPeriodicBackgroundUpdater = new PeriodicBackgroundUpdater(new PeriodicBackgroundUpdater.ProgressBarRunner(mPlayer), 100);
        if (mPlayer.isPlaying()) { mPeriodicBackgroundUpdater.start(); }

        // No audio will play if:
        // 1. Stream isn't ready
        // 2. There is no audio (this shouldn't happen, but just in case...)
        // 3. The view isn't visible.
        // ViewPager calls the 'onCreateView' method for surrounding views, so we can't
        // play the audio automatically in this method.
        if (!StreamManager.ConnectivityManager.getInstance().getStreamIsBound() || episode.getAudio() == null) {
            mAudioButtonManager.toggleStopped();
            mAudioButtonManager.showError(R.string.audio_error);
            return;
        }

        if (!alreadyPlaying) {
            mAudioButtonManager.toggleStopped();
            // Start the episode right away unless it's already playing.
            mAudioButtonManager.clickPlay();
        }
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


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    boolean episodeWasSkipped() {
        if (streamNotAvailable()) {
            return false;
        }

        try {
            long current = getCurrentPlayerPositionSeconds();
            return current < (mPlayer.getDuration() / 4);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    long getCurrentPlayerPositionSeconds() throws IllegalStateException {
        if (streamNotAvailable()) {
            return 0;
        }

        return mPlayer.getCurrentPosition() / 1000;
    }

    private boolean streamNotAvailable() {
        return mPlayer == null ||
                !StreamManager.ConnectivityManager.getInstance().getStreamIsBound() ||
                StreamManager.ConnectivityManager.getInstance().getStreamManager().getCurrentOnDemandPlayer() == null;
    }
}
