package org.kpcc.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public final static String TAG = "kpcc.MainActivity";
    private static final String DONATE_URL = "https://scprcontribute.publicradio.org/contribute.php";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private StreamManager mStreamManager;
    private boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            StreamManager.LocalBinder binder = (StreamManager.LocalBinder) service;
            mStreamManager = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (mBound) {
                mStreamManager.release();
            }

            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Navigation.getInstance().addItem(0, R.string.kpcc_live, R.drawable.menu_antenna,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, LiveFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(1, R.string.programs, R.drawable.menu_microphone,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, ProgramsFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(2, R.string.headlines, R.drawable.menu_glasses,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, HeadlinesFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(3, R.string.donate, R.drawable.menu_heart_plus,
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

        Navigation.getInstance().addItem(4, R.string.feedback, R.drawable.menu_feedback,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, FeedbackFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(5, R.string.settings, R.drawable.menu_settings,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, SettingsFragment.newInstance())
                                .commit();
                    }
                }
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Navigation.NavigationItem item = Navigation.getInstance().getItem(position);

        // update the main content by replacing fragments
        item.performCallback(this);
    }

    public StreamManager getStreamManager() {
        return mStreamManager;
    }

    public boolean streamIsBound() {
        return mBound;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, StreamManager.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        AnalyticsManager.getInstance().flush();
        super.onDestroy();
    }
}
