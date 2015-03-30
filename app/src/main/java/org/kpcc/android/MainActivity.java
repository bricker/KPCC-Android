package org.kpcc.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends ActionBarActivity {
    private static final String DONATE_URL = "https://scprcontribute.publicradio.org/contribute.php";
    public StreamManager streamManager;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private boolean mBound = false;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            StreamManager.LocalBinder binder = (StreamManager.LocalBinder) service;
            streamManager = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (mBound) {
                streamManager.releaseAllActiveStreams();
            }

            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, StreamManager.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Navigation.instance.addItem(0, R.string.kpcc_live, R.drawable.menu_antenna,
                AnalyticsManager.EVENT_MENU_SELECTION_LIVE_STREAM,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        FragmentTransaction trans = fm.beginTransaction();
                        clearFragmentBackStack(fm);
                        trans.replace(R.id.container, new LiveFragment()).commit();
                    }
                }
        );

        Navigation.instance.addItem(1, R.string.programs, R.drawable.menu_microphone,
                AnalyticsManager.EVENT_MENU_SELECTION_PROGRAMS,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        FragmentTransaction trans = fm.beginTransaction();
                        clearFragmentBackStack(fm);
                        trans.replace(R.id.container, new ProgramsFragment()).commit();
                    }
                }
        );

        Navigation.instance.addItem(2, R.string.headlines, R.drawable.menu_glasses,
                AnalyticsManager.EVENT_MENU_SELECTION_HEADLINES,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        FragmentTransaction trans = fm.beginTransaction();
                        clearFragmentBackStack(fm);
                        trans.replace(R.id.container, new HeadlinesFragment()).commit();
                    }
                }
        );

        Navigation.instance.addItem(3, R.string.donate, R.drawable.menu_heart_plus,
                AnalyticsManager.EVENT_MENU_SELECTION_DONATE,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        Uri uri = Uri.parse(DONATE_URL);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    }
                }
        );

        Navigation.instance.addItem(4, R.string.feedback, R.drawable.menu_feedback,
                AnalyticsManager.EVENT_MENU_SELECTION_FEEDBACK,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        FragmentTransaction trans = fm.beginTransaction();
                        clearFragmentBackStack(fm);
                        trans.replace(R.id.container, new FeedbackFragment()).commit();
                    }
                }
        );

        Navigation.instance.addItem(5, R.string.settings, R.drawable.menu_settings,
                AnalyticsManager.EVENT_MENU_SELECTION_SETTINGS,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        FragmentTransaction trans = fm.beginTransaction();
                        clearFragmentBackStack(fm);
                        trans.replace(R.id.container, new SettingsFragment()).commit();
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

    // This gets called when we click a menu item. If a menu item is clicked, the internal
    // back stack (for program and episode lists) should be cleared out.
    private void clearFragmentBackStack(FragmentManager fm) {
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public boolean streamIsBound() {
        return mBound;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AnalyticsManager.instance.flush();

        if (mBound) {
            streamManager.releaseAllActiveStreams();
            unbindService(mConnection);
            mBound = false;
        }
    }
}
