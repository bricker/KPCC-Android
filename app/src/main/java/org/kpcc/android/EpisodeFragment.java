package org.kpcc.android;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kpcc.api.Program;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EpisodeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EpisodeFragment extends Fragment {
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

    public EpisodeFragment() {
        // Required empty public constructor
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
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_episode, container, false);

        TextView title = (TextView) v.findViewById(R.id.episode_title);
        TextView date = (TextView) v.findViewById(R.id.air_date);
        TextView audioUrl = (TextView) v.findViewById(R.id.audio_url);

        title.setText(mEpisodeTitle);
        date.setText(mEpisodeDate);
        audioUrl.setText(mAudioUrl);

        return v;
    }
}
