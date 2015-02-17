package org.kpcc.android;

import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    public AnalyticsManager mAnalyticsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAnalyticsManager = AnalyticsManager.getInstance();

        Navigation.getInstance().addItem(0, R.string.kpcc_live,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, LiveFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(1, R.string.programs,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, ProgramsFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(2, R.string.headlines,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, HeadlinesFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(3, R.string.donate,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, DonateFragment.newInstance())
                                .commit();

                    }
                }
        );

        Navigation.getInstance().addItem(4, R.string.feedback,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform(FragmentManager fm) {
                        fm.beginTransaction()
                                .replace(R.id.container, FeedbackFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(5, R.string.settings,
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

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Navigation.NavigationItem item = Navigation.getInstance().getItem(position);

        // update the main content by replacing fragments
        item.performCallback(this);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(item.getTitleId());
        }
    }


    @Override
    protected void onDestroy() {
        mAnalyticsManager.flush();
        super.onDestroy();
    }
}
