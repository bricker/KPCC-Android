package org.kpcc.android;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeFragment extends Fragment {
    public final static String STACK_TAG = "HomeFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.kpcc_live);

        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 1:
                        return new LiveFragment();
                    default:
                        return new ScheduleFragment();
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
                        return getActivity().getResources().getString(R.string.kpcc_live);
                    default:
                        return getActivity().getResources().getString(R.string.schedule);
                }
            }
        };

        ViewPager viewPager = (ViewPager)view.findViewById(R.id.pager);
        viewPager.setAdapter(adapter);

        return view;
    }
}
