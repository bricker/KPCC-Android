package org.kpcc.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;

import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends ActionBarActivity {
    private static final String DONATE_URL = "https://scprcontribute.publicradio.org/contribute.php";
    public StreamManager streamManager;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    public boolean streamIsBound = false;
    private ArrayList<OnStreamBindListener> mStreamBindListeners = new ArrayList<>();

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            StreamManager.LocalBinder binder = (StreamManager.LocalBinder) service;
            streamManager = binder.getService();
            streamIsBound = true;

            for (OnStreamBindListener listener : mStreamBindListeners) {
                listener.onBind();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (streamIsBound) {
                streamManager.releaseAllActiveStreams();
            }

            streamIsBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, StreamManager.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Navigation.instance.addItem(0, R.string.kpcc_live, R.drawable.menu_antenna, LiveFragment.STACK_TAG,
                AnalyticsManager.EVENT_MENU_SELECTION_LIVE_STREAM,
                new Navigation.NavigationItemSelectedCallback() {
                    @Override
                    public void perform(FragmentManager fm, boolean addToBackStack) {
                        FragmentTransaction trans = fm.beginTransaction();
                        trans.replace(R.id.container, new LiveFragment(), LiveFragment.STACK_TAG);

                        if (addToBackStack) {
                            trans.addToBackStack(LiveFragment.STACK_TAG);
                        }

                        trans.commit();
                    }
                }
        );

        Navigation.instance.addItem(1, R.string.programs, R.drawable.menu_microphone, ProgramsFragment.STACK_TAG,
                AnalyticsManager.EVENT_MENU_SELECTION_PROGRAMS,
                new Navigation.NavigationItemSelectedCallback() {
                    @Override
                    public void perform(FragmentManager fm, boolean addToBackStack) {
                        FragmentTransaction trans = fm.beginTransaction();
                        trans.replace(R.id.container, new ProgramsFragment(), ProgramsFragment.STACK_TAG);

                        if (addToBackStack) {
                            trans.addToBackStack(ProgramsFragment.STACK_TAG);
                        }

                        trans.commit();
                    }
                }
        );

        Navigation.instance.addItem(2, R.string.headlines, R.drawable.menu_glasses, HeadlinesFragment.STACK_TAG,
                AnalyticsManager.EVENT_MENU_SELECTION_HEADLINES,
                new Navigation.NavigationItemSelectedCallback() {
                    @Override
                    public void perform(FragmentManager fm, boolean addToBackStack) {
                        FragmentTransaction trans = fm.beginTransaction();
                        trans.replace(R.id.container, new HeadlinesFragment(), HeadlinesFragment.STACK_TAG);

                        if (addToBackStack) {
                            trans.addToBackStack(HeadlinesFragment.STACK_TAG);
                        }

                        trans.commit();
                    }
                }
        );

        Navigation.instance.addItem(3, R.string.donate, R.drawable.menu_heart_plus, null,
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

        Navigation.instance.addItem(4, R.string.feedback, R.drawable.menu_feedback, FeedbackFragment.STACK_TAG,
                AnalyticsManager.EVENT_MENU_SELECTION_FEEDBACK,
                new Navigation.NavigationItemSelectedCallback() {
                    @Override
                    public void perform(FragmentManager fm, boolean addToBackStack) {
                        FragmentTransaction trans = fm.beginTransaction();
                        trans.replace(R.id.container, new FeedbackFragment(), FeedbackFragment.STACK_TAG);

                        if (addToBackStack) {
                            trans.addToBackStack(FeedbackFragment.STACK_TAG);
                        }

                        trans.commit();
                    }
                }
        );

        Navigation.instance.addItem(5, R.string.settings, R.drawable.menu_settings, SettingsFragment.STACK_TAG,
                AnalyticsManager.EVENT_MENU_SELECTION_SETTINGS,
                new Navigation.NavigationItemSelectedCallback() {
                    @Override
                    public void perform(FragmentManager fm, boolean addToBackStack) {
                        FragmentTransaction trans = fm.beginTransaction();
                        trans.replace(R.id.container, new SettingsFragment(), SettingsFragment.STACK_TAG);

                        if (addToBackStack) {
                            trans.addToBackStack(SettingsFragment.STACK_TAG);
                        }

                        trans.commit();
                    }
                }
        );

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

    public NavigationDrawerFragment getNavigationDrawerFragment() {
        return mNavigationDrawerFragment;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AnalyticsManager.instance.flush();

        if (streamIsBound) {
            unbindService(mConnection);
            streamIsBound = false;
        }
    }

    void addOnStreamBindListener(OnStreamBindListener listener) {
        mStreamBindListeners.add(listener);
    }

    abstract static class OnStreamBindListener {
        public abstract void onBind();
    }
}
