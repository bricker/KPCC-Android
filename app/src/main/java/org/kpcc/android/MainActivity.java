package org.kpcc.android;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends AppCompatActivity {
    private static final String DONATE_URL = AppConfiguration.getInstance().getConfig("donate.url");
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StreamManager.ConnectivityManager.setupInstance(this);
        StreamManager.ConnectivityManager.getInstance().bindStreamService();

        if (Navigation.getInstance().navigationItems.isEmpty()) {
            Navigation.getInstance().addItem(R.string.kpcc_live, R.drawable.menu_antenna, LiveFragment.STACK_TAG,
                    AnalyticsManager.EVENT_MENU_SELECTION_LIVE_STREAM,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            FragmentTransaction trans = fm.beginTransaction();
                            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            trans.replace(R.id.container, new LiveFragment(), LiveFragment.STACK_TAG);

                            if (addToBackStack) {
                                trans.addToBackStack(LiveFragment.STACK_TAG);
                            }

                            trans.commit();
                        }
                    }
            );

            Navigation.getInstance().addItem(R.string.programs, R.drawable.menu_microphone, ProgramsFragment.STACK_TAG,
                    AnalyticsManager.EVENT_MENU_SELECTION_PROGRAMS,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            FragmentTransaction trans = fm.beginTransaction();
                            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            trans.replace(R.id.container, new ProgramsFragment(), ProgramsFragment.STACK_TAG);

                            if (addToBackStack) {
                                trans.addToBackStack(ProgramsFragment.STACK_TAG);
                            }

                            trans.commit();
                        }
                    }
            );

            Navigation.getInstance().addItem(R.string.headlines, R.drawable.menu_glasses, HeadlinesFragment.STACK_TAG,
                    AnalyticsManager.EVENT_MENU_SELECTION_HEADLINES,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            FragmentTransaction trans = fm.beginTransaction();
                            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            trans.replace(R.id.container, new HeadlinesFragment(), HeadlinesFragment.STACK_TAG);

                            if (addToBackStack) {
                                trans.addToBackStack(HeadlinesFragment.STACK_TAG);
                            }

                            trans.commit();
                        }
                    }
            );

            Navigation.getInstance().addItem(R.string.wake_sleep, R.drawable.menu_clock, AlarmFragment.STACK_TAG,
                    AnalyticsManager.EVENT_MENU_SELECTION_WAKE_SLEEP,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            FragmentTransaction trans = fm.beginTransaction();
                            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            trans.replace(R.id.container, new AlarmFragment(), AlarmFragment.STACK_TAG);

                            if (addToBackStack) {
                                trans.addToBackStack(AlarmFragment.STACK_TAG);
                            }

                            trans.commit();
                        }
                    }
            );

            Navigation.getInstance().addItem(R.string.donate, R.drawable.menu_heart_plus, null,
                    AnalyticsManager.EVENT_MENU_SELECTION_DONATE,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            Uri uri = Uri.parse(DONATE_URL);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }
                    }
            );

            Navigation.getInstance().addItem(R.string.feedback, R.drawable.menu_feedback, FeedbackFragment.STACK_TAG,
                    AnalyticsManager.EVENT_MENU_SELECTION_FEEDBACK,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            FragmentTransaction trans = fm.beginTransaction();
                            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            trans.replace(R.id.container, new FeedbackFragment(), FeedbackFragment.STACK_TAG);

                            if (addToBackStack) {
                                trans.addToBackStack(FeedbackFragment.STACK_TAG);
                            }

                            trans.commit();
                        }
                    }
            );

            Navigation.getInstance().addItem(R.string.settings, R.drawable.menu_settings, SettingsFragment.STACK_TAG,
                    AnalyticsManager.EVENT_MENU_SELECTION_SETTINGS,
                    new Navigation.NavigationItemSelectedCallback() {
                        @Override
                        public void perform(FragmentManager fm, boolean addToBackStack) {
                            FragmentTransaction trans = fm.beginTransaction();
                            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            trans.replace(R.id.container, new SettingsFragment(), SettingsFragment.STACK_TAG);

                            if (addToBackStack) {
                                trans.addToBackStack(SettingsFragment.STACK_TAG);
                            }

                            trans.commit();
                        }
                    }
            );
        }

        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowTitleEnabled(true);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void setTitle(CharSequence title) {
        // I don't know how else to set the font face in the activity bar :(
        SpannableString s = new SpannableString(title);
        s.setSpan(new TypefaceSpan("fonts/FreigSanProMed.otf"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        super.setTitle(s);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StreamManager.ConnectivityManager.getInstance().unbindStreamService();
    }

    NavigationDrawerFragment getNavigationDrawerFragment() {
        return mNavigationDrawerFragment;
    }
}
