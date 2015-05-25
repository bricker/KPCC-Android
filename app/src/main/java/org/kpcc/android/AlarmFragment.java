package org.kpcc.android;


import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

public class AlarmFragment extends Fragment {
    public final static String STACK_TAG = "AlarmFragment";

    public AlarmFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.wake_sleep);

        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 1:
                        return new WakeFragment();
                    default:
                        return new SleepFragment();
                }
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 1:
                        return getActivity().getResources().getString(R.string.alarm_clock);
                    default:
                        return getActivity().getResources().getString(R.string.sleep_timer);
                }
            }
        };

        ViewPager viewPager = (ViewPager)view.findViewById(R.id.pager);
        viewPager.setAdapter(adapter);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip)view.findViewById(R.id.tabs);
        tabs.setViewPager(viewPager);

        // For some reason setting this in the view isn't working.
        Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(), "fonts/FreigSanProLig.otf");
        tabs.setTypeface(typeFace, 0);

        return view;
    }
}
