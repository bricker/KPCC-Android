package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Program;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EpisodeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EpisodeFragment extends Fragment {
    public static final String TAG = "kpcc.EpisodeFragment";

    private static final String ARG_PROGRAM_SLUG = "programSlug";
    private static final String ARG_EPISODE_TITLE = "episodeTitle";
    private static final String ARG_EPISODE_DATE = "episodeDate";
    private static final String ARG_AUDIO_URL = "audioUrl";
    private static final String ARG_AUDIO_DURATION = "audioDuration";

    private Program mProgram;
    private String mEpisodeTitle;
    private String mEpisodeDate;
    private String mAudioUrl;
    private int mAudioDuration; // SECONDS
    private NetworkImageView mBackground;
    private StreamManager.EpisodeStream mPlayer;

    public EpisodeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment EpisodeFragment.
     */
    public static EpisodeFragment newInstance(
            String programSlug, String episodeTitle, String episodeDate,
            String audioUrl, int audioDuration) {
        EpisodeFragment fragment = new EpisodeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_SLUG, programSlug);

        // We don't want to store all of the episodes in memory, because that would use a fair
        // amount of memory and the episodes will change/update pretty frequently.
        // Since we already have this data from the previous screen, we're just going to pass it in.
        // We *could* make another API request here to simplify things, but it's not necessary.
        args.putString(ARG_EPISODE_TITLE, episodeTitle);
        args.putString(ARG_EPISODE_DATE, episodeDate);
        args.putString(ARG_AUDIO_URL, audioUrl);
        args.putInt(ARG_AUDIO_DURATION, audioDuration);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            mProgram = ProgramsManager.instance.find(args.getString(ARG_PROGRAM_SLUG));
            mEpisodeTitle = args.getString(ARG_EPISODE_TITLE);
            mEpisodeDate = args.getString(ARG_EPISODE_DATE);
            mAudioUrl = args.getString(ARG_AUDIO_URL);
            mAudioDuration = args.getInt(ARG_AUDIO_DURATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.programs);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_episode, container, false);

        TextView program_title = (TextView) view.findViewById(R.id.program_title);
        TextView episode_title = (TextView) view.findViewById(R.id.episode_title);
        TextView date = (TextView) view.findViewById(R.id.air_date);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        final TextView currentTime = (TextView) view.findViewById(R.id.audio_current_time);

        progressBar.setMax(mAudioDuration);

        TextView totalTime = (TextView) view.findViewById(R.id.audio_total_time);
        totalTime.setText(StreamManager.getTimeFormat(mAudioDuration));

        mBackground = (NetworkImageView) view.findViewById(R.id.background);
        NetworkImageManager.instance.setBackgroundImage(mBackground, mProgram.slug);

        program_title.setText(mProgram.title);
        episode_title.setText(mEpisodeTitle);
        date.setText(mEpisodeDate);

        final AudioButtonManager audioButtonManager = new AudioButtonManager(view);
        boolean alreadyPlaying = false;

        if (!activity.streamIsBound()) {
            // TODO: Handle error for no stream manager available.
        } else {
            StreamManager.EpisodeStream currentPlayer = activity.streamManager.currentEpisodePlayer;
            if (currentPlayer != null && currentPlayer.audioUrl.equals(mAudioUrl) && currentPlayer.isPlaying()) {
                mPlayer = currentPlayer;
                alreadyPlaying = true;
                audioButtonManager.togglePlayingForPause();

                // Audio should be released when next one starts, based on Audio Focus rules.
            }

            if (mPlayer == null) {
                mPlayer = new StreamManager.EpisodeStream(mAudioUrl, activity, new EpisodeAudioCompletionCallback());
            }

            audioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlayer.play();
                    logEpisodeStreamEvent(activity, AnalyticsManager.EVENT_ON_DEMAND_BEGAN);
                }
            });

            audioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlayer.pause();
                    // The key discrepancy is for parity with the iOS app.
                    logEpisodeStreamEvent(activity, AnalyticsManager.EVENT_ON_DEMAND_PAUSED);
                }
            });

            mPlayer.setOnAudioEventListener(new StreamManager.AudioEventListener() {
                @Override
                public void onLoading() {
                    audioButtonManager.toggleLoading();
                }

                @Override
                public void onPlay() {
                    audioButtonManager.togglePlayingForPause();
                }

                @Override
                public void onPause() {
                    audioButtonManager.togglePaused();
                }

                @Override
                public void onStop() {
                    audioButtonManager.toggleStopped();
                }

                @Override
                public void onProgress(int progress) {
                    progressBar.setProgress(progress / 1000);
                    currentTime.setText(StreamManager.getTimeFormat(progress / 1000));
                }

                @Override
                public void onError() {
                    audioButtonManager.toggleError(R.string.audio_error);
                }
            });

            if (!alreadyPlaying) {
                audioButtonManager.toggleStopped();
                // Start the episode right away unless it's already playing.
                audioButtonManager.clickPlay();
            }
        }

        return view;
    }

    private void logEpisodeStreamEvent(MainActivity activity, String key) {
        JSONObject params = new JSONObject();

        try {
            params.put("programPublishedAt", mEpisodeDate);
            params.put("programTitle", mProgram.title);
            params.put("episodeTitle", mEpisodeTitle);
            params.put("programLengthInSeconds", mAudioDuration); // Already seconds

            if (key.equals(AnalyticsManager.EVENT_ON_DEMAND_PAUSED) && activity != null) {
                params.put("playedDurationInSeconds", mPlayer.getCurrentPosition() / 1000);
            }
        } catch (JSONException e) {
            // Nothing to do.
        }

        AnalyticsManager.instance.logEvent(key, params);
    }


    public class EpisodeAudioCompletionCallback {
        public void onCompletion() {
            logEpisodeStreamEvent(null, AnalyticsManager.EVENT_ON_DEMAND_COMPLETED);
            // TODO Load the next episode
        }
    }
}
