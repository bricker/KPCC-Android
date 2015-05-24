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
import org.json.JSONObject;
import org.kpcc.api.Episode;
import org.kpcc.api.Program;

import java.util.concurrent.atomic.AtomicBoolean;


public class EpisodeFragment extends Fragment {
    public static final String STACK_TAG = "EpisodeFragment";
    private static final int SHARE_TYPE_REQUEST = 1;
    private static final String ARG_PROGRAM_SLUG = "programSlug";
    private static final String ARG_EPISODE = "episode";
    private static final String SHARE_TEXT = "%s - %s - %s";
    public final AtomicBoolean pagerVisible = new AtomicBoolean(false);
    public Episode episode;
    public Program program;
    private StreamManager.EpisodeStream mPlayer;
    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private AudioButtonManager mAudioButtonManager;
    // Unrecoverable error. Just show an error message if this is true.
    private boolean mDidError = false;


    public static EpisodeFragment newInstance(String programSlug, String episodeJson) {
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_SLUG, programSlug);
        args.putString(ARG_EPISODE, episodeJson);

        EpisodeFragment fragment = new EpisodeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
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

    @Override
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
                        String.format(SHARE_TEXT, episode.title, program.title, episode.publicUrl));
                sendIntent.setType("text/plain");

                JSONObject params = new JSONObject();
                try {
                    params.put(AnalyticsManager.PARAM_PROGRAM_PUBLISHED_AT, episode.airDate);
                    params.put(AnalyticsManager.PARAM_PROGRAM_TITLE, program.title);
                    params.put(AnalyticsManager.PARAM_EPISODE_TITLE, episode.title);
                    params.put(AnalyticsManager.PARAM_ACTIVITY_TYPE, "UNKNOWN");
                } catch (JSONException e) {
                    // No params will be sent.
                }

                getActivity().startActivityForResult(
                        Intent.createChooser(sendIntent, getString(R.string.share_episode)),
                        SHARE_TYPE_REQUEST);

                AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_EPISODE_SHARED, params);
            }
        });

        mSeekBar = (SeekBar) view.findViewById(R.id.progress_bar);
        mCurrentTime = (TextView) view.findViewById(R.id.audio_current_time);
        mSeekBar.setMax(episode.audio.durationSeconds * 1000);
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
        date.setText(episode.formattedAirDate);
        totalTime.setText(StreamManager.getTimeFormat(episode.audio.durationSeconds));

        String title = episode.title;

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
                mPlayer.play();
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.pause();
            }
        });

        AppConnectivityManager.instance.addOnNetworkConnectivityListener(episode.publicUrl, new AppConnectivityManager.NetworkConnectivityListener() {
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

    @Override
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

        StreamManager.EpisodeStream currentPlayer = AppConnectivityManager.instance.streamManager.currentEpisodePlayer;
        if (currentPlayer != null && currentPlayer.audioUrl.equals(episode.audio.url)) {
            mPlayer = currentPlayer;
            mSeekBar.setEnabled(true);

            if (currentPlayer.isPlaying()) {
                alreadyPlaying = true;
                mAudioButtonManager.togglePlayingForPause();
            }

            // Audio should be released when next one starts, based on Audio Focus rules.
        } else {
            mPlayer = new StreamManager.EpisodeStream(episode.audio.url, program.slug,
                    episode.audio.durationSeconds, activity);
        }

        mPlayer.setOnAudioEventListener(new StreamManager.AudioEventListener() {
            @Override
            public void onLoading() {
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onPlay() {
                mSeekBar.setEnabled(true);
                mAudioButtonManager.togglePlayingForPause();
                logEpisodeStreamEvent(AnalyticsManager.EVENT_ON_DEMAND_BEGAN);
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                logEpisodeStreamEvent(AnalyticsManager.EVENT_ON_DEMAND_PAUSED);
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
            }

            @Override
            public void onCompletion() {
                if (!AppConnectivityManager.instance.streamIsBound || !isVisible()) {
                    return;
                }

                FragmentManager fm = getActivity().getSupportFragmentManager();
                EpisodesPagerFragment fragment = (EpisodesPagerFragment) fm.findFragmentByTag(EpisodesPagerFragment.STACK_TAG);

                if (fragment != null && fragment.pager != null) {
                    if (fragment.pager.canScrollHorizontally(1)) {
                        fragment.pager.setCurrentItem(fragment.pager.getCurrentItem() + 1);
                    } else {
                        // Go back to the episodes list.
                        fm.popBackStackImmediate();
                    }
                }

                logEpisodeStreamEvent(AnalyticsManager.EVENT_ON_DEMAND_COMPLETED);
            }

            @Override
            public void onProgress(int progress) {
                mSeekBar.setProgress(progress);
                mCurrentTime.setText(StreamManager.getTimeFormat(progress / 1000));
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

        // No audio will play if:
        // 1. Stream isn't ready
        // 2. There is no audio (this shouldn't happen, but just in case...)
        // 3. The view isn't visible.
        // ViewPager calls the 'onCreateView' method for surrounding views, so we can't
        // play the audio automatically in this method.
        if (!AppConnectivityManager.instance.streamIsBound || episode.audio == null) {
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

    @Override
    public void onPause() {
        super.onPause();
        pagerVisible.set(false);
        AppConnectivityManager.instance.removeOnNetworkConnectivityListener(episode.publicUrl);
    }

    public boolean episodeWasSkipped() {
        if (streamNotAvailable()) {
            return false;
        }

        try {
            int current = getCurrentPlayerPositionSeconds();
            return current < (mPlayer.durationSeconds / 4);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public int getCurrentPlayerPositionSeconds() throws IllegalStateException {
        if (streamNotAvailable()) {
            return 0;
        }

        return (int) mPlayer.getCurrentPosition() / 1000;
    }

    private boolean streamNotAvailable() {
        return mPlayer == null || !AppConnectivityManager.instance.streamIsBound || AppConnectivityManager.instance.streamManager.currentEpisodePlayer == null;
    }

    private void logEpisodeStreamEvent(String key) {
        JSONObject params = new JSONObject();

        try {
            params.put(AnalyticsManager.PARAM_PROGRAM_PUBLISHED_AT, episode.airDate);
            params.put(AnalyticsManager.PARAM_PROGRAM_TITLE, program.title);
            params.put(AnalyticsManager.PARAM_EPISODE_TITLE, episode.title);
            params.put(AnalyticsManager.PARAM_PROGRAM_LENGTH, episode.audio.durationSeconds);

            if (key.equals(AnalyticsManager.EVENT_ON_DEMAND_PAUSED)) {
                params.put(AnalyticsManager.PARAM_PLAYED_DURATION, mPlayer.getCurrentPosition() / 1000);
            }
        } catch (JSONException e) {
            // Nothing to do. No data will be sent with this event.
        }

        AnalyticsManager.instance.logEvent(key, params);
    }
}
