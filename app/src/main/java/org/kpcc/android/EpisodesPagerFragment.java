package org.kpcc.android;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.json.JSONException;
import org.kpcc.api.Episode;

import java.util.ArrayList;

public class EpisodesPagerFragment extends Fragment {
    public final static String STACK_TAG = "EpisodesPagerFragment";
    private final static String ARG_EPISODE = "args_episodes";
    private final static String ARG_POSITION = "args_position";
    private final static String ARG_PROGRAM_SLUG = "args_program_slug";

    public ViewPager pager;
    private int currentPosition;
    private EpisodePagerAdapter mAdapter;
    private ArrayList<String> mEpisodes;
    private String mProgramSlug;
    private Integer mPrevPos;

    public static EpisodesPagerFragment newInstance(ArrayList<Episode> episodes, int position, String programSlug) {
        Bundle args = new Bundle();
        ArrayList<String> episodesJson = new ArrayList<>();

        for (Episode episode : episodes) {
            try {
                episodesJson.add(episode.toJSON().toString());
            } catch (JSONException e) {
                // implicit continue
                // This episode will not be included.
            }
        }

        args.putSerializable(ARG_EPISODE, episodesJson);
        args.putInt(ARG_POSITION, position);
        args.putString(ARG_PROGRAM_SLUG, programSlug);

        EpisodesPagerFragment fragment = new EpisodesPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            // This is coming from our args that we're setting above
            // The compiler warns about unchecked cast but it's safe.
            mEpisodes = (ArrayList<String>) args.getSerializable(ARG_EPISODE);
            mProgramSlug = args.getString(ARG_PROGRAM_SLUG);
            currentPosition = args.getInt(ARG_POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_episodes_pager, container, false);

        ImageView background = (ImageView) view.findViewById(R.id.background);
        NetworkImageManager.getInstance().setBitmap(getActivity(), background, mProgramSlug);

        mAdapter = new EpisodePagerAdapter(getChildFragmentManager());
        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(mAdapter);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (mPrevPos == null) {
                    mPrevPos = position;
                    return;
                }

                if (position == mPrevPos) {
                    // We haven't changed positions
                    return;
                }

                mPrevPos = position;
            }

            @Override
            public void onPageSelected(int position) {
                EpisodeFragment fragment = mAdapter.getRegisteredFragment(position);

                if (fragment != null) {
                    fragment.pagerVisible.set(true);
                    fragment.onResume();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        pager.setCurrentItem(currentPosition);

        return view;
    }

    private class EpisodePagerAdapter extends FragmentPagerAdapter {
        private final SparseArray<String> registeredFragments = new SparseArray<>();

        public EpisodePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            currentPosition = position;
            String episodeJson = mEpisodes.get(position);
            return EpisodeFragment.newInstance(mProgramSlug, episodeJson);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            EpisodeFragment fragment = (EpisodeFragment) object;

            // Inform the fragment that it's now visible and call onResume() again to reset the
            // buttons and play the audio.
            // There is probably a better way to do this but I couldn't find it.
            fragment.pagerVisible.set(true);
        }

        @Override
        public int getCount() {
            return mEpisodes.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            // We can use getTag here because we're use FragmentPagerAdapter.
            // FragmentStatePagerAdapter doesn't set tags on its fragments.
            registeredFragments.put(position, fragment.getTag());
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public EpisodeFragment getRegisteredFragment(int position) {
            String tag = registeredFragments.get(position);
            return (EpisodeFragment) getChildFragmentManager().findFragmentByTag(tag);
        }
    }
}
