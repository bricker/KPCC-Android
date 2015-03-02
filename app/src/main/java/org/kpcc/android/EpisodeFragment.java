package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

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
    private static final String ARG_AUDIO_FILESIZE = "audioFilesize";
    private static final String ARG_AUDIO_DURATION = "audioDuration";

    private Program mProgram;
    private String mEpisodeTitle;
    private String mEpisodeDate;
    private String mAudioUrl;
    private int mAudioFilesize;
    private int mAudioDuration;
    private Button mPlayButton;
    private Button mPauseButton;
    private NetworkImageView mBackground;

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
            String audioUrl, int audioFilesize, int audioDuration) {
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
        args.putInt(ARG_AUDIO_FILESIZE, audioFilesize);
        args.putInt(ARG_AUDIO_DURATION, audioDuration);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            mProgram = ProgramsManager.getInstance().find(args.getString(ARG_PROGRAM_SLUG));
            mEpisodeTitle = args.getString(ARG_EPISODE_TITLE);
            mEpisodeDate = args.getString(ARG_EPISODE_DATE);
            mAudioUrl = args.getString(ARG_AUDIO_URL);
            mAudioFilesize = args.getInt(ARG_AUDIO_FILESIZE);
            mAudioDuration = args.getInt(ARG_AUDIO_DURATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final MainActivity activity = (MainActivity) getActivity();

        activity.setTitle(R.string.programs);
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.programs);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_episode, container, false);

        TextView title = (TextView) view.findViewById(R.id.episode_title);
        TextView date = (TextView) view.findViewById(R.id.air_date);

        mPlayButton = (Button) view.findViewById((R.id.play_button));
        mPauseButton = (Button) view.findViewById((R.id.pause_button));
        mBackground = (NetworkImageView) view.findViewById(R.id.background);
        RequestManager.getInstance().setBackgroundImage(mBackground, mProgram.getSlug());

        title.setText(mEpisodeTitle);
        date.setText(mEpisodeDate);

        final AudioButtonManager audioButtonManager = new AudioButtonManager(activity, view);

        StreamManager streamManager = activity.getStreamManager();
        if (streamManager != null && streamManager.isPlaying(mAudioUrl)) {
            audioButtonManager.togglePlayingForPause();
        } else {
            audioButtonManager.toggleStopped();
        }

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getStreamManager().playEpisode(mAudioUrl, activity, audioButtonManager);
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getStreamManager().pause(audioButtonManager);
            }
        });

        // Start the episode right away.
        mPlayButton.callOnClick();

        return view;
    }
}
